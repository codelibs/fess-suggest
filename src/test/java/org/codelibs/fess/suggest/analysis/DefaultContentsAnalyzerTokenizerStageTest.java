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
package org.codelibs.fess.suggest.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import org.codelibs.fess.suggest.SuggestTestServer;
import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.settings.AnalyzerSettings;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.codelibs.fesen.opensearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies against a real explain response that the tokenizer stage can be read.
 * A stub cannot detect a mistake in the response keys (e.g. {@code filtered_text}), so these tests use the embedded node.
 */
public class DefaultContentsAnalyzerTokenizerStageTest {

    private static SuggestTestServer server;

    private static SuggestSettings settings;

    private static SuggestAnalyzer analyzer;

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = SuggestTestServer.start("DefaultContentsAnalyzerTokenizerStageTest");
        server.runner().ensureYellow();
        settings = Suggester.builder().build(server.client(), "tokenizerStageTest").settings();
        analyzer = SuggestUtil.createDefaultAnalyzer(server.client(), settings);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.close();
    }

    @Test
    public void test_partOfSpeechReadingAndOffsets() {
        final TokenizerStageResult result = analyzer.analyzeTokenizerStage("新型コロナ", "", "ja");

        assertNotNull(result);
        assertEquals("新型コロナ", result.getCharFilteredText());
        final List<AnalyzeToken> tokens = result.getTokens();
        assertEquals(2, tokens.size());

        assertEquals("新型", tokens.get(0).getTerm());
        assertEquals(0, tokens.get(0).getStartOffset());
        assertEquals(2, tokens.get(0).getEndOffset());
        assertEquals("名詞-一般", tokens.get(0).getAttributes().get("partOfSpeech"));
        assertEquals("シンガタ", tokens.get(0).getAttributes().get("reading"));

        assertEquals("コロナ", tokens.get(1).getTerm());
        assertEquals(2, tokens.get(1).getStartOffset());
        assertEquals(5, tokens.get(1).getEndOffset());
        assertEquals("名詞-一般", tokens.get(1).getAttributes().get("partOfSpeech"));
        assertEquals("コロナ", tokens.get(1).getAttributes().get("reading"));
    }

    @Test
    public void test_partOfSpeechOfSuffixAndProperNoun() {
        final TokenizerStageResult result = analyzer.analyzeTokenizerStage("高市早苗", "", "ja");

        assertNotNull(result);
        final List<AnalyzeToken> tokens = result.getTokens();
        assertEquals(2, tokens.size());
        assertEquals("名詞-固有名詞-人名-姓", tokens.get(0).getAttributes().get("partOfSpeech"));
        assertEquals("名詞-固有名詞-人名-名", tokens.get(1).getAttributes().get("partOfSpeech"));
    }

    @Test
    public void test_offsetsAreInOriginalTextAndFilteredTextIsNormalized() {
        // Half-width katakana with voiced marks is 14 chars, and 11 chars after the char filter.
        final String text = "ｶﾞｲﾄﾞﾌﾞｯｸ新型コロナ";

        final TokenizerStageResult result = analyzer.analyzeTokenizerStage(text, "", "ja");

        assertNotNull(result);
        assertEquals("ガイドブック新型コロナ", result.getCharFilteredText());
        assertNotEquals(text.length(), result.getCharFilteredText().length());

        final AnalyzeToken newModel = findToken(result.getTokens(), "新型");
        assertEquals(text.indexOf("新型"), newModel.getStartOffset());
        assertEquals(text.indexOf("新型") + 2, newModel.getEndOffset());
        assertEquals("新型", text.substring(newModel.getStartOffset(), newModel.getEndOffset()));
        // Applying the offsets to the filtered text would cut out a wrong string.
        assertNotEquals("新型", result.getCharFilteredText().substring(newModel.getStartOffset(), newModel.getEndOffset()));

        final AnalyzeToken corona = findToken(result.getTokens(), "コロナ");
        assertEquals(newModel.getEndOffset(), corona.getStartOffset());
        assertEquals(text.length(), corona.getEndOffset());
    }

    @Test
    public void test_tokensAreTokenizerStageNotFilterStage() {
        // The contents analyzer lowercases and normalizes in token filters, but the tokenizer stage keeps the surface.
        final TokenizerStageResult result = analyzer.analyzeTokenizerStage("ＡＩ活用", "", "ja");

        assertNotNull(result);
        assertEquals("AI活用", result.getCharFilteredText());
        assertEquals("AI", findToken(result.getTokens(), "AI").getTerm());
    }

    @Test
    public void test_tokenWithoutReadingHasNoReadingAttribute() {
        final TokenizerStageResult result = analyzer.analyzeTokenizerStage("新型コロナ ChatGPT", "", "ja");

        assertNotNull(result);
        final AnalyzeToken chatGpt = findToken(result.getTokens(), "ChatGPT");
        assertFalse(chatGpt.getAttributes().containsKey("reading"));
        assertTrue(findToken(result.getTokens(), "新型").getAttributes().containsKey("reading"));
    }

    @Test
    public void test_textLongerThanMaxContentLengthReturnsNull() {
        assertNull(analyzer.analyzeTokenizerStage("あ".repeat(50001), "", "ja"));
    }

    @Test
    public void test_nullTextReturnsNull() {
        assertNull(analyzer.analyzeTokenizerStage(null, "", "ja"));
    }

    @Test
    public void test_languageWithoutPartOfSpeechReturnsResultWithoutTokens() {
        // A normal state of a non-Japanese analyzer, which is told apart from "not available" (null).
        final TokenizerStageResult result = analyzer.analyzeTokenizerStage("Hello World", "", "en");

        assertNotNull(result);
        assertTrue(result.getTokens().isEmpty());
        assertEquals("Hello World", result.getCharFilteredText());
    }

    @Test
    public void test_explainFailureForTooManyTokensReturnsNullWithoutException() {
        // Explain counts the tokenizer stage tokens against index.analyze.max_token_count (10000), unlike the plain analyze.
        // This test depends on the default value of that setting (10000) of the embedded node: the text has 20000 tokens.
        final String text = "円安".repeat(10000);

        assertFalse(analyzer.analyze(text, "", "ja").isEmpty());
        assertNull(analyzer.analyzeTokenizerStage(text, "", "ja"));

        // The failure of one text does not affect the following requests.
        assertNotNull(analyzer.analyzeTokenizerStage("新型コロナ", "", "ja"));
    }

    @Test
    public void test_explainFailureWarningIsLimitedByTime() throws Exception {
        // A new analyzer has its own warning limit, so that the failure of the other tests does not consume it.
        final SuggestAnalyzer newAnalyzer = SuggestUtil.createDefaultAnalyzer(server.client(), settings);
        final org.apache.logging.log4j.core.Logger analyzerSettingsLogger = fetchAnalyzerSettingsLogger();
        final Level originalLevel = analyzerSettingsLogger.getLevel();
        final CapturingAppender appender = new CapturingAppender();
        appender.start();
        analyzerSettingsLogger.addAppender(appender);
        analyzerSettingsLogger.setLevel(Level.DEBUG);
        try {
            final String text = "円安".repeat(10000);

            assertNull(newAnalyzer.analyzeTokenizerStage(text, "", "ja"));
            assertNull(newAnalyzer.analyzeTokenizerStage(text, "", "ja"));
            assertNull(newAnalyzer.analyzeTokenizerStage(text, "", "ja"));

            final List<String> warnings = appender.messages(Level.WARN);
            assertEquals(warnings.toString(), 1, warnings.size());
            // The cause is kept, and the text is not logged.
            assertTrue(warnings.get(0), warnings.get(0).contains("error="));
            assertFalse(warnings.get(0), warnings.get(0).contains("error=null"));
            assertTrue(warnings.get(0), warnings.get(0).contains("textSize=20000"));
            assertFalse(warnings.get(0), warnings.get(0).contains("円安円安"));
            // The suppressed failures are logged at debug level.
            assertEquals(appender.messages(Level.DEBUG).toString(), 2,
                    appender.messages(Level.DEBUG).stream().filter(m -> m.contains("Failed to analyze the tokenizer stage")).count());
        } finally {
            analyzerSettingsLogger.removeAppender(appender);
            appender.stop();
            analyzerSettingsLogger.setLevel(originalLevel);
        }
    }

    /**
     * Returns the logger instance that the analyzer settings really use. When a test that starts an OpenSearch node runs earlier
     * in the same JVM, the logging configuration is rebuilt, and {@code LogManager.getLogger} may no longer return that instance.
     */
    private static org.apache.logging.log4j.core.Logger fetchAnalyzerSettingsLogger() throws ReflectiveOperationException {
        final java.lang.reflect.Field loggerField = AnalyzerSettings.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        return (org.apache.logging.log4j.core.Logger) loggerField.get(null);
    }

    private static class CapturingAppender extends AbstractAppender {

        private final List<LogEvent> events = new CopyOnWriteArrayList<>();

        CapturingAppender() {
            super("CapturingAppender", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(final LogEvent event) {
            events.add(event.toImmutable());
        }

        List<String> messages(final Level level) {
            return events.stream()
                    .filter(e -> e.getLevel() == level)
                    .map(e -> e.getMessage().getFormattedMessage())
                    .collect(Collectors.toList());
        }
    }

    private static AnalyzeToken findToken(final List<AnalyzeToken> tokens, final String term) {
        return tokens.stream().filter(token -> term.equals(token.getTerm())).findFirst().orElseThrow();
    }
}
