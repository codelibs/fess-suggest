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
package org.codelibs.fess.suggest.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.opensearch.action.bulk.BulkRequestBuilder;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.search.CreatePitRequest;
import org.opensearch.action.search.CreatePitResponse;
import org.opensearch.action.search.DeletePitRequest;
import org.opensearch.action.search.SearchRequestBuilder;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.PointInTimeBuilder;
import org.opensearch.transport.client.Client;

/**
 * Utility class for suggest feature.
 */
public final class SuggestUtil {
    private static final int MAX_QUERY_TERM_NUM = 5;
    private static final int MAX_QUERY_TERM_LENGTH = 48;

    private static final Base64.Encoder encoder = Base64.getEncoder();

    private static final int ID_MAX_LENGTH = 445;

    /**
     * Private constructor to prevent instantiation.
     */
    private SuggestUtil() {
    }

    /**
     * Creates a unique identifier for the given text by encoding it to a Base64 string.
     * If the encoded string exceeds the maximum allowed length, it truncates the string
     * to the specified maximum length.
     *
     * @param text the input text to be encoded
     * @return the encoded string, truncated if necessary
     */
    public static String createSuggestTextId(final String text) {
        final String id = encoder.encodeToString(text.getBytes(CoreLibConstants.CHARSET_UTF_8));
        if (id.length() > 445) {
            return id.substring(0, ID_MAX_LENGTH);
        }
        return id;
    }

    /**
     * Parses the given query string and returns an array of keywords.
     *
     * @param q the query string to be parsed
     * @param field the field to be used for keyword extraction
     * @return an array of keywords extracted from the query string, or an empty array if the number of keywords exceeds the maximum allowed or if any keyword exceeds the maximum length
     */
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

    /**
     * Extracts keywords from the given query string based on the specified fields.
     *
     * @param q the query string to parse and extract keywords from
     * @param fields the fields to consider when extracting keywords
     * @return a list of unique keywords extracted from the query string
     */
    public static List<String> getKeywords(final String q, final String[] fields) {
        final List<String> keywords = new ArrayList<>();
        final List<TermQuery> termQueryList;
        try {
            final StandardQueryParser parser = new StandardQueryParser();
            parser.setDefaultOperator(StandardQueryConfigHandler.Operator.AND);

            // Parse with the first field if available, otherwise use "default"
            String defaultField = fields != null && fields.length > 0 ? fields[0] : "default";
            termQueryList = getTermQueryList(parser.parse(q, defaultField), fields);
        } catch (final Exception e) {
            return keywords;
        }
        for (final TermQuery tq : termQueryList) {
            final String text = tq.getTerm().text();
            if (0 == text.length() || keywords.contains(text)) {
                continue;
            }
            keywords.add(text);
        }
        return keywords;
    }

