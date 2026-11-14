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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.codelibs.fess.suggest.analysis.TokenizerStageResult;
import org.junit.Test;

/**
 * Tests of the parsing of the explain response of the tokenizer stage. The response is given as a map, so that the cases that
 * the embedded node cannot produce (e.g. several char filters) can be tested.
 */
public class DefaultContentsAnalyzerParseTokenizerStageTest {

    private static final String TARGET = "analyzer=test";

    private static Map<String, Object> token(final String term, final int startOffset, final int endOffset, final String partOfSpeech) {
        return partOfSpeech == null ? Map.of("token", term, "start_offset", startOffset, "end_offset", endOffset, "position", 0)
                : Map.of("token", term, "start_offset", startOffset, "end_offset", endOffset, "position", 0, "partOfSpeech", partOfSpeech);
    }

    private static Map<String, Object> charFilter(final String name, final String filteredText) {
        return Map.of("name", name, "filtered_text", List.of(filteredText));
    }

    private static Map<String, Object> response(final List<Map<String, Object>> charFilters, final List<Map<String, Object>> tokens) {
        return Map.of("detail",
                Map.of("custom_analyzer", true, "charfilters", charFilters, "tokenizer", Map.of("name", "t", "tokens", tokens)));
    }

    private static TokenizerStageResult parse(final Map<String, Object> response) {
        return AnalyzerSettings.DefaultContentsAnalyzer.parseTokenizerStage(response, TARGET);
    }

    @Test
    public void testTokensAndFilteredText() {
        final TokenizerStageResult result = parse(
                response(List.of(charFilter("mapping_char", "新型コロナ")), List.of(token("新型", 0, 2, "名詞-一般"), token("コロナ", 2, 5, "名詞-一般"))));

        assertNotNull(result);
        assertEquals("新型コロナ", result.getCharFilteredText());
        assertEquals(2, result.getTokens().size());
        assertEquals("名詞-一般", result.getTokens().get(0).getAttributes().get("partOfSpeech"));
    }

    @Test
    public void testLastCharFilterIsUsedWhenThereAreSeveral() {
        // The tokenizer receives the output of the last char filter.
        final TokenizerStageResult result =
                parse(response(List.of(charFilter("first", "ｶﾞｲﾄﾞ"), charFilter("second", "ガイド")), List.of(token("ガイド", 0, 3, "名詞-一般"))));

        assertNotNull(result);
        assertEquals("ガイド", result.getCharFilteredText());
    }

    @Test
    public void testNoPartOfSpeechGivesResultWithoutTokens() {
        final TokenizerStageResult result = parse(response(List.of(charFilter("mapping_char", "Hello World")),
                List.of(token("Hello", 0, 5, null), token("World", 6, 11, null))));

        assertNotNull(result);
        assertTrue(result.getTokens().isEmpty());
        assertEquals("Hello World", result.getCharFilteredText());
    }

    @Test
    public void testNoTokensGivesResultWithoutTokens() {
        final TokenizerStageResult result = parse(response(List.of(charFilter("mapping_char", "。。。")), List.of()));

        assertNotNull(result);
        assertTrue(result.getTokens().isEmpty());
        assertEquals("。。。", result.getCharFilteredText());
    }

    @Test
    public void testNoPartOfSpeechWithoutCharFiltersGivesEmptyResult() {
        // An analyzer without char filter and part of speech (e.g. contents_analyzer_ar) is normal, not a misconfiguration.
        final TokenizerStageResult result = parse(response(List.of(), List.of(token("مرحبا", 0, 5, null))));
        final TokenizerStageResult noCharFiltersKey =
                parse(Map.of("detail", Map.of("tokenizer", Map.of("tokens", List.of(token("مرحبا", 0, 5, null))))));
        final TokenizerStageResult noTokens = parse(response(List.of(), List.of()));

        for (final TokenizerStageResult emptyResult : new TokenizerStageResult[] { result, noCharFiltersKey, noTokens }) {
            assertNotNull(emptyResult);
            assertTrue(emptyResult.getTokens().isEmpty());
            assertEquals("", emptyResult.getCharFilteredText());
        }
    }

    @Test
    public void testReadingAttributeIsExtracted() {
        final Map<String, Object> tokenWithReading =
                Map.of("token", "新型", "start_offset", 0, "end_offset", 2, "position", 0, "partOfSpeech", "名詞-一般", "reading", "シンガタ");

        final TokenizerStageResult result = parse(response(List.of(charFilter("mapping_char", "新型")), List.of(tokenWithReading)));

        assertNotNull(result);
        assertEquals("シンガタ", result.getTokens().get(0).getAttributes().get("reading"));
    }

    @Test
    public void testNoCharFiltersIsNotAvailable() {
        // The original text is not used instead: a wrong key or analyzer configuration must be detected, not hidden.
        assertNull(parse(response(List.of(), List.of(token("新型", 0, 2, "名詞-一般")))));
        assertNull(parse(Map.of("detail", Map.of("tokenizer", Map.of("tokens", List.of(token("新型", 0, 2, "名詞-一般")))))));
    }

    @Test
    public void testCharFilterWithoutFilteredTextIsNotAvailable() {
        // A wrong key such as the plural "filtered_texts" is detected.
        final Map<String, Object> wrongKey = Map.of("name", "mapping_char", "filtered_texts", List.of("新型"));

        assertNull(parse(response(List.of(wrongKey), List.of(token("新型", 0, 2, "名詞-一般")))));
    }

    @Test
    public void testEmptyFilteredTextIsNotAvailable() {
        final Map<String, Object> emptyFilteredText = Map.of("name", "mapping_char", "filtered_text", List.of());

        assertNull(parse(response(List.of(emptyFilteredText), List.of(token("新型", 0, 2, "名詞-一般")))));
    }

    @Test
    public void testMissingDetailOrTokenizerIsNotAvailable() {
        assertNull(parse(Map.of()));
        assertNull(parse(Map.of("detail", Map.of("charfilters", List.of(charFilter("mapping_char", "新型"))))));
        // "tokens" is not a list.
        assertNull(parse(Map.of("detail",
                Map.of("charfilters", List.of(charFilter("mapping_char", "新型")), "tokenizer", Map.of("name", "t", "tokens", "新型")))));
    }

    @Test
    public void testMalformedTokenIsNotAvailable() {
        assertNull(parse(response(List.of(charFilter("mapping_char", "新型")), List.of(Map.of("token", "新型")))));
    }
}
