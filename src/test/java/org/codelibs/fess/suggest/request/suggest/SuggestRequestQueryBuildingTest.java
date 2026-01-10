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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MatchAllQueryBuilder;
import org.opensearch.index.query.PrefixQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.functionscore.FunctionScoreQueryBuilder;

/**
 * Tests for SuggestRequest query building methods.
 * These tests focus on the buildQuery, buildFilterQuery, and buildFunctionScoreQuery methods.
 */
public class SuggestRequestQueryBuildingTest {

    private TestableSuggestRequest request;

    @Before
    public void setUp() {
        request = new TestableSuggestRequest();
    }

    // ============================================================
    // Tests for buildQuery method
    // ============================================================

    @Test
    public void test_buildQuery_emptyQuery() {
        QueryBuilder result = request.buildQuery("", Collections.emptyList());
        assertNotNull(result);
        assertTrue("Empty query should produce MatchAllQuery", result instanceof MatchAllQueryBuilder);
    }

    @Test
    public void test_buildQuery_nullQuery() {
        QueryBuilder result = request.buildQuery(null, Collections.emptyList());
        assertNotNull(result);
        assertTrue("Null query should produce MatchAllQuery", result instanceof MatchAllQueryBuilder);
    }

    @Test
    public void test_buildQuery_singleWord() {
        QueryBuilder result = request.buildQuery("test", Collections.emptyList());
        assertNotNull(result);
        assertTrue("Single word query should produce BoolQuery", result instanceof BoolQueryBuilder);

        String queryString = result.toString();
        // Should contain a prefix query for the last word (since no trailing space)
        assertTrue("Should contain prefix query", queryString.contains("prefix"));
        assertTrue("Should query reading_0", queryString.contains("reading_0"));
    }

    @Test
    public void test_buildQuery_singleWordWithTrailingSpace() {
        QueryBuilder result = request.buildQuery("test ", Collections.emptyList());
        assertNotNull(result);
        assertTrue(result instanceof BoolQueryBuilder);

        String queryString = result.toString();
        // With trailing space, should use term query not prefix
        assertTrue("Should contain term query", queryString.contains("term"));
    }

    @Test
    public void test_buildQuery_multiWordWithSpaces() {
        QueryBuilder result = request.buildQuery("hello world", Collections.emptyList());
        assertNotNull(result);
        assertTrue(result instanceof BoolQueryBuilder);

        String queryString = result.toString();
        // Should have reading_0 and reading_1
        assertTrue("Should query reading_0", queryString.contains("reading_0"));
        assertTrue("Should query reading_1", queryString.contains("reading_1"));
    }

    @Test
    public void test_buildQuery_multiWordWithFullWidthSpaces() {
        QueryBuilder result = request.buildQuery("hello　world", Collections.emptyList());
        assertNotNull(result);
        assertTrue(result instanceof BoolQueryBuilder);

        String queryString = result.toString();
        // Full-width space should be treated same as regular space
        assertTrue("Should query reading_0", queryString.contains("reading_0"));
        assertTrue("Should query reading_1", queryString.contains("reading_1"));
    }

    @Test
    public void test_buildQuery_multiWordWithMultipleSpaces() {
        QueryBuilder result = request.buildQuery("hello   world", Collections.emptyList());
        assertNotNull(result);
        assertTrue(result instanceof BoolQueryBuilder);

        String queryString = result.toString();
        // Multiple spaces should be collapsed to single space
        assertTrue("Should query reading_0", queryString.contains("reading_0"));
        assertTrue("Should query reading_1", queryString.contains("reading_1"));
        // Should NOT have reading_2 (spaces collapsed)
        assertFalse("Should not have reading_2", queryString.contains("reading_2"));
    }

    @Test
    public void test_buildQuery_withNormalizer() {
        Normalizer normalizer = new Normalizer() {
            @Override
            public String normalize(String text, String field, String... langs) {
                return text.toUpperCase();
            }
        };
        request.setNormalizer(normalizer);

        QueryBuilder result = request.buildQuery("test", Collections.emptyList());
        assertNotNull(result);

        String queryString = result.toString();
        // The normalizer should uppercase the query
        assertTrue("Should contain normalized (uppercase) query", queryString.contains("TEST"));
    }

    @Test
    public void test_buildQuery_withReadingConverter() {
        ReadingConverter converter = new ReadingConverter() {
            @Override
            public void init() throws IOException {
            }

            @Override
            public List<String> convert(String text, String field, String... langs) throws IOException {
                // Return multiple readings for the text
                return Arrays.asList(text, text + "_reading");
            }
        };
        request.setReadingConverter(converter);

        QueryBuilder result = request.buildQuery("test", Collections.emptyList());
        assertNotNull(result);

        String queryString = result.toString();
        // Should have both readings
        assertTrue("Should contain original text", queryString.contains("test"));
        assertTrue("Should contain converted reading", queryString.contains("test_reading"));
    }

