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
package org.codelibs.fess.suggest.index.operations;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.concurrent.Deferred;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggestIndexException;
import org.codelibs.fess.suggest.index.SuggestIndexResponse;
import org.codelibs.fess.suggest.index.contents.ContentsParser;
import org.codelibs.fess.suggest.index.contents.document.DocumentReader;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLog;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLogReader;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.opensearch.OpenSearchStatusException;
import org.opensearch.transport.client.Client;

/**
 * Internal operations class for content-based indexing functionality.
 * Handles indexing from query logs, documents, and search words.
 *
 * <p>This class is package-private and intended for internal use by SuggestIndexer.
 */
public class ContentIndexingOperations {

    private static final Logger logger = LogManager.getLogger(ContentIndexingOperations.class);

    private final Client client;
    private final SuggestSettings settings;
    private final ExecutorService threadPool;
    private final IndexingOperations indexingOps;
    private final ContentsParser contentsParser;
    private final SuggestAnalyzer analyzer;
    private final ReadingConverter readingConverter;
    private final ReadingConverter contentsReadingConverter;
    private final Normalizer normalizer;
    private final boolean parallel;

    /**
     * Constructor.
     *
     * @param client The OpenSearch client
     * @param settings The suggest settings
     * @param threadPool The executor service for async operations
     * @param indexingOps The indexing operations for writing items
     * @param contentsParser The contents parser for parsing documents
     * @param analyzer The suggest analyzer
     * @param readingConverter The reading converter
     * @param contentsReadingConverter The contents reading converter
     * @param normalizer The normalizer
     * @param parallel Whether to use parallel processing
     */
    public ContentIndexingOperations(final Client client, final SuggestSettings settings, final ExecutorService threadPool,
            final IndexingOperations indexingOps, final ContentsParser contentsParser, final SuggestAnalyzer analyzer,
            final ReadingConverter readingConverter, final ReadingConverter contentsReadingConverter, final Normalizer normalizer,
            final boolean parallel) {
        this.client = client;
        this.settings = settings;
        this.threadPool = threadPool;
        this.indexingOps = indexingOps;
        this.contentsParser = contentsParser;
        this.analyzer = analyzer;
        this.readingConverter = readingConverter;
        this.contentsReadingConverter = contentsReadingConverter;
        this.normalizer = normalizer;
        this.parallel = parallel;
    }

    /**
     * Indexes a single query log.
     *
     * @param ctx The content indexing context
     * @param queryLog The query log to index
     * @return The SuggestIndexResponse
     */
    public SuggestIndexResponse indexFromQueryLog(final ContentIndexingContext ctx, final QueryLog queryLog) {
        return indexFromQueryLog(ctx, new QueryLog[] { queryLog });
    }

    /**
     * Indexes a single query log.
     *
     * @param index The index name
     * @param queryLog The query log to index
     * @param supportedFields The supported fields
     * @param tagFieldNames The tag field names
     * @param roleFieldName The role field name
     * @param badWords The bad words array
     * @return The SuggestIndexResponse
     */
    public SuggestIndexResponse indexFromQueryLog(final String index, final QueryLog queryLog, final String[] supportedFields,
            final String[] tagFieldNames, final String roleFieldName, final String[] badWords) {
        return indexFromQueryLog(new ContentIndexingContext(index, supportedFields, tagFieldNames, roleFieldName, null, badWords),
                queryLog);
    }

    /**
     * Indexes multiple query logs.
     *
     * @param ctx The content indexing context
     * @param queryLogs The query logs to index
     * @return The SuggestIndexResponse
     */
    public SuggestIndexResponse indexFromQueryLog(final ContentIndexingContext ctx, final QueryLog[] queryLogs) {
        if (logger.isInfoEnabled()) {
            logger.info("Indexing from query logs: count={}, index={}", queryLogs.length, ctx.getIndex());
        }
        try {
            final long start = System.currentTimeMillis();
            Stream<QueryLog> stream = Stream.of(queryLogs);
            if (parallel) {
                stream = stream.parallel();
            }
            final SuggestItem[] array =
                    stream.flatMap(queryLog -> contentsParser
                            .parseQueryLog(queryLog, ctx.getSupportedFields(), ctx.getTagFieldNames(), ctx.getRoleFieldName(),
                                    readingConverter, normalizer)
                            .stream()).toArray(SuggestItem[]::new);
            final long parseTime = System.currentTimeMillis();
            final SuggestIndexResponse response = indexingOps.index(ctx.getIndex(), array, ctx.getBadWords());
            final long indexTime = System.currentTimeMillis();

            if (logger.isInfoEnabled()) {
                printProcessingInfo("queries", queryLogs.length, array, parseTime - start, indexTime - parseTime);
            }
            return new SuggestIndexResponse(array.length, queryLogs.length, response.getErrors(), System.currentTimeMillis() - start);
        } catch (final Exception e) {
            throw new SuggestIndexException("Failed to index from query_string.", e);
        }
    }

