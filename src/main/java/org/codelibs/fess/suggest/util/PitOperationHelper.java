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
package org.codelibs.fess.suggest.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.opensearch.action.search.CreatePitAction;
import org.opensearch.action.search.CreatePitRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchRequestBuilder;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.PointInTimeBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.transport.client.Client;

/**
 * Helper class for Point in Time (PIT) based search operations in OpenSearch.
 * Centralizes the PIT and {@code search_after} logic to reduce code duplication across
 * settings and indexer classes.
 *
 * <p>This class provides methods to walk over every document matching a query with different
 * processing patterns:
 * <ul>
 * <li>{@link #search(Client, SuggestSettings, String, QueryBuilder, int, HitProcessor)} - Collects results into a list</li>
 * <li>{@link #searchWithCallback(Client, SuggestSettings, String, QueryBuilder, int, Consumer)} - Processes each hit individually</li>
 * <li>{@link #searchWithBatchCallback(Client, SuggestSettings, String, QueryBuilder, int, Consumer)} - Processes hits page by page</li>
 * </ul>
 *
 * <p>A PIT search must not carry the index names, the routing nor the preference on the search
 * request itself; they belong to the PIT creation request. {@link #createPit(Client, SuggestSettings, SearchRequest, String...)}
 * takes care of moving them, and the created context is always released in a {@code finally} block.
 */
public final class PitOperationHelper {

    private PitOperationHelper() {
        // Utility class
    }

    /**
     * Functional interface for processing search hits during PIT operations.
     *
     * @param <T> The type of item to accumulate
     */
    @FunctionalInterface
    public interface HitProcessor<T> {
        /**
         * Process a single search hit and optionally add results to the accumulator.
         *
         * @param hit The search hit to process
         * @param accumulator The list to accumulate results
         */
        void process(SearchHit hit, List<T> accumulator);
    }

    /**
     * Performs a PIT based search and collects results into a list.
     *
     * @param <T> The type of items to collect
     * @param client The OpenSearch client
     * @param settings The suggest settings containing timeout configurations
     * @param index The index name to search
     * @param query The query to execute
     * @param pageSize The number of hits per page
     * @param processor The processor to convert each hit into result items
     * @return A list of processed results
     */
    public static <T> List<T> search(final Client client, final SuggestSettings settings, final String index, final QueryBuilder query,
            final int pageSize, final HitProcessor<T> processor) {
        final List<T> results = new ArrayList<>();
        searchWithCallback(client, settings, index, query, pageSize, hit -> processor.process(hit, results));
        return results;
    }

    /**
     * Performs a PIT based search with a callback for each hit.
     *
     * @param client The OpenSearch client
     * @param settings The suggest settings containing timeout configurations
     * @param index The index name to search
     * @param query The query to execute
     * @param pageSize The number of hits per page
     * @param hitCallback The callback to process each search hit
     */
    public static void searchWithCallback(final Client client, final SuggestSettings settings, final String index, final QueryBuilder query,
            final int pageSize, final Consumer<SearchHit> hitCallback) {
        searchWithBatchCallback(client, settings, index, query, pageSize, hits -> {
            for (final SearchHit hit : hits) {
                hitCallback.accept(hit);
            }
        });
    }

    /**
     * Performs a PIT based search with a batch callback for processing hits.
     * This is useful when you need to process hits in batches (e.g. for bulk writes).
     *
     * @param client The OpenSearch client
     * @param settings The suggest settings containing timeout configurations
     * @param index The index name to search
     * @param query The query to execute
     * @param pageSize The number of hits per page
     * @param batchCallback The callback to process each page of search hits
     */
    public static void searchWithBatchCallback(final Client client, final SuggestSettings settings, final String index,
            final QueryBuilder query, final int pageSize, final Consumer<SearchHit[]> batchCallback) {
        final SearchRequestBuilder builder = client.prepareSearch().setQuery(query).setSize(pageSize);
        builder.addSort(SortBuilders.shardDocSort());

        final String pitId = createPit(client, settings, builder.request(), index);
        try {
            builder.setPointInTime(new PointInTimeBuilder(pitId).setKeepAlive(getKeepAlive(settings)));
            Object[] searchAfter = null;
            while (true) {
                if (searchAfter != null) {
                    builder.searchAfter(searchAfter);
                }
                final SearchResponse response = builder.execute().actionGet(settings.getSearchTimeout());
                final SearchHit[] hits = response.getHits().getHits();
                if (hits.length == 0) {
                    break;
                }
                batchCallback.accept(hits);
                searchAfter = hits[hits.length - 1].getSortValues();
            }
        } finally {
            SuggestUtil.deletePitContext(client, pitId);
        }
    }

    /**
     * Gets the total hit count for the given query.
     *
     * @param client The OpenSearch client
     * @param settings The suggest settings containing timeout configurations
     * @param index The index name to search
     * @param query The query to execute
     * @return The total number of hits matching the query
     */
    public static long getTotalHitCount(final Client client, final SuggestSettings settings, final String index, final QueryBuilder query) {

        final SearchResponse response =
                client.prepareSearch().setIndices(index).setQuery(query).setSize(0).execute().actionGet(settings.getSearchTimeout());

        return response.getHits().getTotalHits().value();
    }

    /**
     * Returns the keep alive of a PIT context. The value is configured as the scroll timeout for
     * historical reasons.
     *
     * @param settings The suggest settings containing timeout configurations
     * @return The keep alive of a PIT context
     */
    public static TimeValue getKeepAlive(final SuggestSettings settings) {
        return TimeValue.parseTimeValue(settings.getScrollTimeout(), "keepAlive");
    }

    /**
     * Creates a PIT context for the given indices.
     *
     * <p>OpenSearch rejects a PIT search that carries index names, routing or preference, so the
     * routing and the preference are moved from the given search request to the PIT creation
     * request. The caller must not set the indices on the search request at all.
     *
     * @param client The OpenSearch client
     * @param settings The suggest settings containing timeout configurations
     * @param request The search request the PIT context is created for, or null
     * @param indices The indices the PIT context covers
     * @return The created PIT ID
     */
    public static String createPit(final Client client, final SuggestSettings settings, final SearchRequest request,
            final String... indices) {
        final CreatePitRequest createPitRequest = new CreatePitRequest(getKeepAlive(settings), true, indices);
        if (request != null) {
            if (request.preference() != null) {
                createPitRequest.setPreference(request.preference());
                request.preference(null);
            }
            if (request.routing() != null) {
                createPitRequest.setRouting(request.routing());
                request.routing((String) null);
            }
        }
        return client.execute(CreatePitAction.INSTANCE, createPitRequest).actionGet(settings.getSearchTimeout()).getId();
    }
}
