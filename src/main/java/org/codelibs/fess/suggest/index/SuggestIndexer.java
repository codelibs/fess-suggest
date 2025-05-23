/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.suggest.index;

import java.lang.management.ManagementFactory;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.concurrent.Deferred;
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
import org.opensearch.OpenSearchStatusException;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.transport.client.Client;

/**
 * The SuggestIndexer class is responsible for indexing and managing suggest items in an OpenSearch index.
 * It provides methods to index, delete, and manage suggest items, including handling bad words and elevate words.
 *
 * <p>Constructor:
 * <ul>
 * <li>{@link #SuggestIndexer(Client, String, ReadingConverter, ReadingConverter, Normalizer, SuggestAnalyzer, SuggestSettings, ExecutorService)}
 * </ul>
 *
 * <p>Methods:
 * <ul>
 * <li>{@link #index(SuggestItem)} - Index a single suggest item.
 * <li>{@link #index(SuggestItem[])} - Index multiple suggest items.
 * <li>{@link #delete(String)} - Delete a suggest item by ID.
 * <li>{@link #deleteByQuery(String)} - Delete suggest items by query string.
 * <li>{@link #deleteByQuery(QueryBuilder)} - Delete suggest items by query builder.
 * <li>{@link #deleteAll()} - Delete all suggest items.
 * <li>{@link #deleteDocumentWords()} - Delete document words.
 * <li>{@link #deleteQueryWords()} - Delete query words.
 * <li>{@link #indexFromQueryLog(QueryLog)} - Index from a single query log.
 * <li>{@link #indexFromQueryLog(QueryLog[])} - Index from multiple query logs.
 * <li>{@link #indexFromQueryLog(QueryLogReader, int, long)} - Index from query log reader with specified document per request and request interval.
 * <li>{@link #indexFromDocument(Map[])} - Index from an array of documents.
 * <li>{@link #indexFromSearchWord(String, String[], String[], String[], int, String[])} - Index from search word.
 * <li>{@link #addBadWord(String, boolean)} - Add a bad word and optionally apply it.
 * <li>{@link #deleteBadWord(String)} - Delete a bad word.
 * <li>{@link #addElevateWord(ElevateWord, boolean)} - Add an elevate word and optionally apply it.
 * <li>{@link #deleteElevateWord(String, boolean)} - Delete an elevate word and optionally apply it.
 * <li>{@link #restoreElevateWord()} - Restore elevate words.
 * <li>{@link #deleteOldWords(ZonedDateTime)} - Delete old words based on a threshold date.
 * <li>{@link #setIndexName(String)} - Set the index name.
 * <li>{@link #setSupportedFields(String[])} - Set the supported fields.
 * <li>{@link #setTagFieldNames(String[])} - Set the tag field names.
 * <li>{@link #setRoleFieldName(String)} - Set the role field name.
 * <li>{@link #setReadingConverter(ReadingConverter)} - Set the reading converter.
 * <li>{@link #setNormalizer(Normalizer)} - Set the normalizer.
 * <li>{@link #setAnalyzer(SuggestAnalyzer)} - Set the analyzer.
 * <li>{@link #setContentsParser(ContentsParser)} - Set the contents parser.
 * <li>{@link #setSuggestWriter(SuggestWriter)} - Set the suggest writer.
 * </ul>
 *
 * <p>Fields:
 * <ul>
 * <li>{@link #logger} - Logger instance.
 * <li>{@link #client} - OpenSearch client.
 * <li>{@link #index} - Index name.
 * <li>{@link #settings} - Suggest settings.
 * <li>{@link #supportedFields} - Supported fields for suggestions.
 * <li>{@link #tagFieldNames} - Tag field names.
 * <li>{@link #roleFieldName} - Role field name.
 * <li>{@link #langFieldName} - Language field name.
 * <li>{@link #badWords} - List of bad words.
 * <li>{@link #parallel} - Flag for parallel processing.
 * <li>{@link #readingConverter} - Reading converter.
 * <li>{@link #contentsReadingConverter} - Contents reading converter.
 * <li>{@link #normalizer} - Normalizer.
 * <li>{@link #analyzer} - Suggest analyzer.
 * <li>{@link #contentsParser} - Contents parser.
 * <li>{@link #suggestWriter} - Suggest writer.
 * <li>{@link #threadPool} - Executor service for thread pool.
 * </ul>
 */
public class SuggestIndexer {
    private final static Logger logger = LogManager.getLogger(SuggestIndexer.class);

    protected final Client client;
    protected String index;
    protected SuggestSettings settings;

