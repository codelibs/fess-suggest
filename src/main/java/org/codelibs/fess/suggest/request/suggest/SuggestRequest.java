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
package org.codelibs.fess.suggest.request.suggest;

import java.util.ArrayList;
import java.util.List;

import org.codelibs.fess.suggest.concurrent.Deferred;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.request.Request;
import org.opensearch.action.search.SearchRequestBuilder;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.core.action.ActionListener;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.sort.SortOrder;
import org.opensearch.transport.client.Client;

/**
 * SuggestRequest is a class that handles the request for suggestions.
 * It extends the Request class with a SuggestResponse type.
 *
 * <p>This class provides various methods to set parameters for the suggestion request,
 * such as index, query, size, tags, roles, fields, kinds, languages, and other configurations.
 * It also includes methods to build and process the request using OpenSearch's client.</p>
 *
 * <p>Key functionalities include:</p>
 * <ul>
 *   <li>Setting the index, query, and size of the request.</li>
 *   <li>Adding tags, roles, fields, kinds, and languages to the request.</li>
 *   <li>Configuring suggestion details, reading converter, normalizer, prefix match weight, match word first, and skip duplicate words options.</li>
 *   <li>Building the query and filter query for the suggestion request.</li>
 *   <li>Processing the request and handling the response or failure.</li>
 *   <li>Creating the response from the search results.</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * SuggestRequest suggestRequest = new SuggestRequest();
 * suggestRequest.setIndex("my_index");
 * suggestRequest.setQuery("example query");
 * suggestRequest.setSize(5);
 * suggestRequest.addTag("tag1");
 * suggestRequest.addRole("role1");
 * suggestRequest.addField("field1");
 * suggestRequest.addKind("kind1");
 * suggestRequest.addLang("en");
 * suggestRequest.setSuggestDetail(true);
 * suggestRequest.setReadingConverter(new MyReadingConverter());
 * suggestRequest.setNormalizer(new MyNormalizer());
 * suggestRequest.setPrefixMatchWeight(1.5f);
 * suggestRequest.setMatchWordFirst(false);
 * suggestRequest.setSkipDuplicateWords(false);
 * </pre>
 *
 * <p>Note: This class is designed to work with OpenSearch and requires appropriate dependencies and configurations.</p>
 *
 * @see Request
 * @see SuggestResponse
 */
public class SuggestRequest extends Request<SuggestResponse> {
    /**
     * Constructs a new suggest request.
     */
    public SuggestRequest() {
        // nothing
    }

    private String index = null;

    private String query = "";

    private int size = 10;

    private final List<String> tags = new ArrayList<>();

    private final List<String> roles = new ArrayList<>();

    private final List<String> fields = new ArrayList<>();

    private final List<String> kinds = new ArrayList<>();

    private final List<String> languages = new ArrayList<>();

    private boolean suggestDetail = true;

    private ReadingConverter readingConverter;

    private Normalizer normalizer;

    private float prefixMatchWeight = 2.0f;

    private boolean matchWordFirst = true;

