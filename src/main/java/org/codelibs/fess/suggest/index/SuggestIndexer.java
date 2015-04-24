package org.codelibs.fess.suggest.index;

import org.apache.lucene.analysis.Analyzer;
import org.codelibs.fess.suggest.constants.FieldNames;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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

    protected ExecutorService threadPool;

    public SuggestIndexer(final Client client, final String index, final String type, final ReadingConverter readingConverter,
            final Normalizer normalizer, final Analyzer analyzer, final SuggestSettings settings, final ExecutorService threadPool) {
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
    public SuggestIndexResponse index(final SuggestItem item) throws SuggestIndexException {
        return index(new SuggestItem[] { item });
    }

    //TODO return result
    public SuggestIndexResponse index(final SuggestItem[] items) throws SuggestIndexException {
        SuggestItem[] array = new SuggestItem[items.length];
        int size = 0;
        for (SuggestItem item : items) {
            if (!item.isNgWord(ngWords)) {
                array[size++] = item;
            }
        }
        SuggestItem[] newSizeArray = Arrays.copyOf(array, size);

        final long start = System.currentTimeMillis();
        SuggestWriterResult result = suggestWriter.write(client, settings, index, type, newSizeArray);
        return new SuggestIndexResponse(items.length, items.length, result.getFailures(), System.currentTimeMillis() - start);
    }

    public SuggestDeleteResponse delete(final String id) throws SuggestIndexException {
        final long start = System.currentTimeMillis();
        SuggestWriterResult result = suggestWriter.delete(client, settings, index, type, id);
        return new SuggestDeleteResponse(result.getFailures(), System.currentTimeMillis() - start);
    }

    public SuggestDeleteResponse deleteByQuery(final String queryString) throws SuggestIndexException {
        final long start = System.currentTimeMillis();
        SuggestWriterResult result = suggestWriter.deleteByQuery(client, settings, index, type, queryString);
        return new SuggestDeleteResponse(result.getFailures(), System.currentTimeMillis() - start);
    }

    public SuggestIndexResponse indexFromQueryLog(final QueryLog queryLog) throws SuggestIndexException {
        return indexFromQueryLog(new QueryLog[] { queryLog });
    }

    public SuggestIndexResponse indexFromQueryLog(final QueryLog[] queryLogs) throws SuggestIndexException {
        try {
            final long start = System.currentTimeMillis();
            final List<SuggestItem> items = new ArrayList<>(queryLogs.length * supportedFields.length);
            for (QueryLog queryLog : queryLogs) {
                items.addAll(contentsParser.parseQueryLog(queryLog, supportedFields, tagFieldName, roleFieldName, readingConverter,
                        normalizer));
            }
            SuggestIndexResponse response = index(items.toArray(new SuggestItem[items.size()]));
            return new SuggestIndexResponse(items.size(), queryLogs.length, response.getErrors(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            throw new SuggestIndexException("Failed to index from query_string.", e);
        }
    }

    public IndexingFuture indexFromQueryLog(final QueryLogReader queryLogReader, boolean async) {
        final IndexingFuture indexingFuture = new IndexingFuture();
        indexingFuture.started.set(true);

        Runnable r =
                () -> {
                    final long start = System.currentTimeMillis();
                    int numberOfSuggestDocs = 0;
                    int numberOfInputDocs = 0;
                    try {
                        int maxNum = 1000;

                        List<QueryLog> queryLogs = new ArrayList<>(maxNum);
                        QueryLog queryLog = queryLogReader.read();
                        while (queryLog != null) {
                            if (Thread.currentThread().isInterrupted()) {
                                break;
                            }
                            queryLogs.add(queryLog);
                            queryLog = queryLogReader.read();
                            if ((queryLog == null && !queryLogs.isEmpty()) || queryLogs.size() >= maxNum) {
                                try {
                                    SuggestIndexResponse res = indexFromQueryLog(queryLogs.toArray(new QueryLog[queryLogs.size()]));
                                    numberOfSuggestDocs += res.getNumberOfSuggestDocs();
                                    numberOfInputDocs += res.getNumberOfInputDocs();
                                } catch (SuggestIndexException e) {
                                    indexingFuture.errors().add(e);
                                }
                                queryLogs.clear();
                            }
                        }
                    } catch (Throwable t) {
                        indexingFuture.errors().add(t);
                    } finally {
                        queryLogReader.close();
                        indexingFuture.executeDone(new SuggestIndexResponse(numberOfSuggestDocs, numberOfInputDocs, indexingFuture.errors,
                                System.currentTimeMillis() - start));
                    }
                };

        if (async) {
            indexingFuture.future = threadPool.submit(r);
        } else {
            r.run();
        }

        return indexingFuture;
    }

    public SuggestIndexResponse indexFromDocument(final Map<String, Object>[] documents) throws SuggestIndexException {
        long start = System.currentTimeMillis();
        List<SuggestItem> items = new ArrayList<>(documents.length * supportedFields.length * 100); //TODO
        try {
            for (Map<String, Object> document : documents) {
                items.addAll(contentsParser.parseDocument(document, supportedFields, readingConverter, normalizer, analyzer));
            }
        } catch (Exception e) {
            throw new SuggestIndexException("Failed to index from document", e);
        }
        SuggestIndexResponse response = index(items.toArray(new SuggestItem[items.size()]));
        return new SuggestIndexResponse(items.size(), documents.length, response.getErrors(), System.currentTimeMillis() - start);
    }

    @SuppressWarnings("unchecked")
    public IndexingFuture indexFromDocument(final DocumentReader documentReader, final boolean async, final int maxDocNum) {
        final IndexingFuture indexingFuture = new IndexingFuture();

        Runnable r =
                () -> {
                    final long start = System.currentTimeMillis();
                    int numberOfSuggestDocs = 0;
                    int numberOfInputDocs = 0;

                    try {
                        indexingFuture.started.set(true);
                        List<Map<String, Object>> docs = new ArrayList<>(maxDocNum);
                        Map<String, Object> doc = documentReader.read();
                        while (doc != null) {
                            if (Thread.currentThread().isInterrupted()) {
                                break;
                            }
                            docs.add(doc);
                            doc = documentReader.read();
                            if (doc == null || docs.size() >= maxDocNum) {
                                try {
                                    SuggestIndexResponse res = indexFromDocument(docs.toArray(new Map[docs.size()]));
                                    numberOfSuggestDocs += res.getNumberOfSuggestDocs();
                                    numberOfInputDocs += res.getNumberOfInputDocs();
                                } catch (SuggestIndexException e) {
                                    indexingFuture.errors().add(e);
                                }
                                docs.clear();
                            }
                        }
                    } catch (Throwable t) {
                        indexingFuture.errors().add(t);
                    } finally {
                        documentReader.close();
                        indexingFuture.executeDone(new SuggestIndexResponse(numberOfSuggestDocs, numberOfInputDocs, indexingFuture.errors,
                                System.currentTimeMillis() - start));
                    }
                };

        if (async) {
            indexingFuture.future = threadPool.submit(r);
        } else {
            r.run();
        }

        return indexingFuture;
    }

    public SuggestDeleteResponse addNgWord(String ngWord) throws SuggestIndexException {
        settings.ngword().add(ngWord);
        ngWords = settings.ngword().get();
        return deleteByQuery(FieldNames.TEXT + ":*\"" + ngWord + "\"*");
    }

    public SuggestIndexResponse addElevateWord(ElevateWord elevateWord) throws SuggestIndexException {
        settings.elevateWord().add(elevateWord);
        return index(elevateWord.toSuggestItem());
    }

    public SuggestDeleteResponse deleteElevateWord(String elevateWord) throws SuggestIndexException {
        settings.elevateWord().delete(elevateWord);
        return delete(SuggestUtil.createSuggestTextId(elevateWord));
    }

    public SuggestIndexResponse restoreElevateWord() throws SuggestIndexException {
        final long start = System.currentTimeMillis();
        int numberOfSuggestDocs = 0;
        int numberOfInputDocs = 0;

        ElevateWord[] elevateWords = settings.elevateWord().get();
        List<Throwable> errors = new ArrayList<>(elevateWords.length);
        for (ElevateWord elevateWord : elevateWords) {
            SuggestIndexResponse res = addElevateWord(elevateWord);
            numberOfSuggestDocs += res.getNumberOfSuggestDocs();
            numberOfInputDocs += res.getNumberOfInputDocs();
            errors.addAll(res.getErrors());
        }
        return new SuggestIndexResponse(numberOfSuggestDocs, numberOfInputDocs, errors, System.currentTimeMillis() - start);
    }

    public SuggestDeleteResponse deleteOldWords(LocalDateTime threshold) throws SuggestIndexException {
        final long start = System.currentTimeMillis();
        String query = FieldNames.TIMESTAMP + ":[* TO " + threshold.toString() + "] NOT " + FieldNames.KINDS + ':' + SuggestItem.Kind.USER;
        suggestWriter.deleteByQuery(client, settings, index, type, query);
        return new SuggestDeleteResponse(null, System.currentTimeMillis() - start);
    }

    public SuggestIndexer setIndexName(String index) {
        this.index = index;
        return this;
    }

    public SuggestIndexer setTypeName(String type) {
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

    public static class IndexingFuture {
        final protected AtomicBoolean started = new AtomicBoolean(false);
        final protected List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        protected volatile Future future = null;
        protected volatile Listener listener = null;
        protected final AtomicBoolean needExecuteDone = new AtomicBoolean(false);
        protected volatile SuggestIndexResponse response = null;

        public boolean isStarted() {
            return started.get();
        }

        public boolean isDone() {
            return future == null || future.isDone();
        }

        public boolean isCanceled() {
            return future != null && future.isCancelled();
        }

        public boolean cancel() {
            return future != null && future.cancel(true);
        }

        public boolean hasError() {
            return errors.size() > 0;
        }

        public List<Throwable> errors() {
            return errors;
        }

        public SuggestIndexResponse getResponse() {
            return response;
        }

        public synchronized IndexingFuture done(Listener listener) {
            if (needExecuteDone.get()) {
                listener.onResponse(getResponse());
            } else {
                this.listener = listener;
                needExecuteDone.set(true);
            }
            return this;
        }

        protected synchronized void executeDone(SuggestIndexResponse response) {
            this.response = response;
            if (needExecuteDone.get() && listener != null) {
                this.listener.onResponse(getResponse());
            } else {
                needExecuteDone.set(true);
            }
        }
    }

    public interface Listener {
        void onResponse(SuggestIndexResponse response);
    }
}
