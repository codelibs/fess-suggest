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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.codelibs.fesen.opensearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;
import org.codelibs.fess.suggest.SuggestTestServer;
import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.analysis.TokenizerStageResult;
import org.codelibs.fess.suggest.converter.KatakanaToAlphabetConverter;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.converter.ReadingConverterChain;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.contents.DefaultContentsParser.FragmentExclusion;
import org.codelibs.fess.suggest.index.contents.DefaultContentsParser.InflectionUndetermined;
import org.codelibs.fess.suggest.normalizer.NormalizerChain;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Runs the fragment filters with the real analyzer on the embedded node. A stub cannot detect a mistake in the handling of the
 * response, such as an attribute that is dropped on the way from the node to the parser.
 *
 * <p>The expectations of the inflection forms depend on the tokenizer of the embedded node (kuromoji_neologd_normal).</p>
 */
public class FragmentIntegrationTest {

    private static final String FIELD = "content";

    private static final String INFLECTION_FORM_ATTRIBUTE = "inflectionForm";

    private static final String[] FIELDS = { FIELD };

    private static final String[] TAG_FIELD_NAMES = { "label" };

    private static final String ROLE_FIELD_NAME = "role";

    private static final String LANG_FIELD_NAME = "lang";

    private static final String FILTER_ENABLED_PROPERTY = "fess.suggest.fragment.filter.enabled";

    private static final String INFLECTION_ENABLED_PROPERTY = "fess.suggest.fragment.inflection.enabled";

    private static final String COMPOUND_ENABLED_PROPERTY = "fess.suggest.compound.enabled";

    // The properties that change the result of the parser. They are cleared, so that the tests do not depend on the environment.
    private static final String[] TOUCHED_PROPERTIES =
            { FILTER_ENABLED_PROPERTY, INFLECTION_ENABLED_PROPERTY, COMPOUND_ENABLED_PROPERTY, "fess.suggest.fragment.latin.short.enabled",
                    "fess.suggest.fragment.single.han.enabled", "fess.suggest.compound.max.tokens", "fess.suggest.compound.max.length",
                    "fess.suggest.compound.break.chars", "fess.suggest.compound.break.tokens", "fess.suggest.max.analyzed.content.length" };

    private static SuggestTestServer server;

    private static SuggestAnalyzer analyzer;

