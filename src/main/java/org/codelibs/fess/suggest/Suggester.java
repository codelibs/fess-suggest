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
package org.codelibs.fess.suggest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.index.SuggestIndexer;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.request.popularwords.PopularWordsRequestBuilder;
import org.codelibs.fess.suggest.request.suggest.SuggestRequestBuilder;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.codelibs.fesen.opensearch.OpenSearchStatusException;
import org.codelibs.fesen.opensearch.action.admin.indices.alias.Alias;
import org.codelibs.fesen.opensearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.codelibs.fesen.opensearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions;
import org.codelibs.fesen.opensearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.codelibs.fesen.opensearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.codelibs.fesen.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.codelibs.fesen.opensearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.codelibs.fesen.opensearch.action.admin.indices.get.GetIndexResponse;
import org.codelibs.fesen.opensearch.action.admin.indices.refresh.RefreshResponse;
import org.codelibs.fesen.opensearch.action.search.SearchResponse;
import org.codelibs.fesen.opensearch.cluster.metadata.AliasMetadata;
import org.codelibs.fesen.opensearch.common.xcontent.XContentType;
import org.codelibs.fesen.opensearch.index.query.QueryBuilder;
import org.codelibs.fesen.opensearch.index.query.QueryBuilders;
import org.codelibs.fesen.opensearch.transport.client.Client;

/**
 * The Suggester class provides functionality for managing and querying suggestion indices.
 * It includes methods for creating, refreshing, and switching indices, as well as for
 * retrieving popular words and indexing suggestions.
 *
 * <p>Constructor:
 * <ul>
 *   <li>{@link #Suggester(Client, SuggestSettings, ReadingConverter, ReadingConverter, Normalizer, SuggestAnalyzer, ExecutorService)}: Initializes a new instance of the Suggester class.</li>
 * </ul>
 *
 * <p>Public Methods:
 * <ul>
 *   <li>{@link #suggest()}: Creates a new SuggestRequestBuilder for querying suggestions.</li>
 *   <li>{@link #popularWords()}: Creates a new PopularWordsRequestBuilder for querying popular words.</li>
 *   <li>{@link #refresh()}: Refreshes the suggestion indices.</li>
 *   <li>{@link #shutdown()}: Shuts down the thread pool.</li>
 *   <li>{@link #createIndexIfNothing()}: Creates a new index if no index exists.</li>
 *   <li>{@link #createNextIndex()}: Creates a new index and replaces the current update alias with the new index.</li>
 *   <li>{@link #switchIndex()}: Switches the search alias to the current update index.</li>
 *   <li>{@link #removeDisableIndices()}: Removes disabled indices.</li>
 *   <li>{@link #indexer()}: Creates a new SuggestIndexer for indexing suggestions.</li>
 *   <li>{@link #builder()}: Creates a new SuggesterBuilder for building Suggester instances.</li>
 *   <li>{@link #settings()}: Returns the SuggestSettings instance.</li>
 *   <li>{@link #getReadingConverter()}: Returns the ReadingConverter instance.</li>
 *   <li>{@link #getNormalizer()}: Returns the Normalizer instance.</li>
 *   <li>{@link #getIndex()}: Returns the index name.</li>
 *   <li>{@link #getAllWordsNum()}: Returns the total number of words in the suggestion index.</li>
 *   <li>{@link #getDocumentWordsNum()}: Returns the number of document words in the suggestion index.</li>
 *   <li>{@link #getQueryWordsNum()}: Returns the number of query words in the suggestion index.</li>
 * </ul>
 *
 * <p>Protected Methods:
 * <ul>
 *   <li>{@link #createDefaultIndexer()}: Creates a default SuggestIndexer instance.</li>
 * </ul>
 *
 * <p>Private Methods:
 * <ul>
 *   <li>{@link #getNum(QueryBuilder)}: Returns the number of words matching the given query.</li>
 *   <li>{@link #getSearchAlias(String)}: Returns the search alias for the given index.</li>
 *   <li>{@link #getUpdateAlias(String)}: Returns the update alias for the given index.</li>
 *   <li>{@link #createIndexName(String)}: Creates a new index name based on the current date and time.</li>
 *   <li>{@link #getDefaultMappings()}: Returns the default mappings for the suggestion index.</li>
 *   <li>{@link #getDefaultIndexSettings()}: Returns the default settings for the suggestion index.</li>
 *   <li>{@link #isSuggestIndex(String)}: Checks if the given index name is a suggestion index.</li>
 * </ul>
 */
