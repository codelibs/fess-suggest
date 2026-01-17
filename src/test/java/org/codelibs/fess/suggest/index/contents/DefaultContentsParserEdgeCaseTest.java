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
package org.codelibs.fess.suggest.index.contents;

import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.converter.KatakanaToAlphabetConverter;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.converter.ReadingConverterChain;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLog;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.normalizer.NormalizerChain;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Edge case tests for DefaultContentsParser.
 * Tests boundary conditions, null handling, and error scenarios.
 */
public class DefaultContentsParserEdgeCaseTest {
    static Suggester suggester;
    static OpenSearchRunner runner;
    DefaultContentsParser parser = new DefaultContentsParser();
    String[] supportedFields = new String[] { "content", "title" };
    String[] tagFieldNames = new String[] { "label", "virtual_host" };
    String roleFieldName = "role";

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        })
                .build(newConfigs().clusterName("DefaultContentsParserEdgeCaseTest")
                        .numOfNode(1)
                        .pluginTypes("org.codelibs.opensearch.extension.ExtensionPlugin"));
        runner.ensureYellow();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (runner != null) {
            runner.close();
            runner.clean();
        }
    }

    @Before
    public void before() throws Exception {
        try {
            runner.admin().indices().prepareDelete("DefaultContentsParserEdgeCaseTest*", "fess_suggest*").execute().actionGet();
        } catch (Exception e) {
            // ignore
        }
        runner.refresh();
        suggester = Suggester.builder().build(runner.client(), "DefaultContentsParserEdgeCaseTest");
        suggester.createIndexIfNothing();
    }

    // ============================================================
    // Tests for parseSearchWords edge cases
    // ============================================================

    @Test
    public void test_parseSearchWords_emptyArray() throws Exception {
        String[] words = new String[0];
        String[] fields = new String[] { "content" };

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        SuggestItem item = parser.parseSearchWords(words, null, fields, null, null, 1, createDefaultReadingConverter(),
                createDefaultNormalizer(), analyzer, null);

        assertNull("Empty words array should return null", item);
    }

    @Test
    public void test_parseSearchWords_nullFields() throws Exception {
        String[] words = new String[] { "test" };

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        SuggestItem item = parser.parseSearchWords(words, null, null, null, null, 1, createDefaultReadingConverter(),
                createDefaultNormalizer(), analyzer, null);

        // Should handle null fields gracefully
        assertNotNull(item);
    }

    @Test
    public void test_parseSearchWords_emptyFields() throws Exception {
        String[] words = new String[] { "test" };
        String[] fields = new String[0];

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        SuggestItem item = parser.parseSearchWords(words, null, fields, null, null, 1, createDefaultReadingConverter(),
                createDefaultNormalizer(), analyzer, null);

        // Should handle empty fields gracefully
        assertNotNull(item);
    }

    @Test
    public void test_parseSearchWords_withNullReadingsArray() throws Exception {
        String[] words = new String[] { "test" };
        String[] fields = new String[] { "content" };

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        SuggestItem item = parser.parseSearchWords(words, null, fields, null, null, 1, createDefaultReadingConverter(),
                createDefaultNormalizer(), analyzer, null);

        assertNotNull(item);
        assertEquals("test", item.getText());
    }

    @Test
    public void test_parseSearchWords_withPartialReadings() throws Exception {
        String[] words = new String[] { "word1", "word2", "word3" };
        // Only provide readings for first word
        String[][] readings = new String[][] { new String[] { "reading1" } };
        String[] fields = new String[] { "content" };

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        SuggestItem item = parser.parseSearchWords(words, readings, fields, null, null, 1, createDefaultReadingConverter(),
                createDefaultNormalizer(), analyzer, null);

        assertNotNull(item);
    }

    @Test
    public void test_parseSearchWords_withEmptyReadingsForWord() throws Exception {
        String[] words = new String[] { "test" };
        String[][] readings = new String[][] { new String[0] }; // Empty readings for word
        String[] fields = new String[] { "content" };

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        SuggestItem item = parser.parseSearchWords(words, readings, fields, null, null, 1, createDefaultReadingConverter(),
                createDefaultNormalizer(), analyzer, null);

        assertNotNull(item);
    }

    @Test
    public void test_parseSearchWords_allWordsExcluded() throws Exception {
        // Words that will likely be excluded by analyzer
        String[] words = new String[] { "　", "   ", "" };
        String[] fields = new String[] { "content" };

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        SuggestItem item = parser.parseSearchWords(words, null, fields, null, null, 1, createDefaultReadingConverter(),
                createDefaultNormalizer(), analyzer, null);

        // All words excluded should return null
        assertNull(item);
    }

    @Test
    public void test_parseSearchWords_withEmptyStrings() throws Exception {
        String[] words = new String[] { "", "test", "" };
        String[] fields = new String[] { "content" };

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        SuggestItem item = parser.parseSearchWords(words, null, fields, null, null, 1, createDefaultReadingConverter(),
                createDefaultNormalizer(), analyzer, null);

        // Empty strings should be filtered, valid words should remain
        assertNotNull(item);
    }

    @Test
    public void test_parseSearchWords_withMultipleLanguages() throws Exception {
        String[] words = new String[] { "test" };
        String[] fields = new String[] { "content" };
        String[] langs = new String[] { "en", "ja", "zh" };

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        SuggestItem item = parser.parseSearchWords(words, null, fields, null, null, 1, createDefaultReadingConverter(),
                createDefaultNormalizer(), analyzer, langs);

        assertNotNull(item);
        assertTrue(item.getLanguages().length > 0);
    }

    // ============================================================
    // Tests for parseQueryLog edge cases
    // ============================================================

    @Test
    public void test_parseQueryLog_emptyQueryString() throws Exception {
        QueryLog queryLog = new QueryLog("", null);
        List<SuggestItem> items = parser.parseQueryLog(queryLog, supportedFields, tagFieldNames, roleFieldName,
                createDefaultReadingConverter(), createDefaultNormalizer());

        assertEquals("Empty query should return empty list", 0, items.size());
    }

    @Test
    public void test_parseQueryLog_queryWithNoSupportedFields() throws Exception {
        QueryLog queryLog = new QueryLog("unsupported_field:test", null);
        List<SuggestItem> items = parser.parseQueryLog(queryLog, supportedFields, tagFieldNames, roleFieldName,
                createDefaultReadingConverter(), createDefaultNormalizer());

        // Query for unsupported field should return empty
        assertEquals(0, items.size());
    }

    @Test
    public void test_parseQueryLog_withNullFilterQuery() throws Exception {
        QueryLog queryLog = new QueryLog("content:test", null);
        List<SuggestItem> items = parser.parseQueryLog(queryLog, supportedFields, tagFieldNames, roleFieldName,
                createDefaultReadingConverter(), createDefaultNormalizer());

        assertTrue("Should parse query with null filter", items.size() > 0);
    }

    @Test
    public void test_parseQueryLog_withEmptyFilterQuery() throws Exception {
        QueryLog queryLog = new QueryLog("content:test", "");
        List<SuggestItem> items = parser.parseQueryLog(queryLog, supportedFields, tagFieldNames, roleFieldName,
                createDefaultReadingConverter(), createDefaultNormalizer());

        assertTrue("Should parse query with empty filter", items.size() > 0);
    }

    @Test
    public void test_parseQueryLog_withMultipleTags() throws Exception {
        QueryLog queryLog = new QueryLog("content:test AND label:tag1 AND label:tag2 AND virtual_host:host1", null);
        List<SuggestItem> items = parser.parseQueryLog(queryLog, supportedFields, tagFieldNames, roleFieldName,
                createDefaultReadingConverter(), createDefaultNormalizer());

        assertTrue(items.size() > 0);
        SuggestItem item = items.get(0);
        assertTrue("Should have multiple tags", item.getTags().length >= 2);
    }

    @Test
    public void test_parseQueryLog_withMultipleRoles() throws Exception {
        QueryLog queryLog = new QueryLog("content:test AND role:admin AND role:user", null);
        List<SuggestItem> items = parser.parseQueryLog(queryLog, supportedFields, tagFieldNames, roleFieldName,
                createDefaultReadingConverter(), createDefaultNormalizer());

        assertTrue(items.size() > 0);
        SuggestItem item = items.get(0);
        assertTrue("Should have multiple roles", item.getRoles().length >= 2);
    }

    @Test
    public void test_parseQueryLog_rolesFromBothQueryAndFilter() throws Exception {
        QueryLog queryLog = new QueryLog("content:test AND role:role1", "role:role2");
        List<SuggestItem> items = parser.parseQueryLog(queryLog, supportedFields, tagFieldNames, roleFieldName,
                createDefaultReadingConverter(), createDefaultNormalizer());

        assertTrue(items.size() > 0);
        SuggestItem item = items.get(0);
        assertTrue("Should have roles from both query and filter", item.getRoles().length >= 2);
    }

    @Test
    public void test_parseQueryLog_multipleFieldsMatched() throws Exception {
        QueryLog queryLog = new QueryLog("content:test1 AND title:test2", null);
        List<SuggestItem> items = parser.parseQueryLog(queryLog, supportedFields, tagFieldNames, roleFieldName,
                createDefaultReadingConverter(), createDefaultNormalizer());

        // Should create items for each field that matches
        assertEquals("Should have items for both fields", 2, items.size());
    }

    @Test
    public void test_parseQueryLog_emptyTagFieldNames() throws Exception {
        QueryLog queryLog = new QueryLog("content:test", null);
        List<SuggestItem> items = parser.parseQueryLog(queryLog, supportedFields, new String[0], roleFieldName,
                createDefaultReadingConverter(), createDefaultNormalizer());

        assertTrue(items.size() > 0);
        assertEquals("Should have no tags with empty tag field names", 0, items.get(0).getTags().length);
    }

    // ============================================================
    // Tests for parseDocument edge cases
    // ============================================================

    @Test
    public void test_parseDocument_emptyDocument() throws Exception {
        Map<String, Object> document = new HashMap<>();

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        List<SuggestItem> items = parser.parseDocument(document, supportedFields, tagFieldNames, roleFieldName, "lang",
                createDefaultReadingConverter(), createDefaultReadingConverter(), createDefaultNormalizer(), analyzer);

        assertEquals("Empty document should return empty list", 0, items.size());
    }

    @Test
    public void test_parseDocument_nullFieldValue() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("content", null);

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        List<SuggestItem> items = parser.parseDocument(document, supportedFields, tagFieldNames, roleFieldName, "lang",
                createDefaultReadingConverter(), createDefaultReadingConverter(), createDefaultNormalizer(), analyzer);

        assertEquals("Null field value should be skipped", 0, items.size());
    }

    @Test
    public void test_parseDocument_emptyStringFieldValue() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("content", "");

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        List<SuggestItem> items = parser.parseDocument(document, supportedFields, tagFieldNames, roleFieldName, "lang",
                createDefaultReadingConverter(), createDefaultReadingConverter(), createDefaultNormalizer(), analyzer);

        // Empty string should produce empty or no items
        assertTrue("Empty content should not produce items", items.size() == 0);
    }

    @Test
    public void test_parseDocument_whitespaceOnlyContent() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("content", "   \t\n   ");

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        List<SuggestItem> items = parser.parseDocument(document, supportedFields, tagFieldNames, roleFieldName, "lang",
                createDefaultReadingConverter(), createDefaultReadingConverter(), createDefaultNormalizer(), analyzer);

        // Whitespace-only content should produce empty or filtered items
        // Items with blank words should be filtered
        for (SuggestItem item : items) {
            assertTrue("Items should not have blank text", item.getText().trim().length() > 0);
        }
    }

    @Test
    public void test_parseDocument_withLanguage() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("content", "テスト");
        document.put("lang", "ja");

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        List<SuggestItem> items = parser.parseDocument(document, supportedFields, tagFieldNames, roleFieldName, "lang",
                createDefaultReadingConverter(), createDefaultReadingConverter(), createDefaultNormalizer(), analyzer);

        assertTrue(items.size() > 0);
        SuggestItem item = items.get(0);
        assertTrue("Should have language", item.getLanguages().length > 0);
        assertEquals("ja", item.getLanguages()[0]);
    }

    @Test
    public void test_parseDocument_withNullLanguage() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("content", "test");
        // No lang field

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        List<SuggestItem> items = parser.parseDocument(document, supportedFields, tagFieldNames, roleFieldName, "lang",
                createDefaultReadingConverter(), createDefaultReadingConverter(), createDefaultNormalizer(), analyzer);

        assertTrue(items.size() > 0);
        // Languages should be empty array when no lang field
        assertEquals(0, items.get(0).getLanguages().length);
    }

    @Test
    public void test_parseDocument_tagsAsStringArray() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("content", "test");
        document.put("label", new String[] { "tag1", "tag2" });

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        List<SuggestItem> items = parser.parseDocument(document, supportedFields, tagFieldNames, roleFieldName, "lang",
                createDefaultReadingConverter(), createDefaultReadingConverter(), createDefaultNormalizer(), analyzer);

        assertTrue(items.size() > 0);
        assertTrue("Should have tags from array", items.get(0).getTags().length >= 2);
    }

    @Test
    public void test_parseDocument_tagsAsList() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("content", "test");
        List<String> tags = new ArrayList<>();
        tags.add("tag1");
        tags.add("tag2");
        document.put("label", tags);

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        List<SuggestItem> items = parser.parseDocument(document, supportedFields, tagFieldNames, roleFieldName, "lang",
                createDefaultReadingConverter(), createDefaultReadingConverter(), createDefaultNormalizer(), analyzer);

        assertTrue(items.size() > 0);
        assertTrue("Should have tags from list", items.get(0).getTags().length >= 2);
    }

    @Test
    public void test_parseDocument_nonStringFieldValue() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("content", 12345); // Integer instead of String

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        List<SuggestItem> items = parser.parseDocument(document, supportedFields, tagFieldNames, roleFieldName, "lang",
                createDefaultReadingConverter(), createDefaultReadingConverter(), createDefaultNormalizer(), analyzer);

        // Should convert to string and process
        // May or may not produce items depending on analyzer
        assertNotNull(items);
    }

    @Test
    public void test_parseDocument_multipleFields() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("content", "コンテンツテスト");
        document.put("title", "タイトルテスト");

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        List<SuggestItem> items = parser.parseDocument(document, supportedFields, tagFieldNames, roleFieldName, "lang",
                createDefaultReadingConverter(), createDefaultReadingConverter(), createDefaultNormalizer(), analyzer);

        assertTrue("Should have items from multiple fields", items.size() > 0);

        // Check that items come from different fields
        boolean hasContent = items.stream().anyMatch(i -> i.getFields()[0].equals("content"));
        boolean hasTitle = items.stream().anyMatch(i -> i.getFields()[0].equals("title"));
        assertTrue("Should have items from content field", hasContent);
        assertTrue("Should have items from title field", hasTitle);
    }

    // ============================================================
    // Tests for getFieldValues edge cases
    // ============================================================

    @Test
    public void test_getFieldValues_nullDocument() throws Exception {
        Map<String, Object> document = new HashMap<>();
        String[] values = parser.getFieldValues(document, "nonexistent");

        assertEquals("Non-existent field should return empty array", 0, values.length);
    }

    @Test
    public void test_getFieldValues_emptyList() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("field", new ArrayList<>());

        String[] values = parser.getFieldValues(document, "field");

        assertEquals("Empty list should return empty array", 0, values.length);
    }

    @Test
    public void test_getFieldValues_emptyStringArray() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("field", new String[0]);

        String[] values = parser.getFieldValues(document, "field");

        assertEquals("Empty string array should return empty array", 0, values.length);
    }

    @Test
    public void test_getFieldValues_mixedTypeList() throws Exception {
        Map<String, Object> document = new HashMap<>();
        List<Object> mixed = new ArrayList<>();
        mixed.add("string");
        mixed.add(123);
        mixed.add(true);
        document.put("field", mixed);

        String[] values = parser.getFieldValues(document, "field");

        assertEquals("Should convert all types to strings", 3, values.length);
        assertEquals("string", values[0]);
        assertEquals("123", values[1]);
        assertEquals("true", values[2]);
    }

    // ============================================================
    // Helper methods
    // ============================================================

    protected ReadingConverter createDefaultReadingConverter() throws IOException {
        ReadingConverterChain chain = new ReadingConverterChain();
        chain.addConverter(new KatakanaToAlphabetConverter());
        chain.init();
        return chain;
    }

    protected Normalizer createDefaultNormalizer() {
        return new NormalizerChain();
    }
}
