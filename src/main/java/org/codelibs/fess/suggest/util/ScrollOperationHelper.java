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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.transport.client.Client;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.SearchHit;

/**
 * Helper class for scroll-based search operations in OpenSearch.
 * Centralizes scroll logic to reduce code duplication across settings and indexer classes.
 *
 * <p>This class provides methods to perform scroll-based searches with different processing patterns:
 * <ul>
 * <li>{@link #scrollSearch(Client, SuggestSettings, String, QueryBuilder, int, HitProcessor)} - Collects results into a list</li>
 * <li>{@link #scrollSearchWithCallback(Client, SuggestSettings, String, QueryBuilder, int, Consumer)} - Processes each hit individually</li>
 * <li>{@link #scrollSearchWithBatchCallback(Client, SuggestSettings, String, QueryBuilder, int, BiConsumer)} - Processes hits in batches</li>
 * </ul>
 */
public final class ScrollOperationHelper {

    private ScrollOperationHelper() {
        // Utility class
    }

    /**
     * Functional interface for processing search hits during scroll operations.
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
     * Performs a scroll-based search and collects results into a list.
     *
     * @param <T> The type of items to collect
     * @param client The OpenSearch client
     * @param settings The suggest settings containing timeout configurations
     * @param index The index name to search
     * @param query The query to execute
     * @param pageSize The number of hits per scroll page
     * @param processor The processor to convert each hit into result items
     * @return A list of processed results
     */
    public static <T> List<T> scrollSearch(final Client client, final SuggestSettings settings, final String index,
            final QueryBuilder query, final int pageSize, final HitProcessor<T> processor) {

        final List<T> results = new ArrayList<>();

        SearchResponse response = client.prepareSearch()
                .setIndices(index)
                .setScroll(settings.getScrollTimeout())
                .setQuery(query)
                .setSize(pageSize)
                .execute()
                .actionGet(settings.getSearchTimeout());
        String scrollId = response.getScrollId();

        try {
            while (scrollId != null) {
                final SearchHit[] hits = response.getHits().getHits();
                if (hits.length == 0) {
                    break;
                }
                for (final SearchHit hit : hits) {
                    processor.process(hit, results);
                }
                response = client.prepareSearchScroll(scrollId)
                        .setScroll(settings.getScrollTimeout())
                        .execute()
                        .actionGet(settings.getSearchTimeout());
                if (!scrollId.equals(response.getScrollId())) {
                    SuggestUtil.deleteScrollContext(client, scrollId);
                }
                scrollId = response.getScrollId();
            }
        } finally {
            SuggestUtil.deleteScrollContext(client, scrollId);
        }

        return results;
    }

    /**
     * Performs a scroll-based search with a callback for each hit.
     *
     * @param client The OpenSearch client
     * @param settings The suggest settings containing timeout configurations
     * @param index The index name to search
     * @param query The query to execute
     * @param pageSize The number of hits per scroll page
     * @param hitCallback The callback to process each search hit
     */
    public static void scrollSearchWithCallback(final Client client, final SuggestSettings settings, final String index,
            final QueryBuilder query, final int pageSize, final Consumer<SearchHit> hitCallback) {

        SearchResponse response = client.prepareSearch()
                .setIndices(index)
                .setScroll(settings.getScrollTimeout())
                .setQuery(query)
                .setSize(pageSize)
                .execute()
                .actionGet(settings.getSearchTimeout());
        String scrollId = response.getScrollId();

        try {
            while (scrollId != null) {
                final SearchHit[] hits = response.getHits().getHits();
                if (hits.length == 0) {
                    break;
                }
                for (final SearchHit hit : hits) {
                    hitCallback.accept(hit);
                }
                response = client.prepareSearchScroll(scrollId)
                        .setScroll(settings.getScrollTimeout())
                        .execute()
                        .actionGet(settings.getSearchTimeout());
                if (!scrollId.equals(response.getScrollId())) {
                    SuggestUtil.deleteScrollContext(client, scrollId);
                }
                scrollId = response.getScrollId();
            }
        } finally {
            SuggestUtil.deleteScrollContext(client, scrollId);
        }
    }

    /**
     * Performs a scroll-based search with a batch callback for processing hits.
     * This is useful when you need to process hits in batches (e.g., for bulk writes).
     *
     * @param client The OpenSearch client
     * @param settings The suggest settings containing timeout configurations
     * @param index The index name to search
     * @param query The query to execute
     * @param pageSize The number of hits per scroll page
     * @param batchCallback The callback to process each batch of search hits.
     *        The first parameter is the array of hits, the second is a continuation flag
     *        that can be set to false to stop scrolling.
     */
    public static void scrollSearchWithBatchCallback(final Client client, final SuggestSettings settings, final String index,
            final QueryBuilder query, final int pageSize, final BiConsumer<SearchHit[], StopFlag> batchCallback) {

        SearchResponse response = client.prepareSearch()
                .setIndices(index)
                .setScroll(settings.getScrollTimeout())
                .setQuery(query)
                .setSize(pageSize)
                .execute()
                .actionGet(settings.getSearchTimeout());
        String scrollId = response.getScrollId();
        final StopFlag stopFlag = new StopFlag();

        try {
            while (scrollId != null && !stopFlag.isStopped()) {
                final SearchHit[] hits = response.getHits().getHits();
                if (hits.length == 0) {
                    break;
                }
                batchCallback.accept(hits, stopFlag);
                if (stopFlag.isStopped()) {
                    break;
                }
                response = client.prepareSearchScroll(scrollId)
                        .setScroll(settings.getScrollTimeout())
                        .execute()
                        .actionGet(settings.getSearchTimeout());
                if (!scrollId.equals(response.getScrollId())) {
                    SuggestUtil.deleteScrollContext(client, scrollId);
                }
                scrollId = response.getScrollId();
            }
        } finally {
            SuggestUtil.deleteScrollContext(client, scrollId);
        }
    }

    /**
     * Gets the total hit count from a scroll search.
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
     * A mutable flag to signal stopping a scroll operation.
     */
    public static class StopFlag {
        private boolean stopped = false;

        /**
         * Constructs a new StopFlag with stopped set to false.
         */
        public StopFlag() {
            // Default constructor
        }

        /**
         * Signal to stop the scroll operation.
         */
        public void stop() {
            stopped = true;
        }

        /**
         * Check if the stop signal has been set.
         * @return true if stopped, false otherwise
         */
        public boolean isStopped() {
            return stopped;
        }
    }
}
