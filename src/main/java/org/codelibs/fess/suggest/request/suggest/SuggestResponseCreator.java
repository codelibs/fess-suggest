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

import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.search.SearchHit;

/**
 * Creates SuggestResponse instances from OpenSearch search results.
 *
 * <p>This class encapsulates the response creation logic extracted from SuggestRequest,
 * including duplicate word handling and first-word matching prioritization.</p>
 */
public class SuggestResponseCreator {

    private final String query;
    private final int size;
    private final boolean suggestDetail;
    private final boolean skipDuplicateWords;
    private final boolean matchWordFirst;
    private final SuggestQueryBuilder queryBuilder;

    /**
     * Constructs a new SuggestResponseCreator.
     *
     * @param query The query string.
     * @param size The maximum number of results.
     * @param suggestDetail Whether to include detailed suggestion information.
     * @param skipDuplicateWords Whether to skip duplicate words.
     * @param matchWordFirst Whether to prioritize first word matching.
     * @param queryBuilder The SuggestQueryBuilder instance for query analysis methods.
     */
    public SuggestResponseCreator(final String query, final int size, final boolean suggestDetail, final boolean skipDuplicateWords,
            final boolean matchWordFirst, final SuggestQueryBuilder queryBuilder) {
        this.query = query;
        this.size = size;
        this.suggestDetail = suggestDetail;
        this.skipDuplicateWords = skipDuplicateWords;
        this.matchWordFirst = matchWordFirst;
        this.queryBuilder = queryBuilder;
    }

    /**
     * Creates a SuggestResponse from the OpenSearch SearchResponse.
     * @param searchResponse The OpenSearch SearchResponse.
     * @return A SuggestResponse instance.
     */
    public SuggestResponse createResponse(final SearchResponse searchResponse) {
        final SearchHit[] hits = searchResponse.getHits().getHits();
        final List<String> words = new ArrayList<>();
        final Set<String> seenNormalizedWords = new HashSet<>();
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

        final boolean singleWordQuery = queryBuilder.isSingleWordQuery(query);
        final boolean hiraganaQuery = queryBuilder.isHiraganaQuery(query);
        for (int i = 0; i < hits.length && words.size() < size; i++) {
            final SearchHit hit = hits[i];

            final Map<String, Object> source = hit.getSourceAsMap();
            final String text = source.get(FieldNames.TEXT).toString();
            if (skipDuplicateWords) {
                final String normalizedText = text.replace(" ", "");
                if (!seenNormalizedWords.add(normalizedText)) {
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
}