public class Suggester {
    private static final Logger logger = LogManager.getLogger(Suggester.class);

    /** The expected number of indices for an alias. */
    private static final int EXPECTED_INDEX_COUNT = 1;

    /** The OpenSearch client. */
    protected final Client client;
    /** The suggest settings. */
    protected final SuggestSettings suggestSettings;
    /** The reading converter. */
    protected final ReadingConverter readingConverter;
    /** The contents reading converter. */
    protected final ReadingConverter contentsReadingConverter;
    /** The normalizer. */
    protected final Normalizer normalizer;
    /** The analyzer. */
    protected final SuggestAnalyzer analyzer;

    /** The index name. */
    protected final String index;

    /** The thread pool. */
    protected final ExecutorService threadPool;

    /**
     * Constructor for Suggester.
     * @param client The OpenSearch client.
     * @param settings The SuggestSettings instance.
     * @param readingConverter The ReadingConverter instance.
     * @param contentsReadingConverter The contents ReadingConverter instance.
     * @param normalizer The Normalizer instance.
     * @param analyzer The SuggestAnalyzer instance.
     * @param threadPool The ExecutorService for thread pooling.
     * @throws NullPointerException if any of the required parameters is null.
     */
    public Suggester(final Client client, final SuggestSettings settings, final ReadingConverter readingConverter,
            final ReadingConverter contentsReadingConverter, final Normalizer normalizer, final SuggestAnalyzer analyzer,
            final ExecutorService threadPool) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        suggestSettings = Objects.requireNonNull(settings, "settings must not be null");
        this.readingConverter = Objects.requireNonNull(readingConverter, "readingConverter must not be null");
        this.contentsReadingConverter = Objects.requireNonNull(contentsReadingConverter, "contentsReadingConverter must not be null");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
        this.analyzer = Objects.requireNonNull(analyzer, "analyzer must not be null");
        index = settings.getAsString(SuggestSettings.DefaultKeys.INDEX, StringUtil.EMPTY);
        this.threadPool = Objects.requireNonNull(threadPool, "threadPool must not be null");

