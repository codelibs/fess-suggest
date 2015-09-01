package org.codelibs.fess.suggest.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.StandardQueryConfigHandler;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.IOUtils;
import org.codelibs.core.CoreLibConstants;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.converter.KatakanaConverter;
import org.codelibs.fess.suggest.converter.KatakanaToAlphabetConverter;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.converter.ReadingConverterChain;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.normalizer.FullWidthToHalfWidthAlphabetNormalizer;
import org.codelibs.fess.suggest.normalizer.ICUNormalizer;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.normalizer.NormalizerChain;
import org.codelibs.neologd.ipadic.lucene.analysis.ja.JapaneseAnalyzer;
import org.codelibs.neologd.ipadic.lucene.analysis.ja.JapaneseTokenizer;
import org.codelibs.neologd.ipadic.lucene.analysis.ja.dict.UserDictionary;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.common.xcontent.json.JsonXContent;

public final class SuggestUtil {
    private static final int MAX_QUERY_TERM_NUM = 5;
    private static final int MAX_QUERY_TERM_LENGTH = 48;

    private static final Base64.Encoder encoder = Base64.getEncoder();

    private SuggestUtil() {
    }

    public static String createSuggestTextId(final String text) {
        return encoder.encodeToString(text.getBytes(CoreLibConstants.CHARSET_UTF_8));
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
            final BooleanClause[] clauses = booleanQuery.getClauses();
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
        secondLine.put("kinds", item.getKind());
        secondLine.put("@timestamp", item.getTimestamp());

        try {
            return JsonXContent.contentBuilder().map(firstLineMap).string() + '\n' + JsonXContent.contentBuilder().map(secondLine).string();
        } catch (final IOException e) {
            throw new SuggesterException(e);
        }
    }

    public static ReadingConverter createDefaultReadingConverter() {
        final ReadingConverterChain chain = new ReadingConverterChain();
        chain.addConverter(new KatakanaConverter());
        chain.addConverter(new KatakanaToAlphabetConverter());
        return chain;
    }

    public static Normalizer createDefaultNormalizer() {
        final NormalizerChain normalizerChain = new NormalizerChain();
        normalizerChain.add(new ICUNormalizer("Halfwidth-Fullwidth"));
        normalizerChain.add(new FullWidthToHalfWidthAlphabetNormalizer());
        normalizerChain.add(new ICUNormalizer("Any-Lower"));
        return normalizerChain;
    }

    public static Analyzer createDefaultAnalyzer() {
        try {
            final UserDictionary userDictionary;
            final String userDictionaryPath = System.getProperty(SuggestConstants.USER_DICT_PATH);
            if (Strings.isNullOrEmpty(userDictionaryPath) || !new File(userDictionaryPath).exists()) {
                userDictionary = null;
            } else {
                final InputStream stream = new FileInputStream(new File(userDictionaryPath));
                String encoding = System.getProperty(SuggestConstants.USER_DICT_ENCODING);
                if (encoding == null) {
                    encoding = IOUtils.UTF_8;
                }

                final CharsetDecoder decoder =
                        Charset.forName(encoding).newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                                .onUnmappableCharacter(CodingErrorAction.REPORT);
                final InputStreamReader reader = new InputStreamReader(stream, decoder);
                userDictionary = new UserDictionary(reader);
            }

            final Set<String> stopTags = new HashSet<>();

            return new JapaneseAnalyzer(userDictionary, JapaneseTokenizer.Mode.NORMAL, null, stopTags);
        } catch (final IOException e) {
            throw new SuggesterException("Failed to create default analyzer.", e);
        }
    }
}