    @Test
    public void test_buildQuery_threeWords() {
        QueryBuilder result = request.buildQuery("one two three", Collections.emptyList());
        assertNotNull(result);

        String queryString = result.toString();
        assertTrue("Should query reading_0", queryString.contains("reading_0"));
        assertTrue("Should query reading_1", queryString.contains("reading_1"));
        assertTrue("Should query reading_2", queryString.contains("reading_2"));
    }

    @Test
    public void test_buildQuery_japaneseText() {
        QueryBuilder result = request.buildQuery("検索 エンジン", Collections.emptyList());
        assertNotNull(result);

        String queryString = result.toString();
        assertTrue("Should contain Japanese text", queryString.contains("検索"));
        assertTrue("Should contain Japanese text", queryString.contains("エンジン"));
    }

    // ============================================================
    // Tests for buildFilterQuery method
    // ============================================================

    @Test
    public void test_buildFilterQuery_singleWord() {
        List<String> words = Collections.singletonList("tag1");
        QueryBuilder result = request.buildFilterQuery("tags", words);
        assertNotNull(result);
        assertTrue(result instanceof BoolQueryBuilder);

        String queryString = result.toString();
        assertTrue("Should contain term query for tag1", queryString.contains("tag1"));
    }

    @Test
    public void test_buildFilterQuery_multipleWords() {
        List<String> words = Arrays.asList("tag1", "tag2", "tag3");
        QueryBuilder result = request.buildFilterQuery("tags", words);
        assertNotNull(result);
        assertTrue(result instanceof BoolQueryBuilder);

        String queryString = result.toString();
        assertTrue("Should contain tag1", queryString.contains("tag1"));
        assertTrue("Should contain tag2", queryString.contains("tag2"));
        assertTrue("Should contain tag3", queryString.contains("tag3"));
        // Should be minimum_should_match = 1 (OR logic)
        assertTrue("Should use should clauses", queryString.contains("should"));
    }

    @Test
    public void test_buildFilterQuery_emptyList() {
        List<String> words = Collections.emptyList();
        QueryBuilder result = request.buildFilterQuery("tags", words);
        assertNotNull(result);
        // Empty list should still return a valid query builder
        assertTrue(result instanceof BoolQueryBuilder);
    }

    @Test
    public void test_buildFilterQuery_differentFields() {
        QueryBuilder tagsFilter = request.buildFilterQuery("tags", Arrays.asList("tag1"));
        QueryBuilder rolesFilter = request.buildFilterQuery("roles", Arrays.asList("role1"));
        QueryBuilder fieldsFilter = request.buildFilterQuery("fields", Arrays.asList("field1"));

        assertTrue("Tags filter should contain 'tags'", tagsFilter.toString().contains("tags"));
        assertTrue("Roles filter should contain 'roles'", rolesFilter.toString().contains("roles"));
        assertTrue("Fields filter should contain 'fields'", fieldsFilter.toString().contains("fields"));
    }

    // ============================================================
    // Tests for buildFunctionScoreQuery method
    // ============================================================

    @Test
    public void test_buildFunctionScoreQuery_singleWordQuery() {
        QueryBuilder innerQuery = request.buildQuery("test", Collections.emptyList());
        QueryBuilder result = request.buildFunctionScoreQuery("test", innerQuery);

        assertNotNull(result);
        assertTrue("Should be FunctionScoreQueryBuilder", result instanceof FunctionScoreQueryBuilder);

        String queryString = result.toString();
        // Single word non-hiragana query should have prefix boost
        assertTrue("Should contain text prefix boost", queryString.contains("text"));
    }

    @Test
    public void test_buildFunctionScoreQuery_hiraganaQuery() {
        QueryBuilder innerQuery = request.buildQuery("けんさく", Collections.emptyList());
        QueryBuilder result = request.buildFunctionScoreQuery("けんさく", innerQuery);

        assertNotNull(result);
        assertTrue(result instanceof FunctionScoreQueryBuilder);

        String queryString = result.toString();
        // Hiragana query should NOT have the prefix text boost on text field
        // (the logic excludes hiragana from prefix matching)
        assertTrue("Should still have function score", queryString.contains("field_value_factor"));
    }

    @Test
    public void test_buildFunctionScoreQuery_multiWordQuery() {
        QueryBuilder innerQuery = request.buildQuery("hello world", Collections.emptyList());
        QueryBuilder result = request.buildFunctionScoreQuery("hello world", innerQuery);

        assertNotNull(result);
        assertTrue(result instanceof FunctionScoreQueryBuilder);

        String queryString = result.toString();
        // Multi-word query should NOT have prefix boost on text field
        assertTrue("Should have field value factors", queryString.contains("field_value_factor"));
    }

    @Test
    public void test_buildFunctionScoreQuery_emptyQuery() {
        QueryBuilder innerQuery = request.buildQuery("", Collections.emptyList());
        QueryBuilder result = request.buildFunctionScoreQuery("", innerQuery);

        assertNotNull(result);
        assertTrue(result instanceof FunctionScoreQueryBuilder);
    }

