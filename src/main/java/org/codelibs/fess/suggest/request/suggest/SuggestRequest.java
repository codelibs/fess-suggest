package org.codelibs.fess.suggest.request.suggest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import org.codelibs.fess.suggest.concurrent.Deferred;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.request.Request;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

public class SuggestRequest extends Request<SuggestResponse> {
    private String index = null;

    private String type = null;

    private String query = null;

    private int size = 10;

    private final List<String> tags = new ArrayList<>();

    private final List<String> roles = new ArrayList<>();

    private final List<String> fields = new ArrayList<>();

    private final List<String> kinds = new ArrayList<>();

    private boolean suggestDetail = true;

    private ReadingConverter readingConverter;

    private Normalizer normalizer;

    public void setIndex(final String index) {
        this.index = index;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public void setSize(final int size) {
        this.size = size;
    }

    public void setQuery(final String query) {
        this.query = query;
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

    public void addKind(final String kind) {
        this.kinds.add(kind);
    }

    public void setSuggestDetail(final boolean suggestDetail) {
        this.suggestDetail = suggestDetail;
    }

    public void setReadingConverter(final ReadingConverter readingConverter) {
        this.readingConverter = readingConverter;
    }

    public void setNormalizer(final Normalizer normalizer) {
        this.normalizer = normalizer;
    }

    @Override
    protected String getValidationError() {
        return null;
    }

    @Override
    protected void processRequest(final Client client, final Deferred<SuggestResponse> deferred) {
        final SearchRequestBuilder builder = client.prepareSearch(index);
        if (Strings.isNullOrEmpty(type)) {
            builder.setTypes(type);
        }
        builder.setSize(size);

        // set query.
        final QueryBuilder q = buildQuery(query);

        final QueryBuilder queryBuilder;

        if (!Strings.isNullOrEmpty(query) && !query.contains(" ") && !query.contains("　")) {
            queryBuilder = buildFunctionScoreQuery(query, q);
            builder.addSort("_score", SortOrder.DESC);
        } else {
            queryBuilder = q;
        }

        builder.addSort(SortBuilders.fieldSort(FieldNames.SCORE).unmappedType("double").missing(0).order(SortOrder.DESC));

        //set filter query.
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
            public void onFailure(final Throwable e) {
                deferred.reject(new SuggesterException(e.getMessage(), e));
            }
        });
    }

    protected QueryBuilder buildQuery(final String q) {
        try {

            final QueryBuilder queryBuilder;
            if (Strings.isNullOrEmpty(q)) {
                queryBuilder = QueryBuilders.matchAllQuery();
            } else {
                final boolean prefixQuery = !q.endsWith(" ") && !q.endsWith("　");
                List<String> readingList = new ArrayList<>();

                final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
                final String[] queries = q.replaceAll("　", " ").replaceAll(" +", " ").trim().split(" ");
                for (int i = 0; i < queries.length; i++) {
                    final String fieldName = FieldNames.READING_PREFIX + i;

                    final String query;
                    if (normalizer == null) {
                        query = queries[i];
                    } else {
                        query = normalizer.normalize(queries[i]);
                    }

                    if (readingConverter == null) {
                        readingList.add(query);
                    } else {
                        readingList = readingConverter.convert(query);
                    }

                    final BoolQueryBuilder readingQueryBuilder = QueryBuilders.boolQuery().minimumNumberShouldMatch(1);
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

    protected QueryBuilder buildFilterQuery(final String fieldName, final List<String> words) {
        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery().minimumNumberShouldMatch(1);
        words.stream().forEach(word -> boolQueryBuilder.should(QueryBuilders.termQuery(fieldName, word)));
        return boolQueryBuilder;
    }

    protected QueryBuilder buildFunctionScoreQuery(final String query, final QueryBuilder queryBuilder) {
        final FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(queryBuilder);
        functionScoreQueryBuilder.add(QueryBuilders.prefixQuery(FieldNames.TEXT, query), ScoreFunctionBuilders.weightFactorFunction(2));
        functionScoreQueryBuilder.add(ScoreFunctionBuilders.fieldValueFactorFunction("score").factor(1.0F));
        return functionScoreQueryBuilder;
    }

    protected SuggestResponse createResponse(final SearchResponse searchResponse) {
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

            if (suggestDetail) {
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

        return new SuggestResponse(index, searchResponse.getTookInMillis(), words, searchResponse.getHits().totalHits(), items);
    }
}
