package org.codelibs.fess.suggest.index;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.concurrent.Deferred;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.ElevateWord;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggestIndexException;
import org.codelibs.fess.suggest.index.contents.ContentsParser;
import org.codelibs.fess.suggest.index.contents.DefaultContentsParser;
import org.codelibs.fess.suggest.index.contents.document.DocumentReader;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLog;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLogReader;
import org.codelibs.fess.suggest.index.writer.SuggestIndexWriter;
import org.codelibs.fess.suggest.index.writer.SuggestWriter;
import org.codelibs.fess.suggest.index.writer.SuggestWriterResult;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.elasticsearch.client.Client;

public class SuggestIndexer {
    protected final Client client;
    protected String index;
    protected String type;
    protected SuggestSettings settings;

    protected String[] supportedFields;
    protected String tagFieldName;
    protected String roleFieldName;
    protected String[] badWords;

    protected ReadingConverter readingConverter;
    protected Normalizer normalizer;
    protected SuggestAnalyzer analyzer;

    protected ContentsParser contentsParser;
    protected SuggestWriter suggestWriter;

    protected ExecutorService threadPool;

    public SuggestIndexer(final Client client, final String index, final String type, final ReadingConverter readingConverter,
            final Normalizer normalizer, final SuggestAnalyzer analyzer, final SuggestSettings settings, final ExecutorService threadPool) {
        this.client = client;
        this.index = index;
        this.type = type;

        this.supportedFields = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS);
        this.badWords = settings.badword().get();
        this.tagFieldName = settings.getAsString(SuggestSettings.DefaultKeys.TAG_FIELD_NAME, StringUtil.EMPTY);
        this.roleFieldName = settings.getAsString(SuggestSettings.DefaultKeys.ROLE_FIELD_NAME, StringUtil.EMPTY);
        this.readingConverter = readingConverter;
        this.normalizer = normalizer;
        this.analyzer = analyzer;
        this.settings = settings;

        this.contentsParser = new DefaultContentsParser();
        this.suggestWriter = new SuggestIndexWriter();