    private final Map<String, String> savedProperties = new HashMap<>();

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = SuggestTestServer.start("FragmentIntegrationTest");
        server.runner().ensureYellow();
        final SuggestSettings settings = Suggester.builder().build(server.client(), "fragmentTest").settings();
        analyzer = SuggestUtil.createDefaultAnalyzer(server.client(), settings);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.close();
    }

    @Before
    public void before() {
        for (final String name : TOUCHED_PROPERTIES) {
            final String value = System.getProperty(name);
            if (value != null) {
                savedProperties.put(name, value);
            }
            System.clearProperty(name);
        }
        // The default is true, but the tests of the compound words in the fragments do not rely on it.
        System.setProperty(COMPOUND_ENABLED_PROPERTY, "true");
    }

    @After
    public void after() {
        for (final String name : TOUCHED_PROPERTIES) {
            final String value = savedProperties.get(name);
            if (value == null) {
                System.clearProperty(name);
            } else {
                System.setProperty(name, value);
            }
        }
        savedProperties.clear();
    }

    @Test
    public void test_inflectionFormOfAdjectivesIsObtainedFromRealResponse() {
        // Each word is given alone, so that the tokenizer does not read it as a part of a longer word.
        final Map<String, String> stemForms = inflectionFormsByTerm("なかっ 大き 難し", "ja");
        final Map<String, String> baseForms = inflectionFormsByTerm("大きい 難しい", "ja");

        assertEquals("連用タ接続", stemForms.get("なかっ"));
        assertEquals("ガル接続", stemForms.get("大き"));
        assertEquals("ガル接続", stemForms.get("難し"));
        assertEquals("基本形", baseForms.get("大きい"));
        assertEquals("基本形", baseForms.get("難しい"));
    }

    @Test
    public void test_inflectionFormIsObtainedWithoutLanguage() {
        final Map<String, String> forms = inflectionFormsByTerm("なかっ 大き 大きい", null);

        assertEquals("連用タ接続", forms.get("なかっ"));
        assertEquals("ガル接続", forms.get("大き"));
        assertEquals("基本形", forms.get("大きい"));
    }

    @Test
    public void test_partOfSpeechAndReadingAreKeptWithInflectionForm() {
        final TokenizerStageResult result = analyzeTokenizerStage("なかっ", "ja");

        assertEquals(1, result.getTokens().size());
        final AnalyzeToken token = result.getTokens().get(0);
        assertEquals("形容詞-自立", token.getAttributes().get("partOfSpeech"));
        assertNotNull(token.getAttributes().get("reading"));
        assertEquals("連用タ接続", token.getAttributes().get(INFLECTION_FORM_ATTRIBUTE));
    }

    @Test
    public void test_tokenWithoutInflectionFormHasNoAttribute() {
        final TokenizerStageResult result = analyzeTokenizerStage("新型コロナ", "ja");

        assertFalse(result.getTokens().isEmpty());
        for (final AnalyzeToken token : result.getTokens()) {
            assertTrue(token.getTerm(), token.getAttributes().containsKey("partOfSpeech"));
            assertFalse(token.getTerm(), token.getAttributes().containsKey(INFLECTION_FORM_ATTRIBUTE));
        }
    }

    // ---- Offsets of the final stage and the tokenizer stage ----

    @Test
    public void test_offsetsOfFinalStageMatchTokenizerStage() {
        final String[] texts =
                { "ｶﾞｲﾄﾞﾌﾞｯｸは大きい。ﾜｸﾁﾝ接種が難しかった", "ＡＩ活用の最前線 ＧＰＵサーバーは大きくなかった", "それは大きかったが、なかったし、大きくなった。難しかった問題は大きい。", "𠮷野家の大きい牛丼😀は難しかった" };

        for (final String lang : new String[] { "ja", null }) {
            for (final String text : texts) {
                assertFinalStageMatchesTokenizerStage(text, lang);
            }
        }
    }

    @Test
    public void test_offsetsAreInCoordinatesOfTextBeforeCharFilters() {
        // The char filter changes the length of the text (a half-width katakana with a voiced sound mark becomes 1 character),
        // so the offsets must not be used as positions in the char-filtered text.
        final String text = "ｶﾞｲﾄﾞﾌﾞｯｸは大きい。ﾜｸﾁﾝ接種が難しかった";
        final TokenizerStageResult stage = analyzeTokenizerStage(text, "ja");
        assertTrue(stage.getCharFilteredText().length() < text.length());

        final Map<String, AnalyzeToken> tokensByTerm = new HashMap<>();
        for (final AnalyzeToken token : analyzer.analyze(text, FIELD, "ja")) {
            tokensByTerm.put(token.getTerm(), token);
        }

        final AnalyzeToken adjective = tokensByTerm.get("大きい");
        assertNotNull(tokensByTerm.keySet().toString(), adjective);
        assertEquals("大きい", text.substring(adjective.getStartOffset(), adjective.getEndOffset()));
        final AnalyzeToken inflected = tokensByTerm.get("難しかっ");
        assertNotNull(tokensByTerm.keySet().toString(), inflected);
        assertEquals("難しかっ", text.substring(inflected.getStartOffset(), inflected.getEndOffset()));
    }

    // ---- Inflected forms excluded through parseDocument ----

    @Test
    public void test_inflectedFormsAreNotCandidatesButBaseFormsAre() throws Exception {
        for (final String lang : new String[] { "ja", null }) {
            final DefaultContentsParser parser = new DefaultContentsParser();

            final List<String> texts = texts(parse(parser, "なかっ 大き 難し 大きい 難しい", lang));

            for (final String inflected : List.of("なかっ", "大き", "難し")) {
                assertFalse(lang + ": " + texts, texts.contains(inflected));
            }
            assertTrue(lang + ": " + texts, texts.containsAll(List.of("大きい", "難しい")));
            assertEquals(3, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
            assertNoInflectionUndetermined(parser);
        }
    }

    @Test
    public void test_inflectedFormsInSentenceAreNotCandidates() throws Exception {
        // 大きかっ, なかっ, 難しかっ: 連用タ接続, 大きく: 連用テ接続. 大きい and 難しい are 基本形.
        final String text = "それは大きかったが、なかったし、大きくなった。難しかった問題は大きい。難しい。";
        final DefaultContentsParser parser = new DefaultContentsParser();

        final List<String> texts = texts(parse(parser, text, "ja"));

        for (final String inflected : List.of("大きかっ", "なかっ", "大きく", "難しかっ")) {
            assertFalse(texts.toString(), texts.contains(inflected));
        }
        assertTrue(texts.toString(), texts.containsAll(List.of("大きい", "難しい")));
        assertEquals(4, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
        assertNoInflectionUndetermined(parser);
    }

    @Test
    public void test_inflectedFormsAreNotExcludedWhenInflectionIsDisabled() throws Exception {
        System.setProperty(INFLECTION_ENABLED_PROPERTY, "false");
        final DefaultContentsParser parser = new DefaultContentsParser();

        final List<String> texts = texts(parse(parser, "なかっ 大き 難し 大きい 難しい", "ja"));

        assertTrue(texts.toString(), texts.containsAll(List.of("なかっ", "大き", "難し", "大きい", "難しい")));
        assertEquals(0, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
    }

    @Test
    public void test_inflectedFormsAreExcludedPerChunkInMultiChunkDocument() throws Exception {
        // The first chunk closes right after the 331st filler (9 + 3 * 331 = 1002 characters > 1000), so the second chunk starts with
        // 難しい. The two chunks have the adjectives at the same offsets with different terms (大きい 難しかっ / 難しい 大きかっ):
        // a map of the wrong chunk does not match, and the inflected form stays.
        final String filler = "円安 ".repeat(331);
        final String text = "大きい 難しかっ " + filler + "難しい 大きかっ なかっ";
        for (final String lang : new String[] { "ja", null }) {
            final DefaultContentsParser parser = new DefaultContentsParser();

            final List<String> texts = texts(parse(parser, text, lang));

            assertEquals(2, parser.getCompoundAnalyzedChunkCount());
            for (final String inflected : List.of("難しかっ", "大きかっ", "なかっ")) {
                assertFalse(lang + ": " + inflected, texts.contains(inflected));
            }
            assertTrue(lang + ": " + texts, texts.containsAll(List.of("大きい", "難しい", "円")));
            assertEquals(3, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
            assertNoInflectionUndetermined(parser);
        }

        System.setProperty(INFLECTION_ENABLED_PROPERTY, "false");
        final List<String> legacyTexts = texts(parse(new DefaultContentsParser(), text, "ja"));
        assertTrue(legacyTexts.toString(), legacyTexts.containsAll(List.of("難しかっ", "大きかっ", "なかっ")));
    }

    @Test
    public void test_inflectedFormsAreExcludedForMappedCharFilterInput() throws Exception {
        // The chunk text has half-width katakana: the offsets are not positions in the char-filtered text.
        final DefaultContentsParser parser = new DefaultContentsParser();

        final List<String> texts = texts(parse(parser, "ｶﾞｲﾄﾞﾌﾞｯｸは大きい。ﾜｸﾁﾝ接種が難しかった", "ja"));

        assertFalse(texts.toString(), texts.contains("難しかっ"));
        assertTrue(texts.toString(), texts.containsAll(List.of("ガイドブック", "大きい", "ワクチン", "接種", "ワクチン接種")));
        assertEquals(1, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
        assertNoInflectionUndetermined(parser);
    }

    // ---- Example words: no regression ----

    @Test
    public void test_exampleWordsAreGeneratedAsBefore() throws Exception {
        final String text = "新型コロナ 大統領選 高市早苗 石破首相 ワクチン接種 円安 神奈川県 togetter.com";
        final List<String> examples = List.of("新型コロナ", "大統領選", "高市早苗", "石破首相", "ワクチン接種", "円安", "神奈川県", "togetter.com");

        final Set<String> texts = new HashSet<>(texts(parse(new DefaultContentsParser(), text, "ja")));

        System.setProperty(FILTER_ENABLED_PROPERTY, "false");
        System.setProperty(INFLECTION_ENABLED_PROPERTY, "false");
        final Set<String> legacyTexts = new HashSet<>(texts(parse(new DefaultContentsParser(), text, "ja")));

        for (final String example : examples) {
            assertTrue("generated: " + example + " in " + texts, texts.contains(example));
            assertTrue("generated before: " + example + " in " + legacyTexts, legacyTexts.contains(example));
        }
        // The only difference is the fragment "." of togetter.com (no letter or digit), which is a noise fragment.
        final Set<String> removed = new HashSet<>(legacyTexts);
        removed.removeAll(texts);
        final Set<String> added = new HashSet<>(texts);
        added.removeAll(legacyTexts);
        assertEquals(Set.of("."), removed);
        assertEquals(Set.of(), added);
    }

    // ---- Fragments of long vowel marks, small tsu and symbols ----

    @Test
    public void test_sokuonChouonAndSymbolFragmentsAreNotCandidates() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        final List<String> texts = texts(parse(parser, "ー警視庁編ー 新型コロナ ッ . / ー ッッ 円安 ー", "ja"));

        for (final String noise : List.of("ー", "ッ", ".", "/", "ッッ")) {
            assertFalse("<" + noise + "> in " + texts, texts.contains(noise));
        }
        // The long vowel marks at the ends of the compound word are trimmed.
        assertTrue(texts.toString(), texts.containsAll(List.of("警視庁", "警視庁編", "新型コロナ", "円安")));
        assertFalse(texts.toString(), texts.contains("ー警視庁編ー"));
        assertTrue(parser.getExcludedFragmentCount(FragmentExclusion.SOKUON_CHOUON_ONLY) > 0);
        assertTrue(parser.getExcludedFragmentCount(FragmentExclusion.SYMBOL_ONLY) > 0);
    }

    @Test
    public void test_sokuonChouonAndSymbolFragmentsAreCandidatesWhenFilterIsDisabled() throws Exception {
        System.setProperty(FILTER_ENABLED_PROPERTY, "false");
        final DefaultContentsParser parser = new DefaultContentsParser();

        final List<String> texts = texts(parse(parser, "ー警視庁編ー 新型コロナ ッ . / ー ッッ 円安 ー", "ja"));

        assertTrue(texts.toString(), texts.containsAll(List.of("ー", "ッ", ".", "/", "ッッ")));
        assertTrue(texts.toString(), texts.contains("ー警視庁編ー"));
        assertFalse(texts.toString(), texts.contains("警視庁編"));
        assertEquals(0, parser.getExcludedFragmentCount(FragmentExclusion.SOKUON_CHOUON_ONLY));
        assertEquals(0, parser.getExcludedFragmentCount(FragmentExclusion.SYMBOL_ONLY));
    }

    @Test
    public void test_runsOfSokuonChouonAndSymbolsAreNotCandidates() throws Exception {
        // 〇 and ★ are not in this text: for ja the analyzer already drops them even when the filter is disabled, so they would
        // not show that the filter works (see test_symbolsAreNotCandidatesForOtherLanguage).
        final String text = "ーーー ッ ッッ . / ... 大統領選";

        final List<String> texts = texts(parse(new DefaultContentsParser(), text, "ja"));

        assertEquals(texts.toString(), Set.of("大統領", "大統領選"), new HashSet<>(texts));

        System.setProperty(FILTER_ENABLED_PROPERTY, "false");
        final List<String> legacyTexts = texts(parse(new DefaultContentsParser(), text, "ja"));
        assertTrue(legacyTexts.toString(), legacyTexts.containsAll(List.of("ーーー", "ッ", "ッッ", ".", "/", "...")));
    }

    @Test
    public void test_symbolsAreNotCandidatesForOtherLanguage() throws Exception {
        // The rules of the symbols and the small tsu / long vowel marks do not depend on the language. With en the analyzer
        // does not drop the symbols 〇 and ★, so only the filter does.
        final String text = "ーーー 〇 ★ 大統領選";

        final List<String> texts = texts(parse(new DefaultContentsParser(), text, "en"));

        for (final String noise : List.of("ーーー", "〇", "★")) {
            assertFalse(noise + " in " + texts, texts.contains(noise));
        }
        assertTrue(texts.toString(), texts.contains("選"));

        System.setProperty(FILTER_ENABLED_PROPERTY, "false");
        final List<String> legacyTexts = texts(parse(new DefaultContentsParser(), text, "en"));
        assertTrue(legacyTexts.toString(), legacyTexts.containsAll(List.of("ーーー", "〇", "★")));
    }

    // ---- Single letters, digits and kana by the language ----

    @Test
    public void test_singleAlphanumericAndKanaAreExcludedForJapaneseOrUnspecifiedLanguage() throws Exception {
        // The tokenizer gives x, ろ, ne, 東京 and ai as they are. The final stage of ja drops the number 7 even when the filter is
        // disabled, so the digit is not a proof of the rule here (it is for en).
        final String text = "x ろ 7 ne 東京 ai";
        for (final String lang : new String[] { "ja", null }) {
            final DefaultContentsParser parser = new DefaultContentsParser();

            final List<String> texts = texts(parse(parser, text, lang));

            for (final String excluded : List.of("x", "ろ", "7")) {
                assertFalse(lang + ": <" + excluded + "> in " + texts, texts.contains(excluded));
            }
            // A 2-letter word is kept by default, and the real word stays.
            assertTrue(lang + ": " + texts, texts.containsAll(List.of("ne", "東京", "ai")));
            assertTrue(parser.getExcludedFragmentCount(FragmentExclusion.ALPHANUMERIC_SHORT) > 0);
            assertTrue(parser.getExcludedFragmentCount(FragmentExclusion.SINGLE_KANA) > 0);
        }
    }

    @Test
    public void test_singleAlphanumericAndKanaAreKeptForOtherLanguage() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        final List<String> texts = texts(parse(parser, "x ろ 7 ne 東京 ai", "en"));

        assertTrue(texts.toString(), texts.containsAll(List.of("x", "ろ", "7", "ne", "ai")));
        assertEquals(0, parser.getExcludedFragmentCount(FragmentExclusion.ALPHANUMERIC_SHORT));
        assertEquals(0, parser.getExcludedFragmentCount(FragmentExclusion.SINGLE_KANA));
    }

    @Test
    public void test_singleAlphanumericAndKanaAreCandidatesWhenFilterIsDisabled() throws Exception {
        System.setProperty(FILTER_ENABLED_PROPERTY, "false");
        final DefaultContentsParser parser = new DefaultContentsParser();

        final List<String> texts = texts(parse(parser, "x ろ 7 ne 東京 ai", "ja"));

        // The number 7 is dropped by the analyzer of ja, not by the filter.
        assertTrue(texts.toString(), texts.containsAll(List.of("x", "ろ", "ne", "東京", "ai")));
        assertEquals(0, parser.getExcludedFragmentCount(FragmentExclusion.ALPHANUMERIC_SHORT));
        assertEquals(0, parser.getExcludedFragmentCount(FragmentExclusion.SINGLE_KANA));
    }

    // ---- Helpers ----

    private static void assertFinalStageMatchesTokenizerStage(final String text, final String lang) {
        final TokenizerStageResult stage = analyzeTokenizerStage(text, lang);
        final Map<String, String> stageTerms = new HashMap<>();
        for (final AnalyzeToken token : stage.getTokens()) {
            stageTerms.put(token.getStartOffset() + "," + token.getEndOffset(), token.getTerm().toLowerCase(Locale.ROOT));
        }

        final List<AnalyzeToken> finalTokens = analyzer.analyze(text, FIELD, lang);

        assertFalse(lang + ": " + text, finalTokens.isEmpty());
        for (final AnalyzeToken token : finalTokens) {
            final String key = token.getStartOffset() + "," + token.getEndOffset();
            assertEquals(lang + ": " + text + " at " + key, stageTerms.get(key), token.getTerm());
        }
    }

    private static void assertNoInflectionUndetermined(final DefaultContentsParser parser) {
        for (final InflectionUndetermined cause : InflectionUndetermined.values()) {
            assertEquals(cause.name(), 0, parser.getInflectionUndeterminedChunkCount(cause));
        }
    }

    private static List<SuggestItem> parse(final DefaultContentsParser parser, final String text, final String lang) throws IOException {
        final Map<String, Object> document = new HashMap<>();
        document.put(FIELD, text);
        if (lang != null) {
            document.put(LANG_FIELD_NAME, lang);
        }
        final List<SuggestItem> items = parser.parseDocument(document, FIELDS, TAG_FIELD_NAMES, ROLE_FIELD_NAME, LANG_FIELD_NAME,
                createReadingConverter(), createReadingConverter(), new NormalizerChain(), analyzer);
        assertNotNull(items);
        return items;
    }

    private static List<String> texts(final List<SuggestItem> items) {
        return items.stream().map(SuggestItem::getText).collect(Collectors.toList());
    }

    private static ReadingConverter createReadingConverter() throws IOException {
        final ReadingConverterChain chain = new ReadingConverterChain();
        chain.addConverter(new KatakanaToAlphabetConverter());
        chain.init();
        return chain;
    }

    private static Map<String, String> inflectionFormsByTerm(final String text, final String lang) {
        final Map<String, String> forms = new HashMap<>();
        for (final AnalyzeToken token : analyzeTokenizerStage(text, lang).getTokens()) {
            final Object form = token.getAttributes().get(INFLECTION_FORM_ATTRIBUTE);
            if (form != null) {
                forms.put(token.getTerm(), form.toString());
            }
        }
        return forms;
    }

    private static TokenizerStageResult analyzeTokenizerStage(final String text, final String lang) {
        final TokenizerStageResult result = analyzer.analyzeTokenizerStage(text, FIELD, lang);
        assertNotNull("tokenizer stage of: " + text, result);
        return result;
    }
}
