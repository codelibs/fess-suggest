package org.codelibs.fess.suggest.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.codelibs.core.CoreLibConstants;
import org.codelibs.fess.suggest.converter.AnalyzerConverter;
import org.codelibs.fess.suggest.converter.KatakanaToAlphabetConverter;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.converter.ReadingConverterChain;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.normalizer.AnalyzerNormalizer;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.normalizer.NormalizerChain;
import org.codelibs.fess.suggest.settings.AnalyzerSettings;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;

public final class SuggestUtil {
    private static final int MAX_QUERY_TERM_NUM = 5;
    private static final int MAX_QUERY_TERM_LENGTH = 48;

    private static final Base64.Encoder encoder = Base64.getEncoder();

    private static final int idMaxLength = 445;

    private SuggestUtil() {
    }

    public static String createSuggestTextId(final String text) {
        final String id = encoder.encodeToString(text.getBytes(CoreLibConstants.CHARSET_UTF_8));
        if (id.length() > 445) {
            return id.substring(0, idMaxLength);
        }
        return id;
    }

    public static String[] parseQuery(final String q, final String field) {
        final List<String> keywords = getKeywords(q, new String[] { field });
        if (MAX_QUERY_TERM_NUM < keywords.size()) {
            return new String[0];
        }
        for (final String k : keywords) {
            if (MAX_QUERY_TERM_LENGTH < k.length()) {
                return new String[0];
            }
        }
        return keywords.toArray(new String[keywords.size()]);
    }

    public static List<String> getKeywords(final String q, final String[] fields) {
        final List<String> keywords = new ArrayList<>();
        final List<TermQuery> termQueryList;
        try {
            final StandardQueryParser parser = new StandardQueryParser();
            parser.setDefaultOperator(StandardQueryConfigHandler.Operator.AND);

            termQueryList = getTermQueryList(parser.parse(q, "default"), fields);
        } catch (final Exception e) {
            return keywords;
        }
        for (final TermQuery tq : termQueryList) {
            final String text = tq.getTerm().text();
            if (0 == text.length()) {
                continue;
            }
            if (keywords.contains(text)) {
                continue;
            }
            keywords.add(text);
        }
        return keywords;
    }

    public static List<TermQuery> getTermQueryList(final Query query, final String[] fields) {
        if (query instanceof BooleanQuery) {
            final BooleanQuery booleanQuery = (BooleanQuery) query;
            final List<BooleanClause> clauses = booleanQuery.clauses();
            final List<TermQuery> queryList = new ArrayList<>();
            for (final BooleanClause clause : clauses) {
                final Query q = clause.getQuery();
                if (q instanceof BooleanQuery) {
                    queryList.addAll(getTermQueryList(q, fields));
                } else if (q instanceof TermQuery) {
                    final TermQuery termQuery = (TermQuery) q;
                    for (final String field : fields) {
                        if (field.equals(termQuery.getTerm().field())) {
                            queryList.add(termQuery);
                        }
                    }
                }
            }
            return queryList;
        } else if (query instanceof TermQuery) {
            final TermQuery termQuery = (TermQuery) query;
            for (final String field : fields) {
                if (field.equals(termQuery.getTerm().field())) {
                    final List<TermQuery> queryList = new ArrayList<>(1);
                    queryList.add(termQuery);
                    return queryList;
                }
            }
        }
        return Collections.emptyList();
    }