        this.threadPool = threadPool;
    }

    //TODO return result
    public SuggestIndexResponse index(final SuggestItem item) {
        return index(new SuggestItem[] { item });
    }

    //TODO return result
    public SuggestIndexResponse index(final SuggestItem[] items) {
        // TODO parallel?
        final SuggestItem[] array = Stream.of(items).filter(item -> !item.isNgWord(badWords)).toArray(n -> new SuggestItem[n]);

        final long start = System.currentTimeMillis();
        final SuggestWriterResult result = suggestWriter.write(client, settings, index, type, array);
        return new SuggestIndexResponse(items.length, items.length, result.getFailures(), System.currentTimeMillis() - start);
    }

    public SuggestDeleteResponse delete(final String id) {
        final long start = System.currentTimeMillis();
        final SuggestWriterResult result = suggestWriter.delete(client, settings, index, type, id);
        return new SuggestDeleteResponse(result.getFailures(), System.currentTimeMillis() - start);
    }

    public SuggestDeleteResponse deleteByQuery(final String queryString) {
        final long start = System.currentTimeMillis();
        final SuggestWriterResult result = suggestWriter.deleteByQuery(client, settings, index, type, queryString);
        return new SuggestDeleteResponse(result.getFailures(), System.currentTimeMillis() - start);
    }

    public SuggestIndexResponse indexFromQueryLog(final QueryLog queryLog) {
        return indexFromQueryLog(new QueryLog[] { queryLog });
    }

    public SuggestIndexResponse indexFromQueryLog(final QueryLog[] queryLogs) {
        try {
            final long start = System.currentTimeMillis();
            final SuggestItem[] array =
                    Stream.of(queryLogs)
                            .parallel()
                            .flatMap(
                                    queryLog -> contentsParser.parseQueryLog(queryLog, supportedFields, tagFieldName, roleFieldName,
                                            readingConverter, normalizer).stream()).toArray(n -> new SuggestItem[n]);
            final SuggestIndexResponse response = index(array);
            return new SuggestIndexResponse(array.length, queryLogs.length, response.getErrors(), System.currentTimeMillis() - start);
        } catch (final Exception e) {
            throw new SuggestIndexException("Failed to index from query_string.", e);
        }
    }

    // TODO replace queryLogReader with lambda reader
    public Deferred<SuggestIndexResponse>.Promise indexFromQueryLog(final QueryLogReader queryLogReader, final int docPerReq,
            final long requestInterval) {
        final Deferred<SuggestIndexResponse> deferred = new Deferred<>();
        threadPool.execute(() -> {
            final long start = System.currentTimeMillis();
            int numberOfSuggestDocs = 0;
            int numberOfInputDocs = 0;
            final List<Throwable> errors = new ArrayList<>();

            final List<QueryLog> queryLogs = new ArrayList<>(docPerReq);
            try {
                QueryLog queryLog = queryLogReader.read();
                while (queryLog != null) {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    queryLogs.add(queryLog);
                    queryLog = queryLogReader.read();
                    if ((queryLog == null && !queryLogs.isEmpty()) || queryLogs.size() >= docPerReq) {
                        final SuggestIndexResponse res = indexFromQueryLog(queryLogs.toArray(new QueryLog[queryLogs.size()]));
                        errors.addAll(res.getErrors());
                        numberOfSuggestDocs += res.getNumberOfSuggestDocs();
                        numberOfInputDocs += res.getNumberOfInputDocs();
                        queryLogs.clear();

                        Thread.sleep(requestInterval);
                    }
                }
                deferred.resolve(new SuggestIndexResponse(numberOfSuggestDocs, numberOfInputDocs, errors, System.currentTimeMillis()
                        - start));
            } catch (final Throwable t) {
                deferred.reject(t);
            } finally {
                queryLogReader.close();
            }
        });
        return deferred.promise();
    }

    public SuggestIndexResponse indexFromDocument(final Map<String, Object>[] documents) {
        final long start = System.currentTimeMillis();
        try {
            final SuggestItem[] array =
                    Stream.of(documents)
                            .parallel()
                            .flatMap(
                                    document -> contentsParser.parseDocument(document, supportedFields, tagFieldName, roleFieldName,
                                            readingConverter, normalizer, analyzer).stream()).toArray(n -> new SuggestItem[n]);
            final SuggestIndexResponse response = index(array);
            return new SuggestIndexResponse(array.length, documents.length, response.getErrors(), System.currentTimeMillis() - start);
        } catch (final Exception e) {
            throw new SuggestIndexException("Failed to index from document", e);
        }
    }

    // TODO replace documentReader with lambda reader
    public Deferred<SuggestIndexResponse>.Promise indexFromDocument(final DocumentReader documentReader, final int docPerReq,
            final long requestInterval) {
        final Deferred<SuggestIndexResponse> deferred = new Deferred<>();
        threadPool.execute(() -> {
            final long start = System.currentTimeMillis();
            int numberOfSuggestDocs = 0;
            int numberOfInputDocs = 0;

            final List<Throwable> errors = new ArrayList<>();
            final List<Map<String, Object>> docs = new ArrayList<>(docPerReq);
            try {
                Map<String, Object> doc = documentReader.read();
                while (doc != null) {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    docs.add(doc);
                    doc = documentReader.read();
                    if (doc == null || docs.size() >= docPerReq) {
                        final SuggestIndexResponse res = indexFromDocument(docs.toArray(new Map[docs.size()]));
                        errors.addAll(res.getErrors());
                        numberOfSuggestDocs += res.getNumberOfSuggestDocs();
                        numberOfInputDocs += res.getNumberOfInputDocs();
                        client.admin().indices().prepareRefresh(index).execute().actionGet(SuggestConstants.ACTION_TIMEOUT);
                        docs.clear();

                        Thread.sleep(requestInterval);
                    }
                }
                documentReader.close();

                deferred.resolve(new SuggestIndexResponse(numberOfSuggestDocs, numberOfInputDocs, errors, System.currentTimeMillis()
                        - start));
            } catch (final Throwable t) {
                deferred.reject(t);
            } finally {
                documentReader.close();
            }
        });
        return deferred.promise();
    }

    public SuggestIndexResponse indexFromSearchWord(final String searchWord, final String[] fields, final String[] tags,
            final String[] roles, final int num) {
        final long start = System.currentTimeMillis();
        final StringBuilder buf = new StringBuilder(searchWord.length());
        char prev = 0;
        for (final char c : searchWord.toCharArray()) {
            if (!Character.isWhitespace(c)) {
                buf.append(c);
            } else if (!Character.isWhitespace(prev)) {
                buf.append(' ');
            }
            prev = c;
        }
        final String[] words = buf.toString().trim().split(" ");
        try {
            final SuggestItem item = contentsParser.parseSearchWords(words, null, fields, tags, roles, num, readingConverter, normalizer);
            final SuggestIndexResponse response = index(item);
            return new SuggestIndexResponse(1, 1, response.getErrors(), System.currentTimeMillis() - start);
        } catch (final Exception e) {
            throw new SuggestIndexException("Failed to index from document", e);
        }
    }

    public SuggestDeleteResponse addBadWord(final String badWord) {
        settings.badword().add(badWord);
        badWords = settings.badword().get();
        return deleteByQuery(FieldNames.TEXT + ":*\"" + badWord + "\"*");
    }

    public void deleteBadWord(final String badWord) {
        settings.badword().delete(badWord);
    }

    public SuggestIndexResponse addElevateWord(final ElevateWord elevateWord) {
        settings.elevateWord().add(elevateWord);
        return index(elevateWord.toSuggestItem());
    }

    public SuggestDeleteResponse deleteElevateWord(final String elevateWord) {
        settings.elevateWord().delete(elevateWord);
        return delete(SuggestUtil.createSuggestTextId(elevateWord));
    }

    public SuggestIndexResponse restoreElevateWord() {
        final long start = System.currentTimeMillis();
        int numberOfSuggestDocs = 0;
        int numberOfInputDocs = 0;

        final ElevateWord[] elevateWords = settings.elevateWord().get();
        final List<Throwable> errors = new ArrayList<>(elevateWords.length);
        for (final ElevateWord elevateWord : elevateWords) {
            final SuggestIndexResponse res = addElevateWord(elevateWord);
            numberOfSuggestDocs += res.getNumberOfSuggestDocs();
            numberOfInputDocs += res.getNumberOfInputDocs();
            errors.addAll(res.getErrors());
        }
        return new SuggestIndexResponse(numberOfSuggestDocs, numberOfInputDocs, errors, System.currentTimeMillis() - start);
    }

    public SuggestDeleteResponse deleteOldWords(final LocalDateTime threshold) {
        final long start = System.currentTimeMillis();
        final String query =
                FieldNames.TIMESTAMP + ":[* TO " + threshold.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + "] NOT "
                        + FieldNames.KINDS + ':' + SuggestItem.Kind.USER;
        suggestWriter.deleteByQuery(client, settings, index, type, query);
        return new SuggestDeleteResponse(null, System.currentTimeMillis() - start);
    }

    public SuggestIndexer setIndexName(final String index) {
        this.index = index;
        return this;
    }

    public SuggestIndexer setTypeName(final String type) {
        this.type = type;
        return this;
    }

    public SuggestIndexer setSupportedFields(final String[] supportedFields) {
        this.supportedFields = supportedFields;
        return this;
    }

    public SuggestIndexer setTagFieldName(final String tagFieldName) {
        this.tagFieldName = tagFieldName;
        return this;
    }

    public SuggestIndexer setRoleFieldName(final String roleFieldName) {
        this.roleFieldName = roleFieldName;
        return this;
    }

    public SuggestIndexer setReadingConverter(final ReadingConverter readingConverter) {
        this.readingConverter = readingConverter;
        return this;
    }

    public SuggestIndexer setNormalizer(final Normalizer normalizer) {
        this.normalizer = normalizer;
        return this;
    }

    public SuggestIndexer setAnalyzer(final SuggestAnalyzer analyzer) {
        this.analyzer = analyzer;
        return this;
    }

    public SuggestIndexer setContentsParser(final ContentsParser contentsParser) {
        this.contentsParser = contentsParser;
        return this;
    }

    public SuggestIndexer setSuggestWriter(final SuggestWriter suggestWriter) {
        this.suggestWriter = suggestWriter;
        return this;
    }

}
