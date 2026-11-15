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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.codelibs.fess.suggest.SuggestTestServer;
import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.analysis.TokenizerStageResult;
import org.codelibs.fess.suggest.converter.KatakanaToAlphabetConverter;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.converter.ReadingConverterChain;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.contents.DefaultContentsParser.CompoundDegradation;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.normalizer.NormalizerChain;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Runs {@link DefaultContentsParser#parseDocument} with the real analyzer on the embedded node, so that the compound words are
 * generated from the real explain response. A stub cannot detect a mistake in the response parsing, in which case every chunk
 * silently falls back and no compound word is generated.
 *
 * <p>The expectations of the compound words depend on the tokenizer of the embedded node (kuromoji_neologd_normal).</p>
 */
public class CompoundWordIntegrationTest {

    private static final String[] FIELDS = { "content" };

    private static final String[] TAG_FIELD_NAMES = { "label" };

    private static final String ROLE_FIELD_NAME = "role";

    private static final String LANG_FIELD_NAME = "lang";

    private static final String ENABLED_PROPERTY = "fess.suggest.compound.enabled";

    private static SuggestTestServer server;

    private static SuggestAnalyzer analyzer;

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = SuggestTestServer.start("CompoundWordIntegrationTest");
        server.runner().ensureYellow();
        final SuggestSettings settings = Suggester.builder().build(server.client(), "compoundWordTest").settings();
        analyzer = SuggestUtil.createDefaultAnalyzer(server.client(), settings);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.close();
    }

    @After
    public void after() {
        System.clearProperty(ENABLED_PROPERTY);
    }

    @Test
    public void test_exampleWordsAreGeneratedFromRealExplainResponse() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        final String text = "新型コロナ 大統領選 高市早苗 石破首相 ワクチン接種 円安 神奈川県";

        // The language is given, as the documents of Fess have it: the analyzer of the language is selected.
        final List<String> texts = texts(parse(parser, text, "ja"));

        for (final String expected : List.of("新型コロナ", "大統領選", "高市早苗", "石破首相", "ワクチン接種", "円安", "神奈川県")) {
            assertTrue("compound word is generated: " + expected + " in " + texts, texts.contains(expected));
        }
        assertTrue(parser.getCompoundWordCount() >= 7);
        assertTrue(parser.getCompoundChunkCount() > 0);
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.STAGE_UNAVAILABLE));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.INVALID_OFFSET));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.TEXT_MISMATCH));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.FAILURE));
    }

    @Test
    public void test_compoundWordsInSentence() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        final List<String> texts = texts(parse(parser, "高市早苗氏が石破首相と会談した。ワクチン接種、新型コロナ、円安、検索 エンジン"));

        assertTrue(texts.toString(), texts.containsAll(List.of("高市早苗", "石破首相", "ワクチン接種", "新型コロナ", "円安")));
        // The punctuation and the white space break the run of nouns.
        assertFalse(texts.toString(), texts.contains("ワクチン接種新型コロナ"));
        assertFalse(texts.toString(), texts.contains("検索エンジン"));
    }

    @Test
    public void test_fragmentsAreKept() throws Exception {
        final List<String> texts = texts(parse(new DefaultContentsParser(), "新型コロナ"));

        assertTrue(texts.toString(), texts.containsAll(List.of("新型", "コロナ", "新型コロナ")));
    }

    @Test
    public void test_compoundWordsAreNotGeneratedWhenDisabled() throws Exception {
        final String text = "新型コロナ 大統領選 ワクチン接種";
        final List<String> enabledTexts = texts(parse(new DefaultContentsParser(), text));

        System.setProperty(ENABLED_PROPERTY, "false");
        final DefaultContentsParser disabledParser = new DefaultContentsParser();
        final List<String> disabledTexts = texts(parse(disabledParser, text));

        assertTrue(enabledTexts.contains("新型コロナ"));
        assertFalse(disabledTexts.toString(), disabledTexts.contains("新型コロナ"));
        assertFalse(disabledTexts.toString(), disabledTexts.contains("大統領選"));
        assertTrue(disabledTexts.toString(), disabledTexts.containsAll(List.of("新型", "コロナ", "大統領", "ワクチン", "接種")));
        assertEquals(0, disabledParser.getCompoundAnalyzedChunkCount());
        assertEquals(0, disabledParser.getCompoundWordCount());
    }

    @Test
    public void test_everyCompoundWordIsSubstringOfCharFilteredText() throws Exception {
        final String[] documents = {
                // full-width alphanumerics and half-width katakana
                "ＡＩ活用の最前線 ｶﾞｲﾄﾞﾌﾞｯｸ新型コロナ ﾜｸﾁﾝ接種 ＡＩモデル", "ChatGPT活用 iPhone15の新機能 ＧＰＵサーバー運用",
                // numbers and counters
                "3000万円の令和5年度予算 2024年10月1日の東京都議会選挙", "神奈川県横浜市の人口と大阪府大阪市の人口 ホーム製品情報会社概要",
                // punctuation, white spaces and a long sentence
                "新型コロナウイルス感染症が拡大した。ワクチン接種、新型コロナ、円安、検索 エンジン。", "検索エンジン　全文検索　システム、データベース管理システム(DBMS)の設計", };

        final DefaultContentsParser parser = new DefaultContentsParser();
        for (final String document : documents) {
            final Set<String> fragmentTexts = fragmentTexts(document);
            // The compound word is lower-cased after it is confirmed in the char-filtered text.
            final String charFilteredText = charFilteredText(document).toLowerCase(Locale.ROOT);
            final List<String> compoundTexts =
                    texts(parse(parser, document)).stream().filter(t -> !fragmentTexts.contains(t)).distinct().collect(Collectors.toList());

            for (final String compoundText : compoundTexts) {
                assertTrue("compound word <" + compoundText + "> is a substring of <" + charFilteredText + ">",
                        charFilteredText.contains(compoundText));
            }
            // A document without any compound word would pass the checks above vacuously.
            assertFalse("compound words are generated from: " + document, compoundTexts.isEmpty());
        }

        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.STAGE_UNAVAILABLE));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.INVALID_OFFSET));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.TEXT_MISMATCH));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.FAILURE));
    }

    @Test
    public void test_normalizedCompoundWords() throws Exception {
        final List<String> texts = texts(parse(new DefaultContentsParser(), "ｶﾞｲﾄﾞﾌﾞｯｸ新型コロナ ﾜｸﾁﾝ接種 ＡＩ活用"));

        // Half-width katakana and full-width alphanumerics are normalized by the char filter, and the surface is lower-cased.
        assertTrue(texts.toString(), texts.contains("ワクチン接種"));
        assertTrue(texts.toString(), texts.contains("ai活用"));
        assertTrue(texts.toString(), texts.contains("ガイドブック新型コロナ"));
    }

    @Test
    public void test_compoundItemIdMatchesTextId() throws Exception {
        final List<SuggestItem> items = parse(new DefaultContentsParser(), "新型コロナ 大統領選");

        final SuggestItem compoundItem = items.stream().filter(item -> "新型コロナ".equals(item.getText())).findFirst().orElseThrow();
        assertEquals(SuggestUtil.createSuggestTextId("新型コロナ"), compoundItem.getId());
        assertEquals(SuggestItem.Kind.DOCUMENT, compoundItem.getKinds()[0]);
        assertEquals(1, compoundItem.getDocFreq());
        for (final SuggestItem item : items) {
            assertEquals(SuggestUtil.createSuggestTextId(item.getText()), item.getId());
        }
    }

    @Test
    public void test_longTextWithoutWhiteSpacesIsProcessedAsOneChunk() throws Exception {
        // Over 1000 chars and no white spaces: a single chunk. The punctuation breaks the run of nouns,
        // otherwise the whole text is one run that exceeds the max number of tokens.
        final String text = "新型コロナ、".repeat(300) + "大統領選";
        assertTrue(text.length() > 1000);
        final DefaultContentsParser parser = new DefaultContentsParser();

        final List<String> texts = texts(parse(parser, text));

        assertEquals(1, parser.getCompoundAnalyzedChunkCount());
        assertEquals(1, parser.getCompoundChunkCount());
        assertTrue(texts.toString(), texts.contains("大統領選"));
        assertTrue(texts.contains("新型"));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.STAGE_UNAVAILABLE));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.FAILURE));
    }

    @Test
    public void test_textWithWhiteSpacesIsSplitIntoChunks() throws Exception {
        final String text = "新型コロナ ".repeat(500) + "大統領選";
        final DefaultContentsParser parser = new DefaultContentsParser();

        final List<String> texts = texts(parse(parser, text));

        assertTrue(parser.getCompoundAnalyzedChunkCount() > 1);
        assertTrue(texts.contains("新型コロナ"));
        assertTrue(texts.contains("大統領選"));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.STAGE_UNAVAILABLE));
    }

    @Test
    public void test_tooLongTextFallsBackToFragmentsWithoutException() throws Exception {
        // Explain counts the tokens of the tokenizer stage against index.analyze.max_token_count (10000): the chunk falls back.
        // This test depends on the default value of that setting (10000) of the embedded node: the text has 20000 tokens.
        final String text = "円安".repeat(10000);
        final DefaultContentsParser parser = new DefaultContentsParser();

        final List<SuggestItem> items = parse(parser, text);

        assertFalse(items.isEmpty());
        assertTrue(texts(items).contains("円"));
        assertEquals(0, parser.getCompoundWordCount());
        assertEquals(1, parser.getCompoundAnalyzedChunkCount());
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.STAGE_UNAVAILABLE));
    }

    @Test
    public void test_englishAnalyzerHasNoPartOfSpeechAndGeneratesNoCompoundWord() throws Exception {
        // A real "en" analyzer returns the tokenizer stage without part of speech: it is neither unavailable nor a failure.
        final DefaultContentsParser parser = new DefaultContentsParser();

        final List<SuggestItem> items = parse(parser, "Hello World search engine", "en");

        assertFalse(items.isEmpty());
        assertTrue(parser.getDegradedChunkCount(CompoundDegradation.NO_PART_OF_SPEECH) >= 1);
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.STAGE_UNAVAILABLE));
        assertEquals(0, parser.getCompoundWordCount());
    }

    private List<SuggestItem> parse(final DefaultContentsParser parser, final String text) throws IOException {
        return parse(parser, text, null);
    }

    private List<SuggestItem> parse(final DefaultContentsParser parser, final String text, final String lang) throws IOException {
        final Map<String, Object> document = new HashMap<>();
        document.put("content", text);
        if (lang != null) {
            document.put(LANG_FIELD_NAME, lang);
        }
        final List<SuggestItem> items = parser.parseDocument(document, FIELDS, TAG_FIELD_NAMES, ROLE_FIELD_NAME, LANG_FIELD_NAME,
                createReadingConverter(), createReadingConverter(), createNormalizer(), analyzer);
        assertNotNull(items);
        return items;
    }

    private static List<String> texts(final List<SuggestItem> items) {
        return items.stream().map(SuggestItem::getText).collect(Collectors.toList());
    }

    /** The texts of the fragments, which are generated without the compound words. */
    private Set<String> fragmentTexts(final String document) throws IOException {
        final String previous = System.getProperty(ENABLED_PROPERTY);
        System.setProperty(ENABLED_PROPERTY, "false");
        try {
            return Set.copyOf(texts(parse(new DefaultContentsParser(), document)));
        } finally {
            if (previous == null) {
                System.clearProperty(ENABLED_PROPERTY);
            } else {
                System.setProperty(ENABLED_PROPERTY, previous);
            }
        }
    }

    /** The documents of this test have no more than 1000 characters, and are single-space separated: the same text as the chunk. */
    private String charFilteredText(final String document) {
        final TokenizerStageResult result = analyzer.analyzeTokenizerStage(document.trim(), "content", null);
        assertNotNull("tokenizer stage of: " + document, result);
        return result.getCharFilteredText();
    }

    private static ReadingConverter createReadingConverter() throws IOException {
        final ReadingConverterChain chain = new ReadingConverterChain();
        chain.addConverter(new KatakanaToAlphabetConverter());
        chain.init();
        return chain;
    }

    private static Normalizer createNormalizer() {
        return new NormalizerChain();
    }
}