    protected String[] supportedFields;
    protected String[] tagFieldNames;
    protected String roleFieldName;
    protected String langFieldName;
    protected String[] badWords;
    protected boolean parallel;

    protected ReadingConverter readingConverter;
    protected ReadingConverter contentsReadingConverter;
    protected Normalizer normalizer;
    protected SuggestAnalyzer analyzer;

    protected ContentsParser contentsParser;
    protected SuggestWriter suggestWriter;

    protected ExecutorService threadPool;

    public SuggestIndexer(final Client client, final String index, final ReadingConverter readingConverter,
            final ReadingConverter contentsReadingConverter, final Normalizer normalizer, final SuggestAnalyzer analyzer,
            final SuggestSettings settings, final ExecutorService threadPool) {
        this.client = client;
        this.index = index;

        supportedFields = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS);
        badWords = settings.badword().get(true);
        tagFieldNames = settings.getAsString(SuggestSettings.DefaultKeys.TAG_FIELD_NAME, StringUtil.EMPTY).split(",");
        roleFieldName = settings.getAsString(SuggestSettings.DefaultKeys.ROLE_FIELD_NAME, StringUtil.EMPTY);
        langFieldName = settings.getAsString(SuggestSettings.DefaultKeys.LANG_FIELD_NAME, StringUtil.EMPTY);
        parallel = settings.getAsBoolean(SuggestSettings.DefaultKeys.PARALLEL_PROCESSING, false);
        this.readingConverter = readingConverter;
        this.contentsReadingConverter = contentsReadingConverter;
        this.normalizer = normalizer;
        this.analyzer = analyzer;
        this.settings = settings;

        contentsParser = new DefaultContentsParser();
        suggestWriter = new SuggestIndexWriter();

