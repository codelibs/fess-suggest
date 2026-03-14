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
import java.util.ArrayList;
import java.util.List;

import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.opensearch.common.lucene.search.function.CombineFunction;
import org.opensearch.common.lucene.search.function.FieldValueFactorFunction;
import org.opensearch.common.lucene.search.function.FunctionScoreQuery;
import org.opensearch.core.common.Strings;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.opensearch.index.query.functionscore.ScoreFunctionBuilders;

/**
 * Builds OpenSearch queries for suggestion requests.
 *
 * <p>This class encapsulates the query building logic extracted from SuggestRequest,
 * including reading conversion, normalization, and function score query construction.</p>
 */
public class SuggestQueryBuilder {

    private final ReadingConverter readingConverter;
    private final Normalizer normalizer;
    private final List<String> languages;
    private final float prefixMatchWeight;

    /**
     * Constructs a new SuggestQueryBuilder.
     *
     * @param readingConverter The reading converter.
     * @param normalizer The normalizer.
     * @param languages The list of languages.
     * @param prefixMatchWeight The prefix match weight.
     */
    public SuggestQueryBuilder(final ReadingConverter readingConverter, final Normalizer normalizer, final List<String> languages,
            final float prefixMatchWeight) {
        this.readingConverter = readingConverter;
        this.normalizer = normalizer;
        this.languages = languages;
        this.prefixMatchWeight = prefixMatchWeight;
    }

    /**
     * Builds the query for suggestions.
     * @param q The query string.
     * @param fields The fields to search in.
     * @return The QueryBuilder instance.
     */
    public QueryBuilder buildQuery(final String q, final List<String> fields) {
        try {
            final QueryBuilder queryBuilder;
            if (Strings.isNullOrEmpty(q)) {
                queryBuilder = QueryBuilders.matchAllQuery();
            } else {
                final String fullWidthSpace = "\u3000";
                final boolean prefixQuery = !q.endsWith(" ") && !q.endsWith(fullWidthSpace);
                List<String> readingList = new ArrayList<>();

                final String[] langsArray = languages.toArray(new String[languages.size()]);

                final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
                final String[] queries = q.replace(fullWidthSpace, " ").replaceAll(" +", " ").trim().split(" ");
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
    public QueryBuilder buildFilterQuery(final String fieldName, final List<String> words) {
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
    public QueryBuilder buildFunctionScoreQuery(final String query, final QueryBuilder queryBuilder) {

        final List<FunctionScoreQueryBuilder.FilterFunctionBuilder> flist = new ArrayList<>();

        if (isSingleWordQuery(query) && !isHiraganaQuery(query)) {
            flist.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(QueryBuilders.prefixQuery(FieldNames.TEXT, query),
                    ScoreFunctionBuilders.weightFactorFunction(prefixMatchWeight)));
        }

        flist.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(ScoreFunctionBuilders.fieldValueFactorFunction(FieldNames.DOC_FREQ)
                .missing(0.1f)
                .modifier(FieldValueFactorFunction.Modifier.LOG2P)
                .setWeight(1.0F)));
        flist.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(ScoreFunctionBuilders.fieldValueFactorFunction(FieldNames.QUERY_FREQ)
                .missing(0.1f)
                .modifier(FieldValueFactorFunction.Modifier.LOG2P)
                .setWeight(1.0F)));
        flist.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                ScoreFunctionBuilders.fieldValueFactorFunction(FieldNames.USER_BOOST).missing(1f).setWeight(1.0F)));
        final FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(queryBuilder,
                flist.toArray(new FunctionScoreQueryBuilder.FilterFunctionBuilder[flist.size()]));

        functionScoreQueryBuilder.boostMode(CombineFunction.REPLACE);
        functionScoreQueryBuilder.scoreMode(FunctionScoreQuery.ScoreMode.MULTIPLY);

        return functionScoreQueryBuilder;
    }

    /**
     * Checks if the query is a single word query.
     * @param query The query string.
     * @return True if it is a single word query, false otherwise.
     */
    boolean isSingleWordQuery(final String query) {
        final String fullWidthSpace = "\u3000";
        return !Strings.isNullOrEmpty(query) && !query.contains(" ") && !query.contains(fullWidthSpace);
    }

    /**
     * Checks if the query is a hiragana query.
     * @param query The query string.
     * @return True if it is a hiragana query, false otherwise.
     */
    boolean isHiraganaQuery(final String query) {
        return query.matches("^[\\u3040-\\u309F]+$");
    }
}
