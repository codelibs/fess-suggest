/*
 * Copyright 2009-2021 the CodeLibs Project and the Others.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fesen.action.admin.indices.alias.Alias;
import org.codelibs.fesen.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.codelibs.fesen.action.admin.indices.alias.get.GetAliasesResponse;
import org.codelibs.fesen.action.admin.indices.create.CreateIndexResponse;
import org.codelibs.fesen.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.codelibs.fesen.action.admin.indices.get.GetIndexResponse;
import org.codelibs.fesen.action.admin.indices.refresh.RefreshResponse;
import org.codelibs.fesen.action.search.SearchResponse;
import org.codelibs.fesen.client.Client;
import org.codelibs.fesen.cluster.metadata.AliasMetadata;
import org.codelibs.fesen.common.xcontent.XContentType;
import org.codelibs.fesen.index.query.QueryBuilder;
import org.codelibs.fesen.index.query.QueryBuilders;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.index.SuggestIndexer;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.request.popularwords.PopularWordsRequestBuilder;
import org.codelibs.fess.suggest.request.suggest.SuggestRequestBuilder;
import org.codelibs.fess.suggest.settings.SuggestSettings;

public class Suggester {
    private static final Logger logger = Logger.getLogger(Suggester.class.getName());

    protected final Client client;
    protected final SuggestSettings suggestSettings;
    protected final ReadingConverter readingConverter;
    protected final ReadingConverter contentsReadingConverter;
    protected final Normalizer normalizer;
    protected final SuggestAnalyzer analyzer;

    protected final String index;

    protected final ExecutorService threadPool;

    public Suggester(final Client client, final SuggestSettings settings, final ReadingConverter readingConverter,
            final ReadingConverter contentsReadingConverter, final Normalizer normalizer, final SuggestAnalyzer analyzer,
            final ExecutorService threadPool) {
        this.client = client;
        this.suggestSettings = settings;
        this.readingConverter = readingConverter;
        this.contentsReadingConverter = contentsReadingConverter;
        this.normalizer = normalizer;
        this.analyzer = analyzer;
        this.index = settings.getAsString(SuggestSettings.DefaultKeys.INDEX, StringUtil.EMPTY);
        this.threadPool = threadPool;

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(() -> String.format("Create suggester instance for %s", this.index));
        }
    }

    public SuggestRequestBuilder suggest() {
        return new SuggestRequestBuilder(client, readingConverter, normalizer).setIndex(getSearchAlias(index));
    }

    public PopularWordsRequestBuilder popularWords() {
        return new PopularWordsRequestBuilder(client).setIndex(getSearchAlias(index));
    }

    public RefreshResponse refresh() {
        return client.admin().indices().prepareRefresh().execute().actionGet(suggestSettings.getIndexTimeout());
    }

    public void shutdown() {
        threadPool.shutdownNow();
    }

    public boolean createIndexIfNothing() {
        try {
            boolean created = false;
            final IndicesExistsResponse response =
                    client.admin().indices().prepareExists(getSearchAlias(index)).execute().actionGet(suggestSettings.getIndicesTimeout());
            if (!response.isExists()) {
                final String mappingSource = getDefaultMappings();
                final String settingsSource = getDefaultIndexSettings();
                final String indexName = createIndexName(index);
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(() -> String.format("Create suggest index: %s", indexName));
                }

                client.admin().indices().prepareCreate(indexName).setSettings(settingsSource, XContentType.JSON)
                        .addMapping(SuggestConstants.DEFAULT_TYPE, mappingSource, XContentType.JSON)
                        .addAlias(new Alias(getSearchAlias(index))).addAlias(new Alias(getUpdateAlias(index))).execute()
                        .actionGet(suggestSettings.getIndicesTimeout());

                client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet(suggestSettings.getClusterTimeout());
                created = true;
            }
            return created;
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Failed to create index. index:" + index, e);
            throw new SuggesterException("Failed to create index.", e);
        }
    }

    public void createNextIndex() {
        try {
            final List<String> prevIndices = new ArrayList<>();
            final IndicesExistsResponse response =
                    client.admin().indices().prepareExists(getUpdateAlias(index)).execute().actionGet(suggestSettings.getIndicesTimeout());
            if (response.isExists()) {
                final GetAliasesResponse getAliasesResponse = client.admin().indices().prepareGetAliases(getUpdateAlias(index)).execute()
                        .actionGet(suggestSettings.getIndicesTimeout());
                getAliasesResponse.getAliases().keysIt().forEachRemaining(prevIndices::add);
            }

            final String mappingSource = getDefaultMappings();
            final String settingsSource = getDefaultIndexSettings();
            final String indexName = createIndexName(index);
            if (logger.isLoggable(Level.INFO)) {
                logger.info(() -> String.format("Create next index: %s", indexName));
            }

            final CreateIndexResponse createIndexResponse =
                    client.admin().indices().prepareCreate(indexName).setSettings(settingsSource, XContentType.JSON)
                            .addMapping(SuggestConstants.DEFAULT_TYPE, mappingSource, XContentType.JSON).execute()
                            .actionGet(suggestSettings.getIndicesTimeout());
            if (!createIndexResponse.isAcknowledged()) {
                logger.severe(() -> String.format("Could not create next index: %s", indexName));
                throw new SuggesterException("Could not create next index: " + indexName);
            }
            client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet(suggestSettings.getClusterTimeout());

            final IndicesAliasesRequestBuilder aliasesRequestBuilder =
                    client.admin().indices().prepareAliases().addAlias(indexName, getUpdateAlias(index));
            for (final String prevIndex : prevIndices) {
                aliasesRequestBuilder.removeAlias(prevIndex, getUpdateAlias(index));
            }
            aliasesRequestBuilder.execute().actionGet(suggestSettings.getIndicesTimeout());
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Failed to replace with new index.", e);
            throw new SuggesterException("Failed to replace with new index.", e);
        }
    }

    public void switchIndex() {
        try {
            final List<String> updateIndices = new ArrayList<>();
            final String updateAlias = getUpdateAlias(index);
            final IndicesExistsResponse updateIndicesResponse =
                    client.admin().indices().prepareExists(updateAlias).execute().actionGet(suggestSettings.getIndicesTimeout());
            if (updateIndicesResponse.isExists()) {
                final GetAliasesResponse getAliasesResponse =
                        client.admin().indices().prepareGetAliases(updateAlias).execute().actionGet(suggestSettings.getIndicesTimeout());
                getAliasesResponse.getAliases()
                        .forEach(x -> x.value.stream().filter(y -> updateAlias.equals(y.alias())).forEach(y -> updateIndices.add(x.key)));
            }
            if (updateIndices.size() != 1) {
                logger.severe(() -> String.format("Unexpected update indices num:%s", updateIndices.size()));
                throw new SuggesterException("Unexpected update indices num:" + updateIndices.size());
            }
            final String updateIndex = updateIndices.get(0);

            final List<String> searchIndices = new ArrayList<>();
            final String searchAlias = getSearchAlias(index);
            final IndicesExistsResponse searchIndicesResponse =
                    client.admin().indices().prepareExists(searchAlias).execute().actionGet(suggestSettings.getIndicesTimeout());
            if (searchIndicesResponse.isExists()) {
                final GetAliasesResponse getAliasesResponse =
                        client.admin().indices().prepareGetAliases(searchAlias).execute().actionGet(suggestSettings.getIndicesTimeout());
                getAliasesResponse.getAliases()
                        .forEach(x -> x.value.stream().filter(y -> searchAlias.equals(y.alias())).forEach(y -> searchIndices.add(x.key)));
            }
            if (searchIndices.size() != 1) {
                logger.severe(() -> String.format("Unexpected update indices num:%s", searchIndices.size()));
                throw new SuggesterException("Unexpected search indices num:" + searchIndices.size());
            }
            final String searchIndex = searchIndices.get(0);

            if (updateIndex.equals(searchIndex)) {
                return;
            }

            if (logger.isLoggable(Level.INFO)) {
                logger.info(() -> String.format("Switch suggest.search index. %s => %s", searchIndex, updateIndex));
            }
            client.admin().indices().prepareAliases().removeAlias(searchIndex, searchAlias).addAlias(updateIndex, searchAlias).execute()
                    .actionGet(suggestSettings.getIndicesTimeout());
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Failed to switch index.", e);
            throw new SuggesterException("Failed to switch index.", e);
        }
    }

    public void removeDisableIndices() {
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
            if (logger.isLoggable(Level.INFO)) {
                logger.info(() -> String.format("Delete index: %s", s));
            }
            client.admin().indices().prepareDelete(s).execute().actionGet(suggestSettings.getIndicesTimeout());
        });
    }

    public SuggestIndexer indexer() {
        return createDefaultIndexer();
    }

    public static SuggesterBuilder builder() {
        return new SuggesterBuilder();
    }

    // getter
    public SuggestSettings settings() {
        return suggestSettings;
    }

    public ReadingConverter getReadingConverter() {
        return readingConverter;
    }

    public Normalizer getNormalizer() {
        return normalizer;
    }

    protected SuggestIndexer createDefaultIndexer() {
        return new SuggestIndexer(client, getUpdateAlias(index), readingConverter, contentsReadingConverter, normalizer, analyzer,
                suggestSettings, threadPool);
    }

    public String getIndex() {
        return index;
    }

    public long getAllWordsNum() {
        return getNum(QueryBuilders.matchAllQuery());
    }

    public long getDocumentWordsNum() {
        return getNum(QueryBuilders.rangeQuery(FieldNames.DOC_FREQ).gte(1));
    }

    public long getQueryWordsNum() {
        return getNum(QueryBuilders.rangeQuery(FieldNames.QUERY_FREQ).gte(1));
    }

    private long getNum(final QueryBuilder queryBuilder) {
        final SearchResponse searchResponse = client.prepareSearch().setIndices(getSearchAlias(index)).setSize(0).setQuery(queryBuilder)
                .setTrackTotalHits(true).execute().actionGet(suggestSettings.getSearchTimeout());
        return searchResponse.getHits().getTotalHits().value;
    }

    private String getSearchAlias(final String index) {
        return index;
    }

    private String getUpdateAlias(final String index) {
        return index + ".update";
    }

    private String createIndexName(final String index) {
        return index + '.' + ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String getDefaultMappings() throws IOException {
        final StringBuilder mappingSource = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                this.getClass().getClassLoader().getResourceAsStream("suggest_indices/suggest/mappings-default.json")))) {

            String line;
            while ((line = br.readLine()) != null) {
                mappingSource.append(line);
            }
        }
        return mappingSource.toString();
    }

    private String getDefaultIndexSettings() throws IOException {
        final StringBuilder settingsSource = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("suggest_indices/suggest.json")))) {
            String line;
            while ((line = br.readLine()) != null) {
                settingsSource.append(line);
            }
        }
        return settingsSource.toString();
    }

    private boolean isSuggestIndex(final String indexName) {
        return indexName.startsWith(index);
    }
}