    /**
     * Indexes multiple query logs.
     *
     * @param index The index name
     * @param queryLogs The query logs to index
     * @param supportedFields The supported fields
     * @param tagFieldNames The tag field names
     * @param roleFieldName The role field name
     * @param badWords The bad words array
     * @return The SuggestIndexResponse
     */
    public SuggestIndexResponse indexFromQueryLog(final String index, final QueryLog[] queryLogs, final String[] supportedFields,
            final String[] tagFieldNames, final String roleFieldName, final String[] badWords) {
        return indexFromQueryLog(new ContentIndexingContext(index, supportedFields, tagFieldNames, roleFieldName, null, badWords),
                queryLogs);
    }

    /**
     * Indexes documents from a query log reader asynchronously.
     *
     * @param ctx The content indexing context
     * @param queryLogReader The query log reader
     * @param docPerReq The number of documents to process per request
     * @param requestInterval The interval between requests
     * @return A Promise that will be resolved with the SuggestIndexResponse
     */
    public Deferred<SuggestIndexResponse>.Promise indexFromQueryLog(final ContentIndexingContext ctx, final QueryLogReader queryLogReader,
            final int docPerReq, final long requestInterval) {
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
                        final SuggestIndexResponse res = indexFromQueryLog(ctx, queryLogs.toArray(new QueryLog[queryLogs.size()]));
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

    /**
     * Indexes documents from a query log reader asynchronously.
     *
     * @param index The index name
     * @param queryLogReader The query log reader
     * @param docPerReq The number of documents to process per request
     * @param requestInterval The interval between requests
     * @param supportedFields The supported fields
     * @param tagFieldNames The tag field names
     * @param roleFieldName The role field name
     * @param badWords The bad words array
     * @return A Promise that will be resolved with the SuggestIndexResponse
     */
    public Deferred<SuggestIndexResponse>.Promise indexFromQueryLog(final String index, final QueryLogReader queryLogReader,
            final int docPerReq, final long requestInterval, final String[] supportedFields, final String[] tagFieldNames,
            final String roleFieldName, final String[] badWords) {
        return indexFromQueryLog(new ContentIndexingContext(index, supportedFields, tagFieldNames, roleFieldName, null, badWords),
                queryLogReader, docPerReq, requestInterval);
    }

    /**
     * Indexes documents from an array of maps.
     *
     * @param ctx The content indexing context
     * @param documents The documents to index
     * @return The SuggestIndexResponse
     */
    public SuggestIndexResponse indexFromDocument(final ContentIndexingContext ctx, final Map<String, Object>[] documents) {
        final String index = ctx.getIndex();
        final long start = System.currentTimeMillis();
        try {
            Stream<Map<String, Object>> stream = Stream.of(documents);
            if (parallel) {
                stream = stream.parallel();
            }
            final SuggestItem[] items = stream.flatMap(document -> {
                try {
                    return contentsParser
                            .parseDocument(document, ctx.getSupportedFields(), ctx.getTagFieldNames(), ctx.getRoleFieldName(),
                                    ctx.getLangFieldName(), readingConverter, contentsReadingConverter, normalizer, analyzer)
                            .stream();
                } catch (OpenSearchStatusException | IllegalStateException e) {
                    final String msg = e.getMessage();
                    if (StringUtil.isNotEmpty(msg) && msg.contains("index.analyze.max_token_count")) {
                        logger.warn("Failed to parse document (token count exceeded): index={}, message={}", index, msg);
                        return Stream.empty();
                    }
                    throw e;
                }
            }).toArray(SuggestItem[]::new);
            final long parseTime = System.currentTimeMillis();
            final SuggestIndexResponse response = indexingOps.index(index, items, ctx.getBadWords());
            final long indexTime = System.currentTimeMillis();

            if (logger.isInfoEnabled()) {
                printProcessingInfo("documents", documents.length, items, parseTime - start, indexTime - parseTime);
            }
            return new SuggestIndexResponse(items.length, documents.length, response.getErrors(), indexTime - start);
        } catch (final Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to index from documents: index={}, documentCount={}", index, documents.length, e);
            }
            throw new SuggestIndexException("Failed to index from documents: index=" + index + ", documentCount=" + documents.length, e);
        }
    }