        this.threadPool = threadPool;
    }

    // TODO return result
    public SuggestIndexResponse index(final SuggestItem item) {
        return index(new SuggestItem[] { item });
    }

    // TODO return result
    public SuggestIndexResponse index(final SuggestItem[] items) {
        // TODO parallel?
        final SuggestItem[] array = Stream.of(items).filter(item -> !item.isBadWord(badWords)).toArray(n -> new SuggestItem[n]);

        try {
            final long start = System.currentTimeMillis();
            final SuggestWriterResult result = suggestWriter.write(client, settings, index, array, true);
            return new SuggestIndexResponse(items.length, items.length, result.getFailures(), System.currentTimeMillis() - start);
        } catch (final Exception e) {
            throw new SuggestIndexException("Failed to write items[" + items.length + "] to " + index, e);
        }
    }

    public SuggestDeleteResponse delete(final String id) {
        final long start = System.currentTimeMillis();
        final SuggestWriterResult result = suggestWriter.delete(client, settings, index, id);
        return new SuggestDeleteResponse(result.getFailures(), System.currentTimeMillis() - start);
    }

    public SuggestDeleteResponse deleteByQuery(final String queryString) {
        return deleteByQuery(QueryBuilders.queryStringQuery(queryString).defaultOperator(Operator.AND));
    }

    public SuggestDeleteResponse deleteByQuery(final QueryBuilder queryBuilder) {
        final long start = System.currentTimeMillis();
        final SuggestWriterResult result = suggestWriter.deleteByQuery(client, settings, index, queryBuilder);
        return new SuggestDeleteResponse(result.getFailures(), System.currentTimeMillis() - start);
    }

    public SuggestDeleteResponse deleteAll() {
        final SuggestDeleteResponse response = deleteByQuery(QueryBuilders.matchAllQuery());
        restoreElevateWord();
        return response;
    }

    public SuggestDeleteResponse deleteDocumentWords() {
        final long start = System.currentTimeMillis();

        final SuggestDeleteResponse deleteResponse =
                deleteByQuery(QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery(FieldNames.DOC_FREQ).gte(1))
                        .mustNot(QueryBuilders.matchPhraseQuery(FieldNames.KINDS, SuggestItem.Kind.QUERY.toString()))
                        .mustNot(QueryBuilders.matchPhraseQuery(FieldNames.KINDS, SuggestItem.Kind.USER.toString())));
        if (deleteResponse.hasError()) {
            throw new SuggestIndexException(deleteResponse.getErrors().get(0));
        }

        final List<SuggestItem> updateItems = new ArrayList<>();
        SearchResponse response = client.prepareSearch(index).setSize(500).setScroll(settings.getScrollTimeout())
                .setQuery(QueryBuilders.rangeQuery(FieldNames.DOC_FREQ).gte(1)).execute().actionGet(settings.getSearchTimeout());
        String scrollId = response.getScrollId();
        try {
            while (scrollId != null) {
                final SearchHit[] hits = response.getHits().getHits();
                if (hits.length == 0) {
                    break;
                }
                for (final SearchHit hit : hits) {
                    final SuggestItem item = SuggestItem.parseSource(hit.getSourceAsMap());
                    item.setDocFreq(0);
                    item.setKinds(Stream.of(item.getKinds()).filter(kind -> kind != SuggestItem.Kind.DOCUMENT)
                            .toArray(count -> new SuggestItem.Kind[count]));
                    updateItems.add(item);
                }
                final SuggestWriterResult result =
                        suggestWriter.write(client, settings, index, updateItems.toArray(new SuggestItem[updateItems.size()]), false);
                if (result.hasFailure()) {
                    throw new SuggestIndexException(result.getFailures().get(0));
                }

                response = client.prepareSearchScroll(scrollId).execute().actionGet(settings.getSearchTimeout());
                if (!scrollId.equals(response.getScrollId())) {
                    SuggestUtil.deleteScrollContext(client, scrollId);
                }
                scrollId = response.getScrollId();
            }
        } finally {
            SuggestUtil.deleteScrollContext(client, scrollId);
        }

        return new SuggestDeleteResponse(null, System.currentTimeMillis() - start);
    }

    public SuggestDeleteResponse deleteQueryWords() {
        final long start = System.currentTimeMillis();

        final SuggestDeleteResponse deleteResponse =
                deleteByQuery(QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery(FieldNames.QUERY_FREQ).gte(1))
                        .mustNot(QueryBuilders.matchPhraseQuery(FieldNames.KINDS, SuggestItem.Kind.DOCUMENT.toString()))
                        .mustNot(QueryBuilders.matchPhraseQuery(FieldNames.KINDS, SuggestItem.Kind.USER.toString())));
        if (deleteResponse.hasError()) {
            throw new SuggestIndexException(deleteResponse.getErrors().get(0));
        }

        final List<SuggestItem> updateItems = new ArrayList<>();
        SearchResponse response = client.prepareSearch(index).setSize(500).setScroll(settings.getScrollTimeout())
                .setQuery(QueryBuilders.rangeQuery(FieldNames.QUERY_FREQ).gte(1)).execute().actionGet(settings.getSearchTimeout());
        String scrollId = response.getScrollId();
        try {
            while (scrollId != null) {
                final SearchHit[] hits = response.getHits().getHits();
                if (hits.length == 0) {
                    break;
                }
                for (final SearchHit hit : hits) {
                    final SuggestItem item = SuggestItem.parseSource(hit.getSourceAsMap());
                    item.setQueryFreq(0);
                    item.setKinds(Stream.of(item.getKinds()).filter(kind -> kind != SuggestItem.Kind.QUERY)
                            .toArray(count -> new SuggestItem.Kind[count]));
                    updateItems.add(item);
                }
                final SuggestWriterResult result =
                        suggestWriter.write(client, settings, index, updateItems.toArray(new SuggestItem[updateItems.size()]), false);
                if (result.hasFailure()) {
                    throw new SuggestIndexException(result.getFailures().get(0));
                }

                response = client.prepareSearchScroll(scrollId).execute().actionGet(settings.getSearchTimeout());
                if (!scrollId.equals(response.getScrollId())) {
                    SuggestUtil.deleteScrollContext(client, scrollId);
                }
                scrollId = response.getScrollId();
            }
        } finally {
            SuggestUtil.deleteScrollContext(client, scrollId);
        }

        return new SuggestDeleteResponse(null, System.currentTimeMillis() - start);
    }

    public SuggestIndexResponse indexFromQueryLog(final QueryLog queryLog) {
        return indexFromQueryLog(new QueryLog[] { queryLog });
    }

    public SuggestIndexResponse indexFromQueryLog(final QueryLog[] queryLogs) {
        if (logger.isInfoEnabled()) {
            logger.info("Index from querylog. num: {}", queryLogs.length);
        }
        try {
            final long start = System.currentTimeMillis();
            final Stream<QueryLog> stream = Stream.of(queryLogs);
            if (parallel) {
                stream.parallel();
            }
            final SuggestItem[] array = stream
                    .flatMap(queryLog -> contentsParser
                            .parseQueryLog(queryLog, supportedFields, tagFieldNames, roleFieldName, readingConverter, normalizer).stream())
                    .toArray(n -> new SuggestItem[n]);
            final long parseTime = System.currentTimeMillis();
            final SuggestIndexResponse response = index(array);
            final long indexTime = System.currentTimeMillis();

            if (logger.isInfoEnabled()) {
                printProcessingInfo("queries", queryLogs.length, array, parseTime - start, indexTime - parseTime);
            }
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
                    if (queryLog == null && !queryLogs.isEmpty() || queryLogs.size() >= docPerReq) {
                        final SuggestIndexResponse res = indexFromQueryLog(queryLogs.toArray(new QueryLog[queryLogs.size()]));
                        errors.addAll(res.getErrors());
                        numberOfSuggestDocs += res.getNumberOfSuggestDocs();
                        numberOfInputDocs += res.getNumberOfInputDocs();
                        queryLogs.clear();

                        Thread.sleep(requestInterval);
                    }
                }
                deferred.resolve(
                        new SuggestIndexResponse(numberOfSuggestDocs, numberOfInputDocs, errors, System.currentTimeMillis() - start));
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
            final Stream<Map<String, Object>> stream = Stream.of(documents);
            if (parallel) {
                stream.parallel();
            }
            final SuggestItem[] items = stream.flatMap(document -> {
                try {
                    return contentsParser.parseDocument(document, supportedFields, tagFieldNames, roleFieldName, langFieldName,
                            readingConverter, contentsReadingConverter, normalizer, analyzer).stream();
                } catch (OpenSearchStatusException | IllegalStateException e) {
                    final String msg = e.getMessage();
                    if (StringUtil.isNotEmpty(msg) || msg.contains("index.analyze.max_token_count")) {
                        logger.warn("Failed to parse document. ", e);
                        return Stream.empty();
                    }
                    throw e;
                }
            }).toArray(n -> new SuggestItem[n]);
            final long parseTime = System.currentTimeMillis();
            final SuggestIndexResponse response = index(items);
            final long indexTime = System.currentTimeMillis();

            if (logger.isInfoEnabled()) {
                printProcessingInfo("documents", documents.length, items, parseTime - start, indexTime - parseTime);
            }
            return new SuggestIndexResponse(items.length, documents.length, response.getErrors(), indexTime - start);
        } catch (final Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to index from document", e);
            }
            throw new SuggestIndexException("Failed to index from document", e);
        }
    }

    private void printProcessingInfo(final String type, final int size, final SuggestItem[] items, final long parseTime,
            final long indexTime) {
        final double cpuLoad;
        if (ManagementFactory.getOperatingSystemMXBean() instanceof final com.sun.management.OperatingSystemMXBean operatingSystemMXBean) {
            cpuLoad = operatingSystemMXBean.getProcessCpuLoad();
        } else {
            cpuLoad = -1;
        }
        final long maxMemory = Runtime.getRuntime().maxMemory();
        final long freeMemory = Runtime.getRuntime().freeMemory();
        final String msg = String.format(
                "%d words from %d %s: {\"time\":{\"parse\":%d,\"index\":%d},\"cpu\":%f,\"mem\":{\"heap\":\"%dmb\",\"used\":\"%dmb\"}}",
                items.length, size, type, parseTime, indexTime, cpuLoad, maxMemory / (1024 * 1024),
                (maxMemory - freeMemory) / (1024 * 1024));
        logger.info(msg);
        if (logger.isDebugEnabled()) {
            for (final SuggestItem item : items) {
                logger.debug("[{}] {}", type, item.toJsonString());
            }
        }
    }

    public Deferred<SuggestIndexResponse>.Promise indexFromDocument(final Supplier<DocumentReader> reader, final int docPerReq,
            final Runnable waitController) {
        if (logger.isInfoEnabled()) {
            logger.info("Start index by DocumentReader. docPerReq: {}", docPerReq);
        }
        final Deferred<SuggestIndexResponse> deferred = new Deferred<>();
        threadPool.execute(() -> {
            final long start = System.currentTimeMillis();
            int numberOfSuggestDocs = 0;
            int numberOfInputDocs = 0;

            final List<Throwable> errors = new ArrayList<>();
            final List<Map<String, Object>> docs = new ArrayList<>(docPerReq);
            try (final DocumentReader documentReader = reader.get()) {
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
                        client.admin().indices().prepareRefresh(index).execute().actionGet(settings.getIndicesTimeout());
                        docs.clear();

                        waitController.run();
                    }
                }

                deferred.resolve(
                        new SuggestIndexResponse(numberOfSuggestDocs, numberOfInputDocs, errors, System.currentTimeMillis() - start));
            } catch (final Throwable t) {
                deferred.reject(t);
            }
        });
        return deferred.promise();
    }

    public SuggestIndexResponse indexFromSearchWord(final String searchWord, final String[] fields, final String[] tags,
            final String[] roles, final int num, final String[] langs) {
        if (logger.isDebugEnabled()) {
            logger.debug("Index from searchWord. word: {}", searchWord);
        }

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
            final SuggestItem item =
                    contentsParser.parseSearchWords(words, null, fields, tags, roles, num, readingConverter, normalizer, analyzer, langs);
            if (item == null) {
                return new SuggestIndexResponse(0, 1, null, System.currentTimeMillis() - start);
            }
            final long parseTime = System.currentTimeMillis();
            final SuggestIndexResponse response = index(item);
            final long indexTime = System.currentTimeMillis();
            if (logger.isInfoEnabled()) {
                printProcessingInfo("queries", 1, new SuggestItem[] { item }, parseTime - start, indexTime - parseTime);
            }
            return new SuggestIndexResponse(1, 1, response.getErrors(), System.currentTimeMillis() - start);
        } catch (final Exception e) {
            final String msg = "Failed to index from document: searchWord=" + searchWord//
                    + ", fields=" + Arrays.toString(fields)//
                    + ", tags=" + Arrays.toString(tags)//
                    + ", roles=" + Arrays.toString(roles)//
                    + ", langs=" + Arrays.toString(langs)//
                    + ", num=" + num;
            if (logger.isDebugEnabled()) {
                logger.debug(msg, e);
            }
            throw new SuggestIndexException(msg, e);
        }
    }

    public SuggestDeleteResponse addBadWord(final String badWord, final boolean apply) {
        final String normalized = normalizer.normalize(badWord, "");
        settings.badword().add(normalized);
        badWords = settings.badword().get(true);
        if (apply) {
            return deleteByQuery(QueryBuilders.wildcardQuery(FieldNames.TEXT, "*" + normalized + "*"));
        }
        return new SuggestDeleteResponse(null, 0);
    }

    public void deleteBadWord(final String badWord) {
        settings.badword().delete(normalizer.normalize(badWord, ""));
    }

    public SuggestIndexResponse addElevateWord(final ElevateWord elevateWord, final boolean apply) {
        final String normalizedWord = normalizer.normalize(elevateWord.getElevateWord(), "");
        final List<String> normalizedReadings =
                elevateWord.getReadings().stream().map(reading -> normalizer.normalize(reading, "")).collect(Collectors.toList());
        final ElevateWord normalized = new ElevateWord(normalizedWord, elevateWord.getBoost(), normalizedReadings, elevateWord.getFields(),
                elevateWord.getTags(), elevateWord.getRoles());
        settings.elevateWord().add(normalized);
        if (apply) {
            return index(normalized.toSuggestItem());
        }
        return new SuggestIndexResponse(0, 0, null, 0);
    }

    public SuggestDeleteResponse deleteElevateWord(final String elevateWord, final boolean apply) {
        final String normalized = normalizer.normalize(elevateWord, "");
        settings.elevateWord().delete(normalized);
        if (apply) {
            return delete(SuggestUtil.createSuggestTextId(normalized));
        }
        return new SuggestDeleteResponse(null, 0);
    }

    public SuggestIndexResponse restoreElevateWord() {
        final long start = System.currentTimeMillis();
        int numberOfSuggestDocs = 0;
        int numberOfInputDocs = 0;

        final ElevateWord[] elevateWords = settings.elevateWord().get();
        final List<Throwable> errors = new ArrayList<>(elevateWords.length);
        for (final ElevateWord elevateWord : elevateWords) {
            final SuggestIndexResponse res = addElevateWord(elevateWord, true);
            numberOfSuggestDocs += res.getNumberOfSuggestDocs();
            numberOfInputDocs += res.getNumberOfInputDocs();
            errors.addAll(res.getErrors());
        }
        return new SuggestIndexResponse(numberOfSuggestDocs, numberOfInputDocs, errors, System.currentTimeMillis() - start);
    }

    public SuggestDeleteResponse deleteOldWords(final ZonedDateTime threshold) {
        final long start = System.currentTimeMillis();
        final String query = FieldNames.TIMESTAMP + ":[* TO " + threshold.toInstant().toEpochMilli() + "] NOT " + FieldNames.KINDS + ':'
                + SuggestItem.Kind.USER;
        deleteByQuery(query);
        return new SuggestDeleteResponse(null, System.currentTimeMillis() - start);
    }

    public SuggestIndexer setIndexName(final String index) {
        this.index = index;
        return this;
    }

    public SuggestIndexer setSupportedFields(final String[] supportedFields) {
        this.supportedFields = supportedFields;
        return this;
    }

    public SuggestIndexer setTagFieldNames(final String[] tagFieldNames) {
        this.tagFieldNames = tagFieldNames;
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
