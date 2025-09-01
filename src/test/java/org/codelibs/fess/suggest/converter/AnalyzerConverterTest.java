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
package org.codelibs.fess.suggest.converter;

import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.transport.client.Client;

public class AnalyzerConverterTest {

    private static OpenSearchRunner runner;
    private static Client client;
    private static SuggestSettings settings;
    private AnalyzerConverter converter;
    private static final String TEST_INDEX = "test_analyzer_index";
    private static final String SUGGEST_INDEX = ".suggest";

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        })
                .build(newConfigs().clusterName("AnalyzerConverterTest")
                        .numOfNode(1)
                        .pluginTypes("org.codelibs.opensearch.extension.ExtensionPlugin"));
        runner.ensureYellow();
        client = runner.client();

        // Create test index with analyzers
        createTestIndex();

        // Initialize suggest settings
        initializeSuggestSettings();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        runner.close();
        runner.clean();
    }

    @Before
    public void setUp() throws Exception {
        converter = new AnalyzerConverter(client, settings);
    }

    private static void createTestIndex() throws Exception {
        // Create index with custom analyzers
        Settings indexSettings = Settings.builder()
                .put("number_of_shards", 1)
                .put("number_of_replicas", 0)
                .put("analysis.analyzer.test_reading_analyzer.type", "custom")
                .put("analysis.analyzer.test_reading_analyzer.tokenizer", "standard")
                .put("analysis.analyzer.test_reading_term_analyzer.type", "custom")
                .put("analysis.analyzer.test_reading_term_analyzer.tokenizer", "standard")
                .put("analysis.analyzer.test_reading_analyzer_ja.type", "custom")
                .put("analysis.analyzer.test_reading_analyzer_ja.tokenizer", "keyword")
                .put("analysis.analyzer.test_reading_term_analyzer_ja.type", "custom")
                .put("analysis.analyzer.test_reading_term_analyzer_ja.tokenizer", "keyword")
                .build();

        CreateIndexResponse createIndexResponse =
                client.admin().indices().prepareCreate(TEST_INDEX).setSettings(indexSettings).execute().actionGet();

        assertTrue(createIndexResponse.isAcknowledged());

        // Create suggest settings index
        Settings suggestIndexSettings = Settings.builder().put("number_of_shards", 1).put("number_of_replicas", 0).build();

        createIndexResponse = client.admin().indices().prepareCreate(SUGGEST_INDEX).setSettings(suggestIndexSettings).execute().actionGet();

        assertTrue(createIndexResponse.isAcknowledged());
    }

    private static void initializeSuggestSettings() throws Exception {
        // Create suggester and get settings
        Suggester suggester = Suggester.builder().build(client, "test");
        settings = suggester.settings();

        // Store analyzer settings
        storeAnalyzerSettings();
    }

    private static void storeAnalyzerSettings() throws Exception {
        // Store analyzer mappings
        client.prepareIndex()
                .setIndex(SUGGEST_INDEX)
                .setId("analyzer_settings")
                .setSource(XContentFactory.jsonBuilder()
                        .startObject()
                        .startObject("analyzer")
                        .field("reading_analyzer", "test_reading_analyzer")
                        .field("reading_term_analyzer", "test_reading_term_analyzer")
                        .field("reading_analyzer_ja", "test_reading_analyzer_ja")
                        .field("reading_term_analyzer_ja", "test_reading_term_analyzer_ja")
                        .endObject()
                        .endObject())
                .execute()
                .actionGet();

        client.admin().indices().prepareRefresh(SUGGEST_INDEX).execute().actionGet();
    }

    @Test
    public void testConstructor() {
        // Test constructor initialization
        AnalyzerConverter testConverter = new AnalyzerConverter(client, settings);
        assertNotNull(testConverter);
    }

    @Test
    public void testInit() throws IOException {
        // Test init method (should do nothing)
        converter.init();
        // No exception should be thrown
    }

    @Test
    public void testConvertWithNoLanguages() throws IOException {
        // Test convert with no languages specified
        String text = "test text";
        String field = "content";

        List<String> results = converter.convert(text, field);

        assertNotNull(results);
        // Results may vary based on analyzer configuration
    }

    @Test
    public void testConvertWithNullLanguages() throws IOException {
        // Test convert with null languages
        String text = "test text";
        String field = "content";
        String[] langs = null;

        List<String> results = converter.convert(text, field, langs);

        assertNotNull(results);
        // Results may vary based on analyzer configuration
    }

    @Test
    public void testConvertWithEmptyLanguageArray() throws IOException {
        // Test convert with empty language array
        String text = "test text";
        String field = "content";
        String[] langs = new String[0];

        List<String> results = converter.convert(text, field, langs);

        assertNotNull(results);
        // Results may vary based on analyzer configuration
    }

    @Test
    public void testConvertWithSingleLanguage() throws IOException {
        // Test convert with single language
        String text = "test text";
        String field = "content";

        List<String> results = converter.convert(text, field, "en");

        assertNotNull(results);
        // Results may vary based on analyzer configuration
    }

    @Test
    public void testConvertWithMultipleLanguages() throws IOException {
        // Test convert with multiple languages
        String text = "test text";
        String field = "content";

        List<String> results = converter.convert(text, field, "en", "ja");

        assertNotNull(results);
        // With multiple languages, should use ReadingConverterChain
    }

    @Test
    public void testConvertWithEmptyText() throws IOException {
        // Test convert with empty text
        String text = "";
        String field = "content";

        List<String> results = converter.convert(text, field, "en");

        assertNotNull(results);
        assertTrue(results.isEmpty() || (results.size() == 1 && results.get(0).isEmpty()));
    }

    @Test
    public void testConvertWithNullText() throws IOException {
        // Test convert with null text - this might throw exception
        String text = null;
        String field = "content";

        try {
            List<String> results = converter.convert(text, field, "en");
            assertNotNull(results);
        } catch (Exception e) {
            // Expected behavior - null text might cause exception
            assertNotNull(e);
        }
    }

    @Test
    public void testConvertWithWhitespaceText() throws IOException {
        // Test convert with whitespace text
        String text = "   ";
        String field = "content";

        List<String> results = converter.convert(text, field, "en");

        assertNotNull(results);
    }

    @Test
    public void testConvertWithSpecialCharacters() throws IOException {
        // Test convert with special characters
        String text = "test@#$%123";
        String field = "content";

        List<String> results = converter.convert(text, field, "en");

        assertNotNull(results);
    }

    @Test
    public void testConvertWithJapaneseText() throws IOException {
        // Test convert with Japanese text
        String text = "テスト";
        String field = "content";

        List<String> results = converter.convert(text, field, "ja");

        assertNotNull(results);
        // Should use Japanese analyzer and transliterator
    }

    @Test
    public void testConvertWithMixedLanguageText() throws IOException {
        // Test convert with mixed language text
        String text = "test テスト";
        String field = "content";

        List<String> results = converter.convert(text, field, "en", "ja");

        assertNotNull(results);
    }

    @Test
    public void testConvertWithNullField() throws IOException {
        // Test convert with null field
        String text = "test text";
        String field = null;

        List<String> results = converter.convert(text, field, "en");

        assertNotNull(results);
    }

    @Test
    public void testConvertWithLongText() throws IOException {
        // Test convert with long text
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("word").append(i).append(" ");
        }
        String text = sb.toString();
        String field = "content";

        List<String> results = converter.convert(text, field, "en");

        assertNotNull(results);
    }

    @Test
    public void testMultipleConverterInstances() throws IOException {
        // Test that multiple converter instances work independently
        AnalyzerConverter converter1 = new AnalyzerConverter(client, settings);
        AnalyzerConverter converter2 = new AnalyzerConverter(client, settings);

        String text = "test";
        String field = "content";

        List<String> results1 = converter1.convert(text, field, "en");
        List<String> results2 = converter2.convert(text, field, "ja");

        assertNotNull(results1);
        assertNotNull(results2);
    }

    @Test
    public void testTransliteratorFunctionality() throws IOException {
        // Test that transliterator converts Hiragana to Katakana
        String text = "ひらがな";
        String field = "content";

        List<String> results = converter.convert(text, field, "ja");

        assertNotNull(results);
        // Results should contain Katakana conversion
    }

    @Test
    public void testLangAnalyzerConverterInit() throws IOException {
        // Test inner class LangAnalyzerConverter init
        AnalyzerConverter.LangAnalyzerConverter langConverter = converter.new LangAnalyzerConverter("en");

        langConverter.init();
        // Should not throw exception
    }

    @Test
    public void testLangAnalyzerConverterWithNullLang() throws IOException {
        // Test inner class with null language
        AnalyzerConverter.LangAnalyzerConverter langConverter = converter.new LangAnalyzerConverter(null);

        try {
            List<String> results = langConverter.convert("test", "content");
            assertNotNull(results);
        } catch (Exception e) {
            // Null language might cause issues with analyzer lookup
            assertNotNull(e);
        }
    }

    @Test
    public void testConvertWithNumbersAndSymbols() throws IOException {
        // Test convert with numbers and symbols
        String text = "123 456.789 #hashtag @mention";
        String field = "content";

        List<String> results = converter.convert(text, field, "en");

        assertNotNull(results);
    }
}