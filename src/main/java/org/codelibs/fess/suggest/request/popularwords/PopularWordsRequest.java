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
package org.codelibs.fess.suggest.request.popularwords;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codelibs.fess.suggest.concurrent.Deferred;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.request.Request;
import org.opensearch.action.search.SearchRequestBuilder;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.lucene.search.function.CombineFunction;
import org.opensearch.core.action.ActionListener;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.opensearch.index.query.functionscore.ScoreFunctionBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.rescore.QueryRescorerBuilder;
import org.opensearch.transport.client.Client;

/**
 * Represents a request for popular words. This class extends {@link Request} and is parameterized
 * with {@link PopularWordsResponse}. It allows specifying various criteria for retrieving popular words,
 * such as index, size, tags, roles, fields, languages, and exclusion words.
 *
 * <p>Key functionalities include:</p>
 * <ul>
 *   <li>Setting the target index for the search.</li>
 *   <li>Limiting the number of results (size).</li>
 *   <li>Filtering by tags, roles, fields, and languages.</li>
 *   <li>Excluding specific words from the results.</li>
 *   <li>Building the OpenSearch query and rescorer for the popular words search.</li>
 *   <li>Creating a {@link PopularWordsResponse} from the OpenSearch search response.</li>
 * </ul>
 *
 * @see Request
 * @see PopularWordsResponse
 */
public class PopularWordsRequest extends Request<PopularWordsResponse> {
    /**
     * Constructs a new popular words request.
     */
    public PopularWordsRequest() {
        // nothing
    }

    private String index = null;

    private int size = 10;

    private final List<String> tags = new ArrayList<>();

    private final List<String> roles = new ArrayList<>();

    private final List<String> fields = new ArrayList<>();

    private final List<String> languages = new ArrayList<>();

    private String seed = String.valueOf(System.currentTimeMillis());

    private int windowSize = 20;

    private boolean detail = true;

    private int queryFreqThreshold = 10;

    private final List<String> excludeWords = new ArrayList<>();

    /**
     * Sets the index name.
     * @param index The index name.
     */
    public void setIndex(final String index) {
        this.index = index;
    }

    /**
     * Sets the size of results.
     * @param size The size.
     */
    public void setSize(final int size) {
        this.size = size;
    }

    /**
     * Sets the seed for random function.
     * @param seed The seed.
     */
    public void setSeed(final String seed) {
        this.seed = seed;
    }

    /**
     * Sets the window size for rescoring.
     * @param windowSize The window size.
     */
    public void setWindowSize(final int windowSize) {
        this.windowSize = windowSize;
    }

    /**
     * Adds a tag to filter by.
     * @param tag The tag.
     */
    public void addTag(final String tag) {
        tags.add(tag);
    }

    /**
     * Adds a role to filter by.
     * @param role The role.
     */
    public void addRole(final String role) {
        roles.add(role);
    }

    /**
     * Adds a field to filter by.
     * @param field The field name.
     */
    public void addField(final String field) {
        fields.add(field);
    }

    /**
     * Adds a language to filter by.
     * @param lang The language.
     */
    public void addLanguage(final String lang) {
        languages.add(lang);
    }

    /**
     * Sets the detail flag.
     * @param detail The detail flag.
     */
    public void setDetail(final boolean detail) {
        this.detail = detail;
    }

    /**
     * Adds an exclude word.
     * @param excludeWord The word to exclude.
     */
    public void addExcludeWord(final String excludeWord) {
        excludeWords.add(excludeWord);
    }

    /**
     * Sets the query frequency threshold.
     * @param queryFreqThreshold The query frequency threshold.
     */
    public void setQueryFreqThreshold(final int queryFreqThreshold) {
        this.queryFreqThreshold = queryFreqThreshold;
    }

