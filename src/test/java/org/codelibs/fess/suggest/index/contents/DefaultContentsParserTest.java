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

import java.io.IOException;
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
import org.opensearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;

import junit.framework.TestCase;

public class DefaultContentsParserTest extends TestCase {
    static Suggester suggester;
    static OpenSearchRunner runner;
    DefaultContentsParser defaultContentsParser = new DefaultContentsParser();
    String[] supportedFields = new String[] { "content", "title" };
    String[] tagFieldNames = new String[] { "label", "virtual_host" };
    String roleFieldName = "role";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        }).build(newConfigs().clusterName("DefaultContentsParserTest").numOfNode(1)
                .pluginTypes("org.codelibs.opensearch.extension.ExtensionPlugin"));
        runner.ensureYellow();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (runner != null) {
            runner.close();
            runner.clean();
        }
    }

    @Before
    public void setUp() throws Exception {
        try {
            runner.admin().indices().prepareDelete("_all").execute().actionGet();
        } catch (Exception e) {
            // ignore
        }
        runner.refresh();
        suggester = Suggester.builder().build(runner.client(), "DefaultContentsParserTest");
        suggester.createIndexIfNothing();
    }

    public void test_parseQueryLog() throws Exception {

        QueryLog queryLog = new QueryLog("content:検索エンジン", null);
        List<SuggestItem> items = defaultContentsParser.parseQueryLog(queryLog, supportedFields, tagFieldNames, roleFieldName,
                createDefaultReadingConverter(), createDefaultNormalizer());
        SuggestItem item = items.get(0);
        assertEquals("検索エンジン", item.getText());
        assertEquals(SuggestItem.Kind.QUERY, item.getKinds()[0]);
        assertEquals(1, item.getQueryFreq());
    }

    public void test_parseQueryLog2Word() throws Exception {
        QueryLog queryLog = new QueryLog("content:検索エンジン AND content:柿", null);
        List<SuggestItem> items = defaultContentsParser.parseQueryLog(queryLog, supportedFields, tagFieldNames, roleFieldName,
                createDefaultReadingConverter(), createDefaultNormalizer());
        assertEquals("検索エンジン 柿", items.get(0).getText());
    }

    public void test_parseQueryLogAndRole() throws Exception {
        QueryLog queryLog = new QueryLog("content:検索エンジン AND label:tag1", "role:role1");
        List<SuggestItem> items = defaultContentsParser.parseQueryLog(queryLog, supportedFields, tagFieldNames, roleFieldName,
                createDefaultReadingConverter(), createDefaultNormalizer());
        SuggestItem item = items.get(0);
        assertEquals("検索エンジン", item.getText());
        assertEquals("tag1", item.getTags()[0]);
        assertEquals("role1", item.getRoles()[0]);
    }

    protected ReadingConverter createDefaultReadingConverter() throws IOException {
        ReadingConverterChain chain = new ReadingConverterChain();
        // chain.addConverter(new KatakanaConverter());
        chain.addConverter(new KatakanaToAlphabetConverter());
        chain.init();
        return chain;
    }

    protected Normalizer createDefaultNormalizer() {
        // TODO
        return new NormalizerChain();
    }

    public void test_parseDocument() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("content", "これはテストです。検索エンジン。");
        document.put("title", "タイトル");

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        List<SuggestItem> items = defaultContentsParser.parseDocument(document, supportedFields, tagFieldNames, roleFieldName, "lang",
                createDefaultReadingConverter(), createDefaultReadingConverter(), createDefaultNormalizer(), analyzer);

        assertNotNull(items);
        assertTrue(items.size() > 0);
        for (SuggestItem item : items) {
            assertEquals(SuggestItem.Kind.DOCUMENT, item.getKinds()[0]);
            assertEquals(1, item.getDocFreq());
        }
    }

    public void test_parseDocumentWithTags() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("content", "テストドキュメント");
        document.put("label", "tag1");
        document.put("virtual_host", "host1");

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        List<SuggestItem> items = defaultContentsParser.parseDocument(document, supportedFields, tagFieldNames, roleFieldName, "lang",
                createDefaultReadingConverter(), createDefaultReadingConverter(), createDefaultNormalizer(), analyzer);

        assertNotNull(items);
        assertTrue(items.size() > 0);
        SuggestItem firstItem = items.get(0);
        assertTrue(firstItem.getTags().length >= 2);
    }

    public void test_parseDocumentWithLargeText() throws Exception {
        // Test with text larger than maxAnalyzedContentLength
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            largeText.append("テスト ");
        }

        Map<String, Object> document = new HashMap<>();
        document.put("content", largeText.toString());

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        List<SuggestItem> items = defaultContentsParser.parseDocument(document, supportedFields, tagFieldNames, roleFieldName, "lang",
                createDefaultReadingConverter(), createDefaultReadingConverter(), createDefaultNormalizer(), analyzer);

        assertNotNull(items);
        // Should handle large text by splitting into chunks
        assertTrue(items.size() > 0);
    }

    public void test_analyzeText() throws Exception {
        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        String text = "これはテストです。検索エンジン。";

        List<AnalyzeToken> tokens = defaultContentsParser.analyzeText(analyzer, "content", text, null);

        assertNotNull(tokens);
        assertTrue(tokens.size() > 0);
    }

    public void test_analyzeTextByReading() throws Exception {
        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        String text = "これはテストです。検索エンジン。";

        List<AnalyzeToken> tokens = defaultContentsParser.analyzeTextByReading(analyzer, "content", text, null);

        assertNotNull(tokens);
        // analyzeAndReading may return null for some analyzers
        // Just verify it doesn't throw an exception
    }

    public void test_analyzeTextWithLargeContent() throws Exception {
        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        // Create text larger than maxAnalyzedContentLength (1000 chars by default)
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            largeText.append("テスト ");
        }

        List<AnalyzeToken> tokens = defaultContentsParser.analyzeText(analyzer, "content", largeText.toString(), null);

        assertNotNull(tokens);
        // Should handle large text by splitting into chunks
        assertTrue(tokens.size() > 0);
    }

    public void test_getFieldValues() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("string_field", "value1");
        document.put("string_array_field", new String[] { "value2", "value3" });
        document.put("list_field", java.util.Arrays.asList("value4", "value5"));
        document.put("int_field", 123);

        String[] stringValues = defaultContentsParser.getFieldValues(document, "string_field");
        assertEquals(1, stringValues.length);
        assertEquals("value1", stringValues[0]);

        String[] arrayValues = defaultContentsParser.getFieldValues(document, "string_array_field");
        assertEquals(2, arrayValues.length);
        assertEquals("value2", arrayValues[0]);
        assertEquals("value3", arrayValues[1]);

        String[] listValues = defaultContentsParser.getFieldValues(document, "list_field");
        assertEquals(2, listValues.length);
        assertEquals("value4", listValues[0]);
        assertEquals("value5", listValues[1]);

        String[] intValues = defaultContentsParser.getFieldValues(document, "int_field");
        assertEquals(1, intValues.length);
        assertEquals("123", intValues[0]);

        String[] nullValues = defaultContentsParser.getFieldValues(document, "non_existent");
        assertEquals(0, nullValues.length);
    }

    public void test_parseSearchWords() throws Exception {
        String[] words = new String[] { "検索", "エンジン" };
        String[][] readings = new String[][] { new String[] { "kensaku" }, new String[] { "enjin" } };
        String[] fields = new String[] { "content" };
        String[] tags = new String[] { "tag1" };
        String[] roles = new String[] { "role1" };
        String[] langs = new String[] { "ja" };

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        SuggestItem item = defaultContentsParser.parseSearchWords(words, readings, fields, tags, roles, 10, createDefaultReadingConverter(),
                createDefaultNormalizer(), analyzer, langs);

        assertNotNull(item);
        assertEquals("検索 エンジン", item.getText());
        assertEquals(SuggestItem.Kind.QUERY, item.getKinds()[0]);
        assertEquals(10, item.getQueryFreq());
    }

    public void test_parseSearchWordsWithExcludedWords() throws Exception {
        String[] words = new String[] { "　　　", "エンジン" }; // First word is whitespace only
        String[] fields = new String[] { "content" };

        SuggestAnalyzer analyzer = suggester.settings().analyzer().new DefaultContentsAnalyzer();
        SuggestItem item = defaultContentsParser.parseSearchWords(words, null, fields, null, null, 1, createDefaultReadingConverter(),
                createDefaultNormalizer(), analyzer, null);

        // Should filter out whitespace-only words
        if (item != null) {
            assertFalse(item.getText().contains("　　　"));
        }
    }

}
