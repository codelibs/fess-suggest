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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.analysis.TokenizerStageResult;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.contents.DefaultContentsParser.FragmentExclusion;
import org.codelibs.fess.suggest.normalizer.NormalizerChain;
import org.codelibs.fesen.opensearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests of the fragment filter of {@link DefaultContentsParser}: the fragments of documents, the search words,
 * and the symbol and small tsu / long vowel tokens at both ends of a compound word.
 * The analyzer is a stub that returns fixed tokens, so no OpenSearch is needed.
 */
public class DefaultContentsParserFragmentTest {

    private static final String FIELD = "content";

    private static final String PROP_FILTER = "fess.suggest.fragment.filter.enabled";

    private static final String PROP_LATIN_SHORT = "fess.suggest.fragment.latin.short.enabled";

    private static final String PROP_SINGLE_HAN = "fess.suggest.fragment.single.han.enabled";

    private static final String PROP_COMPOUND = "fess.suggest.compound.enabled";

    private static final String PROP_COMPOUND_MAX_TOKENS = "fess.suggest.compound.max.tokens";

    private static final String PROP_COMPOUND_MAX_LENGTH = "fess.suggest.compound.max.length";

    private static final String[] PROPERTIES =
            { PROP_FILTER, PROP_LATIN_SHORT, PROP_SINGLE_HAN, PROP_COMPOUND, PROP_COMPOUND_MAX_TOKENS, PROP_COMPOUND_MAX_LENGTH };

    private static final Set<String> SUFFIX_TERMS = Set.of("選", "安", "県");

    private final Map<String, String> originalProperties = new HashMap<>();

    @Before
    public void setUp() {
        for (final String name : PROPERTIES) {
            originalProperties.put(name, System.getProperty(name));
            System.clearProperty(name);
        }
        // The fragment tests do not need the compound words. The compound tests enable it again.
        System.setProperty(PROP_COMPOUND, "false");
    }

    @After
    public void tearDown() {
        for (final String name : PROPERTIES) {
            final String original = originalProperties.get(name);
            if (original == null) {
                System.clearProperty(name);
            } else {
                System.setProperty(name, original);
            }
        }
    }

    // ============================================================
    // Fragments of documents
    // ============================================================