    public static String createBulkLine(final String index, final String type, final SuggestItem item) {
        final Map<String, Object> firstLineMap = new HashMap<>();
        final Map<String, Object> firstLineInnerMap = new HashMap<>();
        firstLineInnerMap.put("_index", index);
        firstLineInnerMap.put("_type", type);
        firstLineInnerMap.put("_id", item.getId());
        firstLineMap.put("index", firstLineInnerMap);

        final Map<String, Object> secondLine = new HashMap<>();

        secondLine.put("text", item.getText());

        //reading
        final String[][] readings = item.getReadings();
        for (int i = 0; i < readings.length; i++) {
            secondLine.put("reading_" + i, readings[i]);
        }

        secondLine.put("fields", item.getFields());
        secondLine.put("queryFreq", item.getQueryFreq());
        secondLine.put("docFreq", item.getDocFreq());
        secondLine.put("userBoost", item.getUserBoost());
        secondLine.put("score", (item.getQueryFreq() + item.getDocFreq()) * item.getUserBoost());
        secondLine.put("tags", item.getTags());
        secondLine.put("roles", item.getRoles());
        secondLine.put("kinds", item.getKinds().toString());
        secondLine.put("@timestamp", item.getTimestamp());

        try (OutputStream out1 = JsonXContent.contentBuilder().map(firstLineMap).getOutputStream();
                OutputStream out2 = JsonXContent.contentBuilder().map(secondLine).getOutputStream()) {
            return ((ByteArrayOutputStream) out1).toString(CoreLibConstants.UTF_8) + '\n'
                    + ((ByteArrayOutputStream) out2).toString(CoreLibConstants.UTF_8);
        } catch (final IOException e) {
            throw new SuggesterException(e);
        }
    }

    public static ReadingConverter createDefaultReadingConverter(final Client client, final SuggestSettings settings) {
        final ReadingConverterChain chain = new ReadingConverterChain();
        chain.addConverter(new AnalyzerConverter(client, settings));
        chain.addConverter(new KatakanaToAlphabetConverter());
        return chain;
    }

    public static ReadingConverter createDefaultContentsReadingConverter(final Client client, final SuggestSettings settings) {
        final ReadingConverterChain chain = new ReadingConverterChain();
        chain.addConverter(new KatakanaToAlphabetConverter());
        return chain;
    }

    public static Normalizer createDefaultNormalizer(final Client client, final SuggestSettings settings) {
        final NormalizerChain normalizerChain = new NormalizerChain();
        normalizerChain.add(new AnalyzerNormalizer(client, settings));
        /*
        normalizerChain.add(new HankakuKanaToZenkakuKana());
        normalizerChain.add(new FullWidthToHalfWidthAlphabetNormalizer());
        normalizerChain.add(new ICUNormalizer("Any-Lower"));
        */
        return normalizerChain;
    }

    public static AnalyzerSettings.DefaultContentsAnalyzer createDefaultAnalyzer(final Client client, final SuggestSettings settings) {
        final AnalyzerSettings analyzerSettings = settings.analyzer();
        return analyzerSettings.new DefaultContentsAnalyzer();
    }

    public static List<String> getAsList(final Object value) {
        if (value == null) {
            return new ArrayList<>();
        }

        if (value instanceof String) {
            final List<String> list = new ArrayList<>();
            list.add(value.toString());
            return list;
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            final List<String> list = (List<String>) value;
            return list;
        }
        throw new IllegalArgumentException("The value should be String or List, but " + value.getClass());
    }

    public static boolean deleteByQuery(final Client client, final SuggestSettings settings, final String index, final String type,
            final QueryBuilder queryBuilder) {
        try {
            SearchResponse searchResponse =
                    client.prepareSearch(index).setTypes(type).setQuery(queryBuilder).setSize(500).setScroll(settings.getScrollTimeout())
                            .execute().actionGet(settings.getSearchTimeout());

            while (searchResponse.getHits().getHits().length > 0) {
                final String scrollId = searchResponse.getScrollId();
                final SearchHit[] hits = searchResponse.getHits().getHits();

                final BulkRequestBuilder bulkRequestBuiler = client.prepareBulk();
                Stream.of(hits).map(SearchHit::getId).forEach(id -> bulkRequestBuiler.add(new DeleteRequest(index, type, id)));

                final BulkResponse bulkResponse = bulkRequestBuiler.execute().actionGet(settings.getBulkTimeout());
                if (bulkResponse.hasFailures()) {
                    throw new SuggesterException(bulkResponse.buildFailureMessage());
                }
                searchResponse =
                        client.prepareSearchScroll(scrollId).setScroll(settings.getScrollTimeout()).execute()
                                .actionGet(settings.getSearchTimeout());
            }
            client.admin().indices().prepareRefresh(index).execute().actionGet(settings.getIndicesTimeout());
        } catch (final Exception e) {
            throw new SuggesterException("Failed to exec delete by query.", e);
        }

        return true;
    }

    public static String escapeWildcardQuery(final String query) {
        return query.replace("*", "\\*").replace("?", "\\?");
    }
}
