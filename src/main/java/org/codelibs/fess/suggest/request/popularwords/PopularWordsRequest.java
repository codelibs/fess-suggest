/*
 * Copyright 2012-2021 CodeLibs Project and the Others.
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

import org.codelibs.fesen.action.ActionListener;
import org.codelibs.fesen.action.search.SearchRequestBuilder;
import org.codelibs.fesen.action.search.SearchResponse;
import org.codelibs.fesen.client.Client;
import org.codelibs.fesen.common.Strings;
import org.codelibs.fesen.common.lucene.search.function.CombineFunction;
import org.codelibs.fesen.index.query.BoolQueryBuilder;
import org.codelibs.fesen.index.query.QueryBuilder;
import org.codelibs.fesen.index.query.QueryBuilders;
import org.codelibs.fesen.index.query.functionscore.FunctionScoreQueryBuilder;
import org.codelibs.fesen.index.query.functionscore.ScoreFunctionBuilders;
import org.codelibs.fesen.search.SearchHit;
import org.codelibs.fesen.search.rescore.QueryRescorerBuilder;
import org.codelibs.fess.suggest.concurrent.Deferred;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.request.Request;

public class PopularWordsRequest extends Request<PopularWordsResponse> {
    private String index = null;

    private String type = null;

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

    public void setIndex(final String index) {
        this.index = index;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public void setSize(final int size) {
        this.size = size;
    }

    public void setSeed(final String seed) {
        this.seed = seed;
    }

    public void setWindowSize(final int windowSize) {
        this.windowSize = windowSize;
    }

    public void addTag(final String tag) {
        this.tags.add(tag);
    }

    public void addRole(final String role) {
        this.roles.add(role);
    }

    public void addField(final String field) {
        this.fields.add(field);
    }

    public void addLanguage(final String lang) {
        this.languages.add(lang);
    }

    public void setDetail(final boolean detail) {
        this.detail = detail;
    }

    public void addExcludeWord(final String excludeWord) {
        this.excludeWords.add(excludeWord);
    }

    public void setQueryFreqThreshold(final int queryFreqThreshold) {
        this.queryFreqThreshold = queryFreqThreshold;
    }

    @Override
    protected void processRequest(final Client client, final Deferred<PopularWordsResponse> deferred) {
        final SearchRequestBuilder builder = client.prepareSearch(index);
        if (!Strings.isNullOrEmpty(type)) {
            builder.setTypes(type);
        }
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

    protected QueryRescorerBuilder buildRescore() {
        return new QueryRescorerBuilder(
                QueryBuilders.functionScoreQuery(ScoreFunctionBuilders.randomFunction().seed(seed).setField("_seq_no"))).setQueryWeight(0)
                        .setRescoreQueryWeight(1);
    }

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

        return new PopularWordsResponse(index, searchResponse.getTook().getMillis(), words, searchResponse.getHits().getTotalHits().value,
                items);
    }
}