    /**
     * Indexes documents from an array of maps.
     *
     * @param index The index name
     * @param documents The documents to index
     * @param supportedFields The supported fields
     * @param tagFieldNames The tag field names
     * @param roleFieldName The role field name
     * @param langFieldName The language field name
     * @param badWords The bad words array
     * @return The SuggestIndexResponse
     */
    public SuggestIndexResponse indexFromDocument(final String index, final Map<String, Object>[] documents, final String[] supportedFields,
            final String[] tagFieldNames, final String roleFieldName, final String langFieldName, final String[] badWords) {
        return indexFromDocument(new ContentIndexingContext(index, supportedFields, tagFieldNames, roleFieldName, langFieldName, badWords),
                documents);
    }

    /**
     * Indexes documents from a DocumentReader asynchronously.
     *
     * @param ctx The content indexing context
     * @param reader The supplier for DocumentReader
     * @param docPerReq The number of documents to process per request
     * @param waitController The runnable to control waiting between requests
     * @return A Promise that will be resolved with the SuggestIndexResponse
     */
    public Deferred<SuggestIndexResponse>.Promise indexFromDocument(final ContentIndexingContext ctx, final Supplier<DocumentReader> reader,
            final int docPerReq, final Runnable waitController) {
        final String index = ctx.getIndex();
        if (logger.isInfoEnabled()) {
            logger.info("Starting indexing from DocumentReader: index={}, docsPerRequest={}", index, docPerReq);
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
                        @SuppressWarnings("unchecked")
                        final SuggestIndexResponse res = indexFromDocument(ctx, docs.toArray(new Map[docs.size()]));
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

    /**
     * Indexes documents from a DocumentReader asynchronously.
     *
     * @param index The index name
     * @param reader The supplier for DocumentReader
     * @param docPerReq The number of documents to process per request
     * @param waitController The runnable to control waiting between requests
     * @param supportedFields The supported fields
     * @param tagFieldNames The tag field names
     * @param roleFieldName The role field name
     * @param langFieldName The language field name
     * @param badWords The bad words array
     * @return A Promise that will be resolved with the SuggestIndexResponse
     */
    public Deferred<SuggestIndexResponse>.Promise indexFromDocument(final String index, final Supplier<DocumentReader> reader,
            final int docPerReq, final Runnable waitController, final String[] supportedFields, final String[] tagFieldNames,
            final String roleFieldName, final String langFieldName, final String[] badWords) {
        return indexFromDocument(new ContentIndexingContext(index, supportedFields, tagFieldNames, roleFieldName, langFieldName, badWords),
                reader, docPerReq, waitController);
    }

    /**
     * Indexes a search word.
     *
     * @param index The index name
     * @param searchWord The search word
     * @param fields The fields
     * @param tags The tags
     * @param roles The roles
     * @param num The number
     * @param langs The languages
     * @param badWords The bad words array
     * @return The SuggestIndexResponse
     */
    public SuggestIndexResponse indexFromSearchWord(final String index, final String searchWord, final String[] fields, final String[] tags,
            final String[] roles, final int num, final String[] langs, final String[] badWords) {
        if (logger.isDebugEnabled()) {
            logger.debug("Indexing from search word: word={}, index={}, fields={}", searchWord, index, Arrays.toString(fields));
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
            final SuggestIndexResponse response = indexingOps.index(index, item, badWords);
            final long indexTime = System.currentTimeMillis();
            if (logger.isInfoEnabled()) {
                printProcessingInfo("queries", 1, new SuggestItem[] { item }, parseTime - start, indexTime - parseTime);
            }
            return new SuggestIndexResponse(1, 1, response.getErrors(), System.currentTimeMillis() - start);
        } catch (final Exception e) {
            final String msg = "Failed to index from search word: index=" + index + ", searchWord=" + searchWord + ", fields="
                    + Arrays.toString(fields) + ", tags=" + Arrays.toString(tags) + ", roles=" + Arrays.toString(roles) + ", langs="
                    + Arrays.toString(langs) + ", num=" + num;
            if (logger.isDebugEnabled()) {
                logger.debug(msg, e);
            }
            throw new SuggestIndexException(msg, e);
        }
    }

    /**
     * Prints processing information for logging.
     */
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

    /**
     * Gets the contents parser.
     *
     * @return The contents parser
     */
    public ContentsParser getContentsParser() {
        return contentsParser;
    }
}