    @Test
    public void test_buildFunctionScoreQuery_containsFrequencyFactors() {
        QueryBuilder innerQuery = request.buildQuery("test", Collections.emptyList());
        QueryBuilder result = request.buildFunctionScoreQuery("test", innerQuery);

        String queryString = result.toString();
        // Should contain doc_freq, query_freq, and user_boost factors
        assertTrue("Should contain doc_freq", queryString.contains("doc_freq"));
        assertTrue("Should contain query_freq", queryString.contains("query_freq"));
        assertTrue("Should contain user_boost", queryString.contains("user_boost"));
    }

    @Test
    public void test_buildFunctionScoreQuery_customPrefixMatchWeight() {
        request.setPrefixMatchWeight(5.0f);

        QueryBuilder innerQuery = request.buildQuery("test", Collections.emptyList());
        QueryBuilder result = request.buildFunctionScoreQuery("test", innerQuery);

        assertNotNull(result);
        String queryString = result.toString();
        assertTrue("Should contain weight factor", queryString.contains("weight"));
    }

    // ============================================================
    // Tests for isFirstWordMatching method
    // ============================================================

    @Test
    public void test_isFirstWordMatching_singleWordMatchingText() {
        request.setMatchWordFirst(true);
        boolean result = request.isFirstWordMatching(true, false, "testing");
        assertTrue("Single word query containing 'test' should match", result);
    }

    @Test
    public void test_isFirstWordMatching_singleWordNotMatchingText() {
        request.setMatchWordFirst(true);
        // Need to set the query via reflection or subclass since we're testing with a specific query
        boolean result = request.testIsFirstWordMatching(true, false, "other", "test");
        assertFalse("Text not containing query should not match", result);
    }

    @Test
    public void test_isFirstWordMatching_hiraganaQuery() {
        request.setMatchWordFirst(true);
        boolean result = request.isFirstWordMatching(true, true, "あいうえお");
        assertFalse("Hiragana query should not match first", result);
    }

    @Test
    public void test_isFirstWordMatching_multiWordQuery() {
        request.setMatchWordFirst(true);
        boolean result = request.isFirstWordMatching(false, false, "hello world");
        assertFalse("Multi-word query should not match first", result);
    }

    @Test
    public void test_isFirstWordMatching_matchWordFirstDisabled() {
        request.setMatchWordFirst(false);
        boolean result = request.isFirstWordMatching(true, false, "testing");
        assertFalse("With matchWordFirst disabled, should return false", result);
    }

    @Test
    public void test_isFirstWordMatching_singleHiraganaChar() {
        request.setMatchWordFirst(true);
        // Single hiragana character should not match first
        boolean result = request.testIsFirstWordMatching(true, false, "あtest", "あ");
        assertFalse("Single hiragana char query should not match first", result);
    }

    @Test
    public void test_isFirstWordMatching_singleNonHiraganaChar() {
        request.setMatchWordFirst(true);
        boolean result = request.testIsFirstWordMatching(true, false, "atest", "a");
        assertTrue("Single non-hiragana char query should match first", result);
    }

    // ============================================================
    // Tests for configuration setters
    // ============================================================

    @Test
    public void test_setSkipDuplicateWords() {
        request.setSkipDuplicateWords(true);
        // Verify setting doesn't throw
        request.setSkipDuplicateWords(false);
    }

    @Test
    public void test_setSize() {
        request.setSize(5);
        request.setSize(100);
        request.setSize(1);
    }

    @Test
    public void test_addTag() {
        request.addTag("tag1");
        request.addTag("tag2");
    }

    @Test
    public void test_addRole() {
        request.addRole("role1");
        request.addRole("role2");
    }

    @Test
    public void test_addField() {
        request.addField("field1");
        request.addField("field2");
    }

    @Test
    public void test_addKind() {
        request.addKind("DOCUMENT");
        request.addKind("QUERY");
    }

    @Test
    public void test_addLang() {
        request.addLang("en");
        request.addLang("ja");
    }

    @Test
    public void test_setSuggestDetail() {
        request.setSuggestDetail(true);
        request.setSuggestDetail(false);
    }

    // ============================================================
    // Helper class to expose protected methods for testing
    // ============================================================

    /**
     * Testable subclass that exposes protected methods.
     */
    private static class TestableSuggestRequest extends SuggestRequest {
        private String testQuery = "test";

        @Override
        public QueryBuilder buildQuery(String q, List<String> fields) {
            return super.buildQuery(q, fields);
        }

        @Override
        public QueryBuilder buildFilterQuery(String fieldName, List<String> words) {
            return super.buildFilterQuery(fieldName, words);
        }

        @Override
        public QueryBuilder buildFunctionScoreQuery(String query, QueryBuilder queryBuilder) {
            return super.buildFunctionScoreQuery(query, queryBuilder);
        }

        @Override
        public boolean isFirstWordMatching(boolean singleWordQuery, boolean hiraganaQuery, String text) {
            // For the parent implementation, we need to set query to make it work properly
            setQuery(testQuery);
            return super.isFirstWordMatching(singleWordQuery, hiraganaQuery, text);
        }

        public boolean testIsFirstWordMatching(boolean singleWordQuery, boolean hiraganaQuery, String text, String query) {
            this.testQuery = query;
            setQuery(query);
            return super.isFirstWordMatching(singleWordQuery, hiraganaQuery, text);
        }
    }
}