    private boolean skipDuplicateWords = true;

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
     * Sets the query string.
     * @param query The query string.
     */
    public void setQuery(final String query) {
        this.query = query;
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
     * Adds a kind to filter by.
     * @param kind The kind.
     */
    public void addKind(final String kind) {
        kinds.add(kind);
    }

    /**
     * Sets whether to return detailed suggestion information.
     * @param suggestDetail True to return detailed information, false otherwise.
     */
    public void setSuggestDetail(final boolean suggestDetail) {
        this.suggestDetail = suggestDetail;
    }

    /**
     * Sets the reading converter.
     * @param readingConverter The reading converter.
     */
    public void setReadingConverter(final ReadingConverter readingConverter) {
        this.readingConverter = readingConverter;
    }

    /**
     * Sets the normalizer.
     * @param normalizer The normalizer.
     */
    public void setNormalizer(final Normalizer normalizer) {
        this.normalizer = normalizer;
    }

    /**
     * Sets the prefix match weight.
     * @param prefixMatchWeight The prefix match weight.
     */
    public void setPrefixMatchWeight(final float prefixMatchWeight) {
        this.prefixMatchWeight = prefixMatchWeight;
    }

    /**
     * Sets whether to match the first word.
     * @param matchWordFirst True to match the first word, false otherwise.
     */
    public void setMatchWordFirst(final boolean matchWordFirst) {
        this.matchWordFirst = matchWordFirst;
    }

    /**
     * Sets whether to skip duplicate words.
     * @param skipDuplicateWords True to skip duplicate words, false otherwise.
     */
    public void setSkipDuplicateWords(final boolean skipDuplicateWords) {
        this.skipDuplicateWords = skipDuplicateWords;
    }

    /**
     * Adds a language to filter by.
     * @param lang The language.
     */
    public void addLang(final String lang) {
        languages.add(lang);
    }

    @Override
    protected String getValidationError() {
        return null;
    }

    // ============================================================
    // Deprecated delegate methods for backward compatibility
    // ============================================================

    /**
     * Builds the query for suggestions.
     * @param q The query string.
     * @param fieldList The fields to search in.
     * @return The QueryBuilder instance.
     * @deprecated Use {@link SuggestQueryBuilder#buildQuery(String, List)} instead.
     */
    @Deprecated
    protected QueryBuilder buildQuery(final String q, final List<String> fieldList) {
        return new SuggestQueryBuilder(readingConverter, normalizer, languages, prefixMatchWeight).buildQuery(q, fieldList);
    }

    /**
     * Builds a filter query.
     * @param fieldName The field name.
     * @param words The words to filter by.
     * @return The QueryBuilder instance.
     * @deprecated Use {@link SuggestQueryBuilder#buildFilterQuery(String, List)} instead.
     */
    @Deprecated
    protected QueryBuilder buildFilterQuery(final String fieldName, final List<String> words) {
        return new SuggestQueryBuilder(readingConverter, normalizer, languages, prefixMatchWeight).buildFilterQuery(fieldName, words);
    }

    /**
     * Builds a function score query.
     * @param q The query string.
     * @param queryBuilder The query builder.
     * @return The QueryBuilder instance.
     * @deprecated Use {@link SuggestQueryBuilder#buildFunctionScoreQuery(String, QueryBuilder)} instead.
     */
    @Deprecated
    protected QueryBuilder buildFunctionScoreQuery(final String q, final QueryBuilder queryBuilder) {
        return createOverridableQueryBuilder().buildFunctionScoreQuery(q, queryBuilder);
    }

    /**
     * Creates a SuggestResponse from the OpenSearch SearchResponse.
     * @param searchResponse The OpenSearch SearchResponse.
     * @return A SuggestResponse instance.
     * @deprecated Use {@link SuggestResponseCreator#createResponse(SearchResponse)} instead.
     */
    @Deprecated
    protected SuggestResponse createResponse(final SearchResponse searchResponse) {
        final SuggestQueryBuilder qb = createOverridableQueryBuilder();
        return new SuggestResponseCreator(query, size, suggestDetail, skipDuplicateWords, matchWordFirst, qb) {
            @Override
            protected boolean isFirstWordMatching(final boolean swq, final boolean hq, final String t) {
                return SuggestRequest.this.isFirstWordMatching(swq, hq, t);
            }
        }.createResponse(searchResponse);
    }

    /**
     * Checks if the first word matches.
     * @param singleWordQuery True if it is a single word query.
     * @param hiraganaQuery True if it is a hiragana query.
     * @param text The text to check.
     * @return True if the first word matches, false otherwise.
     * @deprecated Use {@link SuggestResponseCreator#isFirstWordMatching(boolean, boolean, String)} instead.
     */
    @Deprecated
    protected boolean isFirstWordMatching(final boolean singleWordQuery, final boolean hiraganaQuery, final String text) {
        final SuggestQueryBuilder qb = createOverridableQueryBuilder();
        return new SuggestResponseCreator(query, size, suggestDetail, skipDuplicateWords, matchWordFirst, qb)
                .isFirstWordMatching(singleWordQuery, hiraganaQuery, text);
    }

    /**
     * Checks if the query is a hiragana query.
     * @param q The query string.
     * @return True if it is a hiragana query, false otherwise.
     * @deprecated Use {@link SuggestQueryBuilder#isHiraganaQuery(String)} instead.
     */
    @Deprecated
    protected boolean isHiraganaQuery(final String q) {
        return new SuggestQueryBuilder(readingConverter, normalizer, languages, prefixMatchWeight).isHiraganaQuery(q);
    }

    /**
     * Creates a SuggestQueryBuilder that routes isHiraganaQuery calls back through this instance's
     * overridable method, preserving subclass override behavior.
     */
    private SuggestQueryBuilder createOverridableQueryBuilder() {
        return new SuggestQueryBuilder(readingConverter, normalizer, languages, prefixMatchWeight) {
            @Override
            boolean isHiraganaQuery(final String q) {
                return SuggestRequest.this.isHiraganaQuery(q);
            }
        };
    }

    @Override
    protected void processRequest(final Client client, final Deferred<SuggestResponse> deferred) {
        final SearchRequestBuilder builder = client.prepareSearch(index);

        if (skipDuplicateWords) {
            builder.setSize(size * 2);
        } else {
            builder.setSize(size);
        }

        // set query.
        final QueryBuilder q = buildQuery(query, fields);

        // set function score
        final QueryBuilder functionScoreQuery = buildFunctionScoreQuery(query, q);
        builder.addSort("_score", SortOrder.DESC);

        // set filter query.
        final List<QueryBuilder> filterList = new ArrayList<>(10);
        if (!tags.isEmpty()) {
            filterList.add(buildFilterQuery(FieldNames.TAGS, tags));
        }

        // Create a new list to avoid modifying the original roles list
        final List<String> rolesWithDefault = new ArrayList<>(roles.size() + 1);
        rolesWithDefault.addAll(roles);
        if (!rolesWithDefault.contains(SuggestConstants.DEFAULT_ROLE)) {
            rolesWithDefault.add(SuggestConstants.DEFAULT_ROLE);
        }
        filterList.add(buildFilterQuery(FieldNames.ROLES, rolesWithDefault));

        if (!fields.isEmpty()) {
            filterList.add(buildFilterQuery(FieldNames.FIELDS, fields));
        }

        if (!kinds.isEmpty()) {
            filterList.add(buildFilterQuery(FieldNames.KINDS, kinds));
        }

        if (filterList.size() > 0) {
            final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(functionScoreQuery);
            filterList.forEach(boolQueryBuilder::filter);
            builder.setQuery(boolQueryBuilder);
        } else {
            builder.setQuery(functionScoreQuery);
        }

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
                deferred.reject(new SuggesterException(e.getMessage(), e));
            }
        });
    }
}
