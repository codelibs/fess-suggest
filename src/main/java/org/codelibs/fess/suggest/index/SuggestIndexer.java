package org.codelibs.fess.suggest.index;

import org.apache.lucene.analysis.Analyzer;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.ElevateWord;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.index.contents.ContentsParser;
import org.codelibs.fess.suggest.index.contents.DefaultContentsParser;
import org.codelibs.fess.suggest.index.contents.document.DocumentReader;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLog;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLogReader;
import org.codelibs.fess.suggest.index.writer.SuggestIndexWriter;
import org.codelibs.fess.suggest.index.writer.SuggestWriter;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.elasticsearch.client.Client;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class SuggestIndexer {
    protected final Client client;
    protected String index;
    protected String type;
    protected SuggestSettings settings;

    protected String[] supportedFields;
    protected String tagFieldName;
    protected String roleFieldName;
    protected String[] ngWords;

    protected ReadingConverter readingConverter;
    protected Normalizer normalizer;
    protected Analyzer analyzer;

    protected ContentsParser contentsParser;
    protected SuggestWriter suggestWriter;

    protected Executor threadPool;

    public SuggestIndexer(final Client client, final String index, final String type, final ReadingConverter readingConverter,
            final Normalizer normalizer, final Analyzer analyzer, final SuggestSettings settings, final Executor threadPool) {
        this.client = client;
        this.index = index;
        this.type = type;

        this.supportedFields = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS);
        this.ngWords = settings.ngword().get();
        this.tagFieldName = settings.getAsString(SuggestSettings.DefaultKeys.TAG_FIELD_NAME, "");
        this.roleFieldName = settings.getAsString(SuggestSettings.DefaultKeys.ROLE_FIELD_NAME, "");
        this.readingConverter = readingConverter;
        this.normalizer = normalizer;
        this.analyzer = analyzer;
        this.settings = settings;

        this.contentsParser = new DefaultContentsParser();
        this.suggestWriter = new SuggestIndexWriter();

        this.threadPool = threadPool;
    }

    //TODO return result
    public void index(final SuggestItem item) {
        index(new SuggestItem[] { item });
    }

    //TODO return result
    public void index(final SuggestItem[] items) {
        SuggestItem[] array = new SuggestItem[items.length];
        int size = 0;
        for (SuggestItem item : items) {
            if (!item.isNgWord(ngWords)) {
                array[size++] = item;
            }
        }
        SuggestItem[] newSizeArray = Arrays.copyOf(array, size);
        suggestWriter.write(client, settings, index, type, newSizeArray);
    }

    public void delete(final String id) {
        suggestWriter.delete(client, settings, index, type, id);
    }

    public void deleteByQuery(final String queryString) {
        suggestWriter.deleteByQuery(client, settings, index, type, queryString);
    }

    public void indexFromQueryLog(final QueryLog queryLog) {
        indexFromQueryLog(new QueryLog[] { queryLog });
    }

    public void indexFromQueryLog(final QueryLog[] queryLogs) {
        try {
            final List<SuggestItem> items = new ArrayList<>(queryLogs.length * supportedFields.length);
            for (QueryLog queryLog : queryLogs) {
                items.addAll(contentsParser.parseQueryLog(queryLog, supportedFields, tagFieldName, roleFieldName, readingConverter,
                        normalizer));
            }
            index(items.toArray(new SuggestItem[items.size()]));
        } catch (Exception e) {
            throw new SuggesterException("Failed to index from query_string.", e);
        }
    }

    public IndexingStatus indexFromQueryLog(final QueryLogReader queryLogReader, boolean async) {
        final IndexingStatus indexingStatus = new IndexingStatus();
        indexingStatus.running.set(true);
        indexingStatus.done.set(false);

        Runnable r = () -> {
            int maxNum = 1000;

            List<QueryLog> queryLogs = new ArrayList<>(maxNum);
            QueryLog queryLog = queryLogReader.read();
            while (queryLog != null) {
                queryLogs.add(queryLog);
                queryLog = queryLogReader.read();
                if ((queryLog == null && !queryLogs.isEmpty()) || queryLogs.size() >= maxNum) {
                    indexFromQueryLog(queryLogs.toArray(new QueryLog[queryLogs.size()]));
                    queryLogs.clear();
                }
            }

            indexingStatus.running.set(false);
            indexingStatus.done.set(true);
        };

        if (async) {
            threadPool.execute(r);
        } else {
            r.run();
        }

        return indexingStatus;
    }

    public void indexFromDocument(final Map<String, Object>[] documents) {
        List<SuggestItem> items = new ArrayList<>(documents.length * supportedFields.length * 100); //TODO
        try {
            for (Map<String, Object> document : documents) {
                items.addAll(contentsParser.parseDocument(document, supportedFields, readingConverter, normalizer, analyzer));
            }
        } catch (Exception e) {
            throw new SuggesterException("Failed to index from document", e);
        }
        index(items.toArray(new SuggestItem[items.size()]));
    }

    public IndexingStatus indexFromDocument(final DocumentReader documentReader, final boolean async) {
        final IndexingStatus indexingStatus = new IndexingStatus();
        indexingStatus.running.set(true);
        indexingStatus.done.set(false);

        @SuppressWarnings("unchecked")
        Runnable cmd = () -> {
            final int maxDocNum = 10;
            List<Map<String, Object>> docs = new ArrayList<>(maxDocNum);
            Map<String, Object> doc;
            while ((doc = documentReader.read()) != null) {
                docs.add(doc);
                if (docs.size() >= maxDocNum) {
                    indexFromDocument(docs.toArray(new Map[docs.size()]));
                    docs.clear();
                }
            }

            indexingStatus.running.set(false);
            indexingStatus.done.set(true);
        };

        if (async) {
            threadPool.execute(cmd);
        } else {
            cmd.run();
        }

        return indexingStatus;
    }

    public void addNgWord(String ngWord) {
        deleteByQuery(FieldNames.TEXT + ":*\"" + ngWord + "\"*");
        settings.ngword().add(ngWord);
        ngWords = settings.ngword().get();
    }

    public void indexElevateWord(ElevateWord elevateWord) {
        settings.elevateWord().add(elevateWord);
        index(elevateWord.toSuggestItem());
    }

    public void deleteElevateWord(String elevateWord) {
        settings.elevateWord().delete(elevateWord);
        delete(SuggestUtil.createSuggestTextId(elevateWord));
    }

    public void restoreElevateWord() {
        ElevateWord[] elevateWords = settings.elevateWord().get();
        for (ElevateWord elevateWord : elevateWords) {
            indexElevateWord(elevateWord);
        }
    }

    public void deleteOldWords(LocalDateTime threshold) {
        suggestWriter.deleteOldWords(client, settings, index, type, threshold);
    }

    public SuggestIndexer setIndex(String index) {
        this.index = index;
        return this;
    }

    public SuggestIndexer setType(String type) {
        this.type = type;
        return this;
    }

    public SuggestIndexer setSupportedFields(String[] supportedFields) {
        this.supportedFields = supportedFields;
        return this;
    }

    public SuggestIndexer setTagFieldName(String tagFieldName) {
        this.tagFieldName = tagFieldName;
        return this;
    }

    public SuggestIndexer setRoleFieldName(String roleFieldName) {
        this.roleFieldName = roleFieldName;
        return this;
    }

    public SuggestIndexer setReadingConverter(ReadingConverter readingConverter) {
        this.readingConverter = readingConverter;
        return this;
    }

    public SuggestIndexer setNormalizer(Normalizer normalizer) {
        this.normalizer = normalizer;
        return this;
    }

    public SuggestIndexer setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
        return this;
    }

    public SuggestIndexer setContentsParser(ContentsParser contentsParser) {
        this.contentsParser = contentsParser;
        return this;
    }

    public SuggestIndexer setSuggestWriter(SuggestWriter suggestWriter) {
        this.suggestWriter = suggestWriter;
        return this;
    }

    public static class IndexingStatus {
        final protected AtomicBoolean running = new AtomicBoolean(false);
        final protected AtomicBoolean done = new AtomicBoolean(false);
        final protected List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        public boolean isStarted() {
            return running.get() || done.get();
        }

        public boolean isDone() {
            return done.get();
        }

        public boolean hasError() {
            return errors.size() > 0;
        }

        public List<Throwable> errors() {
            return errors;
        }
    }
}
