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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.analysis.TokenizerStageResult;
import org.codelibs.fess.suggest.converter.KatakanaToAlphabetConverter;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.converter.ReadingConverterChain;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.contents.DefaultContentsParser.CompoundDegradation;
import org.codelibs.fess.suggest.normalizer.NormalizerChain;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.codelibs.fesen.opensearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests of the compound word generation of {@link DefaultContentsParser}.
 * The analyzer is a stub that returns fixed tokens, so no OpenSearch is needed.
 * The tokens are the measured results of the tokenizer (neologd, NORMAL mode) unless noted.
 */
public class DefaultContentsParserCompoundWordTest {

    private static final String FIELD = "content";

    private static final String LANG = "ja";

    private static final String PROP_ENABLED = "fess.suggest.compound.enabled";

    private static final String PROP_MAX_TOKENS = "fess.suggest.compound.max.tokens";

    private static final String PROP_MAX_LENGTH = "fess.suggest.compound.max.length";

    private static final String PROP_BREAK_CHARS = "fess.suggest.compound.break.chars";

    private static final String PROP_BREAK_TOKENS = "fess.suggest.compound.break.tokens";

    private static final String[] COMPOUND_PROPERTIES =
            { PROP_ENABLED, PROP_MAX_TOKENS, PROP_MAX_LENGTH, PROP_BREAK_CHARS, PROP_BREAK_TOKENS };

    /** The terms that {@link #compoundsOfTermsWithSuffix} makes 名詞-接尾-一般 tokens, as the tokenizer does for these suffixes. */
    private static final Set<String> SUFFIX_TERMS = Set.of("ら", "たち", "等", "者", "選", "安");

    /** The default break characters: the ASCII brackets and the full-width and Japanese brackets. */
    private static final String DEFAULT_BREAK_CHARS = "()[]{}<>（）［］｛｝＜＞〈〉《》「」『』【】〔〕";

    private final Map<String, String> originalProperties = new HashMap<>();

    private org.apache.logging.log4j.core.Logger parserLogger;

    private CapturingAppender appender;

    private Level originalLogLevel;

    @Before
    public void setUp() {
        for (final String name : COMPOUND_PROPERTIES) {
            originalProperties.put(name, System.getProperty(name));
            System.clearProperty(name);
        }
        parserLogger = fetchParserLogger();
        originalLogLevel = parserLogger.getLevel();
        parserLogger.setLevel(Level.DEBUG);
        appender = new CapturingAppender();
        appender.start();
        parserLogger.addAppender(appender);
    }

    /**
     * Returns the logger instance that the parser really uses. When a test that starts an OpenSearch node runs earlier in the same JVM,
     * the logging configuration is rebuilt, and {@code LogManager.getLogger} may no longer return the instance held by the parser.
     */
    private static org.apache.logging.log4j.core.Logger fetchParserLogger() {
        try {
            final java.lang.reflect.Field loggerField = DefaultContentsParser.class.getDeclaredField("logger");
            loggerField.setAccessible(true);
            return (org.apache.logging.log4j.core.Logger) loggerField.get(null);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to get the logger of DefaultContentsParser.", e);
        }
    }

    @After
    public void tearDown() {
        for (final String name : COMPOUND_PROPERTIES) {
            final String original = originalProperties.get(name);
            if (original == null) {
                System.clearProperty(name);
            } else {
                System.setProperty(name, original);
            }
        }
        parserLogger.removeAppender(appender);
        appender.stop();
        parserLogger.setLevel(originalLogLevel);
    }

    // ============================================================
    // 3.2 Compound words of adjacent nouns
    // ============================================================

    @Test
    public void test_compound_generalNouns() throws Exception {
        final StubAnalyzer analyzer = StubAnalyzer.fixed("新型コロナ", tok("新型", 0, 2, "名詞-一般", "シンガタ"), tok("コロナ", 2, 5, "名詞-一般", "コロナ"));

        final List<SuggestItem> items = parse(new DefaultContentsParser(), analyzer, "新型コロナ");

        // Fragments are kept as before, and the compound word follows them.
        assertEquals(List.of("新型", "コロナ", "新型コロナ"), texts(items));
    }

    @Test
    public void test_compound_suffix() throws Exception {
        final StubAnalyzer daitoryosen = StubAnalyzer.fixed("大統領選", tok("大統領", 0, 3, "名詞-一般", "ダイトウリョウ"), tok("選", 3, 4, "名詞-接尾-一般", "セン"));
        final StubAnalyzer endaka = StubAnalyzer.fixed("円安", tok("円", 0, 1, "名詞-一般", "エン"), tok("安", 1, 2, "名詞-接尾-一般", "ヤス"));

        // The suffix is dropped from the fragments by the pos filter, but it is a part of the compound word.
        assertEquals(List.of("大統領", "大統領選"), texts(parse(new DefaultContentsParser(), daitoryosen, "大統領選")));
        assertEquals(List.of("円", "円安"), texts(parse(new DefaultContentsParser(), endaka, "円安")));
    }

    @Test
    public void test_compound_personNameAndSahen() throws Exception {
        final StubAnalyzer takaichi =
                StubAnalyzer.fixed("高市早苗", tok("高市", 0, 2, "名詞-固有名詞-人名-姓", "タカイチ"), tok("早苗", 2, 4, "名詞-固有名詞-人名-名", "サナエ"));
        final StubAnalyzer ishiba = StubAnalyzer.fixed("石破首相", tok("石破", 0, 2, "名詞-固有名詞-人名-姓", "イシバ"), tok("首相", 2, 4, "名詞-一般", "シュショウ"));
        final StubAnalyzer vaccine = StubAnalyzer.fixed("ワクチン接種", tok("ワクチン", 0, 4, "名詞-一般", "ワクチン"), tok("接種", 4, 6, "名詞-サ変接続", "セッシュ"));

        assertTrue(texts(parse(new DefaultContentsParser(), takaichi, "高市早苗")).contains("高市早苗"));
        assertTrue(texts(parse(new DefaultContentsParser(), ishiba, "石破首相")).contains("石破首相"));
        assertTrue(texts(parse(new DefaultContentsParser(), vaccine, "ワクチン接種")).contains("ワクチン接種"));
    }

    @Test
    public void test_compound_regionSuffix() throws Exception {
        final StubAnalyzer analyzer =
                StubAnalyzer.fixed("神奈川県", tok("神奈川", 0, 3, "名詞-固有名詞-地域-一般", "カナガワ"), tok("県", 3, 4, "名詞-接尾-地域", "ケン"));

        assertEquals(List.of("神奈川", "神奈川県"), texts(parse(new DefaultContentsParser(), analyzer, "神奈川県")));
    }

    @Test
    public void test_compound_allTargetPartsOfSpeech() throws Exception {
        final String[] partsOfSpeech = { "名詞-一般", "名詞-サ変接続", "名詞-固有名詞-一般", "名詞-固有名詞-人名-一般", "名詞-固有名詞-人名-姓", "名詞-固有名詞-人名-名", "名詞-固有名詞-組織",
                "名詞-固有名詞-地域-一般", "名詞-固有名詞-地域-国", "名詞-接尾-一般", "名詞-接尾-サ変接続", "名詞-接尾-地域", "名詞-形容動詞語幹" };
        final DefaultContentsParser parser = new DefaultContentsParser();
        for (final String partOfSpeech : partsOfSpeech) {
            final StubAnalyzer analyzer = StubAnalyzer.fixed("あいうえ", tok("あい", 0, 2, "名詞-一般", "アイ"), tok("うえ", 2, 4, partOfSpeech, "ウエ"));
            assertEquals(partOfSpeech, List.of("あいうえ"), compoundWords(parser, analyzer, "あいうえ", "あい", "うえ"));
        }
    }

