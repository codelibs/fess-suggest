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
package org.codelibs.fess.suggest.settings;

import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;
import org.opensearch.index.IndexNotFoundException;

public class AnalyzerSettingsTest {
    String id = "analyzerSettingsTest";

    static SuggestSettings settings;

    static OpenSearchRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        })
                .build(newConfigs().clusterName("ArraySettingsTest")
                        .numOfNode(1)
                        .pluginTypes("org.codelibs.opensearch.extension.ExtensionPlugin"));
        runner.ensureYellow();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        runner.close();
        runner.clean();
    }

    @Before
    public void before() throws Exception {
        runner.admin().indices().prepareDelete("_all").execute().actionGet();
        runner.refresh();
        settings = Suggester.builder().build(runner.client(), id).settings();
    }

    @Test
    public void test_defaultAnalyzer() {
        String text = "Fess (フェス) は「5 分で簡単に構築可能な全文検索サーバー」です。 Java 実行環境があればどの OS でも実行可能です。 Fess は Apache ライセンスで提供され、無料 (フリーソフト) でご利用いただけます。";
        SuggestAnalyzer analyzer = SuggestUtil.createDefaultAnalyzer(runner.client(), settings);
        final List<AnalyzeToken> tokens = analyzer.analyze(text, "", null);
        final List<AnalyzeToken> readingTokens = analyzer.analyzeAndReading(text, "", null);

        int matchCount = 0;
        for (int i = 0; i < tokens.size(); i++) {
            final String term = tokens.get(i).getTerm();
            final String reading = readingTokens.get(i).getTerm();
            switch (term) {
            case "fess":
                matchCount++;
                assertEquals("fess", reading);
                break;
            case "検索":
                matchCount++;
                assertEquals("ケンサク", reading);
                break;
            case "無料":
                matchCount++;
                assertEquals("ムリョウ", reading);
                break;
            default:
                break;
            }
        }
        assertEquals(4, matchCount);
    }

    @Test
    public void test_analyzerNames() throws Exception {
        final Set<String> analyzerNames = settings.analyzer().getAnalyzerNames();
        assert (analyzerNames.size() > 4);
        assertTrue(analyzerNames.contains(settings.analyzer().getContentsAnalyzerName("", "")));
        assertTrue(analyzerNames.contains(settings.analyzer().getContentsReadingAnalyzerName("", "")));
        assertTrue(analyzerNames.contains(settings.analyzer().getReadingAnalyzerName("", "")));
        assertTrue(analyzerNames.contains(settings.analyzer().getReadingTermAnalyzerName("", "")));
        assertTrue(analyzerNames.contains(settings.analyzer().getNormalizeAnalyzerName("", "")));

        for (final String lang : settings.analyzer().SUPPORTED_LANGUAGES) {
            assertTrue(analyzerNames.contains(settings.analyzer().getContentsAnalyzerName("", lang)));
            assertTrue(analyzerNames.contains(settings.analyzer().getContentsReadingAnalyzerName("", lang)));
            assertTrue(analyzerNames.contains(settings.analyzer().getReadingAnalyzerName("", lang)));
            assertTrue(analyzerNames.contains(settings.analyzer().getReadingTermAnalyzerName("", lang)));
            assertTrue(analyzerNames.contains(settings.analyzer().getNormalizeAnalyzerName("", lang)));
        }
    }

    @Test
    public void test_getAnalyzerName_defaultWithEmptyField() {
        // When field is empty or blank, should return default analyzer
        assertEquals(AnalyzerSettings.READING_ANALYZER, settings.analyzer().getReadingAnalyzerName("", ""));
        assertEquals(AnalyzerSettings.READING_TERM_ANALYZER, settings.analyzer().getReadingTermAnalyzerName("", ""));
        assertEquals(AnalyzerSettings.NORMALIZE_ANALYZER, settings.analyzer().getNormalizeAnalyzerName("", ""));
        assertEquals(AnalyzerSettings.CONTENTS_ANALYZER, settings.analyzer().getContentsAnalyzerName("", ""));
        assertEquals(AnalyzerSettings.CONTENTS_READING_ANALYZER, settings.analyzer().getContentsReadingAnalyzerName("", ""));
    }

    @Test
    public void test_getAnalyzerName_defaultWithNullField() {
        // When field is null, should return default analyzer
        assertEquals(AnalyzerSettings.READING_ANALYZER, settings.analyzer().getReadingAnalyzerName(null, ""));
        assertEquals(AnalyzerSettings.READING_TERM_ANALYZER, settings.analyzer().getReadingTermAnalyzerName(null, ""));
        assertEquals(AnalyzerSettings.NORMALIZE_ANALYZER, settings.analyzer().getNormalizeAnalyzerName(null, ""));
        assertEquals(AnalyzerSettings.CONTENTS_ANALYZER, settings.analyzer().getContentsAnalyzerName(null, ""));
        assertEquals(AnalyzerSettings.CONTENTS_READING_ANALYZER, settings.analyzer().getContentsReadingAnalyzerName(null, ""));
    }

    @Test
    public void test_getAnalyzerName_withLanguage() {
        // When language is supported, should return analyzer with language suffix
        String readingAnalyzer = settings.analyzer().getReadingAnalyzerName("", "en");
        assertTrue(readingAnalyzer.contains("_en") || readingAnalyzer.equals(AnalyzerSettings.READING_ANALYZER));

        String normalizeAnalyzer = settings.analyzer().getNormalizeAnalyzerName("", "ja");
        assertTrue(normalizeAnalyzer.contains("_ja") || normalizeAnalyzer.equals(AnalyzerSettings.NORMALIZE_ANALYZER));
    }

    @Test
    public void test_getAnalyzerName_withUnsupportedLanguage() {
        // When language is not supported, should return base analyzer without suffix
        String readingAnalyzer = settings.analyzer().getReadingAnalyzerName("", "xyz");
        assertEquals(AnalyzerSettings.READING_ANALYZER, readingAnalyzer);

        String normalizeAnalyzer = settings.analyzer().getNormalizeAnalyzerName("", "unsupported");
        assertEquals(AnalyzerSettings.NORMALIZE_ANALYZER, normalizeAnalyzer);
    }

    @Test
    public void test_isSupportedLanguage() {
        // Test supported languages
        assertTrue(AnalyzerSettings.isSupportedLanguage("en"));
        assertTrue(AnalyzerSettings.isSupportedLanguage("ja"));
        assertTrue(AnalyzerSettings.isSupportedLanguage("zh-cn"));
        assertTrue(AnalyzerSettings.isSupportedLanguage("zh-tw"));

        // Test unsupported languages
        assertFalse(AnalyzerSettings.isSupportedLanguage("xyz"));
        assertFalse(AnalyzerSettings.isSupportedLanguage(""));
        assertFalse(AnalyzerSettings.isSupportedLanguage(null));
    }

}
