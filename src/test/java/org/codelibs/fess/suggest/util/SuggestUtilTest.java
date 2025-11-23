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

import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.codelibs.core.CoreLibConstants;
import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.settings.AnalyzerSettings;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.transport.client.Client;

public class SuggestUtilTest {

    private static OpenSearchRunner runner;
    private static Client client;
    private static SuggestSettings settings;
    private static final String TEST_INDEX = "test_suggest_util";

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        }).build(newConfigs().clusterName("SuggestUtilTest").numOfNode(1).pluginTypes("org.codelibs.opensearch.extension.ExtensionPlugin"));
        runner.ensureYellow();
        client = runner.client();

        // Initialize suggest settings
        Suggester suggester = Suggester.builder().build(client, "test");
        settings = suggester.settings();

        // Create test index
        client.admin().indices().prepareCreate(TEST_INDEX).execute().actionGet();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        runner.close();
        runner.clean();
    }

    @Before
    public void setUp() {
        // Setup for each test
    }

    @Test
    public void testPrivateConstructor() throws Exception {
        // Test that SuggestUtil has a private constructor to prevent instantiation
        Constructor<SuggestUtil> constructor = SuggestUtil.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));

        // Make it accessible to test it throws no exception
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    public void testCreateSuggestTextId() {
        // Test normal text
        String text = "test text";
        String id = SuggestUtil.createSuggestTextId(text);
        assertNotNull(id);
        assertEquals(Base64.getEncoder().encodeToString(text.getBytes(CoreLibConstants.CHARSET_UTF_8)), id);

        // Test empty text
        String emptyText = "";
        String emptyId = SuggestUtil.createSuggestTextId(emptyText);
        assertNotNull(emptyId);
        assertEquals(Base64.getEncoder().encodeToString(emptyText.getBytes(CoreLibConstants.CHARSET_UTF_8)), emptyId);

        // Test special characters
        String specialText = "テスト@#$%^&*()";
        String specialId = SuggestUtil.createSuggestTextId(specialText);
        assertNotNull(specialId);
        assertEquals(Base64.getEncoder().encodeToString(specialText.getBytes(CoreLibConstants.CHARSET_UTF_8)), specialId);
    }

    @Test
    public void testCreateSuggestTextIdWithLongText() {
        // Test text that exceeds maximum length after encoding
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            sb.append("0123456789");
        }
        String longText = sb.toString();
        String id = SuggestUtil.createSuggestTextId(longText);
        assertNotNull(id);
        assertEquals(445, id.length()); // Should be truncated to ID_MAX_LENGTH
    }

    @Test
    public void testParseQuery() {
        // Test normal query
        String query = "test query";
        String field = "content";
        String[] keywords = SuggestUtil.parseQuery(query, field);
        assertNotNull(keywords);
        assertEquals(2, keywords.length);
        assertEquals("test", keywords[0]);
        assertEquals("query", keywords[1]);

        // Test empty query
        String[] emptyKeywords = SuggestUtil.parseQuery("", field);
        assertNotNull(emptyKeywords);
        assertEquals(0, emptyKeywords.length);

        // Test single word query
        String[] singleKeyword = SuggestUtil.parseQuery("single", field);
        assertNotNull(singleKeyword);
        assertEquals(1, singleKeyword.length);
        assertEquals("single", singleKeyword[0]);
    }

    @Test
    public void testParseQueryWithTooManyTerms() {
        // Test query with more than MAX_QUERY_TERM_NUM (5) terms
        String query = "one two three four five six seven";
        String field = "content";
        String[] keywords = SuggestUtil.parseQuery(query, field);
        assertNotNull(keywords);
        assertEquals(0, keywords.length); // Should return empty array
    }

    @Test
    public void testParseQueryWithLongTerm() {
        // Test query with term longer than MAX_QUERY_TERM_LENGTH (48)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("a");
        }
        String query = sb.toString();
        String field = "content";
        String[] keywords = SuggestUtil.parseQuery(query, field);
        assertNotNull(keywords);
        assertEquals(0, keywords.length); // Should return empty array
    }

    @Test
    public void testGetKeywords() {
        // Test normal query
        String query = "test AND query";
        String[] fields = { "content", "title" };
        List<String> keywords = SuggestUtil.getKeywords(query, fields);
        assertNotNull(keywords);
        assertEquals(2, keywords.size());
        assertTrue(keywords.contains("test"));
        assertTrue(keywords.contains("query"));

        // Test with OR operator
        List<String> orKeywords = SuggestUtil.getKeywords("test OR query", fields);
        assertNotNull(orKeywords);
        assertEquals(2, orKeywords.size());

        // Test with invalid query
        List<String> invalidKeywords = SuggestUtil.getKeywords("((invalid", fields);
        assertNotNull(invalidKeywords);
        assertEquals(0, invalidKeywords.size());
    }

    @Test
    public void testGetKeywordsWithDuplicates() {
        // Test that duplicates are removed
        String query = "test test query";
        String[] fields = { "content" };
        List<String> keywords = SuggestUtil.getKeywords(query, fields);
        assertNotNull(keywords);
        assertEquals(2, keywords.size());
        assertTrue(keywords.contains("test"));
        assertTrue(keywords.contains("query"));
    }

    @Test
    public void testGetTermQueryList() {
        // Test with TermQuery
        Term term = new Term("content", "test");
        TermQuery termQuery = new TermQuery(term);
        String[] fields = { "content" };
        List<TermQuery> queryList = SuggestUtil.getTermQueryList(termQuery, fields);
        assertNotNull(queryList);
        assertEquals(1, queryList.size());
        assertEquals(termQuery, queryList.get(0));

        // Test with non-matching field
        String[] wrongFields = { "title" };
        List<TermQuery> emptyList = SuggestUtil.getTermQueryList(termQuery, wrongFields);
        assertNotNull(emptyList);
        assertEquals(0, emptyList.size());
    }

    @Test
    public void testGetTermQueryListWithBooleanQuery() {
        // Test with BooleanQuery
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        Term term1 = new Term("content", "test");
        Term term2 = new Term("title", "query");
        builder.add(new TermQuery(term1), BooleanClause.Occur.SHOULD);
        builder.add(new TermQuery(term2), BooleanClause.Occur.SHOULD);
        BooleanQuery booleanQuery = builder.build();

        String[] fields = { "content", "title" };
        List<TermQuery> queryList = SuggestUtil.getTermQueryList(booleanQuery, fields);
        assertNotNull(queryList);
        assertEquals(2, queryList.size());
    }

    @Test
    public void testGetTermQueryListWithNestedBooleanQuery() {
        // Test with nested BooleanQuery
        BooleanQuery.Builder innerBuilder = new BooleanQuery.Builder();
        innerBuilder.add(new TermQuery(new Term("content", "inner")), BooleanClause.Occur.SHOULD);

        BooleanQuery.Builder outerBuilder = new BooleanQuery.Builder();
        outerBuilder.add(innerBuilder.build(), BooleanClause.Occur.SHOULD);
        outerBuilder.add(new TermQuery(new Term("content", "outer")), BooleanClause.Occur.SHOULD);
        BooleanQuery nestedQuery = outerBuilder.build();

        String[] fields = { "content" };
        List<TermQuery> queryList = SuggestUtil.getTermQueryList(nestedQuery, fields);
        assertNotNull(queryList);
        assertEquals(2, queryList.size());
    }

    @Test
    public void testCreateBulkLine() {
        // Test creating bulk line for indexing
        SuggestItem item = new SuggestItem(new String[] { "test text" }, new String[][] { { "reading1" }, { "reading2" } },
                new String[] { "field1", "field2" }, 10, // queryFreq
                5, // docFreq
                2.0f, // userBoost
                new String[] { "tag1", "tag2" }, new String[] { "role1" }, new String[] {}, // languages
                SuggestItem.Kind.DOCUMENT);
        item.setTimestamp(ZonedDateTime.now());

        String bulkLine = SuggestUtil.createBulkLine("test_index", "_doc", item);
        assertNotNull(bulkLine);
        assertTrue(bulkLine.contains("test_index"));
        assertTrue(bulkLine.contains("_doc"));
        assertTrue(bulkLine.contains("test text"));
        assertTrue(bulkLine.contains("reading1"));
        assertTrue(bulkLine.contains("reading2"));
    }

    @Test(expected = SuggesterException.class)
    public void testCreateBulkLineWithNullItem() {
        // Test that null item causes exception
        SuggestUtil.createBulkLine("test_index", "_doc", null);
    }

    @Test
    public void testCreateDefaultReadingConverter() {
        // Test creating default reading converter
        ReadingConverter converter = SuggestUtil.createDefaultReadingConverter(client, settings);
        assertNotNull(converter);
    }

    @Test
    public void testCreateDefaultContentsReadingConverter() {
        // Test creating default contents reading converter
        ReadingConverter converter = SuggestUtil.createDefaultContentsReadingConverter(client, settings);
        assertNotNull(converter);
    }

    @Test
    public void testCreateDefaultNormalizer() {
        // Test creating default normalizer
        Normalizer normalizer = SuggestUtil.createDefaultNormalizer(client, settings);
        assertNotNull(normalizer);
    }

    @Test
    public void testCreateDefaultAnalyzer() {
        // Test creating default analyzer
        AnalyzerSettings.DefaultContentsAnalyzer analyzer = SuggestUtil.createDefaultAnalyzer(client, settings);
        assertNotNull(analyzer);
    }

    @Test
    public void testGetAsList() {
        // Test with null
        List<String> nullList = SuggestUtil.getAsList(null);
        assertNotNull(nullList);
        assertEquals(0, nullList.size());

        // Test with String
        List<String> stringList = SuggestUtil.getAsList("test");
        assertNotNull(stringList);
        assertEquals(1, stringList.size());
        assertEquals("test", stringList.get(0));

        // Test with List
        List<String> originalList = Arrays.asList("one", "two", "three");
        List<String> resultList = SuggestUtil.getAsList(originalList);
        assertNotNull(resultList);
        assertEquals(3, resultList.size());
        assertEquals(originalList, resultList);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAsListWithInvalidType() {
        // Test with invalid type (should throw exception)
        SuggestUtil.getAsList(123);
    }

    @Test
    public void testDeleteByQuery() {
        // Create test data
        client.prepareIndex().setIndex(TEST_INDEX).setId("1").setSource("field", "value1").execute().actionGet();
        client.prepareIndex().setIndex(TEST_INDEX).setId("2").setSource("field", "value2").execute().actionGet();
        client.admin().indices().prepareRefresh(TEST_INDEX).execute().actionGet();

        // Delete by query
        boolean result = SuggestUtil.deleteByQuery(client, settings, TEST_INDEX, QueryBuilders.termQuery("field", "value1"));
        assertTrue(result);

        // Verify deletion
        client.admin().indices().prepareRefresh(TEST_INDEX).execute().actionGet();
        long count = client.prepareSearch(TEST_INDEX)
                .setQuery(QueryBuilders.termQuery("field", "value1"))
                .execute()
                .actionGet()
                .getHits()
                .getTotalHits()
                .value();
        assertEquals(0, count);
    }

    @Test
    public void testDeleteByQueryWithNoMatches() {
        // Test delete with query that matches nothing
        boolean result = SuggestUtil.deleteByQuery(client, settings, TEST_INDEX, QueryBuilders.termQuery("nonexistent", "value"));
        assertTrue(result); // Should still return true even with no matches
    }

    @Test(expected = SuggesterException.class)
    public void testDeleteByQueryWithInvalidIndex() {
        // Test delete with non-existent index
        SuggestUtil.deleteByQuery(client, settings, "nonexistent_index", QueryBuilders.matchAllQuery());
    }

    @Test
    public void testDeleteScrollContext() {
        // Test with null scrollId (should not throw exception)
        SuggestUtil.deleteScrollContext(client, null);

        // Test with non-null scrollId
        String scrollId = "test_scroll_id";
        SuggestUtil.deleteScrollContext(client, scrollId);
        // No exception should be thrown
    }

    @Test
    public void testEscapeWildcardQuery() {
        // Test escaping asterisk
        String query1 = "test*query";
        String escaped1 = SuggestUtil.escapeWildcardQuery(query1);
        assertEquals("test\\*query", escaped1);

        // Test escaping question mark
        String query2 = "test?query";
        String escaped2 = SuggestUtil.escapeWildcardQuery(query2);
        assertEquals("test\\?query", escaped2);

        // Test escaping both
        String query3 = "test*query?end";
        String escaped3 = SuggestUtil.escapeWildcardQuery(query3);
        assertEquals("test\\*query\\?end", escaped3);

        // Test with no wildcards
        String query4 = "test query";
        String escaped4 = SuggestUtil.escapeWildcardQuery(query4);
        assertEquals("test query", escaped4);

        // Test with multiple wildcards
        String query5 = "***???";
        String escaped5 = SuggestUtil.escapeWildcardQuery(query5);
        assertEquals("\\*\\*\\*\\?\\?\\?", escaped5);

        // Test empty string
        String query6 = "";
        String escaped6 = SuggestUtil.escapeWildcardQuery(query6);
        assertEquals("", escaped6);
    }

    @Test
    public void testGetAsListWithEmptyList() {
        // Test with empty list
        List<String> emptyList = new ArrayList<>();
        List<String> result = SuggestUtil.getAsList(emptyList);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testGetKeywordsWithEmptyField() {
        // Test with empty field array
        String query = "test query";
        String[] fields = {};
        List<String> keywords = SuggestUtil.getKeywords(query, fields);
        assertNotNull(keywords);
        assertEquals(0, keywords.size()); // No keywords because no fields match
    }

    @Test
    public void testGetKeywordsWithComplexQuery() {
        // Test with complex query structure
        String query = "(test AND query) OR (another AND search)";
        String[] fields = { "content" };
        List<String> keywords = SuggestUtil.getKeywords(query, fields);
        assertNotNull(keywords);
        // Should extract all unique terms
        assertTrue(keywords.size() > 0);
    }

    @Test
    public void testCreateBulkLineWithMinimalItem() {
        // Test with minimal SuggestItem
        SuggestItem item = new SuggestItem(new String[] { "minimal" }, new String[0][0], new String[0], 0, // queryFreq
                0, // docFreq
                1.0f, // userBoost
                new String[0], new String[0], new String[0], SuggestItem.Kind.DOCUMENT);
        item.setTimestamp(ZonedDateTime.now());

        String bulkLine = SuggestUtil.createBulkLine("test_index", "_doc", item);
        assertNotNull(bulkLine);
        assertTrue(bulkLine.contains("minimal"));
    }

    @Test
    public void testParseQueryWithSpecialCharacters() {
        // Test query with special characters
        String query = "test+query-search";
        String field = "content";
        String[] keywords = SuggestUtil.parseQuery(query, field);
        assertNotNull(keywords);
        // Parser should handle special characters
    }

    @Test
    public void testGetTermQueryListWithNull() {
        // Test with null query
        String[] fields = { "content" };
        List<TermQuery> queryList = SuggestUtil.getTermQueryList(null, fields);
        assertNotNull(queryList);
        assertEquals(0, queryList.size());
    }

    // Additional tests for createSuggestTextId boundary cases
    @Test
    public void testCreateSuggestTextIdBoundaryExactly445() {
        // Create text that encodes to exactly 445 characters
        // Base64 encoding increases size by ~4/3, so original size should be ~333 bytes
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 333; i++) {
            sb.append("a");
        }
        String text = sb.toString();
        String id = SuggestUtil.createSuggestTextId(text);
        assertNotNull(id);
        // Should be exactly 445 or less
        assertTrue(id.length() <= 445);
    }

    @Test
    public void testCreateSuggestTextIdBoundaryJustUnder445() {
        // Create text that encodes to just under 445 characters
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            sb.append("b");
        }
        String text = sb.toString();
        String id = SuggestUtil.createSuggestTextId(text);
        assertNotNull(id);
        String expected = Base64.getEncoder().encodeToString(text.getBytes(CoreLibConstants.CHARSET_UTF_8));
        assertEquals(expected, id); // Should not be truncated
    }

    @Test
    public void testCreateSuggestTextIdWithUnicodeCharacters() {
        // Test with various Unicode characters that may encode differently
        String unicodeText = "日本語テスト🎌🗾こんにちは世界";
        String id = SuggestUtil.createSuggestTextId(unicodeText);
        assertNotNull(id);
        assertTrue(id.length() <= 445);
    }

    @Test
    public void testCreateSuggestTextIdConsistency() {
        // Test that same input always produces same output
        String text = "consistency test";
        String id1 = SuggestUtil.createSuggestTextId(text);
        String id2 = SuggestUtil.createSuggestTextId(text);
        assertEquals(id1, id2);
    }

    // Additional tests for getAsList edge cases
    @Test
    public void testGetAsListWithSingleCharacterString() {
        // Test with single character
        List<String> result = SuggestUtil.getAsList("x");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("x", result.get(0));
    }

    @Test
    public void testGetAsListWithEmptyString() {
        // Test with empty string
        List<String> result = SuggestUtil.getAsList("");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("", result.get(0));
    }

    @Test
    public void testGetAsListWithWhitespaceString() {
        // Test with whitespace string
        List<String> result = SuggestUtil.getAsList("   ");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("   ", result.get(0));
    }

    @Test
    public void testGetAsListWithListContainingNulls() {
        // Test with list containing null elements
        List<String> listWithNulls = new ArrayList<>();
        listWithNulls.add("valid");
        listWithNulls.add(null);
        listWithNulls.add("another");

        List<String> result = SuggestUtil.getAsList(listWithNulls);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("valid", result.get(0));
        assertEquals(null, result.get(1));
        assertEquals("another", result.get(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAsListWithArray() {
        // Test with array (should throw exception)
        String[] array = { "one", "two" };
        SuggestUtil.getAsList(array);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAsListWithDouble() {
        // Test with double (should throw exception)
        SuggestUtil.getAsList(3.14);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAsListWithBoolean() {
        // Test with boolean (should throw exception)
        SuggestUtil.getAsList(true);
    }

    // Additional tests for createBulkLine validation
    @Test(expected = SuggesterException.class)
    public void testCreateBulkLineWithNullId() {
        // Test that null ID causes exception
        SuggestItem item = new SuggestItem(new String[] { "text" }, new String[0][0], new String[0], 0, 0, 1.0f, new String[0],
                new String[0], new String[0], SuggestItem.Kind.DOCUMENT);
        // Explicitly set ID to null (constructor auto-generates ID)
        item.setId(null);
        item.setTimestamp(ZonedDateTime.now());
        SuggestUtil.createBulkLine("test_index", "_doc", item);
    }

    @Test(expected = SuggesterException.class)
    public void testCreateBulkLineWithNullText() {
        // Test that null text causes exception
        // Create item with valid text first, then set to null
        SuggestItem item = new SuggestItem(new String[] { "text" }, new String[0][0], new String[0], 0, 0, 1.0f, new String[0],
                new String[0], new String[0], SuggestItem.Kind.DOCUMENT);
        // Set text to null after construction
        item.setText(null);
        item.setTimestamp(ZonedDateTime.now());
        SuggestUtil.createBulkLine("test_index", "_doc", item);
    }

    @Test
    public void testCreateBulkLineWithMultipleReadings() {
        // Test with multiple reading levels
        SuggestItem item = new SuggestItem(new String[] { "test" },
                new String[][] { { "reading1a", "reading1b" }, { "reading2a", "reading2b" }, { "reading3" } }, new String[] { "field1" }, 5,
                3, 1.5f, new String[] { "tag1" }, new String[] { "role1" }, new String[0], SuggestItem.Kind.QUERY);
        item.setTimestamp(ZonedDateTime.now());

        String bulkLine = SuggestUtil.createBulkLine("test_index", "_doc", item);
        assertNotNull(bulkLine);
        assertTrue(bulkLine.contains("reading1a"));
        assertTrue(bulkLine.contains("reading2a"));
        assertTrue(bulkLine.contains("reading3"));
    }

    @Test
    public void testCreateBulkLineWithEmptyArrays() {
        // Test with empty arrays for optional fields
        SuggestItem item = new SuggestItem(new String[] { "text" }, new String[0][0], new String[0], 0, 0, 1.0f, new String[0],
                new String[0], new String[0], SuggestItem.Kind.DOCUMENT);
        item.setTimestamp(ZonedDateTime.now());

        String bulkLine = SuggestUtil.createBulkLine("test_index", "_doc", item);
        assertNotNull(bulkLine);
        assertTrue(bulkLine.length() > 0);
    }

    // Additional tests for parseQuery edge cases
    @Test
    public void testParseQueryWithExactly5Terms() {
        // Test query with exactly MAX_QUERY_TERM_NUM (5) terms - should succeed
        String query = "one two three four five";
        String field = "content";
        String[] keywords = SuggestUtil.parseQuery(query, field);
        assertNotNull(keywords);
        assertEquals(5, keywords.length);
    }

    @Test
    public void testParseQueryWithExactly48CharacterTerm() {
        // Test query with term exactly at MAX_QUERY_TERM_LENGTH (48) - should succeed
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 48; i++) {
            sb.append("a");
        }
        String query = sb.toString();
        String field = "content";
        String[] keywords = SuggestUtil.parseQuery(query, field);
        assertNotNull(keywords);
        assertEquals(1, keywords.length);
        assertEquals(48, keywords[0].length());
    }

    @Test
    public void testParseQueryWithExactly49CharacterTerm() {
        // Test query with term just over MAX_QUERY_TERM_LENGTH (49) - should fail
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 49; i++) {
            sb.append("a");
        }
        String query = sb.toString();
        String field = "content";
        String[] keywords = SuggestUtil.parseQuery(query, field);
        assertNotNull(keywords);
        assertEquals(0, keywords.length); // Should return empty array
    }

    // Additional tests for escapeWildcardQuery
    @Test
    public void testEscapeWildcardQueryWithMixedContent() {
        // Test with realistic query containing wildcards and regular text
        String query = "find*all?documents.pdf";
        String escaped = SuggestUtil.escapeWildcardQuery(query);
        assertEquals("find\\*all\\?documents.pdf", escaped);
    }

    @Test
    public void testEscapeWildcardQueryWithJapanese() {
        // Test with Japanese text containing wildcards
        String query = "テスト*検索?クエリ";
        String escaped = SuggestUtil.escapeWildcardQuery(query);
        assertEquals("テスト\\*検索\\?クエリ", escaped);
    }
}