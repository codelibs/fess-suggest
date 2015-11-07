package org.codelibs.fess.suggest.request.popularwords;

import com.google.common.base.Strings;
import org.codelibs.fess.suggest.concurrent.Deferred;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.request.Request;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.rescore.RescoreBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PopularWordsRequest extends Request<PopularWordsResponse> {
    private String index = null;

    private String type = null;

    private int size = 10;

    private final List<String> tags = new ArrayList<>();

    private final List<String> roles = new ArrayList<>();

    private final List<String> fields = new ArrayList<>();

    private String seed = String.valueOf(System.currentTimeMillis());

    private int windowSize = 20;

    private boolean detail = true;

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

    public void setDetail(final boolean detail) {
        this.detail = detail;
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
            public void onFailure(final Throwable e) {
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
        queryBuilder.must(QueryBuilders.matchAllQuery());
        queryBuilder.must(QueryBuilders.missingQuery(FieldNames.READING_PREFIX + 1));
        if (tags.size() > 0) {
            queryBuilder.must(QueryBuilders.termsQuery(FieldNames.TAGS, tags));
        }
        if (roles.size() > 0) {
            queryBuilder.must(QueryBuilders.termsQuery(FieldNames.ROLES, roles));
        } else {
            queryBuilder.must(QueryBuilders.termQuery(FieldNames.ROLES, SuggestConstants.DEFAULT_ROLE));
        }
        if (fields.size() > 0) {
            queryBuilder.must(QueryBuilders.termsQuery(FieldNames.FIELDS, fields));
        }

        final BoolQueryBuilder filterBuilder = QueryBuilders.boolQuery();
        filterBuilder.filter(QueryBuilders.missingQuery(FieldNames.READING_PREFIX + 1));

        final FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(queryBuilder);
        functionScoreQueryBuilder.boostMode(CombineFunction.REPLACE).add(
                ScoreFunctionBuilders.fieldValueFactorFunction(FieldNames.QUERY_FREQ));

        return functionScoreQueryBuilder;
    }

    protected RescoreBuilder.QueryRescorer buildRescore() {
        return RescoreBuilder.queryRescorer(QueryBuilders.functionScoreQuery().add(ScoreFunctionBuilders.randomFunction(seed)))
                .setQueryWeight(0).setRescoreQueryWeight(1);
    }

    protected PopularWordsResponse createResponse(final SearchResponse searchResponse) {
        final SearchHit[] hits = searchResponse.getHits().getHits();
        final List<String> words = new ArrayList<>();
        final List<SuggestItem> items = new ArrayList<>();

        final String index;
        if (hits.length > 0) {
            index = hits[0].index();
        } else {
            index = SuggestConstants.EMPTY_STRING;
        }

        for (final SearchHit hit : hits) {
            final Map<String, Object> source = hit.sourceAsMap();
            final String text = source.get(FieldNames.TEXT).toString();
            words.add(text);

            if (detail) {
                int readingCount = 0;
                Object readingObj;
                final List<String[]> readings = new ArrayList<>();
                while ((readingObj = source.get(FieldNames.READING_PREFIX + readingCount++)) != null) {
                    final List<String> reading = SuggestUtil.getAsList(readingObj);
                    readings.add(reading.toArray(new String[reading.size()]));
                }

                final List<String> fields = SuggestUtil.getAsList(source.get(FieldNames.FIELDS));
                final List<String> tags = SuggestUtil.getAsList(source.get(FieldNames.TAGS));
                final List<String> roles = SuggestUtil.getAsList(source.get(FieldNames.ROLES));
                final List<String> kinds = SuggestUtil.getAsList(source.get(FieldNames.KINDS));
                SuggestItem.Kind kind;
                long freq;
                if (SuggestItem.Kind.USER.toString().equals(kinds.get(0))) {
                    kind = SuggestItem.Kind.USER;
                    freq = 0;
                } else if (SuggestItem.Kind.QUERY.toString().equals(kinds.get(0))) {
                    kind = SuggestItem.Kind.QUERY;
                    freq = Long.parseLong(source.get(FieldNames.QUERY_FREQ).toString());
                } else {
                    kind = SuggestItem.Kind.DOCUMENT;
                    freq = Long.parseLong(source.get(FieldNames.DOC_FREQ).toString());
                }

                items.add(new SuggestItem(text.split(" "), readings.toArray(new String[readings.size()][]), fields
                        .toArray(new String[fields.size()]), freq, Float.valueOf(source.get(FieldNames.USER_BOOST).toString()), tags
                        .toArray(new String[tags.size()]), roles.toArray(new String[tags.size()]), kind));
            }
        }

        return new PopularWordsResponse(index, searchResponse.getTookInMillis(), words, searchResponse.getHits().totalHits(), items);
    }
}