    /**
     * Extracts a list of TermQuery objects from the given Query object that match the specified fields.
     *
     * @param query the Query object to extract TermQuery objects from
     * @param fields an array of field names to match against the TermQuery objects
     * @return a list of TermQuery objects that match the specified fields, or an empty list if no matches are found
     */
    public static List<TermQuery> getTermQueryList(final Query query, final String[] fields) {
        if (query instanceof final BooleanQuery booleanQuery) {
            final List<BooleanClause> clauses = booleanQuery.clauses();
            final List<TermQuery> queryList = new ArrayList<>();
            for (final BooleanClause clause : clauses) {
                final Query q = clause.query();
                if (q instanceof BooleanQuery) {
                    queryList.addAll(getTermQueryList(q, fields));
                } else if (q instanceof final TermQuery termQuery) {
                    for (final String field : fields) {
                        if (field.equals(termQuery.getTerm().field())) {
                            queryList.add(termQuery);
                        }
                    }
                }
            }
            return queryList;
        }
        if (query instanceof final TermQuery termQuery) {
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

    /**
     * Creates a bulk line for OpenSearch indexing from the given parameters.
     *
     * @param index the name of the OpenSearch index
     * @param type the type of the document (deprecated in newer versions of OpenSearch)
     * @param item the SuggestItem containing the data to be indexed
     * @return a string representing the bulk line for OpenSearch indexing
     * @throws SuggesterException if an I/O error occurs during the creation of the bulk line
     */
    public static String createBulkLine(final String index, final String type, final SuggestItem item) {
        if (item == null || item.getId() == null || item.getText() == null) {
            throw new SuggesterException("Invalid SuggestItem: item, id, or text is null");
        }

        final Map<String, Object> firstLineMap = new HashMap<>();
        final Map<String, Object> firstLineInnerMap = new HashMap<>();
        firstLineInnerMap.put("_index", index);
        firstLineInnerMap.put("_type", type);
        firstLineInnerMap.put("_id", item.getId());
        firstLineMap.put("index", firstLineInnerMap);

        final Map<String, Object> secondLine = new HashMap<>();

        secondLine.put("text", item.getText());

        // reading
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
        secondLine.put("kinds", Arrays.toString(item.getKinds()));
        secondLine.put("@timestamp", item.getTimestamp());

        try (OutputStream out1 = getXContentOutputStream(firstLineMap); OutputStream out2 = getXContentOutputStream(secondLine)) {
            return ((ByteArrayOutputStream) out1).toString(CoreLibConstants.UTF_8) + '\n'
                    + ((ByteArrayOutputStream) out2).toString(CoreLibConstants.UTF_8);
        } catch (final IOException e) {
            throw new SuggesterException(e);
        }
    }

    private static OutputStream getXContentOutputStream(final Map<String, Object> firstLineMap) throws IOException {
        try (XContentBuilder builder = JsonXContent.contentBuilder().map(firstLineMap)) {
            builder.flush();
            return builder.getOutputStream();
        }
    }

    /**
     * Creates a default ReadingConverter with a chain of converters.
     * The chain includes an AnalyzerConverter and a KatakanaToAlphabetConverter.
     *
     * @param client   the client to be used by the AnalyzerConverter
     * @param settings the settings to be used by the AnalyzerConverter
     * @return a ReadingConverterChain with the default converters added
     */
    public static ReadingConverter createDefaultReadingConverter(final Client client, final SuggestSettings settings) {
        final ReadingConverterChain chain = new ReadingConverterChain();
        chain.addConverter(new AnalyzerConverter(client, settings));
        chain.addConverter(new KatakanaToAlphabetConverter());
        return chain;
    }

    /**
     * Creates a default ReadingConverterChain with a KatakanaToAlphabetConverter.
     *
     * @param client   the client instance used for the converter
     * @param settings the suggest settings used for the converter
     * @return a ReadingConverterChain with a KatakanaToAlphabetConverter added
     */
    public static ReadingConverter createDefaultContentsReadingConverter(final Client client, final SuggestSettings settings) {
        final ReadingConverterChain chain = new ReadingConverterChain();
        chain.addConverter(new KatakanaToAlphabetConverter());
        return chain;
    }

    /**
     * Creates a default normalizer using the provided client and suggest settings.
     * The normalizer chain includes an AnalyzerNormalizer.
     *
     * @param client the client to be used for creating the normalizer
     * @param settings the suggest settings to be used for creating the normalizer
     * @return a NormalizerChain with the default normalizers
     */
    public static Normalizer createDefaultNormalizer(final Client client, final SuggestSettings settings) {
        final NormalizerChain normalizerChain = new NormalizerChain();
        normalizerChain.add(new AnalyzerNormalizer(client, settings));
        /*
         * normalizerChain.add(new HankakuKanaToZenkakuKana()); normalizerChain.add(new
         * FullWidthToHalfWidthAlphabetNormalizer()); normalizerChain.add(new ICUNormalizer("Any-Lower"));
         */
        return normalizerChain;
    }

    /**
     * Creates a new instance of DefaultContentsAnalyzer using the provided client and suggest settings.
     *
     * @param client   the OpenSearch client to be used for the analyzer
     * @param settings the suggest settings containing the analyzer configuration
     * @return a new instance of AnalyzerSettings.DefaultContentsAnalyzer
     */
    public static AnalyzerSettings.DefaultContentsAnalyzer createDefaultAnalyzer(final Client client, final SuggestSettings settings) {
        final AnalyzerSettings analyzerSettings = settings.analyzer();
        return analyzerSettings.new DefaultContentsAnalyzer();
    }

    /**
     * Converts the given object to a list of strings.
     * <p>
     * If the object is null, an empty list is returned.
     * If the object is a string, a list containing that string is returned.
     * If the object is a list, it is cast to a list of strings and returned.
     * </p>
     *
     * @param value the object to be converted to a list of strings
     * @return a list of strings representing the given object
     * @throws IllegalArgumentException if the object is not a string or a list
     */
    public static List<String> getAsList(final Object value) {
        if (value == null) {
            return new ArrayList<>();
        }

        if (value instanceof String) {
            final List<String> list = new ArrayList<>();
            list.add(value.toString());
            return list;
        }
        if (value instanceof List) {
            return (List<String>) value;
        }
        throw new IllegalArgumentException("The value should be String or List, but " + value.getClass());
    }

    /**
     * Deletes documents from the specified index based on the given query using PIT API.
     *
     * @param client the OpenSearch client to use for executing the query and delete operations
     * @param settings the settings for the suggest feature, including timeouts and PIT settings
     * @param index the name of the index from which documents should be deleted
     * @param queryBuilder the query used to identify documents to delete
     * @return true if the operation completes successfully
     * @throws SuggesterException if any error occurs during the delete operation
     */
    public static boolean deleteByQuery(final Client client, final SuggestSettings settings, final String index,
            final QueryBuilder queryBuilder) {
        String pitId = null;
        try {
            // Create PIT
            final CreatePitRequest createPitRequest = new CreatePitRequest(
                    TimeValue.parseTimeValue(settings.getScrollTimeout(), TimeValue.timeValueMinutes(1), "keep_alive"),
                    index);

            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            final java.util.concurrent.atomic.AtomicReference<CreatePitResponse> responseRef = new java.util.concurrent.atomic.AtomicReference<>();
            final java.util.concurrent.atomic.AtomicReference<Exception> exceptionRef = new java.util.concurrent.atomic.AtomicReference<>();

            client.createPit(createPitRequest, ActionListener.wrap(
                resp -> {
                    responseRef.set(resp);
                    latch.countDown();
                },
                e -> {
                    exceptionRef.set(e);
                    latch.countDown();
                }
            ));

            try {
                latch.await(settings.getSearchTimeout().millis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SuggesterException("Interrupted while creating PIT", e);
            }

            if (exceptionRef.get() != null) {
                throw new SuggesterException("Failed to create PIT", exceptionRef.get());
            }

            final CreatePitResponse createPitResponse = responseRef.get();
            if (createPitResponse == null) {
                throw new SuggesterException("PIT creation timed out");
            }
            pitId = createPitResponse.getId();

            Object[] searchAfter = null;
            while (true) {
                final SearchRequestBuilder searchBuilder = client.prepareSearch()
                        .setQuery(queryBuilder)
                        .setSize(500)
                        .setPointInTime(new PointInTimeBuilder(pitId));

                if (searchAfter != null) {
                    searchBuilder.searchAfter(searchAfter);
                }

                final SearchResponse response = searchBuilder.execute().actionGet(settings.getSearchTimeout());
                final SearchHit[] hits = response.getHits().getHits();
                if (hits.length == 0) {
                    break;
                }

                final BulkRequestBuilder bulkRequestBuiler = client.prepareBulk();
                Stream.of(hits).map(SearchHit::getId).forEach(id -> bulkRequestBuiler.add(new DeleteRequest(index, id)));

                final BulkResponse bulkResponse = bulkRequestBuiler.execute().actionGet(settings.getBulkTimeout());
                if (bulkResponse.hasFailures()) {
                    throw new SuggesterException(bulkResponse.buildFailureMessage());
                }

                // Update search_after for next iteration
                final SearchHit lastHit = hits[hits.length - 1];
                searchAfter = lastHit.getSortValues();
            }

            client.admin().indices().prepareRefresh(index).execute().actionGet(settings.getIndicesTimeout());
        } catch (final Exception e) {
            throw new SuggesterException("Failed to exec delete by query.", e);
        } finally {
            deletePitContext(client, pitId);
        }

        return true;
    }

    /**
     * Deletes the PIT context associated with the given PIT ID.
     *
     * @param client the OpenSearch client used to delete the PIT context
     * @param pitId the ID of the PIT context to be deleted; if null, no action is taken
     */
    public static void deletePitContext(final Client client, final String pitId) {
        if (pitId != null) {
            try {
                final DeletePitRequest deletePitRequest = new DeletePitRequest(pitId);
                // Use generic execute method with DeletePitAction for Transport Client compatibility
                client.execute(org.opensearch.action.search.DeletePitAction.INSTANCE, deletePitRequest,
                    ActionListener.wrap(res -> {}, e -> {}));
            } catch (final Exception e) {
                // Silently ignore deletion errors as they are not critical
            }
        }
    }

    /**
     * Deletes the scroll context associated with the given scroll ID.
     * @deprecated Use {@link #deletePitContext(Client, String)} instead
     *
     * @param client the OpenSearch client used to clear the scroll context
     * @param scrollId the ID of the scroll context to be deleted; if null, no action is taken
     */
    @Deprecated
    public static void deleteScrollContext(final Client client, final String scrollId) {
        if (scrollId != null) {
            client.prepareClearScroll().addScrollId(scrollId).execute(ActionListener.wrap(res -> {}, e -> {}));
        }
    }

    /**
     * Escapes wildcard characters in the given query string.
     *
     * This method replaces all occurrences of '*' with '\*' and
     * all occurrences of '?' with '\?' to ensure that these characters
     * are treated as literals rather than wildcard characters in queries.
     *
     * @param query the query string to escape
     * @return the escaped query string
     */
    public static String escapeWildcardQuery(final String query) {
        return query.replace("*", "\\*").replace("?", "\\?");
    }
}