    @Test
    public void test_compound_adjectivalNounStemIsJoinedWithNouns() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        // 健康診断: 健康 is 名詞-形容動詞語幹, 診断 is 名詞-サ変接続.
        final StubAnalyzer head = StubAnalyzer.fixed("健康診断", tok("健康", 0, 2, "名詞-形容動詞語幹", "ケンコウ"), tok("診断", 2, 4, "名詞-サ変接続", "シンダン"));
        assertEquals(List.of("健康診断"), compoundWords(parser, head, "健康診断", "健康", "診断"));
        // 特別養護老人ホーム: one run of 4 tokens starting with the stem.
        final StubAnalyzer longer = StubAnalyzer.fixed("特別養護老人ホーム", tok("特別", 0, 2, "名詞-形容動詞語幹", "トクベツ"), tok("養護", 2, 4, "名詞-サ変接続", "ヨウゴ"),
                tok("老人", 4, 6, "名詞-一般", "ロウジン"), tok("ホーム", 6, 9, "名詞-一般", "ホーム"));
        assertEquals(List.of("特別養護老人ホーム"), compoundWords(parser, longer, "特別養護老人ホーム", "特別", "養護", "老人", "ホーム"));
        // 静かな町: the auxiliary verb な ends the run, so a lone stem is not a compound.
        final StubAnalyzer alone =
                StubAnalyzer.fixed("静かな町", tok("静か", 0, 2, "名詞-形容動詞語幹", "シズカ"), tok("な", 2, 3, "助動詞", "ナ"), tok("町", 3, 4, "名詞-一般", "マチ"));
        assertEquals(List.of(), compoundWords(parser, alone, "静かな町", "静か", "町"));
    }

    @Test
    public void test_compound_nonTargetPartsOfSpeechAreNotJoinedWithAdjacentGeneralNoun() throws Exception {
        final String[] nonTargets = { "名詞-数", "名詞-接尾-助数詞", "名詞-接尾-人名", "名詞-非自立", "名詞-代名詞", "名詞-副詞可能", "動詞-自立", "助詞-連体化", "形容詞-自立", "記号-一般",
                "名詞-接尾-副詞可能", "名詞-接尾-助動詞語幹", "名詞-固有名詞-地域", "名詞-ナイ形容詞語幹", "名詞-特殊-助動詞語幹", "名詞-引用文字列" };
        final DefaultContentsParser parser = new DefaultContentsParser();
        for (final String partOfSpeech : nonTargets) {
            // The non-target is adjacent to a 名詞-一般, so only the part of speech can prevent the join.
            final StubAnalyzer after = StubAnalyzer.fixed("あいうえ", tok("あい", 0, 2, "名詞-一般", "アイ"), tok("うえ", 2, 4, partOfSpeech, "ウエ"));
            final StubAnalyzer before = StubAnalyzer.fixed("うえあい", tok("うえ", 0, 2, partOfSpeech, "ウエ"), tok("あい", 2, 4, "名詞-一般", "アイ"));
            assertEquals(partOfSpeech, List.of(), compoundWords(parser, after, "あいうえ", "あい", "うえ"));
            assertEquals(partOfSpeech, List.of(), compoundWords(parser, before, "うえあい", "あい", "うえ"));
        }
        assertEquals(0, parser.getCompoundWordCount());
    }

    // ============================================================
    // Breaks of a run
    // ============================================================

    @Test
    public void test_break_whiteSpaceToken() throws Exception {
        // A white space is a token (記号-空白), which is not a target.
        final StubAnalyzer analyzer = StubAnalyzer.fixed("検索 エンジン", tok("検索", 0, 2, "名詞-サ変接続", "ケンサク"), tok(" ", 2, 3, "記号-空白", " "),
                tok("エンジン", 3, 7, "名詞-一般", "エンジン"));

        assertEquals(List.of("検索", "エンジン"), texts(parse(new DefaultContentsParser(), analyzer, "検索 エンジン")));
    }

    @Test
    public void test_break_punctuation() throws Exception {
        final StubAnalyzer analyzer =
                StubAnalyzer.fixed("ワクチン接種、新型コロナ", tok("ワクチン", 0, 4, "名詞-一般", "ワクチン"), tok("接種", 4, 6, "名詞-サ変接続", "セッシュ"),
                        tok("、", 6, 7, "記号-読点", "、"), tok("新型", 7, 9, "名詞-一般", "シンガタ"), tok("コロナ", 9, 12, "名詞-一般", "コロナ"));

        assertEquals(List.of("ワクチン接種", "新型コロナ"),
                compoundWords(new DefaultContentsParser(), analyzer, "ワクチン接種、新型コロナ", "ワクチン", "接種", "新型", "コロナ"));
    }

    @Test
    public void test_break_particle() throws Exception {
        final StubAnalyzer analyzer = StubAnalyzer.fixed("ワクチンの接種", tok("ワクチン", 0, 4, "名詞-一般", "ワクチン"), tok("の", 4, 5, "助詞-連体化", "ノ"),
                tok("接種", 5, 7, "名詞-サ変接続", "セッシュ"));

        assertEquals(List.of("ワクチン", "接種"), texts(parse(new DefaultContentsParser(), analyzer, "ワクチンの接種")));
    }

    @Test
    public void test_break_offsetsNotAdjacent() throws Exception {
        // No token between the two nouns, but the offsets are not adjacent: they are not in a run.
        final StubAnalyzer analyzer = StubAnalyzer.fixed("新型 コロナ", tok("新型", 0, 2, "名詞-一般", "シンガタ"), tok("コロナ", 3, 6, "名詞-一般", "コロナ"));

        assertEquals(List.of("新型", "コロナ"), texts(parse(new DefaultContentsParser(), analyzer, "新型 コロナ")));
    }

    // ============================================================
    // Non-targets
    // ============================================================

    @Test
    public void test_nonTarget_numberAndCounter() throws Exception {
        final StubAnalyzer manEn = StubAnalyzer.fixed("3000万円", tok("3000", 0, 4, "名詞-数", "3000"), tok("万", 4, 5, "名詞-数", "マン"),
                tok("円", 5, 6, "名詞-接尾-助数詞", "エン"));
        final StubAnalyzer reiwa = StubAnalyzer.fixed("令和5年度", tok("令和", 0, 2, "名詞-一般", "レイワ"), tok("5", 2, 3, "名詞-数", "5"),
                tok("年度", 3, 5, "名詞-接尾-助数詞", "ネンド"));

        assertEquals(List.of(), compoundWords(new DefaultContentsParser(), manEn, "3000万円", "令和"));
        assertEquals(List.of(), compoundWords(new DefaultContentsParser(), reiwa, "令和5年度", "令和"));
    }

    @Test
    public void test_nonTarget_verbAndParticle() throws Exception {
        final StubAnalyzer analyzer = StubAnalyzer.fixed("検索する", tok("検索", 0, 2, "名詞-サ変接続", "ケンサク"), tok("する", 2, 4, "動詞-自立", "スル"));
        final StubAnalyzer personSuffix =
                StubAnalyzer.fixed("高市氏", tok("高市", 0, 2, "名詞-固有名詞-人名-姓", "タカイチ"), tok("氏", 2, 3, "名詞-接尾-人名", "シ"));

        assertEquals(List.of("検索"), texts(parse(new DefaultContentsParser(), analyzer, "検索する")));
        assertEquals(List.of(), compoundWords(new DefaultContentsParser(), personSuffix, "高市氏", "高市"));
    }

    // ============================================================
    // Limits
    // ============================================================

    @Test
    public void test_limit_sixTokensGiveNoCompoundWord() throws Exception {
        final StubAnalyzer analyzer = nounRun("ホーム", "新型", "コロナ", "ワクチン", "接種", "案内");
        final DefaultContentsParser parser = new DefaultContentsParser();

        // Neither the whole run nor its useful parts (新型コロナ, ワクチン接種) are generated. Only the fragments remain.
        assertEquals(List.of("ホーム", "新型", "コロナ", "ワクチン", "接種", "案内"), texts(parse(parser, analyzer, "ホーム新型コロナワクチン接種案内")));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.OVER_LIMIT));
        assertEquals(0, parser.getCompoundWordCount());
    }

    @Test
    public void test_limit_fiveTokensIsAccepted() throws Exception {
        final StubAnalyzer analyzer = nounRun("新型", "コロナ", "ウイルス", "感染", "症");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("新型コロナウイルス感染症"), compoundWords(parser, analyzer, "新型コロナウイルス感染症", "新型", "コロナ", "ウイルス", "感染", "症"));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.OVER_LIMIT));
    }

    @Test
    public void test_limit_twentyOneCharactersGiveNoCompoundWord() throws Exception {
        final String first = "あ".repeat(11);
        final String second = "い".repeat(10);
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of(first, second), texts(parse(parser, nounRun(first, second), first + second)));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.OVER_LIMIT));
    }

    @Test
    public void test_limit_twentyCharactersIsAccepted() throws Exception {
        final String first = "あ".repeat(10);
        final String second = "い".repeat(10);

        assertEquals(List.of(first + second),
                compoundWords(new DefaultContentsParser(), nounRun(first, second), first + second, first, second));
    }

    @Test
    public void test_limit_lengthIsCheckedAfterLowerCase() throws Exception {
        // "İ".toLowerCase(Locale.ROOT) is 2 chars: the raw surface is 16 chars, but the lower-cased surface is 24 chars.
        final String first = "İ".repeat(8);
        final String second = "あ".repeat(8);
        assertTrue((first + second).length() <= 20);
        assertTrue((first + second).toLowerCase(Locale.ROOT).length() > 20);
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of(), compoundWords(parser, nounRun(first, second), first + second, first, second));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.OVER_LIMIT));
    }

    @Test
    public void test_limit_threeTokenRunKeepsOnlyTheLongestRun() throws Exception {
        // Known and intentional: the middle/tail part (コロナウイルス) and the head part (新型コロナ) are not compound words.
        final StubAnalyzer analyzer = nounRun("新型", "コロナ", "ウイルス");

        assertEquals(List.of("新型", "コロナ", "ウイルス", "新型コロナウイルス"), texts(parse(new DefaultContentsParser(), analyzer, "新型コロナウイルス")));
    }

    @Test
    public void test_limit_singleToken() throws Exception {
        final StubAnalyzer analyzer = nounRun("コロナ");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("コロナ"), texts(parse(parser, analyzer, "コロナ")));
        assertEquals(0, parser.getCompoundWordCount());
        assertEquals(1, parser.getCompoundAnalyzedChunkCount());
        for (final CompoundDegradation cause : CompoundDegradation.values()) {
            assertEquals(cause.name(), 0, parser.getDegradedChunkCount(cause));
        }
    }

    // ============================================================
    // Invalid offsets
    // ============================================================

    @Test
    public void test_invalidOffset_zeroOffsets() throws Exception {
        final StubAnalyzer analyzer = StubAnalyzer.fixed("新型コロナ", tok("新型", 0, 0, "名詞-一般", "シンガタ"), tok("コロナ", 0, 0, "名詞-一般", "コロナ"));
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("新型", "コロナ"), texts(parse(parser, analyzer, "新型コロナ")));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.INVALID_OFFSET));
        assertEquals(0, parser.getCompoundWordCount());
    }

    @Test
    public void test_invalidOffset_endNotGreaterThanStart() throws Exception {
        final StubAnalyzer analyzer = StubAnalyzer.fixed("新型コロナ", tok("新型", 0, 2, "名詞-一般", "シンガタ"), tok("コロナ", 5, 3, "名詞-一般", "コロナ"));
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("新型", "コロナ"), texts(parse(parser, analyzer, "新型コロナ")));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.INVALID_OFFSET));
    }

    @Test
    public void test_invalidOffset_negativeStart() throws Exception {
        final StubAnalyzer analyzer = StubAnalyzer.fixed("新型コロナ", tok("新型", -1, 2, "名詞-一般", "シンガタ"), tok("コロナ", 2, 5, "名詞-一般", "コロナ"));
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("新型", "コロナ"), texts(parse(parser, analyzer, "新型コロナ")));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.INVALID_OFFSET));
    }

    @Test
    public void test_invalidOffset_overlappingTokensAreNotJoined() throws Exception {
        // Overlapping tokens as produced by the search mode: 西国 overlaps 関西, so 関西 and 国際 are not joined.
        final StubAnalyzer analyzer = StubAnalyzer.fixed("関西国際", tok("関西", 0, 2, "名詞-固有名詞-地域-一般", "カンサイ"), tok("西国", 1, 3, "名詞-一般", "サイゴク"),
                tok("国際", 3, 5, "名詞-一般", "コクサイ"));
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("関西", "西国", "国際"), texts(parse(parser, analyzer, "関西国際")));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.INVALID_OFFSET));
        assertEquals(0, parser.getCompoundWordCount());
    }

    // ============================================================
    // Check with the char-filtered text
    // ============================================================

    @Test
    public void test_textMismatch_surfaceNotInText() throws Exception {
        final StubAnalyzer analyzer = new StubAnalyzer(
                chunk -> new TokenizerStageResult(List.of(tok("新型", 0, 2, "名詞-一般", "シンガタ"), tok("コロナ", 2, 5, "名詞-一般", "コロナ")), "全く別の本文"),
                chunk -> List.of(tok("新型", 0, 2, "名詞-一般", "シンガタ"), tok("コロナ", 2, 5, "名詞-一般", "コロナ")));
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("新型", "コロナ"), texts(parse(parser, analyzer, "新型コロナ")));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.TEXT_MISMATCH));
    }

    @Test
    public void test_textMismatch_emptyText() throws Exception {
        final List<AnalyzeToken> tokens = List.of(tok("新型", 0, 2, "名詞-一般", "シンガタ"), tok("コロナ", 2, 5, "名詞-一般", "コロナ"));
        final StubAnalyzer analyzer = new StubAnalyzer(chunk -> new TokenizerStageResult(tokens, ""), chunk -> tokens);
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("新型", "コロナ"), texts(parse(parser, analyzer, "新型コロナ")));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.TEXT_MISMATCH));
    }

    @Test
    public void test_textMismatch_sameSurfaceAtAnotherPositionIsNotEnough() throws Exception {
        // The tokens are 新型 [0,2) and コロナ [3,6): not adjacent. The text has 新型コロナ later, but contains() alone must not decide.
        final List<AnalyzeToken> tokens = List.of(tok("新型", 0, 2, "名詞-一般", "シンガタ"), tok("コロナ", 3, 6, "名詞-一般", "コロナ"));
        final StubAnalyzer analyzer = new StubAnalyzer(chunk -> new TokenizerStageResult(tokens, "新型 コロナ。新型コロナ"), chunk -> tokens);
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("新型", "コロナ"), texts(parse(parser, analyzer, "新型 コロナ。新型コロナ")));
        assertEquals(0, parser.getCompoundWordCount());
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.TEXT_MISMATCH));
    }

    @Test
    public void test_textMismatch_limitOfContainsCheck() throws Exception {
        // contains() is a necessary condition only: adjacent tokens whose surface exists elsewhere in the text pass it.
        // This pins the current behavior (the offsets decide the run, the text only rejects the evident inconsistency).
        final List<AnalyzeToken> tokens = List.of(tok("新型", 0, 2, "名詞-一般", "シンガタ"), tok("コロナ", 2, 5, "名詞-一般", "コロナ"));
        final StubAnalyzer analyzer = new StubAnalyzer(chunk -> new TokenizerStageResult(tokens, "ＸＹＺ 新型コロナ"), chunk -> tokens);

        assertEquals(List.of("新型", "コロナ", "新型コロナ"), texts(parse(new DefaultContentsParser(), analyzer, "ＸＹ新型コロナ")));
    }

    // ============================================================
    // Normalization by the char filters
    // ============================================================

    @Test
    public void test_normalization_fullWidthAlphabet() throws Exception {
        // ＡＩ活用 -> AI活用 (the offsets are of the original text)
        final String filtered = "AI活用";
        final List<AnalyzeToken> tokens = List.of(tok("AI", 0, 2, "名詞-一般", "エーアイ"), tok("活用", 2, 4, "名詞-サ変接続", "カツヨウ"));
        final StubAnalyzer analyzer = new StubAnalyzer(chunk -> new TokenizerStageResult(tokens, filtered), chunk -> tokens);

        final List<SuggestItem> items = parse(new DefaultContentsParser(), analyzer, "ＡＩ活用");

        assertEquals(List.of("AI", "活用", "ai活用"), texts(items));
        assertTrue(filtered.toLowerCase(Locale.ROOT).contains(items.get(2).getText()));
    }

    @Test
    public void test_normalization_halfWidthKatakanaWithOffsetsOfOriginalText() throws Exception {
        // ｶﾞｲﾄﾞﾌﾞｯｸ新型コロナ (14 chars) -> ガイドブック新型コロナ (11 chars). Applying the offsets to the filtered text would be wrong.
        final String filtered = "ガイドブック新型コロナ";
        final List<AnalyzeToken> tokens =
                List.of(tok("ガイドブック", 0, 9, "名詞-一般", "ガイドブック"), tok("新型", 9, 11, "名詞-一般", "シンガタ"), tok("コロナ", 11, 14, "名詞-一般", "コロナ"));
        final StubAnalyzer analyzer = new StubAnalyzer(chunk -> new TokenizerStageResult(tokens, filtered), chunk -> tokens);
        final DefaultContentsParser parser = new DefaultContentsParser();

        final List<SuggestItem> items = parse(parser, analyzer, "ｶﾞｲﾄﾞﾌﾞｯｸ新型コロナ");

        assertEquals(List.of("ガイドブック", "新型", "コロナ", "ガイドブック新型コロナ"), texts(items));
        assertTrue(filtered.contains(items.get(3).getText()));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.TEXT_MISMATCH));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.INVALID_OFFSET));
    }

    // ============================================================
    // Degradation
    // ============================================================

    @Test
    public void test_degradation_stageUnavailable() throws Exception {
        final StubAnalyzer analyzer = new StubAnalyzer(chunk -> null, chunk -> List.of(tok("新型", 0, 2, "名詞-一般", "シンガタ")));
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("新型"), texts(parse(parser, analyzer, "新型コロナ")));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.STAGE_UNAVAILABLE));
        assertEquals(1, parser.getCompoundAnalyzedChunkCount());
        assertEquals(0, parser.getCompoundChunkCount());
        assertWarnContains("STAGE_UNAVAILABLE");
    }

    @Test
    public void test_degradation_defaultAnalyzerImplementationReturnsNull() throws Exception {
        // An analyzer that does not override analyzeTokenizerStage (the default method returns null).
        final SuggestAnalyzer analyzer = new SuggestAnalyzer() {
            @Override
            public List<AnalyzeToken> analyze(final String text, final String field, final String lang) {
                return List.of(tok("新型", 0, 2, "名詞-一般", "シンガタ"));
            }

            @Override
            public List<AnalyzeToken> analyzeAndReading(final String text, final String field, final String lang) {
                return List.of(tok("シンガタ", 0, 2, "名詞-一般", "シンガタ"));
            }
        };
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("新型"), texts(parse(parser, analyzer, "新型コロナ")));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.STAGE_UNAVAILABLE));
    }

    @Test
    public void test_degradation_noPartOfSpeechIsCountedAndSummarizedAsInfoWithoutWarning() throws Exception {
        // The analyzer returns a result without tokens when the tokenizer provides no part of speech (e.g. an English analyzer).
        final StubAnalyzer analyzer = new StubAnalyzer(chunk -> new TokenizerStageResult(List.of(), "hello world"),
                chunk -> List.of(tok("hello", 0, 5, "名詞-一般", "hello")));
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("hello"), texts(parse(parser, analyzer, "hello world")));

        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.NO_PART_OF_SPEECH));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.STAGE_UNAVAILABLE));
        assertEquals(1, parser.getCompoundAnalyzedChunkCount());
        assertEquals(0, parser.getCompoundWordCount());
        assertEquals(0, appender.count(Level.WARN));
        assertEquals(1, appender.count(Level.INFO));
        final String info = appender.messages(Level.INFO).get(0);
        assertTrue(info, info.contains("field=" + FIELD));
        assertTrue(info, info.contains("lang=" + LANG));
        assertTrue(info, info.contains("noPartOfSpeechChunks=1"));
        assertTrue(info, info.contains("analyzedChunks=1"));
        assertTrue(info, info.contains("compoundWords=0"));
        assertTrue(info, info.contains("Kuromoji"));
        // The text is not logged.
        assertFalse(info, info.contains("hello world"));
    }

    @Test
    public void test_degradation_noPartOfSpeechInfoIsLimitedByTime() throws Exception {
        final StubAnalyzer analyzer = new StubAnalyzer(chunk -> new TokenizerStageResult(List.of(), "hello world"), chunk -> List.of());
        final DefaultContentsParser parser = new DefaultContentsParser();

        parse(parser, analyzer, "hello world");
        parse(parser, analyzer, "hello world");
        parse(parser, analyzer, "hello world");

        assertEquals(3, parser.getDegradedChunkCount(CompoundDegradation.NO_PART_OF_SPEECH));
        assertEquals(1, appender.count(Level.INFO));
        assertEquals(0, appender.count(Level.WARN));
        // The suppressed summaries are logged at debug level, with the counts at that time.
        assertTrue(appender.contains(Level.DEBUG, "noPartOfSpeechChunks=2"));
        assertTrue(appender.contains(Level.DEBUG, "noPartOfSpeechChunks=3"));
    }

    @Test
    public void test_degradation_tokensWithoutPartOfSpeechMakeNoRun() throws Exception {
        // Not expected from the default analyzer, but tokens without partOfSpeech must not make a run.
        final StubAnalyzer analyzer =
                StubAnalyzer.fixed("新型コロナ", tokenWithoutPartOfSpeech("新型", 0, 2), tokenWithoutPartOfSpeech("コロナ", 2, 5));
        final DefaultContentsParser parser = new DefaultContentsParser();

        final List<SuggestItem> items = parse(parser, analyzer, "新型コロナ");

        assertFalse(texts(items).contains("新型コロナ"));
        assertEquals(0, parser.getCompoundWordCount());
        // The tokens are valid and simply make no run, so no degradation is counted.
        for (final CompoundDegradation cause : CompoundDegradation.values()) {
            assertEquals(cause.name(), 0, parser.getDegradedChunkCount(cause));
        }
    }

    @Test
    public void test_degradation_disabled() throws Exception {
        System.setProperty(PROP_ENABLED, "false");
        final StubAnalyzer analyzer = StubAnalyzer.fixed("新型コロナ", tok("新型", 0, 2, "名詞-一般", "シンガタ"), tok("コロナ", 2, 5, "名詞-一般", "コロナ"));
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("新型", "コロナ"), texts(parse(parser, analyzer, "新型コロナ")));
        // No additional analysis request is made.
        assertEquals(List.of(), analyzer.stageChunks);
        assertEquals(0, parser.getCompoundAnalyzedChunkCount());
    }

    @Test
    public void test_degradation_overLimitIsCountedWithoutWarning() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        parse(parser, nounRun("あ", "い", "う", "え", "お", "か"), "あいうえおか");

        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.OVER_LIMIT));
        assertEquals(0, appender.count(Level.WARN));
        assertTrue(appender.contains(Level.DEBUG, "OVER_LIMIT"));
    }

    @Test
    public void test_degradation_invalidOffsetAndTextMismatchWarnWithCause() throws Exception {
        final StubAnalyzer invalidOffset = StubAnalyzer.fixed("新型コロナ", tok("新型", 0, 0, "名詞-一般", "シンガタ"), tok("コロナ", 0, 0, "名詞-一般", "コロナ"));
        final List<AnalyzeToken> tokens = List.of(tok("新型", 0, 2, "名詞-一般", "シンガタ"), tok("コロナ", 2, 5, "名詞-一般", "コロナ"));
        final StubAnalyzer textMismatch = new StubAnalyzer(chunk -> new TokenizerStageResult(tokens, "別の本文"), chunk -> tokens);

        parse(new DefaultContentsParser(), invalidOffset, "新型コロナ");
        assertWarnContains("INVALID_OFFSET");
        appender.clear();
        parse(new DefaultContentsParser(), textMismatch, "新型コロナ");
        assertWarnContains("TEXT_MISMATCH");
    }

    @Test
    public void test_degradation_exceptionDoesNotBreakParseDocument() throws Exception {
        final List<AnalyzeToken> tokens = List.of(tok("新型", 0, 2, "名詞-一般", "シンガタ"), tok("コロナ", 2, 5, "名詞-一般", "コロナ"));
        final StubAnalyzer analyzer = new StubAnalyzer(chunk -> {
            throw new IllegalStateException("explain failed");
        }, chunk -> tokens);
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("新型", "コロナ"), texts(parse(parser, analyzer, "新型コロナ")));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.FAILURE));
        assertWarnContains("explain failed");
        // The stack trace is not in the warning but at debug level.
        assertTrue(appender.events().stream().filter(e -> e.getLevel() == Level.WARN).allMatch(e -> e.getThrown() == null));
        assertTrue(appender.events()
                .stream()
                .filter(e -> e.getLevel() == Level.DEBUG)
                .anyMatch(e -> e.getThrown() instanceof IllegalStateException && e.getThrown().getMessage().equals("explain failed")));
    }

    @Test
    public void test_degradation_failureWhileCreatingItemsDiscardsTheChunk() throws Exception {
        final StubAnalyzer analyzer = StubAnalyzer.fixed("新型コロナ", tok("新型", 0, 2, "名詞-一般", "シンガタ"), tok("コロナ", 2, 5, "名詞-一般", "コロナ"));
        final ReadingConverter failing = new ReadingConverter() {
            @Override
            public void init() {
            }

            @Override
            public List<String> convert(final String text, final String field, final String... lang) throws IOException {
                if (text.equals("シンガタコロナ")) {
                    throw new IOException("convert failed");
                }
                return new ArrayList<>(List.of(text));
            }
        };
        final DefaultContentsParser parser = new DefaultContentsParser();

        final List<SuggestItem> items = parser.parseDocument(document("新型コロナ"), new String[] { FIELD }, new String[0], "role", "lang",
                failing, failing, new NormalizerChain(), analyzer);

        assertEquals(List.of("新型", "コロナ"), texts(items));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.FAILURE));
        assertEquals(0, parser.getCompoundWordCount());
    }

    @Test
    public void test_degradation_warningsAreLimitedByTime() throws Exception {
        final StubAnalyzer analyzer = new StubAnalyzer(chunk -> null, chunk -> List.of());
        final DefaultContentsParser parser = new DefaultContentsParser();

        parse(parser, analyzer, "検索エンジン");
        parse(parser, analyzer, "検索エンジン");
        parse(parser, analyzer, "検索エンジン");

        assertEquals(1, appender.count(Level.WARN));
        assertEquals(3, parser.getDegradedChunkCount(CompoundDegradation.STAGE_UNAVAILABLE));
    }

    @Test
    public void test_degradation_warningIsAggregatedPerDocumentField() throws Exception {
        // A text without white spaces is one chunk. Several chunks of a field make a single warning.
        final StubAnalyzer analyzer = new StubAnalyzer(chunk -> null, chunk -> List.of());
        final DefaultContentsParser parser = new DefaultContentsParser();

        parse(parser, analyzer, ("検索エンジン ").repeat(400));

        final long degradedChunks = parser.getDegradedChunkCount(CompoundDegradation.STAGE_UNAVAILABLE);
        assertTrue(degradedChunks >= 2);
        assertEquals(1, appender.count(Level.WARN));
        // The single warning reports the number of the chunks of this field, not only the first one.
        final Matcher matcher =
                Pattern.compile("degradedChunks=\\{STAGE_UNAVAILABLE=(\\d+)\\}").matcher(appender.messages(Level.WARN).get(0));
        assertTrue(appender.messages(Level.WARN).get(0), matcher.find());
        assertEquals(degradedChunks, Long.parseLong(matcher.group(1)));
    }

    @Test
    public void test_degradation_failureWarningHasItsOwnTimeLimit() throws Exception {
        // A frequent cause (STAGE_UNAVAILABLE) must not hide the first FAILURE, because they have separate warning limits.
        final List<AnalyzeToken> tokens = List.of(tok("新型", 0, 2, "名詞-一般", "シンガタ"));
        final AtomicInteger calls = new AtomicInteger();
        final StubAnalyzer analyzer = new StubAnalyzer(chunk -> {
            if (calls.incrementAndGet() == 1) {
                return null;
            }
            throw new IllegalStateException("explain failed " + calls.get());
        }, chunk -> tokens);
        final DefaultContentsParser parser = new DefaultContentsParser();

        parse(parser, analyzer, "新型コロナ");
        assertEquals(1, appender.count(Level.WARN));
        parse(parser, analyzer, "新型コロナ");
        assertEquals(2, appender.count(Level.WARN));
        assertWarnContains("explain failed 2");
        // The second FAILURE within the interval is suppressed (logged at debug level).
        parse(parser, analyzer, "新型コロナ");
        assertEquals(2, appender.count(Level.WARN));
        assertTrue(appender.contains(Level.DEBUG, "explain failed 3"));
        assertTrue(appender.events()
                .stream()
                .anyMatch(e -> e.getLevel() == Level.DEBUG && e.getThrown() != null
                        && e.getThrown().getMessage().equals("explain failed 3")));
        assertEquals(2, parser.getDegradedChunkCount(CompoundDegradation.FAILURE));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.STAGE_UNAVAILABLE));
    }

    @Test
    public void test_counters_generatedChunksAndWords() throws Exception {
        final StubAnalyzer analyzer = StubAnalyzer.fixed("新型コロナ、円安", tok("新型", 0, 2, "名詞-一般", "シンガタ"), tok("コロナ", 2, 5, "名詞-一般", "コロナ"),
                tok("、", 5, 6, "記号-読点", "、"), tok("円", 6, 7, "名詞-一般", "エン"), tok("安", 7, 8, "名詞-接尾-一般", "ヤス"));
        final DefaultContentsParser parser = new DefaultContentsParser();

        final List<SuggestItem> items = parse(parser, analyzer, "新型コロナ、円安");

        assertEquals(List.of("新型", "コロナ", "円", "新型コロナ", "円安"), texts(items));
        assertEquals(1, parser.getCompoundChunkCount());
        assertEquals(2, parser.getCompoundWordCount());
    }

    @Test
    public void test_counters_areThreadSafe() throws Exception {
        final StubAnalyzer analyzer = StubAnalyzer.fixed("新型コロナ", tok("新型", 0, 2, "名詞-一般", "シンガタ"), tok("コロナ", 2, 5, "名詞-一般", "コロナ"));
        final DefaultContentsParser parser = new DefaultContentsParser();
        final int numThreads = 4;
        final int numDocuments = 50;

        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        try {
            final List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < numThreads; i++) {
                futures.add(executor.submit(() -> {
                    for (int j = 0; j < numDocuments; j++) {
                        parse(parser, analyzer, "新型コロナ");
                    }
                    return null;
                }));
            }
            for (final Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(numThreads * numDocuments, parser.getCompoundAnalyzedChunkCount());
        assertEquals(numThreads * numDocuments, parser.getCompoundChunkCount());
        assertEquals(numThreads * numDocuments, parser.getCompoundWordCount());
    }

    // ============================================================
    // Items of compound words
    // ============================================================

    @Test
    public void test_item_readingsAndSurface() throws Exception {
        final StubAnalyzer analyzer = StubAnalyzer.fixed("新型コロナ", tok("新型", 0, 2, "名詞-一般", "シンガタ"), tok("コロナ", 2, 5, "名詞-一般", "コロナ"));
        final RecordingReadingConverter readingConverter = new RecordingReadingConverter("R:");
        final RecordingReadingConverter contentsReadingConverter = new RecordingReadingConverter("C:");

        final List<SuggestItem> items = new DefaultContentsParser().parseDocument(document("新型コロナ"), new String[] { FIELD },
                new String[] { "label" }, "role", "lang", readingConverter, contentsReadingConverter, new NormalizerChain(), analyzer);

        final SuggestItem compound = items.get(items.size() - 1);
        assertEquals("新型コロナ", compound.getText());
        // The katakana readings of the tokens are joined and converted once. The surface is appended.
        assertTrue(contentsReadingConverter.inputs.contains("シンガタコロナ"));
        assertEquals(List.of("C:シンガタコロナ", "新型コロナ"), Arrays.asList(compound.getReadings()[0]));
        assertTrue(readingConverter.inputs.isEmpty());
        assertEquals(1, compound.getDocFreq());
        assertEquals(0, compound.getQueryFreq());
        assertEquals(SuggestItem.Kind.DOCUMENT, compound.getKinds()[0]);
        assertEquals(List.of(FIELD), Arrays.asList(compound.getFields()));
        assertEquals(List.of(LANG), Arrays.asList(compound.getLanguages()));
    }

    @Test
    public void test_item_sameTagsRolesAsFragments() throws Exception {
        final StubAnalyzer analyzer = StubAnalyzer.fixed("新型コロナ", tok("新型", 0, 2, "名詞-一般", "シンガタ"), tok("コロナ", 2, 5, "名詞-一般", "コロナ"));
        final Map<String, Object> document = document("新型コロナ");
        document.put("label", "tag1");
        document.put("role", "role1");

        final List<SuggestItem> items = new DefaultContentsParser().parseDocument(document, new String[] { FIELD },
                new String[] { "label" }, "role", "lang", readingConverter(), readingConverter(), new NormalizerChain(), analyzer);

        assertEquals(3, items.size());
        for (final SuggestItem item : items) {
            assertEquals(List.of("tag1"), Arrays.asList(item.getTags()));
            assertEquals(List.of("role1"), Arrays.asList(item.getRoles()));
        }
    }

    @Test
    public void test_item_realConverterKeepsSurfaceAtTheEnd() throws Exception {
        final StubAnalyzer analyzer = StubAnalyzer.fixed("新型コロナ", tok("新型", 0, 2, "名詞-一般", "シンガタ"), tok("コロナ", 2, 5, "名詞-一般", "コロナ"));

        final List<SuggestItem> items = parse(new DefaultContentsParser(), analyzer, "新型コロナ");

        // The joined reading シンガタコロナ is converted by KatakanaToAlphabetConverter, and the surface is kept at the end.
        assertEquals(List.of("シンガタコロナ", "sinngatakorona", "shinngatakorona", "新型コロナ"), Arrays.asList(items.get(2).getReadings()[0]));
    }

    @Test
    public void test_item_tokenWithoutReadingUsesLowerCasedSurface() throws Exception {
        final StubAnalyzer analyzer =
                StubAnalyzer.fixed("ChatGPT活用", tokenWithoutReading("ChatGPT", 0, 7, "名詞-一般"), tok("活用", 7, 9, "名詞-サ変接続", "カツヨウ"));
        final RecordingReadingConverter readingConverter = new RecordingReadingConverter("R:");
        final RecordingReadingConverter contentsReadingConverter = new RecordingReadingConverter("C:");

        final List<SuggestItem> items = new DefaultContentsParser().parseDocument(document("ChatGPT活用"), new String[] { FIELD },
                new String[0], "role", "lang", readingConverter, contentsReadingConverter, new NormalizerChain(), analyzer);

        final SuggestItem compound = items.get(items.size() - 1);
        assertEquals("chatgpt活用", compound.getText());
        assertTrue(contentsReadingConverter.inputs.contains("chatgptカツヨウ"));
        assertEquals(List.of("C:chatgptカツヨウ", "chatgpt活用"), Arrays.asList(compound.getReadings()[0]));
    }

    @Test
    public void test_item_noReadingAtAllUsesReadingConverterOfTheSurface() throws Exception {
        final StubAnalyzer analyzer = StubAnalyzer.fixed("ChatGPTiPhone", tokenWithoutReading("ChatGPT", 0, 7, "名詞-一般"),
                tokenWithoutReading("iPhone", 7, 13, "名詞-一般"));
        final RecordingReadingConverter readingConverter = new RecordingReadingConverter("R:");
        final RecordingReadingConverter contentsReadingConverter = new RecordingReadingConverter("C:");

        final List<SuggestItem> items = new DefaultContentsParser().parseDocument(document("ChatGPTiPhone"), new String[] { FIELD },
                new String[0], "role", "lang", readingConverter, contentsReadingConverter, new NormalizerChain(), analyzer);

        final SuggestItem compound = items.get(items.size() - 1);
        assertEquals("chatgptiphone", compound.getText());
        assertEquals(List.of("R:chatgptiphone", "chatgptiphone"), Arrays.asList(compound.getReadings()[0]));
    }

    @Test
    public void test_item_surfaceIsLowerCasedAndIdMatches() throws Exception {
        final StubAnalyzer analyzer = StubAnalyzer.fixed("AIモデル", tok("AI", 0, 2, "名詞-一般", "エーアイ"), tok("モデル", 2, 5, "名詞-一般", "モデル"));

        final List<SuggestItem> items = parse(new DefaultContentsParser(), analyzer, "AIモデル");

        final SuggestItem compound = items.get(items.size() - 1);
        assertEquals("aiモデル", compound.getText());
        // The same word from the search log has the same ID, so they are merged when written.
        final SuggestItem fromSearchLog = new SuggestItem(new String[] { "aiモデル" }, new String[][] { { "aimoderu" } },
                new String[] { FIELD }, 0, 1, -1, null, null, null, SuggestItem.Kind.QUERY);
        assertEquals(fromSearchLog.getId(), compound.getId());
        assertEquals(SuggestUtil.createSuggestTextId("aiモデル"), compound.getId());
    }

    // ============================================================
    // Chunks
    // ============================================================

    @Test
    public void test_chunk_compoundWordsDoNotCrossChunks() throws Exception {
        // 新型 コロナ pairs separated by white spaces; each chunk (longer than 1000 chars) is analyzed separately.
        final StubAnalyzer analyzer = StubAnalyzer.spaceSeparated();
        final String text = ("新型コロナ ").repeat(400) + "新型";
        final DefaultContentsParser parser = new DefaultContentsParser();

        final List<SuggestItem> items = parse(parser, analyzer, text);

        assertTrue(analyzer.stageChunks.size() >= 2);
        // The same chunks are used for the tokens, the readings and the compound words.
        assertEquals(analyzer.analyzeChunks, analyzer.stageChunks);
        assertEquals(analyzer.analyzeAndReadingChunks, analyzer.stageChunks);
        for (final String chunk : analyzer.stageChunks) {
            assertTrue(chunk.length() <= 1000 + "新型コロナ ".length());
        }
        final List<String> compoundWords =
                items.stream().map(SuggestItem::getText).filter(t -> t.equals("新型コロナ")).collect(Collectors.toList());
        assertEquals(400, compoundWords.size());
        assertFalse(texts(items).contains("コロナ新型"));
        assertEquals(analyzer.stageChunks.size(), parser.getCompoundAnalyzedChunkCount());
    }

    @Test
    public void test_chunk_textWithoutWhiteSpaceIsOneChunk() throws Exception {
        final StubAnalyzer analyzer = StubAnalyzer.fixed("x", tok("検索", 0, 2, "名詞-サ変接続", "ケンサク"));
        final String text = "検索".repeat(1500);
        final DefaultContentsParser parser = new DefaultContentsParser();

        parse(parser, analyzer, text);

        assertEquals(List.of(text), analyzer.stageChunks);
        assertEquals(List.of(text), analyzer.analyzeChunks);
        assertEquals(1, parser.getCompoundAnalyzedChunkCount());
    }

    @Test
    public void test_chunk_blankTextIsNotAnalyzed() throws Exception {
        final StubAnalyzer analyzer = StubAnalyzer.spaceSeparated();

        parse(new DefaultContentsParser(), analyzer, "");

        assertEquals(List.of(), analyzer.stageChunks);
    }

    // ============================================================
    // Symbol tokens at both ends of a run
    // ============================================================

    @Test
    public void test_symbol_insideSymbolsAreKept() throws Exception {
        // The tokenizer gives the ASCII symbols the part of speech 名詞-サ変接続, so they are in the run.
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("togetter.com"), compoundsOfTerms(parser, "togetter", ".", "com"));
        assertEquals(List.of("www.example.com"), compoundsOfTerms(parser, "www", ".", "example", ".", "com"));
        assertEquals(List.of("aws-emulator"), compoundsOfTerms(parser, "aws", "-", "emulator"));
        assertEquals(3, parser.getCompoundWordCount());
    }

    @Test
    public void test_symbol_trailingSymbolIsRemoved() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("新型コロナ"), compoundsOfTerms(parser, "新型", "コロナ", "-"));
        assertEquals(List.of("dojo.example"), compoundsOfTerms(parser, "dojo", ".", "example", "."));
    }

    @Test
    public void test_symbol_runOfOneTokenAfterTrimmingGivesNoCompoundWord() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of(), compoundsOfTerms(parser, "place", "-"));
        assertEquals(List.of(), compoundsOfTerms(parser, "ゴジラ", "-"));
        assertEquals(List.of(), compoundsOfTerms(parser, "dojo", "."));
        assertEquals(List.of(), compoundsOfTerms(parser, ".", "jp"));
        assertEquals(List.of(), compoundsOfTerms(parser, "inc", ".", ")"));
        // It is the same as a run of one token: nothing is counted as a degradation.
        assertEquals(0, parser.getCompoundWordCount());
        for (final CompoundDegradation cause : CompoundDegradation.values()) {
            assertEquals(cause.name(), 0, parser.getDegradedChunkCount(cause));
        }
    }

    @Test
    public void test_symbol_leadingSymbolIsRemoved() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("twit_shirokuma"), compoundsOfTerms(parser, "(@", "twit", "_", "shirokuma", ")"));
        assertEquals(List.of("新型コロナ"), compoundsOfTerms(parser, ">>", "新型", "コロナ"));
    }

    @Test
    public void test_symbol_tokenWithLetterAndSymbolIsNotRemoved() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        // A token is a symbol only if it has no letter or digit: @twit has a letter, so it stays at the end of the run.
        assertEquals(List.of("@twit_shirokuma"), compoundsOfTerms(parser, "@twit", "_", "shirokuma"));
        assertEquals(List.of("shirokuma_twit@"), compoundsOfTerms(parser, "shirokuma", "_", "twit@"));
    }

    @Test
    public void test_symbol_consecutiveRunsWithSymbolsAreTrimmedIndependently() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        final String text = "(新型コロナ)、(円安)";

        final StubAnalyzer analyzer =
                onlyCompoundWords(text, tok("(", 0, 1, "名詞-サ変接続", "("), tok("新型", 1, 3, "名詞-一般", "シンガタ"), tok("コロナ", 3, 6, "名詞-一般", "コロナ"),
                        tok(")", 6, 7, "名詞-サ変接続", ")"), tok("、", 7, 8, "記号-読点", "、"), tok("(", 8, 9, "名詞-サ変接続", "("),
                        tok("円", 9, 10, "名詞-一般", "エン"), tok("安", 10, 11, "名詞-一般", "ヤス"), tok(")", 11, 12, "名詞-サ変接続", ")"));
        assertEquals(List.of("新型コロナ", "円安"), texts(parse(parser, analyzer, text)));
    }

    @Test
    public void test_symbol_manySymbolsAtBothEndsAreRemoved() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("購入手続"), compoundsOfTerms(parser, ">", ">", ">>", "購入", "手続", ".", ")", "..."));
        // A symbol in the middle stays even if symbols are removed at the ends.
        assertEquals(List.of("a-b"), compoundsOfTerms(parser, "(", "(", "a", "-", "b", ")", ")"));
    }

    @Test
    public void test_symbol_runOfOnlySymbolsGivesNoCompoundWord() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of(), compoundsOfTerms(parser, ".", "-", "_"));
        assertEquals(List.of(), compoundsOfTerms(parser, "("));
        assertEquals(0, parser.getCompoundWordCount());
        for (final CompoundDegradation cause : CompoundDegradation.values()) {
            assertEquals(cause.name(), 0, parser.getDegradedChunkCount(cause));
        }
    }

    @Test
    public void test_symbol_maxTokensIsCheckedAfterTrimming() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        // 7 tokens with the symbols, but 5 tokens (the maximum) without them.
        assertEquals(List.of("あいうえお"), compoundsOfTerms(parser, "(", "あ", "い", "う", "え", "お", ")"));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.OVER_LIMIT));

        // 6 tokens even without the symbols.
        assertEquals(List.of(), compoundsOfTerms(parser, "(", "あ", "い", "う", "え", "お", "か", ")"));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.OVER_LIMIT));
    }

    @Test
    public void test_symbol_maxLengthIsCheckedAfterTrimming() throws Exception {
        System.setProperty(PROP_MAX_LENGTH, "6");
        final DefaultContentsParser parser = new DefaultContentsParser();

        // The surface is 8 characters with the symbols, and 6 characters without them.
        assertEquals(List.of("あいうえおか"), compoundsOfTerms(parser, "(", "あいう", "えおか", ")"));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.OVER_LIMIT));

        assertEquals(List.of(), compoundsOfTerms(parser, "(", "あいう", "えおかき", ")"));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.OVER_LIMIT));
    }

    @Test
    public void test_symbol_readingIsJoinedFromTheTrimmedTokensOnly() throws Exception {
        final StubAnalyzer analyzer = onlyCompoundWords("(新型コロナ)", tok("(", 0, 1, "名詞-サ変接続", "("), tok("新型", 1, 3, "名詞-一般", "シンガタ"),
                tok("コロナ", 3, 6, "名詞-一般", "コロナ"), tok(")", 6, 7, "名詞-サ変接続", ")"));
        final RecordingReadingConverter readingConverter = new RecordingReadingConverter("R:");
        final RecordingReadingConverter contentsReadingConverter = new RecordingReadingConverter("C:");

        final List<SuggestItem> items = new DefaultContentsParser().parseDocument(document("(新型コロナ)"), new String[] { FIELD },
                new String[0], "role", "lang", readingConverter, contentsReadingConverter, new NormalizerChain(), analyzer);

        assertEquals(List.of("新型コロナ"), texts(items));
        assertEquals(List.of("シンガタコロナ"), contentsReadingConverter.inputs);
        assertEquals(List.of("C:シンガタコロナ", "新型コロナ"), Arrays.asList(items.get(0).getReadings()[0]));
    }

    @Test
    public void test_symbol_textCheckUsesTheTrimmedSurface() throws Exception {
        // The text does not have the parentheses (e.g. removed by a char filter), but the trimmed surface is in the text.
        final DefaultContentsParser parser = new DefaultContentsParser();
        final StubAnalyzer analyzer = onlyCompoundWords("新型コロナ", tok("(", 0, 1, "名詞-サ変接続", "("), tok("新型", 1, 3, "名詞-一般", "シンガタ"),
                tok("コロナ", 3, 6, "名詞-一般", "コロナ"), tok(")", 6, 7, "名詞-サ変接続", ")"));

        assertEquals(List.of("新型コロナ"), texts(parse(parser, analyzer, "(新型コロナ)")));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.TEXT_MISMATCH));
    }

    @Test
    public void test_symbol_textCheckStillRejectsAMissingInsideSymbol() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        final StubAnalyzer analyzer = onlyCompoundWords("togetter com", tok("togetter", 0, 8, "名詞-固有名詞-組織", "togetter"),
                tok(".", 8, 9, "名詞-サ変接続", "."), tok("com", 9, 12, "名詞-固有名詞-組織", "com"));

        assertEquals(List.of(), texts(parse(parser, analyzer, "togetter.com")));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.TEXT_MISMATCH));
    }

    @Test
    public void test_symbol_longVowelMarkIsALetter() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        // ー (U+30FC) is a letter, so it is not removed.
        assertEquals(List.of("ーquiet"), compoundsOfTerms(parser, "ー", "quiet"));
        assertEquals(List.of("quietー"), compoundsOfTerms(parser, "quiet", "ー"));
    }

    @Test
    public void test_symbol_fullWidthSymbols() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        // ・ (U+30FB) has no letter or digit: removed at the ends, kept inside.
        assertEquals(List.of("新型コロナ"), compoundsOfTerms(parser, "・", "新型", "コロナ", "・"));
        assertEquals(List.of("ゴジラ・キング"), compoundsOfTerms(parser, "ゴジラ", "・", "キング"));
        // 、 (記号-読点) is not a target, so it breaks the run as before.
        final StubAnalyzer analyzer = onlyCompoundWords("新型コロナ、円安", tok("新型", 0, 2, "名詞-一般", "シンガタ"), tok("コロナ", 2, 5, "名詞-一般", "コロナ"),
                tok("、", 5, 6, "記号-読点", "、"), tok("円", 6, 7, "名詞-一般", "エン"), tok("安", 7, 8, "名詞-一般", "ヤス"));
        assertEquals(List.of("新型コロナ", "円安"), texts(parse(parser, analyzer, "新型コロナ、円安")));
    }

    @Test
    public void test_symbol_judgedByCodePointsNotChars() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        // 𠮷 (U+20BB7) is a letter made of a surrogate pair: it is not a symbol, although each surrogate char is not a letter.
        assertEquals(2, Character.charCount(0x20BB7));
        assertEquals(List.of("𠮷野家"), compoundsOfTerms(parser, "𠮷", "野家"));
        // 😀 (U+1F600) is a symbol made of a surrogate pair: it is removed at the ends.
        assertEquals(List.of("新型コロナ"), compoundsOfTerms(parser, "😀", "新型", "コロナ", "😀"));
        assertEquals(List.of("a😀b"), compoundsOfTerms(parser, "a", "😀", "b"));
    }

    @Test
    public void test_symbol_digitIsNotASymbol() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("2024-新型"), compoundsOfTerms(parser, "2024", "-", "新型"));
        assertEquals(List.of("新型-2024"), compoundsOfTerms(parser, "新型", "-", "2024"));
        // A token of digits stays at the end, and only the symbol after it is removed.
        assertEquals(List.of("2024-2025"), compoundsOfTerms(parser, "-", "2024", "-", "2025", "-"));
    }

    // ============================================================
    // Break characters
    // ============================================================

    @Test
    public void test_break_bracketsCutTheRunByDefault() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        // The tokens on both sides of the bracket are one token each, so there is no compound word.
        assertEquals(List.of(), compoundsOfTerms(parser, "公表", "(", "出典"));
        assertEquals(List.of(), compoundsOfTerms(parser, "紫藤春香", "(", "はる"));
        assertEquals(List.of(), compoundsOfTerms(parser, "watch", "[", "sponsored"));
        assertEquals(List.of(), compoundsOfTerms(parser, "n", "<", "div"));
        assertEquals(List.of(), compoundsOfTerms(parser, "無料", ">", "ジャンプ"));
        assertEquals(List.of(), compoundsOfTerms(parser, "伝達", "(", "不法領得"));
        assertEquals(List.of("新型コロナ", "円安"), compoundsOfTerms(parser, "新型", "コロナ", "(", "円", "安", ")"));
        assertEquals(2, parser.getCompoundWordCount());
    }

    @Test
    public void test_break_everyDefaultBreakCharCutsTheRun() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        for (final int codePoint : DEFAULT_BREAK_CHARS.codePoints().toArray()) {
            final String breakChar = new String(Character.toChars(codePoint));
            assertEquals(breakChar, List.of("あい", "うえ"), compoundsOfTerms(parser, "あ", "い", breakChar, "う", "え"));
            assertEquals(breakChar, List.of(), compoundsOfTerms(parser, "あ", breakChar, "う"));
        }
    }

    @Test
    public void test_break_otherSymbolsDoNotCutTheRunByDefault() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        for (final String symbol : new String[] { ".", "-", "_", "/", ":", ",", "@", "&" }) {
            assertEquals(symbol, List.of("あ" + symbol + "い"), compoundsOfTerms(parser, "あ", symbol, "い"));
        }
        assertEquals(List.of("togetter.com"), compoundsOfTerms(parser, "togetter", ".", "com"));
        assertEquals(List.of("aws-emulator"), compoundsOfTerms(parser, "aws", "-", "emulator"));
        assertEquals(List.of("zenn.dev/abc"), compoundsOfTerms(parser, "zenn", ".", "dev", "/", "abc"));
    }

    @Test
    public void test_break_tokenWithBreakCharAndOtherSymbolIsBreakAsAWhole() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        // (@ and .) are not cut into pieces: the whole token is a break token.
        assertEquals(List.of("twit_shirokuma"), compoundsOfTerms(parser, "(@", "twit", "_", "shirokuma", ")"));
        assertEquals(List.of("あい", "うえ"), compoundsOfTerms(parser, "あ", "い", ".)", "う", "え"));
        assertEquals(List.of("あい", "うえ"), compoundsOfTerms(parser, "あ", "い", "(@", "う", "え"));
        // Even if the token has letters, it is not cut: neither "a" nor "b" joins a neighbor.
        assertEquals(List.of("あい", "うえ"), compoundsOfTerms(parser, "あ", "い", "a(b", "う", "え"));
        assertEquals(List.of(), compoundsOfTerms(parser, "あ", "a(b", "う"));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.OVER_LIMIT));
    }

    @Test
    public void test_break_tokenAtBothEndsOfARunIsNotInTheRun() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("新型コロナ"), compoundsOfTerms(parser, "(", "新型", "コロナ", ")"));
        assertEquals(List.of("新型コロナ"), compoundsOfTerms(parser, "「", "新型", "コロナ", "」"));
        // A break token with letters at the ends: it is not in the run (a symbol trim would not remove it).
        assertEquals(List.of("新型コロナ"), compoundsOfTerms(parser, "a(", "新型", "コロナ", ")b"));
    }

    @Test
    public void test_break_runsOnBothSidesAreIndependent() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of(), compoundsOfTerms(parser, "あ", "(", "い"));
        assertEquals(List.of("あい", "うえ"), compoundsOfTerms(parser, "あ", "い", "(", "う", "え"));
        assertEquals(List.of("あい"), compoundsOfTerms(parser, "あ", "い", "(", "う"));
        assertEquals(List.of("あい", "うえ", "おか"), compoundsOfTerms(parser, "あ", "い", "(", "う", "え", "「", "お", "か"));
        // Consecutive break tokens do not make an empty run.
        assertEquals(List.of("あい", "うえ"), compoundsOfTerms(parser, "あ", "い", "(", ")", "う", "え"));
    }

    @Test
    public void test_break_breakTokenIsTheOnlyReasonTheRunIsCut() throws Exception {
        final String text = "公表(出典";
        final StubAnalyzer analyzer = onlyCompoundWords(text, tok("公表", 0, 2, "名詞-一般", "コウヒョウ"), tok("(", 2, 3, "名詞-サ変接続", "("),
                tok("出典", 3, 5, "名詞-一般", "シュッテン"));

        assertEquals(List.of(), texts(parse(new DefaultContentsParser(), analyzer, text)));

        // The same tokens make a compound word when no break character is set: the break token is the only reason.
        System.setProperty(PROP_BREAK_CHARS, "");
        assertEquals(List.of("公表(出典"), texts(parse(new DefaultContentsParser(), analyzer, text)));
    }

    @Test
    public void test_break_trimIsAppliedToEachRun() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        // "-" at the ends of each run is removed, and "-" inside stays. The runs are evaluated independently.
        assertEquals(List.of("あい", "うえ"), compoundsOfTerms(parser, "-", "あ", "い", "-", "(", "-", "う", "え", "-"));
        assertEquals(List.of("a-b", "c.d"), compoundsOfTerms(parser, ".", "a", "-", "b", ".", "[", "c", ".", "d", "]", "-"));
        // A run that has only one token after the trim gives nothing, and is not a degradation.
        assertEquals(List.of(), compoundsOfTerms(parser, "あ", "-", "(", "い", "-"));
    }

    @Test
    public void test_break_maxTokensIsAppliedToEachRun() throws Exception {
        System.setProperty(PROP_MAX_TOKENS, "3");
        final DefaultContentsParser parser = new DefaultContentsParser();

        // 7 tokens would exceed the maximum, but each run has 3 tokens.
        assertEquals(List.of("あいう", "えおか"), compoundsOfTerms(parser, "あ", "い", "う", "(", "え", "お", "か"));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.OVER_LIMIT));

        // The run over the maximum is not used, and the other run is not affected.
        assertEquals(List.of("えお"), compoundsOfTerms(parser, "あ", "い", "う", "か", "(", "え", "お"));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.OVER_LIMIT));
    }

    @Test
    public void test_break_maxLengthIsAppliedToEachRun() throws Exception {
        System.setProperty(PROP_MAX_LENGTH, "4");
        final DefaultContentsParser parser = new DefaultContentsParser();

        // 8 characters in total, but each run has 4.
        assertEquals(List.of("あいうえ", "おかきく"), compoundsOfTerms(parser, "あいう", "え", "(", "おか", "きく"));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.OVER_LIMIT));

        assertEquals(List.of("おか"), compoundsOfTerms(parser, "あいう", "えお", "(", "お", "か"));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.OVER_LIMIT));
    }

    @Test
    public void test_break_cuttingIsNotADegradation() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of(), compoundsOfTerms(parser, "公表", "(", "出典"));
        assertEquals(List.of("新型コロナ", "円安"), compoundsOfTerms(parser, "新型", "コロナ", "(", "円", "安", ")"));

        assertEquals(2, parser.getCompoundWordCount());
        for (final CompoundDegradation cause : CompoundDegradation.values()) {
            assertEquals(cause.name(), 0, parser.getDegradedChunkCount(cause));
        }
        assertEquals(0, appender.count(Level.WARN));
        assertEquals(0, appender.count(Level.INFO));
    }

    @Test
    public void test_break_notSetMeansTheDefaultBrackets() throws Exception {
        assertNull(System.getProperty(PROP_BREAK_CHARS));

        assertEquals(List.of(), compoundsOfTerms(new DefaultContentsParser(), "公表", "(", "出典"));
    }

    @Test
    public void test_break_customCharsReplaceTheDefault() throws Exception {
        System.setProperty(PROP_BREAK_CHARS, "/");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("あい", "うえ"), compoundsOfTerms(parser, "あ", "い", "/", "う", "え"));
        assertEquals(List.of("zenn.dev"), compoundsOfTerms(parser, "zenn", ".", "dev", "/", "abc"));
        // A token that has "/" with other symbols is a break token as a whole.
        assertEquals(List.of(), compoundsOfTerms(parser, "あ", "./", "い"));
        // The brackets are not break characters any more (not merged with the default).
        assertEquals(List.of("公表(出典"), compoundsOfTerms(parser, "公表", "(", "出典"));
        assertEquals(List.of("watch[sponsored"), compoundsOfTerms(parser, "watch", "[", "sponsored"));
        // The trim of the symbol tokens at the ends works as before.
        assertEquals(List.of("新型コロナ"), compoundsOfTerms(parser, "(", "新型", "コロナ", ")"));
    }

    @Test
    public void test_break_eachCodePointOfTheValueIsABreakChar() throws Exception {
        System.setProperty(PROP_BREAK_CHARS, "/:");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("あい", "うえ"), compoundsOfTerms(parser, "あ", "い", "/", "う", "え"));
        assertEquals(List.of("あい", "うえ"), compoundsOfTerms(parser, "あ", "い", ":", "う", "え"));
        // The value is not one break string: "/:" as a whole is not required.
        assertEquals(List.of(), compoundsOfTerms(parser, "あ", ":", "い"));
        assertEquals(List.of("あ.い"), compoundsOfTerms(parser, "あ", ".", "い"));
    }

    @Test
    public void test_break_emptyValueDisablesTheBreak() throws Exception {
        System.setProperty(PROP_BREAK_CHARS, "");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("公表(出典"), compoundsOfTerms(parser, "公表", "(", "出典"));
        assertEquals(List.of("あい)う"), compoundsOfTerms(parser, "あ", "い", ")", "う"));
        assertEquals(List.of("watch[sponsored"), compoundsOfTerms(parser, "watch", "[", "sponsored"));
        // The trim of the symbol tokens at the ends works as before.
        assertEquals(List.of("新型コロナ"), compoundsOfTerms(parser, "(", "新型", "コロナ", ")"));
        assertEquals(List.of("twit_shirokuma"), compoundsOfTerms(parser, "(@", "twit", "_", "shirokuma", ")"));
    }

    @Test
    public void test_break_surrogatePairCharIsABreakChar() throws Exception {
        System.setProperty(PROP_BREAK_CHARS, "😀");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("新型コロナ", "円安"), compoundsOfTerms(parser, "新型", "コロナ", "😀", "円", "安"));
        assertEquals(List.of(), compoundsOfTerms(parser, "a", "😀", "b"));
        // A token that has the pair among other characters is a break token.
        assertEquals(List.of("あい", "うえ"), compoundsOfTerms(parser, "あ", "い", "x😀", "う", "え"));
        // Not the default any more, and the halves of the pair are not break characters by themselves.
        assertEquals(List.of("公表(出典"), compoundsOfTerms(parser, "公表", "(", "出典"));
        assertEquals(List.of("あ😁い"), compoundsOfTerms(parser, "あ", "😁", "い"));
    }

    @Test
    public void test_break_valueIsReadOnlyWhenTheParserIsCreated() throws Exception {
        System.setProperty(PROP_BREAK_CHARS, "/");
        final DefaultContentsParser parser = new DefaultContentsParser();

        System.setProperty(PROP_BREAK_CHARS, "");
        assertEquals(List.of("あい", "うえ"), compoundsOfTerms(parser, "あ", "い", "/", "う", "え"));
        assertEquals(List.of("公表(出典"), compoundsOfTerms(parser, "公表", "(", "出典"));

        // A parser created later reads the new value.
        assertEquals(List.of("あ/い"), compoundsOfTerms(new DefaultContentsParser(), "あ", "/", "い"));

        System.clearProperty(PROP_BREAK_CHARS);
        assertEquals(List.of("あい", "うえ"), compoundsOfTerms(parser, "あ", "い", "/", "う", "え"));
        assertEquals(List.of(), compoundsOfTerms(new DefaultContentsParser(), "公表", "(", "出典"));
    }

    // ============================================================
    // Break tokens
    // ============================================================

    @Test
    public void test_breakToken_defaultCutsTheRunAtRa() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("研究者"), compoundsOfTermsWithSuffix(parser, "研究", "者", "ら"));
        assertEquals(List.of("神谷浩史"), compoundsOfTermsWithSuffix(parser, "神谷", "浩史", "ら"));
        // 大統領 is a single token, so only the run on the right, 民主党幹部, makes a compound word.
        assertEquals(List.of("民主党幹部"), compoundsOfTermsWithSuffix(parser, "大統領", "ら", "民主党", "幹部"));
        assertEquals(List.of("米民主党幹部"), compoundsOfTermsWithSuffix(parser, "米", "民主党", "幹部", "ら"));
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "日本", "ら"));
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "ちゃ", "ら"));
        // Other suffixes still make compound words.
        assertEquals(List.of("大統領選"), compoundsOfTermsWithSuffix(parser, "大統領", "選"));
        assertEquals(List.of("円安"), compoundsOfTermsWithSuffix(parser, "円", "安"));
        assertEquals(List.of("研究者"), compoundsOfTermsWithSuffix(parser, "研究", "者"));
    }

    @Test
    public void test_breakToken_raAtTheStartInARowOrManyTimes() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("研究者"), compoundsOfTermsWithSuffix(parser, "ら", "研究", "者"));
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "ら"));
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "ら", "ら"));
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "ら", "ら", "ら"));
        assertEquals(List.of("研究者", "神谷浩史"), compoundsOfTermsWithSuffix(parser, "研究", "者", "ら", "ら", "神谷", "浩史", "ら"));
        assertEquals(List.of("研究者", "神谷浩史"), compoundsOfTermsWithSuffix(parser, "ら", "研究", "者", "ら", "神谷", "浩史", "ら", "ら"));
        // The token between the two breaks is one token, and the break splits it from the other.
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "研究", "ら", "者"));
    }

    @Test
    public void test_breakToken_onlyAnExactMatchIsABreakToken() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("人いくら"), compoundsOfTermsWithSuffix(parser, "人", "いくら"));
        assertEquals(List.of("新鮮天ぷら"), compoundsOfTermsWithSuffix(parser, "新鮮", "天ぷら"));
        assertEquals(List.of("あかしら"), compoundsOfTermsWithSuffix(parser, "あ", "かしら"));
        // A token that has ら is not a break token. One token alone makes no compound word.
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "彼ら"));
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "ちゃら"));
        assertEquals(List.of("彼らちゃら"), compoundsOfTermsWithSuffix(parser, "彼ら", "ちゃら"));
        assertEquals(List.of("ちゃらいくら"), compoundsOfTermsWithSuffix(parser, "ちゃら", "いくら"));
        // The same surface as a break token cuts the run.
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "あ", "ら", "い"));
        // A token that only starts with the surface of a break token is not a break token.
        assertEquals(List.of("あらい"), compoundsOfTermsWithSuffix(parser, "あ", "らい"));
        assertEquals(List.of("らいあ"), compoundsOfTermsWithSuffix(parser, "らい", "あ"));
    }

    @Test
    public void test_breakToken_isCaseSensitiveWhenTheSettingIsLowerCase() throws Exception {
        System.setProperty(PROP_BREAK_TOKENS, "ab");
        final DefaultContentsParser parser = new DefaultContentsParser();

        // The token is checked by the surface as it is: AB is not ab.
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "x", "ab", "y"));
        assertEquals(List.of("xaby"), compoundsOfTermsWithSuffix(parser, "x", "AB", "y"));
    }

    @Test
    public void test_breakToken_isCaseSensitive() throws Exception {
        System.setProperty(PROP_BREAK_TOKENS, "AB");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("xaby"), compoundsOfTermsWithSuffix(parser, "x", "ab", "y"));
        assertEquals(List.of("xabyz"), compoundsOfTermsWithSuffix(parser, "x", "ab", "y", "z"));
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "x", "AB", "y"));
        assertEquals(List.of("xy", "zw"), compoundsOfTermsWithSuffix(parser, "x", "y", "AB", "z", "w"));
        // The item text is normalized (lower-cased), but the token is checked by the surface as it is.
        assertEquals(List.of("xabyz"), compoundsOfTermsWithSuffix(parser, "x", "Ab", "y", "z"));
    }

    @Test
    public void test_breakToken_runsOnBothSidesAreIndependent() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("研究者", "神谷浩史"), compoundsOfTermsWithSuffix(parser, "研究", "者", "ら", "神谷", "浩史"));
        assertEquals(List.of("研究者"), compoundsOfTermsWithSuffix(parser, "研究", "者", "ら", "神谷"));
        // The trim of the symbol tokens is applied to each run.
        assertEquals(List.of("あい", "うえ"), compoundsOfTermsWithSuffix(parser, "-", "あ", "い", "-", "ら", "-", "う", "え", "-"));
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "あ", "-", "ら", "い", "-"));
        // The symbol tokens inside a run stay.
        assertEquals(List.of("a-b", "c.d"), compoundsOfTermsWithSuffix(parser, "a", "-", "b", "ら", "c", ".", "d"));
    }

    @Test
    public void test_breakToken_maxTokensIsAppliedToEachRun() throws Exception {
        System.setProperty(PROP_MAX_TOKENS, "3");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("あいう", "えおか"), compoundsOfTermsWithSuffix(parser, "あ", "い", "う", "ら", "え", "お", "か"));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.OVER_LIMIT));

        assertEquals(List.of("えお"), compoundsOfTermsWithSuffix(parser, "あ", "い", "う", "か", "ら", "え", "お"));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.OVER_LIMIT));
    }

    @Test
    public void test_breakToken_maxLengthIsAppliedToEachRun() throws Exception {
        System.setProperty(PROP_MAX_LENGTH, "4");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("あいうえ", "おかきく"), compoundsOfTermsWithSuffix(parser, "あいう", "え", "ら", "おか", "きく"));
        assertEquals(0, parser.getDegradedChunkCount(CompoundDegradation.OVER_LIMIT));

        assertEquals(List.of("おか"), compoundsOfTermsWithSuffix(parser, "あいう", "えお", "ら", "お", "か"));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.OVER_LIMIT));
    }

    @Test
    public void test_breakToken_cuttingIsNotADegradation() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("研究者"), compoundsOfTermsWithSuffix(parser, "研究", "者", "ら"));
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "日本", "ら"));
        assertEquals(List.of("研究者", "神谷浩史"), compoundsOfTermsWithSuffix(parser, "研究", "者", "ら", "神谷", "浩史"));

        assertEquals(3, parser.getCompoundWordCount());
        for (final CompoundDegradation cause : CompoundDegradation.values()) {
            assertEquals(cause.name(), 0, parser.getDegradedChunkCount(cause));
        }
        assertEquals(0, appender.count(Level.WARN));
        assertEquals(0, appender.count(Level.INFO));
    }

    @Test
    public void test_breakToken_notSetMeansRa() throws Exception {
        assertNull(System.getProperty(PROP_BREAK_TOKENS));

        assertEquals(List.of("研究者"), compoundsOfTermsWithSuffix(new DefaultContentsParser(), "研究", "者", "ら"));
        // Other suffixes are not break tokens by default.
        assertEquals(List.of("子どもたち"), compoundsOfTermsWithSuffix(new DefaultContentsParser(), "子ども", "たち"));
        assertEquals(List.of("お知らせ等"), compoundsOfTermsWithSuffix(new DefaultContentsParser(), "お知らせ", "等"));
    }

    @Test
    public void test_breakToken_emptyValueDisablesTheBreakTokens() throws Exception {
        System.setProperty(PROP_BREAK_TOKENS, "");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("研究者ら"), compoundsOfTermsWithSuffix(parser, "研究", "者", "ら"));
        assertEquals(List.of("神谷浩史ら"), compoundsOfTermsWithSuffix(parser, "神谷", "浩史", "ら"));
        assertEquals(List.of("日本ら"), compoundsOfTermsWithSuffix(parser, "日本", "ら"));
        // The break characters are not affected.
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "公表", "(", "出典"));
    }

    @Test
    public void test_breakToken_valueOfOnlySeparatorsAndSpacesDisablesTheBreakTokens() throws Exception {
        for (final String value : new String[] { " ", ",", " , ,, ", "\t" }) {
            System.setProperty(PROP_BREAK_TOKENS, value);
            assertEquals(value, List.of("研究者ら"), compoundsOfTermsWithSuffix(new DefaultContentsParser(), "研究", "者", "ら"));
        }
    }

    @Test
    public void test_breakToken_customValueReplacesTheDefault() throws Exception {
        System.setProperty(PROP_BREAK_TOKENS, "たち,等");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "子ども", "たち"));
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "お知らせ", "等"));
        assertEquals(List.of("あい", "うえ"), compoundsOfTermsWithSuffix(parser, "あ", "い", "たち", "う", "え", "等"));
        // ら is not a break token any more: not merged with the default.
        assertEquals(List.of("研究者ら"), compoundsOfTermsWithSuffix(parser, "研究", "者", "ら"));
        assertEquals(List.of("日本ら"), compoundsOfTermsWithSuffix(parser, "日本", "ら"));
    }

    @Test
    public void test_breakToken_theDefaultCanBeKeptByListingIt() throws Exception {
        System.setProperty(PROP_BREAK_TOKENS, "ら,たち");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("研究者"), compoundsOfTermsWithSuffix(parser, "研究", "者", "ら"));
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "子ども", "たち"));
        assertEquals(List.of("お知らせ等"), compoundsOfTermsWithSuffix(parser, "お知らせ", "等"));
    }

    @Test
    public void test_breakToken_spacesAndEmptyElementsAreIgnored() throws Exception {
        System.setProperty(PROP_BREAK_TOKENS, " たち , ,等 ");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "子ども", "たち"));
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "お知らせ", "等"));
        assertEquals(List.of("研究者ら"), compoundsOfTermsWithSuffix(parser, "研究", "者", "ら"));
        // An empty element is not a break token: a token is never empty, and nothing else is cut.
        assertEquals(List.of("あい"), compoundsOfTermsWithSuffix(parser, "あ", "い"));
    }

    @Test
    public void test_breakToken_valueIsReadOnlyWhenTheParserIsCreated() throws Exception {
        System.setProperty(PROP_BREAK_TOKENS, "たち");
        final DefaultContentsParser parser = new DefaultContentsParser();

        System.setProperty(PROP_BREAK_TOKENS, "");
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "子ども", "たち"));
        assertEquals(List.of("研究者ら"), compoundsOfTermsWithSuffix(parser, "研究", "者", "ら"));

        // A parser created later reads the new value.
        assertEquals(List.of("子どもたち"), compoundsOfTermsWithSuffix(new DefaultContentsParser(), "子ども", "たち"));

        System.clearProperty(PROP_BREAK_TOKENS);
        assertEquals(List.of("研究者ら"), compoundsOfTermsWithSuffix(parser, "研究", "者", "ら"));
        assertEquals(List.of("研究者"), compoundsOfTermsWithSuffix(new DefaultContentsParser(), "研究", "者", "ら"));
    }

    @Test
    public void test_breakToken_worksTogetherWithBreakChars() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();

        // Both cut the run, in one sequence.
        assertEquals(List.of("研究者", "神谷浩史", "円安"), compoundsOfTermsWithSuffix(parser, "研究", "者", "ら", "神谷", "浩史", "(", "円", "安", ")"));
        assertEquals(List.of(), compoundsOfTermsWithSuffix(parser, "あ", "ら", "(", "い"));
    }

    @Test
    public void test_breakToken_eachSettingWorksWithoutTheOther() throws Exception {
        System.setProperty(PROP_BREAK_CHARS, "");
        final DefaultContentsParser withoutChars = new DefaultContentsParser();
        assertEquals(List.of("研究者", "公表(出典"), compoundsOfTermsWithSuffix(withoutChars, "研究", "者", "ら", "公表", "(", "出典"));

        System.clearProperty(PROP_BREAK_CHARS);
        System.setProperty(PROP_BREAK_TOKENS, "");
        final DefaultContentsParser withoutTokens = new DefaultContentsParser();
        assertEquals(List.of("研究者ら", "新型コロナ"), compoundsOfTermsWithSuffix(withoutTokens, "研究", "者", "ら", "(", "新型", "コロナ", ")"));
        assertEquals(List.of("研究者ら"), compoundsOfTermsWithSuffix(withoutTokens, "研究", "者", "ら", "(", "新型", ")"));
    }

    // ============================================================
    // Settings
    // ============================================================

    @Test
    public void test_setting_maxTokens() throws Exception {
        System.setProperty(PROP_MAX_TOKENS, "3");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("あいう"), compoundsOfRun(parser, "あ", "い", "う"));
        assertEquals(List.of(), compoundsOfRun(parser, "あ", "い", "う", "え"));
    }

    @Test
    public void test_setting_maxTokensTwoIsTheMinimum() throws Exception {
        System.setProperty(PROP_MAX_TOKENS, "2");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("あい"), compoundsOfRun(parser, "あ", "い"));
        assertEquals(List.of(), compoundsOfRun(parser, "あ", "い", "う"));
    }

    @Test
    public void test_setting_maxTokensInvalidFallsBackToDefault() throws Exception {
        // 1 is invalid too: a compound word needs at least 2 tokens, so it would silently disable the feature.
        for (final String invalid : new String[] { "0", "1", "-3", "abc", "" }) {
            System.setProperty(PROP_MAX_TOKENS, invalid);
            final DefaultContentsParser parser = new DefaultContentsParser();
            assertEquals(invalid, List.of("あいうえお"), compoundsOfRun(parser, "あ", "い", "う", "え", "お"));
            assertEquals(invalid, List.of(), compoundsOfRun(parser, "あ", "い", "う", "え", "お", "か"));
        }
    }

    @Test
    public void test_setting_maxLength() throws Exception {
        System.setProperty(PROP_MAX_LENGTH, "6");
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("あいうえおか"), compoundsOfRun(parser, "あいう", "えおか"));
        assertEquals(List.of(), compoundsOfRun(parser, "あいう", "えおかき"));
    }

    @Test
    public void test_setting_maxLengthIsRoundedTo30() throws Exception {
        System.setProperty(PROP_MAX_LENGTH, "100");
        final DefaultContentsParser parser = new DefaultContentsParser();
        final String first = "あ".repeat(15);

        assertEquals(List.of(first + "い".repeat(15)), compoundsOfRun(parser, first, "い".repeat(15)));
        assertEquals(List.of(), compoundsOfRun(parser, first, "い".repeat(16)));
    }

    @Test
    public void test_setting_maxLengthInvalidFallsBackToDefault() throws Exception {
        for (final String invalid : new String[] { "0", "-1", "abc" }) {
            System.setProperty(PROP_MAX_LENGTH, invalid);
            final DefaultContentsParser parser = new DefaultContentsParser();
            final String first = "あ".repeat(10);
            assertEquals(invalid, List.of(first + "い".repeat(10)), compoundsOfRun(parser, first, "い".repeat(10)));
            assertEquals(invalid, List.of(), compoundsOfRun(parser, first, "い".repeat(11)));
        }
    }

    @Test
    public void test_setting_enabledValues() throws Exception {
        final StubAnalyzer analyzer = nounRun("新型", "コロナ");

        assertTrue(texts(parse(new DefaultContentsParser(), analyzer, "新型コロナ")).contains("新型コロナ"));
        System.setProperty(PROP_ENABLED, "true");
        assertTrue(texts(parse(new DefaultContentsParser(), analyzer, "新型コロナ")).contains("新型コロナ"));
        System.setProperty(PROP_ENABLED, "FALSE");
        assertFalse(texts(parse(new DefaultContentsParser(), analyzer, "新型コロナ")).contains("新型コロナ"));
    }

    @Test
    public void test_setting_enabledIsCaseInsensitive() throws Exception {
        System.setProperty(PROP_ENABLED, "TRUE");

        assertTrue(texts(parse(new DefaultContentsParser(), nounRun("新型", "コロナ"), "新型コロナ")).contains("新型コロナ"));
    }

    @Test
    public void test_setting_enabledInvalidValueDisablesTheFeature() throws Exception {
        // Documented behavior: any value other than "true" (ignoring case) disables the feature.
        final StubAnalyzer analyzer = nounRun("新型", "コロナ");
        for (final String invalid : new String[] { "yes", "1", "", "tru" }) {
            System.setProperty(PROP_ENABLED, invalid);
            assertFalse(invalid, texts(parse(new DefaultContentsParser(), analyzer, "新型コロナ")).contains("新型コロナ"));
        }
    }

    // ============================================================
    // Helpers
    // ============================================================

    private static AnalyzeToken tok(final String term, final int startOffset, final int endOffset, final String partOfSpeech,
            final String reading) {
        final Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("partOfSpeech", partOfSpeech);
        attributes.put("reading", reading);
        return new AnalyzeToken(term, 0, startOffset, endOffset, 1, "word", attributes);
    }

    private static AnalyzeToken tokenWithoutReading(final String term, final int startOffset, final int endOffset,
            final String partOfSpeech) {
        return new AnalyzeToken(term, 0, startOffset, endOffset, 1, "word", Map.of("partOfSpeech", partOfSpeech));
    }

    private static AnalyzeToken tokenWithoutPartOfSpeech(final String term, final int startOffset, final int endOffset) {
        return new AnalyzeToken(term, 0, startOffset, endOffset, 1, "word", Map.of());
    }

    /** Adjacent 名詞-一般 tokens; the char-filtered text is the joined surfaces. */
    private static StubAnalyzer nounRun(final String... terms) {
        final List<AnalyzeToken> tokens = new ArrayList<>();
        int offset = 0;
        for (final String term : terms) {
            tokens.add(tok(term, offset, offset + term.length(), "名詞-一般", term));
            offset += term.length();
        }
        return StubAnalyzer.fixed(String.join("", terms), tokens.toArray(AnalyzeToken[]::new));
    }

    /**
     * The compound words generated from adjacent tokens of the given terms. A term without a letter or a digit is a
     * 名詞-サ変接続 token (as the tokenizer gives to ASCII symbols), the others are 名詞-一般 tokens. The fragments are not generated.
     */
    private static List<String> compoundsOfTerms(final DefaultContentsParser parser, final String... terms) throws IOException {
        return compoundsOfTermsWithPartOfSpeech(parser, Set.of(), terms);
    }

    /** Same as {@link #compoundsOfTerms}, but the terms in {@link #SUFFIX_TERMS} are 名詞-接尾-一般 tokens, as in the real tokenizer. */
    private static List<String> compoundsOfTermsWithSuffix(final DefaultContentsParser parser, final String... terms) throws IOException {
        return compoundsOfTermsWithPartOfSpeech(parser, SUFFIX_TERMS, terms);
    }

    private static List<String> compoundsOfTermsWithPartOfSpeech(final DefaultContentsParser parser, final Set<String> suffixTerms,
            final String... terms) throws IOException {
        final List<AnalyzeToken> tokens = new ArrayList<>();
        int offset = 0;
        for (final String term : terms) {
            final boolean symbol = term.codePoints().noneMatch(Character::isLetterOrDigit);
            final String partOfSpeech = suffixTerms.contains(term) ? "名詞-接尾-一般" : symbol ? "名詞-サ変接続" : "名詞-一般";
            tokens.add(tok(term, offset, offset + term.length(), partOfSpeech, term));
            offset += term.length();
        }
        final String text = String.join("", terms);
        return texts(parse(parser, onlyCompoundWords(text, tokens.toArray(AnalyzeToken[]::new)), text));
    }

    /** An analyzer that returns the tokens at the tokenizer stage, and no fragments, so that only the compound words are generated. */
    private static StubAnalyzer onlyCompoundWords(final String charFilteredText, final AnalyzeToken... tokens) {
        final List<AnalyzeToken> tokenList = List.of(tokens);
        return new StubAnalyzer(chunk -> new TokenizerStageResult(tokenList, charFilteredText), chunk -> List.of());
    }

    private static Map<String, Object> document(final String content) {
        final Map<String, Object> document = new HashMap<>();
        document.put(FIELD, content);
        document.put("lang", LANG);
        return document;
    }

    private static ReadingConverter readingConverter() throws IOException {
        final ReadingConverterChain chain = new ReadingConverterChain();
        chain.addConverter(new KatakanaToAlphabetConverter());
        chain.init();
        return chain;
    }

    private static List<SuggestItem> parse(final DefaultContentsParser parser, final SuggestAnalyzer analyzer, final String content)
            throws IOException {
        final ReadingConverter converter = readingConverter();
        return parser.parseDocument(document(content), new String[] { FIELD }, new String[] { "label" }, "role", "lang", converter,
                converter, new NormalizerChain(), analyzer);
    }

    private static List<String> texts(final List<SuggestItem> items) {
        return items.stream().map(SuggestItem::getText).collect(Collectors.toList());
    }

    /** The texts of the items other than the given fragments. */
    private static List<String> compoundWords(final DefaultContentsParser parser, final SuggestAnalyzer analyzer, final String content,
            final String... fragments) throws IOException {
        final List<String> fragmentList = Arrays.asList(fragments);
        return texts(parse(parser, analyzer, content)).stream().filter(t -> !fragmentList.contains(t)).collect(Collectors.toList());
    }

    /** The compound words generated from a run of adjacent 名詞-一般 tokens. */
    private static List<String> compoundsOfRun(final DefaultContentsParser parser, final String... terms) throws IOException {
        return compoundWords(parser, nounRun(terms), String.join("", terms), terms);
    }

    private void assertWarnContains(final String expected) {
        assertTrue("WARN log containing " + expected + " but was " + appender.messages(Level.WARN),
                appender.contains(Level.WARN, expected));
    }

    /**
     * A stub of the analyzer. The tokens of the stage result are given by {@code stage} and the fragments (the result of analyze)
     * by {@code fragments}, for each chunk.
     */
    private static class StubAnalyzer implements SuggestAnalyzer {

        final Function<String, TokenizerStageResult> stage;

        final Function<String, List<AnalyzeToken>> fragments;

        final List<String> stageChunks = new CopyOnWriteArrayList<>();

        final List<String> analyzeChunks = new CopyOnWriteArrayList<>();

        final List<String> analyzeAndReadingChunks = new CopyOnWriteArrayList<>();

        StubAnalyzer(final Function<String, TokenizerStageResult> stage, final Function<String, List<AnalyzeToken>> fragments) {
            this.stage = stage;
            this.fragments = fragments;
        }

        /** Returns the same tokens and char-filtered text for every chunk. The fragments are the tokens kept by the pos filter. */
        static StubAnalyzer fixed(final String charFilteredText, final AnalyzeToken... tokens) {
            final List<AnalyzeToken> tokenList = List.of(tokens);
            return new StubAnalyzer(chunk -> new TokenizerStageResult(tokenList, charFilteredText), chunk -> keptByPosFilter(tokenList));
        }

        /**
         * Tokenizes 新型コロナ separated by white spaces. A white space is a token (記号-空白).
         */
        static StubAnalyzer spaceSeparated() {
            final Function<String, List<AnalyzeToken>> tokenizer = chunk -> {
                final List<AnalyzeToken> tokens = new ArrayList<>();
                int offset = 0;
                for (final String word : chunk.split(" ", -1)) {
                    if (word.equals("新型コロナ")) {
                        tokens.add(tok("新型", offset, offset + 2, "名詞-一般", "シンガタ"));
                        tokens.add(tok("コロナ", offset + 2, offset + 5, "名詞-一般", "コロナ"));
                    } else if (!word.isEmpty()) {
                        tokens.add(tok(word, offset, offset + word.length(), "名詞-一般", word));
                    }
                    offset += word.length();
                    tokens.add(tok(" ", offset, offset + 1, "記号-空白", " "));
                    offset += 1;
                }
                return tokens;
            };
            return new StubAnalyzer(chunk -> new TokenizerStageResult(tokenizer.apply(chunk), chunk),
                    chunk -> keptByPosFilter(tokenizer.apply(chunk)));
        }

        private static List<AnalyzeToken> keptByPosFilter(final List<AnalyzeToken> tokens) {
            final List<AnalyzeToken> kept = new ArrayList<>();
            for (final AnalyzeToken token : tokens) {
                final String partOfSpeech = (String) token.getAttributes().get("partOfSpeech");
                final boolean noun = partOfSpeech != null && partOfSpeech.startsWith("名詞");
                final boolean dropped = partOfSpeech == null || partOfSpeech.startsWith("名詞-接尾") || partOfSpeech.startsWith("名詞-数");
                if (noun && !dropped) {
                    kept.add(token);
                }
            }
            return kept;
        }

        @Override
        public List<AnalyzeToken> analyze(final String text, final String field, final String lang) {
            analyzeChunks.add(text);
            return fragments.apply(text);
        }

        @Override
        public List<AnalyzeToken> analyzeAndReading(final String text, final String field, final String lang) {
            analyzeAndReadingChunks.add(text);
            return fragments.apply(text).stream().map(t -> {
                final Object reading = t.getAttributes().get("reading");
                return new AnalyzeToken(reading == null ? t.getTerm() : reading.toString(), t.getPosition(), t.getStartOffset(),
                        t.getEndOffset(), 1, "word", Map.of());
            }).collect(Collectors.toList());
        }

        @Override
        public TokenizerStageResult analyzeTokenizerStage(final String text, final String field, final String lang) {
            stageChunks.add(text);
            return stage.apply(text);
        }
    }

    /** A reading converter that records the inputs and returns a prefixed text, to see which converter is used with which input. */
    private static class RecordingReadingConverter implements ReadingConverter {

        final String prefix;

        final List<String> inputs = Collections.synchronizedList(new ArrayList<>());

        RecordingReadingConverter(final String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void init() {
        }

        @Override
        public List<String> convert(final String text, final String field, final String... lang) {
            inputs.add(text);
            return new ArrayList<>(List.of(prefix + text));
        }
    }

    /** Captures the log events of {@link DefaultContentsParser}. */
    private static class CapturingAppender extends AbstractAppender {

        private final List<LogEvent> events = new CopyOnWriteArrayList<>();

        CapturingAppender() {
            super("CapturingAppender", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(final LogEvent event) {
            events.add(event.toImmutable());
        }

        long count(final Level level) {
            return events.stream().filter(e -> e.getLevel() == level).count();
        }

        List<String> messages(final Level level) {
            return events.stream()
                    .filter(e -> e.getLevel() == level)
                    .map(e -> e.getMessage().getFormattedMessage())
                    .collect(Collectors.toList());
        }

        boolean contains(final Level level, final String text) {
            return messages(level).stream().anyMatch(m -> m.contains(text));
        }

        List<LogEvent> events() {
            return events;
        }

        void clear() {
            events.clear();
        }
    }
}
