package org.codelibs.fess.suggest.request.suggest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codelibs.fess.suggest.concurrent.SuggestFuture;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.request.Request;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

public class SuggestRequest extends Request<SuggestResponse> {
    private String index = null;

    private String type = null;

    private String query = null;

    private int size = 10;

    private final List<String> tags = new ArrayList<>();

    private final List<String> roles = new ArrayList<>();

    private final List<String> fields = new ArrayList<>();

    private boolean suggestDetail = true;

    private ReadingConverter readingConverter;

    private Normalizer normalizer;

    private static final String _AND_ = " AND ";

    private static final String _OR_ = " OR ";

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
    protected void processRequest(final Client client, final SuggestFuture<SuggestResponse> future) {
        final SearchRequestBuilder builder = client.prepareSearch(index);
        if (Strings.isNullOrEmpty(type)) {
            builder.setTypes(type);
        }
        builder.setSize(size);

        // set query.
        final String q = buildQueryString(query);

        if (!query.contains(" ") && !query.contains("　")) {
            builder.setQuery(buildFunctionScoreQuery(query, q));
            builder.addSort("_score", SortOrder.DESC);
        } else {
            builder.setQuery(QueryBuilders.queryStringQuery(q).analyzeWildcard(false).defaultOperator(QueryStringQueryBuilder.Operator.AND));
        }

        builder.addSort(FieldNames.SCORE, SortOrder.DESC);

        //set filter query.
        final List<FilterBuilder> filterBuilderList = new ArrayList<>();
        if (!tags.isEmpty()) {
            final String fq = buildFilterQuery(FieldNames.TAGS, tags);
            filterBuilderList.add(FilterBuilders.queryFilter(QueryBuilders.queryStringQuery(fq)));
        }

        roles.add(SuggestConstants.DEFAULT_ROLE);
        if (!roles.isEmpty()) {
            final String fq = buildFilterQuery(FieldNames.ROLES, roles);
            filterBuilderList.add(FilterBuilders.queryFilter(QueryBuilders.queryStringQuery(fq)));
        }

        if (!fields.isEmpty()) {
            final String fq = buildFilterQuery(FieldNames.FIELDS, fields);
            filterBuilderList.add(FilterBuilders.queryFilter(QueryBuilders.queryStringQuery(fq)));
        }

        if (filterBuilderList.size() == 1) {
            builder.setPostFilter(filterBuilderList.get(0));
        } else if (filterBuilderList.size() > 1) {
            final AndFilterBuilder andFilterBuilder = new AndFilterBuilder();
            filterBuilderList.forEach(andFilterBuilder::add);
            builder.setPostFilter(andFilterBuilder.cache(true));
        }

        builder.execute(new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(final SearchResponse searchResponse) {
                if (searchResponse.getFailedShards() > 0) {
                    future.resolve(null, new SuggesterException("Search failure. Failed shards num:" + searchResponse.getFailedShards()));
                } else {
                    future.resolve(createResponse(searchResponse), null);
                }
            }

            @Override
            public void onFailure(final Throwable e) {
                future.resolve(null, new SuggesterException(e.getMessage(), e));
            }
        });
    }

    protected String buildQueryString(final String q) {
        try {
            final boolean prefixQuery = !q.endsWith(" ") && !q.endsWith("　");

            final String queryString;
            if (Strings.isNullOrEmpty(q)) {
                queryString = FieldNames.ID + ":*";
            } else {
                List<String> readingList = new ArrayList<>();

                final StringBuilder buf = new StringBuilder(50);
                final String[] queries = q.replaceAll("　", " ").replaceAll(" +", " ").trim().split(" ");
                for (int i = 0; i < queries.length; i++) {
                    if (i > 0) {
                        buf.append(_AND_);
                    }
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

                    final int readingNum = readingList.size();
                    if (queries.length > 1 && readingNum > 1) {
                        buf.append('(');
                    }
                    for (int readingCount = 0; readingCount < readingNum; readingCount++) {
                        if (readingCount > 0) {
                            buf.append(_OR_);
                        }
                        buf.append(fieldName).append(':').append(readingList.get(readingCount));

                        if (i + 1 == queries.length && prefixQuery) {
                            buf.append('*');
                        }
                    }
                    if (queries.length > 1 && readingNum > 1) {
                        buf.append(')');
                    }
                    readingList.clear();
                }
                queryString = buf.toString();
            }

            return queryString;
        } catch (final IOException e) {
            throw new SuggesterException("Failed to create queryString.", e);
        }
    }

    protected String buildFilterQuery(final String fieldName, final List<String> words) {
        final StringBuilder buf = new StringBuilder(20);
        if (!words.isEmpty()) {
            for (int i = 0; i < words.size(); i++) {
                if (i > 0) {
                    buf.append(_OR_);
                }
                buf.append(fieldName).append(':').append(words.get(i));
            }
        }
        return buf.toString();
    }

    protected QueryBuilder buildFunctionScoreQuery(final String query, final String queryString) {
        final FunctionScoreQueryBuilder functionScoreQueryBuilder =
                QueryBuilders.functionScoreQuery(QueryBuilders.queryStringQuery(queryString).analyzeWildcard(false)
                        .defaultOperator(QueryStringQueryBuilder.Operator.AND));

        final FilterBuilder textScoreFilterBuiler =
                FilterBuilders.queryFilter(QueryBuilders.queryStringQuery(FieldNames.TEXT + ":" + query + '*').analyzeWildcard(false)
                        .defaultOperator(QueryStringQueryBuilder.Operator.AND));
        functionScoreQueryBuilder.add(textScoreFilterBuiler, ScoreFunctionBuilders.weightFactorFunction(10));

        functionScoreQueryBuilder.add(ScoreFunctionBuilders.fieldValueFactorFunction("score").factor(1.0F));

        return functionScoreQueryBuilder;
    }

    protected SuggestResponse createResponse(final SearchResponse searchResponse) {
        final SearchHit[] hits = searchResponse.getHits().getHits();
        final List<String> words = new ArrayList<>();
        final List<SuggestItem> items = new ArrayList<>();
        for (final SearchHit hit : hits) {
            final Map<String, Object> source = hit.sourceAsMap();
            final String text = source.get(FieldNames.TEXT).toString();
            words.add(text);

            if (suggestDetail) {
                int readingCount = 0;
                Object readingObj;
                final List<String[]> readings = new ArrayList<>();
                while ((readingObj = source.get(FieldNames.READING_PREFIX + readingCount++)) != null) {
                    final List<String> reading = getAsList(readingObj);
                    readings.add(reading.toArray(new String[reading.size()]));
                }

                final List<String> fields = getAsList(source.get(FieldNames.FIELDS));
                final List<String> tags = getAsList(source.get(FieldNames.TAGS));
                final List<String> roles = getAsList(source.get(FieldNames.ROLES));
                final List<String> kinds = getAsList(source.get(FieldNames.KINDS));
                SuggestItem.Kind kind;
                long freq;
                if (SuggestItem.Kind.USER.toString().equals(kinds.get(0))) {
                    kind = SuggestItem.Kind.USER;
                    freq = 0;
                } else if (SuggestItem.Kind.QUERY.toString().equals(kinds.get(0))) {
                    kind = SuggestItem.Kind.QUERY;
                    freq = Long.valueOf(source.get(FieldNames.QUERY_FREQ).toString());
                } else {
                    kind = SuggestItem.Kind.DOCUMENT;
                    freq = Long.valueOf(source.get(FieldNames.DOC_FREQ).toString());
                }

                items.add(new SuggestItem(text.split(" "), readings.toArray(new String[readings.size()][]), fields
                        .toArray(new String[fields.size()]), freq, Float.valueOf(source.get(FieldNames.USER_BOOST).toString()), tags
                        .toArray(new String[tags.size()]), roles.toArray(new String[tags.size()]), kind));
            }
        }

        return new SuggestResponse(searchResponse.getTookInMillis(), words, searchResponse.getHits().totalHits(), items);
    }

    private List<String> getAsList(final Object value) {
        if (value instanceof String) {
            final List<String> list = new ArrayList<>();
            list.add(value.toString());
            return list;
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            final List<String> list = (List<String>) value;
            return list;
        }
        throw new IllegalArgumentException("The value should be String or List, but " + value);
    }
}
