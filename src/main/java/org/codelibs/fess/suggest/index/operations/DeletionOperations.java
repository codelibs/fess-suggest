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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggestIndexException;
import org.codelibs.fess.suggest.index.SuggestDeleteResponse;
import org.codelibs.fess.suggest.index.writer.SuggestWriter;
import org.codelibs.fess.suggest.index.writer.SuggestWriterResult;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.transport.client.Client;

/**
 * Internal operations class for deletion functionality.
 * Handles deletion of suggest items by ID, query, or kind.
 *
 * <p>This class is package-private and intended for internal use by SuggestIndexer.
 */
public class DeletionOperations {

    private static final Logger logger = LogManager.getLogger(DeletionOperations.class);

    /** Default page size for scroll operations. */
    private static final int SCROLL_PAGE_SIZE = 500;

    private final Client client;
    private final SuggestSettings settings;
    private final SuggestWriter suggestWriter;

    /**
     * Constructor.
     *
     * @param client The OpenSearch client
     * @param settings The suggest settings
     * @param suggestWriter The suggest writer for performing deletions
     */
    public DeletionOperations(final Client client, final SuggestSettings settings, final SuggestWriter suggestWriter) {
        this.client = client;
        this.settings = settings;
        this.suggestWriter = suggestWriter;
    }

    /**
     * Deletes a suggest item by ID.
     *
     * @param index The index name
     * @param id The ID of the item to delete
     * @return The SuggestDeleteResponse
     */
    public SuggestDeleteResponse delete(final String index, final String id) {
        if (logger.isDebugEnabled()) {
            logger.debug("Deleting suggest item: index={}, id={}", index, id);
        }
        final long start = System.currentTimeMillis();
        final SuggestWriterResult result = suggestWriter.delete(client, settings, index, id);
        return new SuggestDeleteResponse(result.getFailures(), System.currentTimeMillis() - start);
    }

    /**
     * Deletes suggest items by a query string.
     *
     * @param index The index name
     * @param queryString The query string
     * @return The SuggestDeleteResponse
     */
    public SuggestDeleteResponse deleteByQuery(final String index, final String queryString) {
        return deleteByQuery(index, QueryBuilders.queryStringQuery(queryString).defaultOperator(Operator.AND));
    }

    /**
     * Deletes suggest items by a query builder.
     *
     * @param index The index name
     * @param queryBuilder The query builder
     * @return The SuggestDeleteResponse
     */
    public SuggestDeleteResponse deleteByQuery(final String index, final QueryBuilder queryBuilder) {
        final long start = System.currentTimeMillis();
        final SuggestWriterResult result = suggestWriter.deleteByQuery(client, settings, index, queryBuilder);
        return new SuggestDeleteResponse(result.getFailures(), System.currentTimeMillis() - start);
    }

    /**
     * Deletes all suggest items.
     *
     * @param index The index name
     * @return The SuggestDeleteResponse
     */
    public SuggestDeleteResponse deleteAll(final String index) {
        if (logger.isInfoEnabled()) {
            logger.info("Deleting all suggest items: index={}", index);
        }
        return deleteByQuery(index, QueryBuilders.matchAllQuery());
    }

    /**
     * Deletes document words (words with DOC_FREQ >= 1 that don't have QUERY or USER kinds).
     *
     * @param index The index name
     * @return The SuggestDeleteResponse
     */
    public SuggestDeleteResponse deleteDocumentWords(final String index) {
        if (logger.isInfoEnabled()) {
            logger.info("Deleting document words: index={}", index);
        }
        return deleteWordsByKind(index, FieldNames.DOC_FREQ, SuggestItem.Kind.DOCUMENT, item -> item.setDocFreq(0), SuggestItem.Kind.QUERY,
                SuggestItem.Kind.USER);
    }

    /**
     * Deletes query words (words with QUERY_FREQ >= 1 that don't have DOCUMENT or USER kinds).
     *
     * @param index The index name
     * @return The SuggestDeleteResponse
     */
    public SuggestDeleteResponse deleteQueryWords(final String index) {
        if (logger.isInfoEnabled()) {
            logger.info("Deleting query words: index={}", index);
        }
        return deleteWordsByKind(index, FieldNames.QUERY_FREQ, SuggestItem.Kind.QUERY, item -> item.setQueryFreq(0),
                SuggestItem.Kind.DOCUMENT, SuggestItem.Kind.USER);
    }

    /**
     * Common method to delete words by kind.
     *
     * @param index The index name
     * @param freqField The frequency field name
     * @param kindToRemove The kind to remove
     * @param freqSetter The consumer to set frequency to 0
     * @param excludeKinds The kinds to exclude from deletion
     * @return The SuggestDeleteResponse
     */
    public SuggestDeleteResponse deleteWordsByKind(final String index, final String freqField, final SuggestItem.Kind kindToRemove,
            final Consumer<SuggestItem> freqSetter, final SuggestItem.Kind... excludeKinds) {
        final long start = System.currentTimeMillis();

        // Build query to exclude certain kinds
        final BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery(freqField).gte(1));
        for (final SuggestItem.Kind kind : excludeKinds) {
            boolQuery.mustNot(QueryBuilders.matchPhraseQuery(FieldNames.KINDS, kind.toString()));
        }

        final SuggestDeleteResponse deleteResponse = deleteByQuery(index, boolQuery);
        if (deleteResponse.hasError()) {
            throw new SuggestIndexException(deleteResponse.getErrors().get(0));
        }

        final List<SuggestItem> updateItems = new ArrayList<>();
        SearchResponse response = client.prepareSearch(index)
                .setSize(SCROLL_PAGE_SIZE)
                .setScroll(settings.getScrollTimeout())
                .setQuery(QueryBuilders.rangeQuery(freqField).gte(1))
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
                    final SuggestItem item = SuggestItem.parseSource(hit.getSourceAsMap());
                    freqSetter.accept(item);
                    item.setKinds(Stream.of(item.getKinds()).filter(kind -> kind != kindToRemove).toArray(SuggestItem.Kind[]::new));
                    updateItems.add(item);
                }
                final SuggestWriterResult result =
                        suggestWriter.write(client, settings, index, updateItems.toArray(new SuggestItem[updateItems.size()]), false);
                if (result.hasFailure()) {
                    throw new SuggestIndexException(result.getFailures().get(0));
                }
                updateItems.clear();

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

    /**
     * Deletes old words based on a threshold date.
     *
     * @param index The index name
     * @param threshold The threshold date
     * @return The SuggestDeleteResponse
     */
    public SuggestDeleteResponse deleteOldWords(final String index, final ZonedDateTime threshold) {
        if (logger.isInfoEnabled()) {
            logger.info("Deleting old words: index={}, threshold={}", index, threshold);
        }
        final long start = System.currentTimeMillis();
        final String query = FieldNames.TIMESTAMP + ":[* TO " + threshold.toInstant().toEpochMilli() + "] NOT " + FieldNames.KINDS + ':'
                + SuggestItem.Kind.USER;
        deleteByQuery(index, query);
        return new SuggestDeleteResponse(null, System.currentTimeMillis() - start);
    }
}