        if (logger.isDebugEnabled()) {
            logger.debug("Created suggester instance: index={}", index);
        }
    }

    /**
     * Creates a new SuggestRequestBuilder for querying suggestions.
     * @return A SuggestRequestBuilder instance.
     */
    public SuggestRequestBuilder suggest() {
        return new SuggestRequestBuilder(client, readingConverter, normalizer).setIndex(getSearchAlias(index));
    }

    /**
     * Creates a new PopularWordsRequestBuilder for querying popular words.
     * @return A PopularWordsRequestBuilder instance.
     */
    public PopularWordsRequestBuilder popularWords() {
        return new PopularWordsRequestBuilder(client).setIndex(getSearchAlias(index));
    }

    /**
     * Refreshes the suggestion indices.
     * @return The RefreshResponse.
     */
    public RefreshResponse refresh() {
        if (logger.isDebugEnabled()) {
            logger.debug("Refreshing indices: index={}", index);
        }
        return client.admin().indices().prepareRefresh().execute().actionGet(suggestSettings.getIndexTimeout());
    }

    /**
     * Shuts down the thread pool.
     */
    public void shutdown() {
        if (logger.isInfoEnabled()) {
            logger.info("Shutting down suggester: index={}", index);
        }
        threadPool.shutdownNow();
    }

    /**
     * Creates a new index if no index exists.
     *
     * <p>Several processes that share the index name can call this at the same time against a cluster that has no suggest
     * index yet. The index is therefore created together with its aliases in one request, and the update alias is marked as
     * the write index. The search engine applies index creations one at a time and refuses a second write index for the
     * same alias, so only the first request succeeds; every other process uses the index that request created.</p>
     *
     * <p>When the index exists already, the update alias is made writable if it has no write index (see
     * {@link #ensureUpdateAliasWriteIndex()}).</p>
     *
     * @return True if an index was created, false otherwise.
     */
    public boolean createIndexIfNothing() {
        try {
            final IndicesExistsResponse response =
                    client.admin().indices().prepareExists(getSearchAlias(index)).execute().actionGet(suggestSettings.getIndicesTimeout());
            if (response.isExists()) {
                ensureUpdateAliasWriteIndex();
                return false;
            }
            final String mappingSource = getDefaultMappings();
            final String settingsSource = getDefaultIndexSettings();
            final String indexName = createIndexName(index);
            if (logger.isInfoEnabled()) {
                logger.info("Creating suggest index: index={}, searchAlias={}, updateAlias={}", indexName, getSearchAlias(index),
                        getUpdateAlias(index));
            }

            boolean created = true;
            try {
                client.admin()
                        .indices()
                        .prepareCreate(indexName)
                        .setSettings(settingsSource, XContentType.JSON)
                        .setMapping(mappingSource)
                        .addAlias(new Alias(getSearchAlias(index)))
                        .addAlias(new Alias(getUpdateAlias(index)).writeIndex(true))
                        .execute()
                        .actionGet(suggestSettings.getIndicesTimeout());
            } catch (final OpenSearchStatusException e) {
                if (!SuggestUtil.isResourceAlreadyExistsException(e) && !SuggestUtil.isMultipleWriteIndicesException(e)) {
                    throw e;
                }
                // another process sharing this index name created the suggest index after the lookup above
                if (logger.isInfoEnabled()) {
                    logger.info("Using the suggest index created by another process: index={}, updateAlias={}, indices={}", index,
                            getUpdateAlias(index), getIndicesForAlias(getUpdateAlias(index)));
                }
                created = false;
            }

            client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet(suggestSettings.getClusterTimeout());
            return created;
        } catch (final Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to create suggest index: index={}", index, e);
            }
            throw new SuggesterException("Failed to create suggest index: " + index, e);
        }
    }

    /**
     * Marks the index behind the update alias as its write index when the alias points at that single index with
     * {@code is_write_index: false}.
     *
     * <p>fesen-httpclient 3.8.0 and earlier sent {@code is_write_index: false} for every alias of a create index request that
     * did not set it, so a suggest index created by Fess 15.8 or earlier has no write index behind the update alias, and every
     * write through it fails with "no write index is defined for alias" until the next index rotation replaces the alias.
     * An alias without the flag, or one that points at several indices, is left alone.</p>
     */
    private void ensureUpdateAliasWriteIndex() {
        final String updateAlias = getUpdateAlias(index);
        final IndicesExistsResponse existsResponse =
                client.admin().indices().prepareExists(updateAlias).execute().actionGet(suggestSettings.getIndicesTimeout());
        if (!existsResponse.isExists()) {
            return;
        }
        final Map<String, AliasMetadata> aliasMap = new HashMap<>();
        final GetAliasesResponse getAliasesResponse =
                client.admin().indices().prepareGetAliases(updateAlias).execute().actionGet(suggestSettings.getIndicesTimeout());
        for (final Map.Entry<String, List<AliasMetadata>> entry : getAliasesResponse.getAliases().entrySet()) {
            entry.getValue().stream().filter(a -> updateAlias.equals(a.alias())).forEach(a -> aliasMap.put(entry.getKey(), a));
        }
        if (aliasMap.size() != 1) {
            return;
        }
        final Map.Entry<String, AliasMetadata> entry = aliasMap.entrySet().iterator().next();
        if (!Boolean.FALSE.equals(entry.getValue().writeIndex())) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Marking the suggest index as the write index of its update alias: index={}, updateAlias={}", entry.getKey(),
                    updateAlias);
        }
        final IndicesAliasesRequest request = new IndicesAliasesRequest();
        request.addAliasAction(AliasActions.add().index(entry.getKey()).alias(updateAlias).writeIndex(true));
        client.admin().indices().aliases(request).actionGet(suggestSettings.getIndicesTimeout());
    }

    /**
     * Creates a new index and replaces the current update alias with the new index.
     */
    public void createNextIndex() {
        try {
            final List<String> prevIndices = getIndicesForAlias(getUpdateAlias(index));

            final String mappingSource = getDefaultMappings();
            final String settingsSource = getDefaultIndexSettings();
            final String indexName = createIndexName(index);
            if (logger.isInfoEnabled()) {
                logger.info("Creating next index: index={}, updateAlias={}, previousIndices={}", indexName, getUpdateAlias(index),
                        prevIndices);
            }

            final CreateIndexResponse createIndexResponse = client.admin()
                    .indices()
                    .prepareCreate(indexName)
                    .setSettings(settingsSource, XContentType.JSON)
                    .setMapping(mappingSource)
                    .execute()
                    .actionGet(suggestSettings.getIndicesTimeout());
            if (!createIndexResponse.isAcknowledged()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to create next index (not acknowledged): index={}", indexName);
                }
                throw new SuggesterException("Failed to create next index (not acknowledged): " + indexName);
            }
            client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet(suggestSettings.getClusterTimeout());

            final IndicesAliasesRequestBuilder aliasesRequestBuilder =
                    client.admin().indices().prepareAliases().addAlias(indexName, getUpdateAlias(index));
            for (final String prevIndex : prevIndices) {
                aliasesRequestBuilder.removeAlias(prevIndex, getUpdateAlias(index));
            }
            aliasesRequestBuilder.execute().actionGet(suggestSettings.getIndicesTimeout());
        } catch (final SuggesterException e) {
            // Re-throw SuggesterException with original message
            throw e;
        } catch (final Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to create and switch to next index: baseIndex={}", index, e);
            }
            throw new SuggesterException("Failed to create and switch to next index: " + index, e);
        }
    }

    /**
     * Switches the search alias to the current update index.
     */
    public void switchIndex() {
        try {
            final String updateAlias = getUpdateAlias(index);
            final List<String> updateIndices = getIndicesForAlias(updateAlias);
            if (updateIndices.size() != EXPECTED_INDEX_COUNT) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Unexpected number of update indices: expected={}, actual={}, updateAlias={}, indices={}",
                            EXPECTED_INDEX_COUNT, updateIndices.size(), updateAlias, updateIndices);
                }
                throw new SuggesterException(
                        "Unexpected number of update indices: expected=" + EXPECTED_INDEX_COUNT + ", actual=" + updateIndices.size());
            }
            final String updateIndex = updateIndices.get(0);

            final String searchAlias = getSearchAlias(index);
            final List<String> searchIndices = getIndicesForAlias(searchAlias);
            if (searchIndices.size() != EXPECTED_INDEX_COUNT) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Unexpected number of search indices: expected={}, actual={}, searchAlias={}, indices={}",
                            EXPECTED_INDEX_COUNT, searchIndices.size(), searchAlias, searchIndices);
                }
                throw new SuggesterException(
                        "Unexpected number of search indices: expected=" + EXPECTED_INDEX_COUNT + ", actual=" + searchIndices.size());
            }
            final String searchIndex = searchIndices.get(0);

            if (updateIndex.equals(searchIndex)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Search and update indices are already the same: index={}", searchIndex);
                }
                return;
            }

            if (logger.isInfoEnabled()) {
                logger.info("Switching search index: searchAlias={}, from={}, to={}", searchAlias, searchIndex, updateIndex);
            }
            client.admin()
                    .indices()
                    .prepareAliases()
                    .removeAlias(searchIndex, searchAlias)
                    .addAlias(updateIndex, searchAlias)
                    .execute()
                    .actionGet(suggestSettings.getIndicesTimeout());
        } catch (final SuggesterException e) {
            // Re-throw SuggesterException with original message
            throw e;
        } catch (final Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to switch search index: baseIndex={}", index, e);
            }
            throw new SuggesterException("Failed to switch search index: " + index, e);
        }
    }

    /**
     * Removes disabled indices.
     */
    public void removeDisableIndices() {
        if (logger.isInfoEnabled()) {
            logger.info("Removing disabled indices: baseIndex={}", index);
        }
        final GetIndexResponse response =
                client.admin().indices().prepareGetIndex().addIndices("*").execute().actionGet(suggestSettings.getIndicesTimeout());
        Stream.of(response.getIndices()).filter(s -> {
            if (!isSuggestIndex(s)) {
                return false;
            }
            final List<AliasMetadata> list = response.getAliases().get(s);
            if (list == null) {
                return true;
            }
            return list.isEmpty();
        }).forEach(s -> {
            if (logger.isInfoEnabled()) {
                logger.info("Deleting disabled index (no aliases): index={}", s);
            }
            client.admin().indices().prepareDelete(s).execute().actionGet(suggestSettings.getIndicesTimeout());
        });
    }

    /**
     * Creates a new SuggestIndexer for indexing suggestions.
     * @return A SuggestIndexer instance.
     */
    public SuggestIndexer indexer() {
        return createDefaultIndexer();
    }

    /**
     * Creates a new SuggesterBuilder for building Suggester instances.
     * @return A SuggesterBuilder instance.
     */
    public static SuggesterBuilder builder() {
        return new SuggesterBuilder();
    }

    // getter
    /**
     * Returns the SuggestSettings instance.
     * @return The SuggestSettings instance.
     */
    public SuggestSettings settings() {
        return suggestSettings;
    }

    /**
     * Returns the ReadingConverter instance.
     * @return The ReadingConverter instance.
     */
    public ReadingConverter getReadingConverter() {
        return readingConverter;
    }

    /**
     * Returns the Normalizer instance.
     * @return The Normalizer instance.
     */
    public Normalizer getNormalizer() {
        return normalizer;
    }

    /**
     * Creates a default SuggestIndexer instance.
     * @return A SuggestIndexer instance.
     */
    protected SuggestIndexer createDefaultIndexer() {
        return new SuggestIndexer(client, getUpdateAlias(index), readingConverter, contentsReadingConverter, normalizer, analyzer,
                suggestSettings, threadPool);
    }

    /**
     * Returns the index name.
     * @return The index name.
     */
    public String getIndex() {
        return index;
    }

    /**
     * Returns the total number of words in the suggestion index.
     * @return The total number of words.
     */
    public long getAllWordsNum() {
        return getNum(QueryBuilders.matchAllQuery());
    }

    /**
     * Returns the number of document words in the suggestion index.
     * @return The number of document words.
     */
    public long getDocumentWordsNum() {
        return getNum(QueryBuilders.rangeQuery(FieldNames.DOC_FREQ).gte(1));
    }

    /**
     * Returns the number of query words in the suggestion index.
     * @return The number of query words.
     */
    public long getQueryWordsNum() {
        return getNum(QueryBuilders.rangeQuery(FieldNames.QUERY_FREQ).gte(1));
    }

    private long getNum(final QueryBuilder queryBuilder) {
        final SearchResponse searchResponse = client.prepareSearch()
                .setIndices(getSearchAlias(index))
                .setSize(0)
                .setQuery(queryBuilder)
                .setTrackTotalHits(true)
                .execute()
                .actionGet(suggestSettings.getSearchTimeout());
        return searchResponse.getHits().getTotalHits().value();
    }

    private String getSearchAlias(final String index) {
        return index;
    }

    private String getUpdateAlias(final String index) {
        return index + ".update";
    }

    private String createIndexName(final String index) {
        return index + '.' + ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String getDefaultMappings() throws IOException {
        try (final InputStream is = this.getClass().getClassLoader().getResourceAsStream("suggest_indices/suggest/mappings-default.json")) {
            if (is == null) {
                throw new IOException("Resource not found: suggest_indices/suggest/mappings-default.json");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String getDefaultIndexSettings() throws IOException {
        try (final InputStream is = this.getClass().getClassLoader().getResourceAsStream("suggest_indices/suggest.json")) {
            if (is == null) {
                throw new IOException("Resource not found: suggest_indices/suggest.json");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private boolean isSuggestIndex(final String indexName) {
        return indexName.startsWith(index);
    }

    /**
     * Returns a list of indices associated with the given alias.
     * @param alias The alias name.
     * @return A list of index names associated with the alias.
     */
    private List<String> getIndicesForAlias(final String alias) {
        final List<String> indices = new ArrayList<>();
        final IndicesExistsResponse response =
                client.admin().indices().prepareExists(alias).execute().actionGet(suggestSettings.getIndicesTimeout());
        if (response.isExists()) {
            final GetAliasesResponse getAliasesResponse =
                    client.admin().indices().prepareGetAliases(alias).execute().actionGet(suggestSettings.getIndicesTimeout());
            getAliasesResponse.getAliases()
                    .entrySet()
                    .forEach(x -> x.getValue().stream().filter(y -> alias.equals(y.alias())).forEach(y -> indices.add(x.getKey())));
        }
        return indices;
    }
}