    @Test
    public void test_c1_sokuonChouonOnlyIsExcluded() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        for (final String term : new String[] { "ッ", "ッッ", "ー", "ーー", "ーッッ", "っ", "ｰ", "ｯ" }) {
            assertEquals(term, List.of(), fragments(parser, "ja", term));
        }
        assertEquals(8, parser.getExcludedFragmentCount(FragmentExclusion.SOKUON_CHOUON_ONLY));
    }

    @Test
    public void test_c1_wordWithLetterIsKept() throws Exception {
        assertEquals(List.of("ーメン", "ラー"), fragments(new DefaultContentsParser(), "ja", "ーメン", "ラー"));
    }

    @Test
    public void test_c1_isAppliedToAnyLanguage() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        assertEquals(List.of(), fragments(parser, "en", "ー"));
        assertEquals(List.of(), fragments(parser, "xx", "ッ"));
    }

    @Test
    public void test_c2_noLetterOrDigitIsExcluded() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        // The last one is an emoji (a surrogate pair).
        for (final String term : new String[] { ".", "/", "://", "-", "(@", "!!!", "😀" }) {
            assertEquals(term, List.of(), fragments(parser, "ja", term));
        }
        assertEquals(7, parser.getExcludedFragmentCount(FragmentExclusion.SYMBOL_ONLY));
        // Any language.
        assertEquals(List.of(), fragments(parser, "en", "!!!"));
        assertEquals(List.of(), fragments(parser, null, "!!!"));
    }

    @Test
    public void test_singleAlphanumeric_isExcludedForJapaneseOrUnspecified() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        for (final String lang : new String[] { "ja", null, "" }) {
            assertEquals(lang, List.of(), fragments(parser, lang, "x", "7"));
        }
        assertEquals(6, parser.getExcludedFragmentCount(FragmentExclusion.ALPHANUMERIC_SHORT));
    }

    @Test
    public void test_singleAlphanumeric_isKeptForOtherLanguages() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        // "ja_JP" is not "ja": the length rules apply only to a language known to be Japanese.
        for (final String lang : new String[] { "en", "xx", "ja_JP" }) {
            assertEquals(lang, List.of("x", "7"), fragments(parser, lang, "x", "7"));
        }
    }

    @Test
    public void test_twoAlphanumerics_areKeptByDefault() throws Exception {
        assertEquals(List.of("ne", "ai"), fragments(new DefaultContentsParser(), "ja", "ne", "ai"));
    }

    @Test
    public void test_twoAlphanumerics_areExcludedByLatinShort() throws Exception {
        System.setProperty(PROP_LATIN_SHORT, "true");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("com"), fragments(parser, "ja", "ne", "ai", "x", "com"));
        assertEquals(3, parser.getExcludedFragmentCount(FragmentExclusion.ALPHANUMERIC_SHORT));
        // Not for the other languages.
        assertEquals(List.of("ne", "ai"), fragments(parser, "en", "ne", "ai"));
        // A letter with a non-ASCII character is not "alphanumeric 2 characters".
        assertEquals(List.of("aあ", "あい"), fragments(parser, "ja", "aあ", "あい"));
    }

    @Test
    public void test_singleKana_isExcluded() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        assertEquals(List.of(), fragments(parser, "ja", "ろ", "ガ", "ん"));
        assertEquals(3, parser.getExcludedFragmentCount(FragmentExclusion.SINGLE_KANA));
        // Not for the other languages.
        assertEquals(List.of("ろ"), fragments(parser, "en", "ろ"));
    }

    @Test
    public void test_singleHan_isKeptByDefaultAndExcludedBySetting() throws Exception {
        assertEquals(List.of("月", "米"), fragments(new DefaultContentsParser(), "ja", "月", "米"));

        System.setProperty(PROP_SINGLE_HAN, "true");
        final DefaultContentsParser parser = new DefaultContentsParser();
        assertEquals(List.of(), fragments(parser, "ja", "月", "米"));
        assertEquals(2, parser.getExcludedFragmentCount(FragmentExclusion.SINGLE_HAN));
        // A han character outside the BMP is one code point.
        assertEquals(List.of(), fragments(parser, "ja", "𠮷"));
        assertEquals(List.of("月"), fragments(parser, "en", "月"));
    }

    @Test
    public void test_boundary_longerWordsAreKept() throws Exception {
        System.setProperty(PROP_SINGLE_HAN, "true");
        assertEquals(List.of("あと", "日本", "com", "ーメン"), fragments(new DefaultContentsParser(), "ja", "あと", "日本", "com", "ーメン"));
    }

    @Test
    public void test_readings_areNotShiftedByExcludedFragments() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        final List<SuggestItem> items = parse(parser, FragmentStubAnalyzer.forDocument("ー", "東京", ".", "x", "大阪", "ッ"), "ja");

        assertEquals(List.of("東京", "大阪"), texts(items));
        // The reading given by the analyzer is "yomi-" + the surface of the token at the same index.
        assertEquals("yomi-東京", items.get(0).getReadings()[0][0]);
        assertEquals("yomi-大阪", items.get(1).getReadings()[0][0]);
    }

    @Test
    public void test_excludedFragmentCounts_areSplitByReason() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        fragments(parser, "ja", "ー", "ッ", ".", "x", "ろ", "月", "ne", "東京");

        assertEquals(2, parser.getExcludedFragmentCount(FragmentExclusion.SOKUON_CHOUON_ONLY));
        assertEquals(1, parser.getExcludedFragmentCount(FragmentExclusion.SYMBOL_ONLY));
        assertEquals(1, parser.getExcludedFragmentCount(FragmentExclusion.ALPHANUMERIC_SHORT));
        assertEquals(1, parser.getExcludedFragmentCount(FragmentExclusion.SINGLE_KANA));
        assertEquals(0, parser.getExcludedFragmentCount(FragmentExclusion.SINGLE_HAN));
        assertEquals(0, parser.getExcludedSearchWordCount(FragmentExclusion.SYMBOL_ONLY));
    }

    // ============================================================
    // Settings
    // ============================================================

    @Test
    public void test_filterDisabled_restoresTheBehaviorBeforeTheFilter() throws Exception {
        System.setProperty(PROP_FILTER, "false");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("ー", "ッ", ".", "x", "ろ"), fragments(parser, "ja", "ー", "ッ", ".", "x", "ろ"));
        assertEquals(0, parser.getExcludedFragmentCount(FragmentExclusion.SOKUON_CHOUON_ONLY));
    }

    @Test
    public void test_filterDisabled_disablesOptionalRules() throws Exception {
        System.setProperty(PROP_FILTER, "false");
        System.setProperty(PROP_LATIN_SHORT, "true");
        System.setProperty(PROP_SINGLE_HAN, "true");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("ne", "ai", "月", "x"), fragments(parser, "ja", "ne", "ai", "月", "x"));
    }

    @Test
    public void test_filterEnabledExplicitly_and_invalidValueIsFalse() throws Exception {
        System.setProperty(PROP_FILTER, "TRUE");
        assertEquals(List.of(), fragments(new DefaultContentsParser(), "ja", "ー"));

        // Boolean.parseBoolean: anything but "true" is false.
        System.setProperty(PROP_FILTER, "yes");
        assertEquals(List.of("ー"), fragments(new DefaultContentsParser(), "ja", "ー"));
    }

    @Test
    public void test_optionalRulesAreOffByDefault_andFalseIsSameAsDefault() throws Exception {
        System.setProperty(PROP_LATIN_SHORT, "false");
        System.setProperty(PROP_SINGLE_HAN, "false");
        assertEquals(List.of("ne", "月"), fragments(new DefaultContentsParser(), "ja", "ne", "月"));
    }

    // ============================================================
    // Both ends of a compound word
    // ============================================================

    @Test
    public void test_compoundTrim_sokuonChouonAtTheHeadAndTheTailAreRemoved() throws Exception {
        System.setProperty(PROP_COMPOUND, "true");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("めん才遊記"), compoundsOf(parser, "ー", "めん", "才遊記"));
        assertEquals(List.of("警視庁編"), compoundsOf(parser, "ー", "警視庁", "編", "ー"));
        assertEquals(List.of("東京大学"), compoundsOf(parser, ".", "ー", "東京", "大学", "ー"));
    }

    @Test
    public void test_compoundTrim_everySokuonChouonCharacterIsRemovedAtBothEnds() throws Exception {
        System.setProperty(PROP_COMPOUND, "true");
        final DefaultContentsParser parser = new DefaultContentsParser();

        // The small tsu and the long vowel mark, in full width, hiragana and half width, at the head and at the tail.
        assertEquals(List.of("東京大学"), compoundsOf(parser, "ッ", "東京", "大学", "ｯ"));
        assertEquals(List.of("東京大学"), compoundsOf(parser, "っ", "東京", "大学", "ｰ"));
        assertEquals(List.of("東京大学"), compoundsOf(parser, "ｰ", "東京", "大学", "っ"));
    }

    @Test
    public void test_compoundTrim_maxLengthIsAppliedAfterTrimming() throws Exception {
        System.setProperty(PROP_COMPOUND, "true");
        System.setProperty(PROP_COMPOUND_MAX_LENGTH, "4");
        final DefaultContentsParser parser = new DefaultContentsParser();

        // 6 characters with both ends, which is over the limit, and 4 characters (the limit) after trimming.
        assertEquals(List.of("東京大学"), compoundsOf(parser, "ッ", "東京", "大学", "ｯ"));
        assertEquals(0, parser.getDegradedChunkCount(DefaultContentsParser.CompoundDegradation.OVER_LIMIT));
        // The limit is really applied: 5 characters after trimming is over it.
        assertEquals(List.of(), compoundsOf(parser, "ッ", "東京", "大学院", "ｯ"));
        assertEquals(1, parser.getDegradedChunkCount(DefaultContentsParser.CompoundDegradation.OVER_LIMIT));
    }

    @Test
    public void test_compoundTrim_middleIsKept() throws Exception {
        System.setProperty(PROP_COMPOUND, "true");
        assertEquals(List.of("ぴー先生"), compoundsOf(new DefaultContentsParser(), "ぴ", "ー", "先生"));
    }

    @Test
    public void test_compoundTrim_lessThanTwoTokensAfterTrimmingMakesNoCompoundWord() throws Exception {
        System.setProperty(PROP_COMPOUND, "true");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of(), compoundsOf(parser, "ゆ", "ー"));
        assertEquals(List.of(), compoundsOf(parser, "ー", "ー"));
    }

    @Test
    public void test_compoundTrim_limitsAreAppliedAfterTrimming() throws Exception {
        System.setProperty(PROP_COMPOUND, "true");
        System.setProperty(PROP_COMPOUND_MAX_TOKENS, "5");
        final DefaultContentsParser parser = new DefaultContentsParser();

        // 7 tokens with both ends, 5 tokens after trimming.
        assertEquals(List.of("東京大阪名古屋福岡札幌"), compoundsOf(parser, "ー", "東京", "大阪", "名古屋", "福岡", "札幌", "ー"));
    }

    @Test
    public void test_compoundTrim_filterDisabledKeepsSokuonChouonButStillTrimsSymbols() throws Exception {
        System.setProperty(PROP_COMPOUND, "true");
        System.setProperty(PROP_FILTER, "false");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("ーめん才遊記"), compoundsOf(parser, "ー", "めん", "才遊記"));
        assertEquals(List.of("東京大学"), compoundsOf(parser, ".", "東京", "大学", "."));
    }

    @Test
    public void test_compound_existingExamplesAreStillGenerated() throws Exception {
        System.setProperty(PROP_COMPOUND, "true");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("新型コロナ"), compoundsOf(parser, "新型", "コロナ"));
        assertEquals(List.of("大統領選"), compoundsOf(parser, "大統領", "選"));
        assertEquals(List.of("高市早苗"), compoundsOf(parser, "高市", "早苗"));
        assertEquals(List.of("石破首相"), compoundsOf(parser, "石破", "首相"));
        assertEquals(List.of("ワクチン接種"), compoundsOf(parser, "ワクチン", "接種"));
        assertEquals(List.of("円安"), compoundsOf(parser, "円", "安"));
        assertEquals(List.of("togetter.com"), compoundsOf(parser, "togetter", ".", "com"));
        assertEquals(List.of("神奈川県"), compoundsOf(parser, "神奈川", "県"));
    }

    // ============================================================
    // Search words (search log)
    // ============================================================

    @Test
    public void test_searchWords_sokuonChouonAndSymbolsAreExcludedWithoutAnalysis() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        final FragmentStubAnalyzer analyzer = FragmentStubAnalyzer.forSearchWords();

        for (final String word : new String[] { "ッ", "ー", "!!!", "..." }) {
            assertNull(word, parseSearchWords(parser, analyzer, null, word));
            assertNull(word, parseSearchWords(parser, analyzer, new String[] { "ja" }, word));
            // The rules do not depend on the language.
            assertNull(word, parseSearchWords(parser, analyzer, new String[] { "en" }, word));
        }
        assertEquals(List.of(), analyzer.analyzeCalls);
        // Each word is parsed three times (without a language, and with ja and en).
        assertEquals(6, parser.getExcludedSearchWordCount(FragmentExclusion.SOKUON_CHOUON_ONLY));
        assertEquals(6, parser.getExcludedSearchWordCount(FragmentExclusion.SYMBOL_ONLY));
    }

    @Test
    public void test_searchWords_shortWordsAndAdjectiveFormsAreKept() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        final FragmentStubAnalyzer analyzer = FragmentStubAnalyzer.forSearchWords();

        for (final String word : new String[] { "ai", "月", "x", "大きく" }) {
            final SuggestItem item = parseSearchWords(parser, analyzer, null, word);
            assertNotNull(word, item);
            assertEquals(word, item.getText());
        }
        for (final String word : new String[] { "ai", "月", "x" }) {
            assertNotNull(word, parseSearchWords(parser, analyzer, new String[] { "ja" }, word));
            assertNotNull(word, parseSearchWords(parser, analyzer, new String[] { "en" }, word));
        }
    }

    @Test
    public void test_searchWords_excludedWordsAreSkippedAndTheRestMakesOneItem() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        final FragmentStubAnalyzer analyzer = FragmentStubAnalyzer.forSearchWords();

        final SuggestItem item = parseSearchWords(parser, analyzer, null, "ー", "映画");
        assertNotNull(item);
        assertEquals("映画", item.getText());
        assertEquals(List.of("映画"), analyzer.analyzeCalls);

        assertNull(parseSearchWords(parser, analyzer, null, "ー", "ッ"));
    }

    @Test
    public void test_searchWords_nullAndEmptyAreExcluded() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        final FragmentStubAnalyzer analyzer = FragmentStubAnalyzer.forSearchWords();

        // null is not judged by the filter but by the analyzer, as before: the stub returns no token for it.
        assertNull(parseSearchWords(parser, analyzer, null, (String) null));
        assertEquals(Arrays.asList((String) null), analyzer.analyzeCalls);
        assertNull(parseSearchWords(parser, analyzer, null, ""));
        assertEquals(1, analyzer.analyzeCalls.size());
    }

    @Test
    public void test_searchWords_filterDisabledPassesEverythingToTheAnalyzer() throws Exception {
        System.setProperty(PROP_FILTER, "false");
        final DefaultContentsParser parser = new DefaultContentsParser();
        final FragmentStubAnalyzer analyzer = FragmentStubAnalyzer.forSearchWords();

        assertNotNull(parseSearchWords(parser, analyzer, null, "ー"));
        assertEquals(List.of("ー"), analyzer.analyzeCalls);
    }

    @Test
    public void test_searchWords_excludedCountsAreSplitByReason() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        final FragmentStubAnalyzer analyzer = FragmentStubAnalyzer.forSearchWords();

        parseSearchWords(parser, analyzer, null, "ッ", "ー", "!!!", "ai");

        assertEquals(2, parser.getExcludedSearchWordCount(FragmentExclusion.SOKUON_CHOUON_ONLY));
        assertEquals(1, parser.getExcludedSearchWordCount(FragmentExclusion.SYMBOL_ONLY));
        assertEquals(0, parser.getExcludedSearchWordCount(FragmentExclusion.ALPHANUMERIC_SHORT));
        assertEquals(0, parser.getExcludedFragmentCount(FragmentExclusion.SYMBOL_ONLY));
    }

    // ============================================================
    // Helpers
    // ============================================================

    private static ReadingConverter readingConverter() {
        return new ReadingConverter() {
            @Override
            public void init() {
            }

            @Override
            public List<String> convert(final String text, final String field, final String... langs) {
                return new ArrayList<>(List.of(text));
            }
        };
    }

    private static List<SuggestItem> parse(final DefaultContentsParser parser, final SuggestAnalyzer analyzer, final String lang)
            throws IOException {
        final Map<String, Object> document = new HashMap<>();
        document.put(FIELD, "text");
        if (lang != null) {
            document.put("lang", lang);
        }
        final ReadingConverter converter = readingConverter();
        return parser.parseDocument(document, new String[] { FIELD }, new String[] { "label" }, "role", "lang", converter, converter,
                new NormalizerChain(), analyzer);
    }

    /** The texts of the fragments made from the tokens of the given terms. */
    private static List<String> fragments(final DefaultContentsParser parser, final String lang, final String... terms) throws IOException {
        return texts(parse(parser, FragmentStubAnalyzer.forDocument(terms), lang));
    }

    /** The texts of the compound words made from the adjacent tokens of the given terms (no fragment is generated). */
    private static List<String> compoundsOf(final DefaultContentsParser parser, final String... terms) throws IOException {
        final List<AnalyzeToken> tokens = new ArrayList<>();
        int offset = 0;
        for (final String term : terms) {
            tokens.add(stageToken(term, offset));
            offset += term.length();
        }
        final TokenizerStageResult stageResult = new TokenizerStageResult(tokens, String.join("", terms));
        final FragmentStubAnalyzer analyzer = new FragmentStubAnalyzer(chunk -> List.of(), chunk -> stageResult);
        return texts(parse(parser, analyzer, "ja"));
    }

    // The tokenizer gives the ASCII symbols 名詞-サ変接続, and the suffixes 名詞-接尾-一般.
    private static AnalyzeToken stageToken(final String term, final int offset) {
        final boolean symbol = term.codePoints().noneMatch(Character::isLetterOrDigit);
        final String partOfSpeech = SUFFIX_TERMS.contains(term) ? "名詞-接尾-一般" : symbol ? "名詞-サ変接続" : "名詞-一般";
        return new AnalyzeToken(term, 0, offset, offset + term.length(), 1, "word", Map.of("partOfSpeech", partOfSpeech, "reading", term));
    }

    private static SuggestItem parseSearchWords(final DefaultContentsParser parser, final SuggestAnalyzer analyzer, final String[] langs,
            final String... words) throws IOException {
        final ReadingConverter converter = readingConverter();
        return parser.parseSearchWords(words, null, new String[] { FIELD }, new String[0], new String[0], 1, converter,
                new NormalizerChain(), analyzer, langs);
    }

    private static List<String> texts(final List<SuggestItem> items) {
        return items.stream().map(SuggestItem::getText).collect(Collectors.toList());
    }

    /** A stub of the analyzer that returns fixed tokens, and records the words passed to {@code analyze}. */
    private static class FragmentStubAnalyzer implements SuggestAnalyzer {

        private final Function<String, List<AnalyzeToken>> tokenizer;

        private final Function<String, TokenizerStageResult> stage;

        final List<String> analyzeCalls = new CopyOnWriteArrayList<>();

        FragmentStubAnalyzer(final Function<String, List<AnalyzeToken>> tokenizer, final Function<String, TokenizerStageResult> stage) {
            this.tokenizer = tokenizer;
            this.stage = stage;
        }

        /** Returns the tokens of the terms for every text. The reading of a token is "yomi-" + the term. */
        static FragmentStubAnalyzer forDocument(final String... terms) {
            final List<AnalyzeToken> tokens = new ArrayList<>();
            int offset = 0;
            for (final String term : terms) {
                tokens.add(new AnalyzeToken(term, 0, offset, offset + term.length(), 1, "word", Map.of()));
                offset += term.length();
            }
            return new FragmentStubAnalyzer(text -> tokens, text -> null);
        }

        /** Returns one token for a text, and no token for null or an empty text (as the real analyzer does for a text without words). */
        static FragmentStubAnalyzer forSearchWords() {
            return new FragmentStubAnalyzer(text -> text == null || text.isEmpty() ? List.of()
                    : List.of(new AnalyzeToken(text, 0, 0, text.length(), 1, "word", Map.of())), text -> null);
        }

        @Override
        public List<AnalyzeToken> analyze(final String text, final String field, final String lang) {
            analyzeCalls.add(text);
            return tokenizer.apply(text);
        }

        @Override
        public List<AnalyzeToken> analyzeAndReading(final String text, final String field, final String lang) {
            return tokenizer.apply(text)
                    .stream()
                    .map(t -> new AnalyzeToken("yomi-" + t.getTerm(), t.getPosition(), t.getStartOffset(), t.getEndOffset(), 1, "word",
                            Map.of()))
                    .collect(Collectors.toList());
        }

        @Override
        public TokenizerStageResult analyzeTokenizerStage(final String text, final String field, final String lang) {
            return stage.apply(text);
        }
    }
}