    @Override
    protected void processRequest(final Client client, final Deferred<PopularWordsResponse> deferred) {
        final SearchRequestBuilder builder = client.prepareSearch(index);
        builder.setSize(size);
        builder.setQuery(buildQuery());
        builder.setRescorer(buildRescore(), windowSize);

        builder.execute(new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(final SearchResponse searchResponse) {
                if (searchResponse.getFailedShards() > 0) {
                    deferred.reject(new SuggesterException("Search failure. Failed shards num:" + searchResponse.getFailedShards()));
                } else {
                    deferred.resolve(createResponse(searchResponse));
                }
            }

            @Override
            public void onFailure(final Exception e) {
                deferred.reject(e);
            }
        });
    }

    @Override
    protected String getValidationError() {
        return null;
    }

    /**
     * Builds the OpenSearch query for popular words.
     * @return The QueryBuilder instance.
     */
    protected QueryBuilder buildQuery() {
        final BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        queryBuilder.must(QueryBuilders.termQuery(FieldNames.KINDS, SuggestItem.Kind.QUERY.toString()));
        queryBuilder.mustNot(QueryBuilders.existsQuery(FieldNames.READING_PREFIX + "1"));
        queryBuilder.must(QueryBuilders.rangeQuery(FieldNames.QUERY_FREQ).gte(queryFreqThreshold));
        if (!tags.isEmpty()) {
            queryBuilder.must(QueryBuilders.termsQuery(FieldNames.TAGS, tags));
        }
        if (!roles.isEmpty()) {
            queryBuilder.must(QueryBuilders.termsQuery(FieldNames.ROLES, roles));
        } else {
            queryBuilder.must(QueryBuilders.termQuery(FieldNames.ROLES, SuggestConstants.DEFAULT_ROLE));
        }
        if (!fields.isEmpty()) {
            queryBuilder.must(QueryBuilders.termsQuery(FieldNames.FIELDS, fields));
        }
        if (!languages.isEmpty()) {
            queryBuilder.must(QueryBuilders.termsQuery(FieldNames.LANGUAGES, languages));
        }
        if (!excludeWords.isEmpty()) {
            queryBuilder.mustNot(QueryBuilders.termsQuery(FieldNames.TEXT, excludeWords));
        }
        final FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(queryBuilder,
                ScoreFunctionBuilders.fieldValueFactorFunction(FieldNames.QUERY_FREQ).missing(0));
        functionScoreQueryBuilder.boostMode(CombineFunction.REPLACE);
        return functionScoreQueryBuilder;
    }

    /**
     * Builds the OpenSearch rescorer for popular words.
     * @return The QueryRescorerBuilder instance.
     */
    protected QueryRescorerBuilder buildRescore() {
        return new QueryRescorerBuilder(
                QueryBuilders.functionScoreQuery(ScoreFunctionBuilders.randomFunction().seed(seed).setField("_seq_no"))).setQueryWeight(0)
                        .setRescoreQueryWeight(1);
    }

    /**
     * Creates a PopularWordsResponse from the OpenSearch SearchResponse.
     * @param searchResponse The OpenSearch SearchResponse.
     * @return A PopularWordsResponse instance.
     */
    protected PopularWordsResponse createResponse(final SearchResponse searchResponse) {
        final SearchHit[] hits = searchResponse.getHits().getHits();
        final List<String> words = new ArrayList<>();
        final List<SuggestItem> items = new ArrayList<>();

        final String index;
        if (hits.length > 0) {
            index = hits[0].getIndex();
        } else {
            index = SuggestConstants.EMPTY_STRING;
        }

        for (final SearchHit hit : hits) {
            final Map<String, Object> source = hit.getSourceAsMap();
            final String text = source.get(FieldNames.TEXT).toString();
            words.add(text);

            if (detail) {
                items.add(SuggestItem.parseSource(source));
            }
        }

        return new PopularWordsResponse(index, searchResponse.getTook().getMillis(), words, searchResponse.getHits().getTotalHits().value(),
                items);
    }
}
