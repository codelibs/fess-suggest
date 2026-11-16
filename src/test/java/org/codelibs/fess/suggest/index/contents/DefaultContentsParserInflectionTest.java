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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.analysis.TokenizerStageResult;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.contents.DefaultContentsParser.CompoundDegradation;
import org.codelibs.fess.suggest.index.contents.DefaultContentsParser.FragmentExclusion;
import org.codelibs.fess.suggest.index.contents.DefaultContentsParser.InflectionUndetermined;
import org.codelibs.fess.suggest.normalizer.NormalizerChain;
import org.codelibs.fesen.opensearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests of the exclusion of the inflected adjectives and the sharing of the tokenizer stage of {@link DefaultContentsParser}.
 * The analyzer is a stub that returns fixed tokens for the final stage and the tokenizer stage, so no OpenSearch is needed.
 */
public class DefaultContentsParserInflectionTest {

    private static final String FIELD = "content";

    private static final String ADJECTIVE = "形容詞-自立";

    private static final String PROP_INFLECTION = "fess.suggest.fragment.inflection.enabled";

    private static final String PROP_FILTER = "fess.suggest.fragment.filter.enabled";

    private static final String PROP_COMPOUND = "fess.suggest.compound.enabled";

    private static final String PROP_MAX_CONTENT_LENGTH = "fess.suggest.max.analyzed.content.length";

    private static final String[] PROPERTIES = { PROP_INFLECTION, PROP_FILTER, PROP_COMPOUND, PROP_MAX_CONTENT_LENGTH };

    private final Map<String, String> originalProperties = new HashMap<>();

    private org.apache.logging.log4j.core.Logger parserLogger;

    private CapturingAppender appender;

    private Level originalLogLevel;

    @Before
    public void setUp() {
        for (final String name : PROPERTIES) {
            originalProperties.put(name, System.getProperty(name));
            System.clearProperty(name);
        }
        // The tests of the inflected forms do not need the compound words. The tests of the sharing enable it again.
        System.setProperty(PROP_COMPOUND, "false");
        parserLogger = fetchParserLogger();
        originalLogLevel = parserLogger.getLevel();
        parserLogger.setLevel(Level.DEBUG);
        appender = new CapturingAppender();
        appender.start();
        parserLogger.addAppender(appender);
    }

