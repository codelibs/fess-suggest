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

import java.io.IOException;
import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codelibs.fess.suggest.concurrent.Deferred;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.request.Request;
import org.opensearch.action.search.SearchRequestBuilder;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.lucene.search.function.CombineFunction;
import org.opensearch.common.lucene.search.function.FieldValueFactorFunction;
import org.opensearch.common.lucene.search.function.FunctionScoreQuery;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.common.Strings;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.opensearch.index.query.functionscore.ScoreFunctionBuilders;
import org.opensearch.search.SearchHit;
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
        final QueryBuilder queryBuilder = buildFunctionScoreQuery(query, q);
        builder.addSort("_score", SortOrder.DESC);

        // set filter query.
        final List<QueryBuilder> filterList = new ArrayList<>(10);
        if (!tags.isEmpty()) {
            filterList.add(buildFilterQuery(FieldNames.TAGS, tags));
        }

        roles.add(SuggestConstants.DEFAULT_ROLE);
        if (!roles.isEmpty()) {
            filterList.add(buildFilterQuery(FieldNames.ROLES, roles));
        }

        if (!fields.isEmpty()) {
            filterList.add(buildFilterQuery(FieldNames.FIELDS, fields));
        }

        if (!kinds.isEmpty()) {
            filterList.add(buildFilterQuery(FieldNames.KINDS, kinds));
        }

        if (filterList.size() > 0) {
            final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(queryBuilder);
            filterList.forEach(boolQueryBuilder::filter);
            builder.setQuery(boolQueryBuilder);
        } else {
            builder.setQuery(queryBuilder);
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

    private boolean isSingleWordQuery(final String query) {
        return !Strings.isNullOrEmpty(query) && !query.contains(" ") && !query.contains("　");
    }

    /**
     * Builds the query for suggestions.
     * @param q The query string.
     * @param fields The fields to search in.
     * @return The QueryBuilder instance.
     */
    protected QueryBuilder buildQuery(final String q, final List<String> fields) {
        try {
            final QueryBuilder queryBuilder;
            if (Strings.isNullOrEmpty(q)) {
                queryBuilder = QueryBuilders.matchAllQuery();
            } else {
                final boolean prefixQuery = !q.endsWith(" ") && !q.endsWith("　");
                List<String> readingList = new ArrayList<>();

                final String[] langsArray = languages.toArray(new String[languages.size()]);

                final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
                final String[] queries = q.replace("　", " ").replaceAll(" +", " ").trim().split(" ");
                for (int i = 0; i < queries.length; i++) {
                    final String fieldName = FieldNames.READING_PREFIX + i;

                    final String query;
                    if (normalizer == null) {
                        query = queries[i];
                    } else {
                        query = normalizer.normalize(queries[i], "", langsArray);
                    }

                    if (readingConverter == null) {
                        readingList.add(query);
                    } else {
                        readingList = readingConverter.convert(query, "", langsArray);
                    }

                    final BoolQueryBuilder readingQueryBuilder = QueryBuilders.boolQuery().minimumShouldMatch(1);
                    final int readingNum = readingList.size();
                    for (int readingCount = 0; readingCount < readingNum; readingCount++) {
                        final String reading = readingList.get(readingCount);
                        if (i + 1 == queries.length && prefixQuery) {
                            readingQueryBuilder.should(QueryBuilders.prefixQuery(fieldName, reading));
                        } else {
                            readingQueryBuilder.should(QueryBuilders.termQuery(fieldName, reading));
                        }
                    }
                    readingList.clear();
                    boolQueryBuilder.must(readingQueryBuilder);
                }
                queryBuilder = boolQueryBuilder;
            }

            return queryBuilder;
        } catch (final IOException e) {
            throw new SuggesterException("Failed to create queryString.", e);
        }
    }

    /**
     * Builds a filter query.
     * @param fieldName The field name.
     * @param words The words to filter by.
     * @return The QueryBuilder instance.
     */
    protected QueryBuilder buildFilterQuery(final String fieldName, final List<String> words) {
        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery().minimumShouldMatch(1);
        words.stream().forEach(word -> boolQueryBuilder.should(QueryBuilders.termQuery(fieldName, word)));
        return boolQueryBuilder;
    }

    /**
     * Builds a function score query.
     * @param query The query string.
     * @param queryBuilder The query builder.
     * @return The QueryBuilder instance.
     */
    protected QueryBuilder buildFunctionScoreQuery(final String query, final QueryBuilder queryBuilder) {

        final List<FunctionScoreQueryBuilder.FilterFunctionBuilder> flist = new ArrayList<>();

        if (isSingleWordQuery(query) && !isHiraganaQuery(query)) {
            flist.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(QueryBuilders.prefixQuery(FieldNames.TEXT, query),
                    ScoreFunctionBuilders.weightFactorFunction(prefixMatchWeight)));
        }

        flist.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(ScoreFunctionBuilders.fieldValueFactorFunction(FieldNames.DOC_FREQ)
                .missing(0.1f).modifier(FieldValueFactorFunction.Modifier.LOG2P).setWeight(1.0F)));
        flist.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(ScoreFunctionBuilders.fieldValueFactorFunction(FieldNames.QUERY_FREQ)
                .missing(0.1f).modifier(FieldValueFactorFunction.Modifier.LOG2P).setWeight(1.0F)));
        flist.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                ScoreFunctionBuilders.fieldValueFactorFunction(FieldNames.USER_BOOST).missing(1f).setWeight(1.0F)));
        final FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(queryBuilder,
                flist.toArray(new FunctionScoreQueryBuilder.FilterFunctionBuilder[flist.size()]));

        functionScoreQueryBuilder.boostMode(CombineFunction.REPLACE);
        functionScoreQueryBuilder.scoreMode(FunctionScoreQuery.ScoreMode.MULTIPLY);

        return functionScoreQueryBuilder;
    }

    /**
     * Creates a SuggestResponse from the OpenSearch SearchResponse.
     * @param searchResponse The OpenSearch SearchResponse.
     * @return A SuggestResponse instance.
     */
    protected SuggestResponse createResponse(final SearchResponse searchResponse) {
        final SearchHit[] hits = searchResponse.getHits().getHits();
        final List<String> words = new ArrayList<>();
        final List<String> firstWords = new ArrayList<>();
        final List<String> secondWords = new ArrayList<>();
        final List<SuggestItem> firstItems = new ArrayList<>();
        final List<SuggestItem> secondItems = new ArrayList<>();

        final String index;
        if (hits.length > 0) {
            index = hits[0].getIndex();
        } else {
            index = SuggestConstants.EMPTY_STRING;
        }

        final boolean singleWordQuery = isSingleWordQuery(query);
        final boolean hiraganaQuery = isHiraganaQuery(query);
        for (int i = 0; i < hits.length && words.size() < size; i++) {
            final SearchHit hit = hits[i];

            final Map<String, Object> source = hit.getSourceAsMap();
            final String text = source.get(FieldNames.TEXT).toString();
            if (skipDuplicateWords) {
                final String duplicateCheckStr = text.replace(" ", "");
                if (words.stream().map(word -> word.replace(" ", "")).anyMatch(word -> word.equals(duplicateCheckStr))) {
                    // skip duplicate word.
                    continue;
                }
            }

            words.add(text);
            final boolean isFirstWords = isFirstWordMatching(singleWordQuery, hiraganaQuery, text);
            if (isFirstWords) {
                firstWords.add(text);
            } else {
                secondWords.add(text);
            }

            if (suggestDetail) {
                final SuggestItem item = SuggestItem.parseSource(source);
                if (isFirstWords) {
                    firstItems.add(item);
                } else {
                    secondItems.add(item);
                }
            }
        }
        firstWords.addAll(secondWords);
        firstItems.addAll(secondItems);
        return new SuggestResponse(index, searchResponse.getTook().getMillis(), firstWords, searchResponse.getHits().getTotalHits().value(),
                firstItems);
    }

    /**
     * Checks if the first word matches.
     * @param singleWordQuery True if it is a single word query.
     * @param hiraganaQuery True if it is a hiragana query.
     * @param text The text to check.
     * @return True if the first word matches, false otherwise.
     */
    protected boolean isFirstWordMatching(final boolean singleWordQuery, final boolean hiraganaQuery, final String text) {
        if (matchWordFirst && !hiraganaQuery && singleWordQuery && text.contains(query)) {
            if (query.length() == 1) {
                return UnicodeBlock.of(query.charAt(0)) != UnicodeBlock.HIRAGANA;
            }
            return true;
        }
        return false;
    }

    /**
     * Checks if the query is a hiragana query.
     * @param query The query string.
     * @return True if it is a hiragana query, false otherwise.
     */
    protected boolean isHiraganaQuery(final String query) {
        return query.matches("^[\\u3040-\\u309F]+$");
    }
}