    /** The logger instance that the parser really uses (see DefaultContentsParserCompoundWordTest). */
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
        for (final String name : PROPERTIES) {
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
    // Inflected forms and basic forms
    // ============================================================

    @Test
    public void test_inflectedAdjectivesAreExcluded() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        final StubAnalyzer analyzer = StubAnalyzer.singleChunk(
                List.of(stage("なかっ", 0, 3, ADJECTIVE, "連用タ接続"), stage("大き", 3, 5, ADJECTIVE, "ガル接続"), stage("難し", 5, 7, ADJECTIVE, "ガル接続")),
                fin("なかっ", 0, 3), fin("大き", 3, 5), fin("難し", 5, 7));

        assertEquals(List.of(), texts(parse(parser, analyzer, "text")));
        assertEquals(3, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
        assertEquals(0, undeterminedChunks(parser));
    }

    @Test
    public void test_basicFormsAreKept() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        final StubAnalyzer analyzer = StubAnalyzer.singleChunk(
                List.of(stage("大きい", 0, 3, ADJECTIVE, "基本形"), stage("ない", 3, 5, ADJECTIVE, "基本形"), stage("いい", 5, 7, ADJECTIVE, "基本形")),
                fin("大きい", 0, 3), fin("ない", 3, 5), fin("いい", 5, 7));

        assertEquals(List.of("大きい", "ない", "いい"), texts(parse(parser, analyzer, "text")));
        assertEquals(0, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
    }

    @Test
    public void test_tokensThatAreNotInflectedAdjectivesAreKept() throws Exception {
        final DefaultContentsParser parser = new DefaultContentsParser();
        // The offsets of the final stage tokens are the same as those of the stage tokens, except the 5th and the 6th.
        final StubAnalyzer analyzer = StubAnalyzer.singleChunk(List.of(
                // An adjective without the inflection form: nothing is known about it, so it is kept.
                stage("美しく", 0, 3, ADJECTIVE, null),
                // An empty inflection form.
                stage("楽し", 3, 5, ADJECTIVE, ""),
                // A noun.
                stage("新型", 5, 7, "名詞-一般", null),
                // The part of speech is not 形容詞-自立, even if there is an inflection form.
                stage("なくっ", 7, 10, "形容詞-非自立", "連用タ接続"),
                // The inflected adjective is at (20, 23), but the final stage token is at (10, 13).
                stage("なかっ", 20, 23, ADJECTIVE, "連用タ接続"),
                // The same offsets, but the term of the final stage is another one.
                stage("大き", 30, 32, ADJECTIVE, "ガル接続")), fin("美しく", 0, 3), fin("楽し", 3, 5), fin("新型", 5, 7), fin("なくっ", 7, 10),
                fin("なかっ", 10, 13), fin("別語", 30, 32));

        assertEquals(List.of("美しく", "楽し", "新型", "なくっ", "なかっ", "別語"), texts(parse(parser, analyzer, "text")));
        assertEquals(0, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
    }

    @Test
    public void test_tokenWithOnlyTheStartOffsetDifferentIsKept() throws Exception {
        // The term and the end offset are the same as those of the inflected adjective, so only the start offset tells them apart.
        final DefaultContentsParser parser = new DefaultContentsParser();
        final StubAnalyzer analyzer = StubAnalyzer.singleChunk(List.of(stage("なかっ", 0, 3, ADJECTIVE, "連用タ接続")), fin("なかっ", 1, 3));

        assertEquals(List.of("なかっ"), texts(parse(parser, analyzer, "text")));
        assertEquals(0, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
    }

    @Test
    public void test_tokenWithOnlyTheEndOffsetDifferentIsKept() throws Exception {
        // The term and the start offset are the same as those of the inflected adjective, so only the end offset tells them apart.
        final DefaultContentsParser parser = new DefaultContentsParser();
        final StubAnalyzer analyzer = StubAnalyzer.singleChunk(List.of(stage("なかっ", 0, 3, ADJECTIVE, "連用タ接続")), fin("なかっ", 0, 4));

        assertEquals(List.of("なかっ"), texts(parse(parser, analyzer, "text")));
        assertEquals(0, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
    }

    @Test
    public void test_termIsComparedAfterLowerCasing() throws Exception {
        // The final stage has a lowercase filter, so its term is lower-cased while the term of the tokenizer stage is not.
        final DefaultContentsParser parser = new DefaultContentsParser();
        final StubAnalyzer analyzer = StubAnalyzer.singleChunk(List.of(stage("NAKATTA", 0, 7, ADJECTIVE, "連用タ接続")), fin("nakatta", 0, 7));

        assertEquals(List.of(), texts(parse(parser, analyzer, "text")));
        assertEquals(1, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
    }

    // ============================================================
    // Chunks
    // ============================================================

    @Test
    public void test_tokensAreMatchedWithTheTokenizerStageOfTheirOwnChunk() throws Exception {
        // Every word is a chunk. Both chunks have a token at (0, 3) with the same term, but only the first one is an inflected adjective.
        System.setProperty(PROP_MAX_CONTENT_LENGTH, "3");
        final StubAnalyzer analyzer = new StubAnalyzer(chunk -> List.of(fin("なかっ", 0, 3)), chunk -> {
            final String partOfSpeech = chunk.equals("aaaa") ? ADJECTIVE : "名詞-一般";
            final String form = chunk.equals("aaaa") ? "連用タ接続" : null;
            return new TokenizerStageResult(List.of(stage("なかっ", 0, 3, partOfSpeech, form)), chunk);
        });
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("なかっ"), texts(parse(parser, analyzer, "aaaa bbbb")));
        assertEquals(List.of("aaaa", "bbbb"), analyzer.stageChunks);
        assertEquals(1, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
    }

    @Test
    public void test_tokenOfAnotherChunkIsNotExcludedByAChunkWithoutFinalTokens() throws Exception {
        // The first chunk yields no token of the final stage (the start index of the second chunk is the same as the first one).
        System.setProperty(PROP_MAX_CONTENT_LENGTH, "3");
        final StubAnalyzer analyzer = new StubAnalyzer(chunk -> chunk.equals("aaaa") ? List.of() : List.of(fin("なかっ", 0, 3)), chunk -> {
            final boolean inflected = chunk.equals("aaaa");
            return new TokenizerStageResult(List.of(inflected ? stage("なかっ", 0, 3, ADJECTIVE, "連用タ接続") : stage("なかっ", 0, 3, "名詞-一般", null)),
                    chunk);
        });
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("なかっ"), texts(parse(parser, analyzer, "aaaa bbbb")));
        assertEquals(0, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
    }

    @Test
    public void test_inflectedAdjectiveOfEachChunkIsExcluded() throws Exception {
        System.setProperty(PROP_MAX_CONTENT_LENGTH, "3");
        final StubAnalyzer analyzer = new StubAnalyzer(chunk -> List.of(fin("なかっ", 0, 3)),
                chunk -> new TokenizerStageResult(List.of(stage("なかっ", 0, 3, ADJECTIVE, "連用タ接続")), chunk));
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of(), texts(parse(parser, analyzer, "aaaa bbbb cccc")));
        assertEquals(3, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
    }

    // ============================================================
    // Degradation
    // ============================================================

    @Test
    public void test_inflectedFormsAreKeptWhenTheTokenizerStageIsNotUsable() throws Exception {
        final List<AnalyzeToken> finalTokens = List.of(fin("なかっ", 0, 3), fin("ー", 3, 4), fin("!!!", 4, 7));
        final Map<InflectionUndetermined, Function<String, TokenizerStageResult>> stages =
                new java.util.EnumMap<>(InflectionUndetermined.class);
        stages.put(InflectionUndetermined.STAGE_UNAVAILABLE, chunk -> null);
        stages.put(InflectionUndetermined.FAILURE, chunk -> {
            throw new IllegalStateException("explain failed");
        });
        stages.put(InflectionUndetermined.NO_PART_OF_SPEECH, chunk -> new TokenizerStageResult(List.of(), chunk));

        for (final Map.Entry<InflectionUndetermined, Function<String, TokenizerStageResult>> entry : stages.entrySet()) {
            final DefaultContentsParser parser = new DefaultContentsParser();
            final StubAnalyzer analyzer = new StubAnalyzer(chunk -> finalTokens, entry.getValue());

            // The inflected form stays as before, and the fragments that do not need the tokenizer stage (C1, C2) are excluded.
            assertEquals(entry.getKey().toString(), List.of("なかっ"), texts(parse(parser, analyzer, "text")));
            for (final InflectionUndetermined cause : InflectionUndetermined.values()) {
                assertEquals(entry.getKey() + "/" + cause, cause == entry.getKey() ? 1 : 0,
                        parser.getInflectionUndeterminedChunkCount(cause));
            }
            assertEquals(0, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
            assertEquals(1, parser.getExcludedFragmentCount(FragmentExclusion.SOKUON_CHOUON_ONLY));
            assertEquals(1, parser.getExcludedFragmentCount(FragmentExclusion.SYMBOL_ONLY));
        }
    }

    @Test
    public void test_onlyTheUnusableChunkKeepsTheInflectedForms() throws Exception {
        System.setProperty(PROP_MAX_CONTENT_LENGTH, "3");
        final StubAnalyzer analyzer = new StubAnalyzer(chunk -> List.of(fin("なかっ", 0, 3)),
                chunk -> chunk.equals("aaaa") ? null : new TokenizerStageResult(List.of(stage("なかっ", 0, 3, ADJECTIVE, "連用タ接続")), chunk));
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("なかっ"), texts(parse(parser, analyzer, "aaaa bbbb")));
        assertEquals(1, parser.getInflectionUndeterminedChunkCount(InflectionUndetermined.STAGE_UNAVAILABLE));
        assertEquals(1, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
    }

    @Test
    public void test_compoundDegradationsAreCountedSeparatelyForNullAndException() throws Exception {
        System.setProperty(PROP_COMPOUND, "true");
        System.setProperty(PROP_MAX_CONTENT_LENGTH, "3");
        final StubAnalyzer analyzer = new StubAnalyzer(chunk -> List.of(fin("新型", 0, 2)), chunk -> {
            if (chunk.equals("aaaa")) {
                return null;
            }
            throw new IllegalStateException("explain failed");
        });
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("新型", "新型"), texts(parse(parser, analyzer, "aaaa bbbb")));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.STAGE_UNAVAILABLE));
        assertEquals(1, parser.getDegradedChunkCount(CompoundDegradation.FAILURE));
        assertEquals(2, parser.getCompoundAnalyzedChunkCount());
        // The inflection side counts the same chunks by its own causes.
        assertEquals(1, parser.getInflectionUndeterminedChunkCount(InflectionUndetermined.STAGE_UNAVAILABLE));
        assertEquals(1, parser.getInflectionUndeterminedChunkCount(InflectionUndetermined.FAILURE));
        // Each kind of warning is limited separately, so the first one of each is logged.
        assertEquals(2, appender.count(Level.WARN));
    }

    @Test
    public void test_compoundWordsAndInflectionShareTheTokenizerStage() throws Exception {
        System.setProperty(PROP_COMPOUND, "true");
        final List<AnalyzeToken> stageTokens =
                List.of(stage("新型", 0, 2, "名詞-一般", null), stage("コロナ", 2, 5, "名詞-一般", null), stage("なかっ", 5, 8, ADJECTIVE, "連用タ接続"));
        final StubAnalyzer analyzer = new StubAnalyzer(chunk -> List.of(fin("新型", 0, 2), fin("コロナ", 2, 5), fin("なかっ", 5, 8)),
                chunk -> new TokenizerStageResult(stageTokens, "新型コロナなかっ"));
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("新型", "コロナ", "新型コロナ"), texts(parse(parser, analyzer, "text")));
        assertEquals(1, analyzer.stageChunks.size());
        assertEquals(1, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
    }

    // ============================================================
    // Readings
    // ============================================================

    @Test
    public void test_readingsOfTheRemainingWordsAreNotShifted() throws Exception {
        final StubAnalyzer analyzer = StubAnalyzer.singleChunk(List.of(stage("なかっ", 3, 6, ADJECTIVE, "連用タ接続")), fin("東京", 0, 2),
                fin("なかっ", 3, 6), fin("大きい", 6, 9), fin("!!!", 9, 12), fin("大阪", 12, 14));

        final List<SuggestItem> items = parse(new DefaultContentsParser(), analyzer, "text");

        assertEquals(List.of("東京", "大きい", "大阪"), texts(items));
        assertEquals("yomi-東京", items.get(0).getReadings()[0][0]);
        assertEquals("yomi-大きい", items.get(1).getReadings()[0][0]);
        assertEquals("yomi-大阪", items.get(2).getReadings()[0][0]);
    }

    // ============================================================
    // Settings
    // ============================================================

    @Test
    public void test_inflectionDisabledKeepsTheInflectedFormsAndDoesNotCallTheTokenizerStage() throws Exception {
        System.setProperty(PROP_INFLECTION, "false");
        final StubAnalyzer analyzer = inflectedFormAnalyzer();
        final DefaultContentsParser parser = new DefaultContentsParser();

        assertEquals(List.of("なかっ"), texts(parse(parser, analyzer, "text")));
        assertEquals(List.of(), analyzer.stageChunks);
        assertEquals(0, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
    }

    @Test
    public void test_inflectionIsEnabledByDefaultAndByTrueIgnoringCase() throws Exception {
        assertEquals(List.of(), texts(parse(new DefaultContentsParser(), inflectedFormAnalyzer(), "text")));
        System.setProperty(PROP_INFLECTION, "TRUE");
        assertEquals(List.of(), texts(parse(new DefaultContentsParser(), inflectedFormAnalyzer(), "text")));
    }

    @Test
    public void test_valueOtherThanTrueDisablesInflection() throws Exception {
        System.setProperty(PROP_INFLECTION, "yes");
        assertEquals(List.of("なかっ"), texts(parse(new DefaultContentsParser(), inflectedFormAnalyzer(), "text")));
    }

    @Test
    public void test_inflectionIsIndependentOfTheFragmentFilter() throws Exception {
        System.setProperty(PROP_FILTER, "false");
        final DefaultContentsParser parser = new DefaultContentsParser();
        final StubAnalyzer analyzer =
                StubAnalyzer.singleChunk(List.of(stage("なかっ", 0, 3, ADJECTIVE, "連用タ接続")), fin("なかっ", 0, 3), fin("ー", 3, 4));

        // The inflected form is excluded, but the filter is off, so that the long vowel mark stays.
        assertEquals(List.of("ー"), texts(parse(parser, analyzer, "text")));
        assertEquals(1, parser.getExcludedFragmentCount(FragmentExclusion.INFLECTION_FORM));
    }

    // ============================================================
    // The number of the requests of the tokenizer stage
    // ============================================================

    @Test
    public void test_tokenizerStageIsRequestedOncePerChunkWhenCompoundAndInflectionAreEnabled() throws Exception {
        System.setProperty(PROP_COMPOUND, "true");
        System.setProperty(PROP_MAX_CONTENT_LENGTH, "3");
        final StubAnalyzer analyzer = inflectedFormAnalyzer();

        parse(new DefaultContentsParser(), analyzer, "aaaa bbbb cccc");

        assertEquals(List.of("aaaa", "bbbb", "cccc"), analyzer.stageChunks);
        assertEquals(analyzer.analyzeChunks, analyzer.stageChunks);
    }

    @Test
    public void test_tokenizerStageIsRequestedOncePerChunkWhenOnlyInflectionIsEnabled() throws Exception {
        final StubAnalyzer analyzer = inflectedFormAnalyzer();

        parse(new DefaultContentsParser(), analyzer, "text");

        assertEquals(List.of("text"), analyzer.stageChunks);
    }

    @Test
    public void test_tokenizerStageIsRequestedOncePerChunkWhenOnlyCompoundIsEnabled() throws Exception {
        System.setProperty(PROP_COMPOUND, "true");
        System.setProperty(PROP_INFLECTION, "false");
        final StubAnalyzer analyzer = inflectedFormAnalyzer();

        parse(new DefaultContentsParser(), analyzer, "text");

        assertEquals(List.of("text"), analyzer.stageChunks);
    }

    @Test
    public void test_tokenizerStageIsNotRequestedWhenBothAreDisabled() throws Exception {
        System.setProperty(PROP_INFLECTION, "false");
        final StubAnalyzer analyzer = inflectedFormAnalyzer();

        parse(new DefaultContentsParser(), analyzer, "text");

        assertEquals(List.of(), analyzer.stageChunks);
    }

    @Test
    public void test_tokenizerStageIsNotRequestedForBlankText() throws Exception {
        for (final String compound : new String[] { "true", "false" }) {
            System.setProperty(PROP_COMPOUND, compound);
            final StubAnalyzer analyzer = inflectedFormAnalyzer();
            final DefaultContentsParser parser = new DefaultContentsParser();

            parse(parser, analyzer, "");
            parse(parser, analyzer, "   ");

            assertEquals(compound, List.of(), analyzer.stageChunks);
            assertEquals(compound, 0, undeterminedChunks(parser));
            assertEquals(compound, 0, parser.getCompoundAnalyzedChunkCount());
        }
    }

    @Test
    public void test_noCompoundWordWarningWhenOnlyInflectionIsEnabled() throws Exception {
        final List<Function<String, TokenizerStageResult>> unusableStages = List.of(chunk -> null, chunk -> {
            throw new IllegalStateException("explain failed");
        }, chunk -> new TokenizerStageResult(List.of(), chunk));

        for (final Function<String, TokenizerStageResult> unusableStage : unusableStages) {
            final DefaultContentsParser parser = new DefaultContentsParser();
            final StubAnalyzer analyzer = new StubAnalyzer(chunk -> List.of(fin("なかっ", 0, 3)), unusableStage);

            // It neither fails nor warns, and the fragment is kept.
            assertEquals(List.of("なかっ"), texts(parse(parser, analyzer, "text")));
            assertEquals(1, undeterminedChunks(parser));
            assertEquals(0, parser.getCompoundAnalyzedChunkCount());
            for (final CompoundDegradation cause : CompoundDegradation.values()) {
                assertEquals(cause.toString(), 0, parser.getDegradedChunkCount(cause));
            }
        }
        assertEquals(appender.messages(Level.WARN).toString(), 0, appender.count(Level.WARN));
        assertEquals(appender.messages(Level.INFO).toString(), 0, appender.count(Level.INFO));
        assertFalse(appender.events()
                .stream()
                .anyMatch(e -> e.getMessage().getFormattedMessage().toLowerCase(java.util.Locale.ROOT).contains("compound")
                        && (e.getLevel() == Level.WARN || e.getLevel() == Level.INFO)));
        // The cause of the chunks that could not be judged is only logged at debug level.
        assertTrue(appender.contains(Level.DEBUG, "Inflection forms were not judged"));
    }

    // ============================================================
    // Helpers
    // ============================================================

    /** An analyzer whose tokenizer stage says that なかっ is an inflected adjective, and whose final stage has なかっ. */
    private static StubAnalyzer inflectedFormAnalyzer() {
        return new StubAnalyzer(chunk -> List.of(fin("なかっ", 0, 3)),
                chunk -> new TokenizerStageResult(List.of(stage("なかっ", 0, 3, ADJECTIVE, "連用タ接続")), chunk));
    }

    private static long undeterminedChunks(final DefaultContentsParser parser) {
        long total = 0;
        for (final InflectionUndetermined cause : InflectionUndetermined.values()) {
            total += parser.getInflectionUndeterminedChunkCount(cause);
        }
        return total;
    }

    /** A token of the tokenizer stage. The inflection form is omitted when it is null. */
    private static AnalyzeToken stage(final String term, final int startOffset, final int endOffset, final String partOfSpeech,
            final String inflectionForm) {
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put("partOfSpeech", partOfSpeech);
        attributes.put("reading", term);
        if (inflectionForm != null) {
            attributes.put("inflectionForm", inflectionForm);
        }
        return new AnalyzeToken(term, 0, startOffset, endOffset, 1, "word", attributes);
    }

    /** A token of the final stage, which has no attribute. */
    private static AnalyzeToken fin(final String term, final int startOffset, final int endOffset) {
        return new AnalyzeToken(term, 0, startOffset, endOffset, 1, "word", Map.of());
    }

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

    private static List<SuggestItem> parse(final DefaultContentsParser parser, final SuggestAnalyzer analyzer, final String content)
            throws IOException {
        final Map<String, Object> document = new HashMap<>();
        document.put(FIELD, content);
        document.put("lang", "ja");
        final ReadingConverter converter = readingConverter();
        return parser.parseDocument(document, new String[] { FIELD }, new String[] { "label" }, "role", "lang", converter, converter,
                new NormalizerChain(), analyzer);
    }

    private static List<String> texts(final List<SuggestItem> items) {
        return items.stream().map(SuggestItem::getText).collect(Collectors.toList());
    }

    /** A stub of the analyzer. The final stage tokens and the tokenizer stage are given for each chunk. */
    private static class StubAnalyzer implements SuggestAnalyzer {

        private final Function<String, List<AnalyzeToken>> finalTokens;

        private final Function<String, TokenizerStageResult> stage;

        final List<String> stageChunks = new CopyOnWriteArrayList<>();

        final List<String> analyzeChunks = new CopyOnWriteArrayList<>();

        StubAnalyzer(final Function<String, List<AnalyzeToken>> finalTokens, final Function<String, TokenizerStageResult> stage) {
            this.finalTokens = finalTokens;
            this.stage = stage;
        }

        /** The same tokens for every chunk. The char-filtered text is not used by the inflection. */
        static StubAnalyzer singleChunk(final List<AnalyzeToken> stageTokens, final AnalyzeToken... finalTokens) {
            final List<AnalyzeToken> finalTokenList = List.of(finalTokens);
            return new StubAnalyzer(chunk -> finalTokenList, chunk -> new TokenizerStageResult(stageTokens, chunk));
        }

        @Override
        public List<AnalyzeToken> analyze(final String text, final String field, final String lang) {
            analyzeChunks.add(text);
            return finalTokens.apply(text);
        }

        @Override
        public List<AnalyzeToken> analyzeAndReading(final String text, final String field, final String lang) {
            return finalTokens.apply(text)
                    .stream()
                    .map(t -> new AnalyzeToken("yomi-" + t.getTerm(), t.getPosition(), t.getStartOffset(), t.getEndOffset(), 1, "word",
                            Map.of()))
                    .collect(Collectors.toList());
        }

        @Override
        public TokenizerStageResult analyzeTokenizerStage(final String text, final String field, final String lang) {
            stageChunks.add(text);
            return stage.apply(text);
        }
    }

    /** Captures the log events of {@link DefaultContentsParser}. */
    private static class CapturingAppender extends AbstractAppender {

        private final List<LogEvent> events = new CopyOnWriteArrayList<>();

        CapturingAppender() {
            super("InflectionCapturingAppender", null, null, true, Property.EMPTY_ARRAY);
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
    }
}
