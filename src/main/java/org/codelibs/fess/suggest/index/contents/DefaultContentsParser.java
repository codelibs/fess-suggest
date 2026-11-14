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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.analysis.TokenizerStageResult;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLog;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.util.LogRateLimiter;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.codelibs.fesen.opensearch.OpenSearchStatusException;
import org.codelibs.fesen.opensearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;

/**
 * DefaultContentsParser is an implementation of the ContentsParser interface.
 * It provides methods to parse search words, query logs, and documents into SuggestItem objects.
 *
 * <p>This class uses various utilities such as ReadingConverter, Normalizer, and SuggestAnalyzer
 * to process and analyze the input data.</p>
 *
 * <p>It also handles the exclusion of search words based on certain criteria and manages the
 * maximum length of analyzed content.</p>
 *
 * <p>Methods in this class may throw SuggesterException in case of failures during the parsing process.</p>
 *
 * <h2>Compound words</h2>
 * <p>{@link #parseDocument} adds, besides the one-candidate-per-token items, one item for each maximal run of
 * adjacent nouns (for example {@code 新型コロナ}). The run is detected from the tokenizer stage of the analyzer
 * ({@link SuggestAnalyzer#analyzeTokenizerStage}), using the part of speech and the offsets of the tokens.
 * Only runs of 2 to {@code fess.suggest.compound.max.tokens} tokens whose surface is at most
 * {@code fess.suggest.compound.max.length} characters are used, and sub-sequences of a longer run are not generated.
 * A compound word that cannot be confirmed to exist in the char-filtered text is not generated.</p>
 *
 * <p>System properties:</p>
 * <ul>
 * <li>{@code fess.suggest.compound.enabled}: {@code false} stops the generation and the additional analysis request.
 * Default {@code true}. <strong>The default is provisional: it must not be fixed as the production default
 * until it is evaluated in the real environment (performance, quality, and how to apply or stop it).</strong>
 * Any value other than {@code true} (ignoring case) disables the feature.</li>
 * <li>{@code fess.suggest.compound.max.tokens}: the maximum number of tokens in a compound word. Default 5.
 * A value less than 2 (a compound word needs at least 2 tokens), or an invalid value, falls back to the default.</li>
 * <li>{@code fess.suggest.compound.max.length}: the maximum number of characters in a compound word. Default 20,
 * and a value larger than 30 is rounded down to 30. A value of 0 or less, or an invalid value, falls back to the default.</li>
 * <li>{@code fess.suggest.compound.break.chars}: the break characters, each code point of the value is one break character.
 * A token whose surface has a break character is a break token: it is not put in any run, and it ends the run before it, so
 * a compound word never contains a break character and never spans a break token. Default (the property is not set): the
 * brackets {@code ()[]{}<>} and their full-width and Japanese forms ({@code （）［］｛｝＜＞〈〉《》「」『』【】〔〕}), because the tokenizer gives
 * the ASCII brackets a target part of speech and they join sentences into noises like {@code 公表(出典}. An empty value means
 * no break character. Any other value is used as it is (not merged with the default). It accepts any string, so there is no
 * invalid value.</li>
 * <li>{@code fess.suggest.compound.break.tokens}: the break tokens, a comma-separated list of surfaces. A token whose surface is
 * exactly equal to one of them (case-sensitive, not a partial match) is a break token, which is treated like a token that has a break
 * character. Default (the property is not set): {@code ら}, because the tokenizer gives the plural suffix {@code ら} the same part of
 * speech as the suffixes that make a compound word ({@code 選} of {@code 大統領選}), and it makes noises like {@code 研究者ら}. Each element is
 * stripped and an empty element is ignored, so an element cannot have a comma or a leading or trailing space. An empty value means
 * no break token. Any other value is used as it is (not merged with the default), for example {@code ら,たち,等}. It accepts any
 * string, so there is no invalid value.</li>
 * </ul>
 *
 * <p>If the compound words cannot be generated for a chunk of text, the chunk falls back to the one-candidate-per-token items
 * without throwing an exception. The number of chunks that produced compound words, the number of the generated compound words,
 * and the number of fallback chunks per cause are kept in this instance (see {@link CompoundDegradation}).
 * A chunk is counted once for each cause it hits, so the counts of the causes can exceed the number of the chunks.</p>
 *
 * <p>Warnings: a warning is logged for {@link CompoundDegradation#STAGE_UNAVAILABLE}, {@link CompoundDegradation#INVALID_OFFSET}
 * and {@link CompoundDegradation#TEXT_MISMATCH}, aggregated for each field of a document, and a warning is logged for
 * {@link CompoundDegradation#FAILURE} when it happens. The two kinds of warnings are limited separately to once per minute
 * for each instance (the suppressed ones are logged at debug level), so that a frequent cause does not hide the other.
 * {@link CompoundDegradation#OVER_LIMIT} is a designed limit and is only counted and logged at debug level.
 * {@link CompoundDegradation#NO_PART_OF_SPEECH} is normal for a non-Japanese analyzer, so it is not a warning, but an info
 * summary of the counts is logged at most once per minute (the suppressed ones at debug level): if it keeps appearing in a
 * Japanese (Kuromoji) environment, the part of speech is not obtained and no compound word is generated.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * DefaultContentsParser parser = new DefaultContentsParser();
 * SuggestItem item = parser.parseSearchWords(words, readings, fields, tags, roles, score, readingConverter, normalizer, analyzer, langs);
 * }
 * </pre>
 *
 * @see ContentsParser
 * @see SuggestItem
 * @see ReadingConverter
 * @see Normalizer
 * @see SuggestAnalyzer
 */
public class DefaultContentsParser implements ContentsParser {

    private static final Logger logger = LogManager.getLogger(DefaultContentsParser.class);

    private static final String PART_OF_SPEECH_ATTRIBUTE = "partOfSpeech";

    private static final String READING_ATTRIBUTE = "reading";

    /** Exact match with the part of speech given by the tokenizer. Numbers, counters and non-nouns are not included. */
    private static final Set<String> COMPOUND_PARTS_OF_SPEECH = Set.of("名詞-一般", "名詞-サ変接続", "名詞-固有名詞-一般", "名詞-固有名詞-人名-一般", "名詞-固有名詞-人名-姓",
            "名詞-固有名詞-人名-名", "名詞-固有名詞-組織", "名詞-固有名詞-地域-一般", "名詞-固有名詞-地域-国", "名詞-接尾-一般", "名詞-接尾-サ変接続", "名詞-接尾-地域");

    /** The brackets: ASCII ones and the full-width and Japanese ones. Used when the break chars property is not set. */
    private static final String DEFAULT_COMPOUND_BREAK_CHARS = "()[]{}<>（）［］｛｝＜＞〈〉《》「」『』【】〔〕";

    /** The plural suffix. Used when the break tokens property is not set. */
    private static final String DEFAULT_COMPOUND_BREAK_TOKENS = "ら";

    private static final int DEFAULT_COMPOUND_MAX_TOKENS = 5;

    /** A compound word consists of at least 2 tokens, so a smaller max would disable the feature without saying so. */
    private static final int MIN_COMPOUND_MAX_TOKENS = 2;

    private static final int DEFAULT_COMPOUND_MAX_LENGTH = 20;

    /** Same as the max of the content_length_filter of the contents analyzers. */
    private static final int COMPOUND_MAX_LENGTH_LIMIT = 30;

    /** Interval of the warning logs of the compound word generation, to avoid flooding the log. */
    private static final long COMPOUND_WARN_INTERVAL_MILLIS = 60_000L;

    /**
     * The causes why compound words are not generated for a chunk of text (the chunk falls back to the fragments only).
     */
    public enum CompoundDegradation {
        /**
         * {@code analyzeTokenizerStage} returned null: not supported, the text is too long, the analysis failed,
         * or the response lacks the tokenizer stage or the char-filtered text. The cause is logged by the analyzer at debug level.
         */
        STAGE_UNAVAILABLE,
        /** The tokenizer provides no part of speech (e.g. a non-Japanese analyzer) or no token. This is normal for such an analyzer. */
        NO_PART_OF_SPEECH,
        /** A token has an invalid offset (not {@code 0 <= start < end}, or overlapping the previous token). */
        INVALID_OFFSET,
        /** The surface of a compound word is not contained in the char-filtered text. */
        TEXT_MISMATCH,
        /** A run of nouns exceeds the max number of tokens or the max length. This is a designed limit, not a failure. */
        OVER_LIMIT,
        /** An exception was thrown while generating compound words. */
        FAILURE
    }

    private final int maxAnalyzedContentLength;

    private final boolean compoundEnabled;

    private final int compoundMaxTokens;

    private final int compoundMaxLength;

    /** The code points of the break characters. Read once, so that a token is checked without scanning the property again. */
    private final Set<Integer> compoundBreakCodePoints;

    /** The surfaces of the break tokens. Read once, like the break characters. */
    private final Set<String> compoundBreakTokens;

    private final AtomicLong compoundAnalyzedChunkCount = new AtomicLong();

    private final AtomicLong compoundChunkCount = new AtomicLong();

    private final AtomicLong compoundWordCount = new AtomicLong();

    private final Map<CompoundDegradation, AtomicLong> degradedChunkCounts = new EnumMap<>(CompoundDegradation.class);

    private final LogRateLimiter degradationWarnLimiter = new LogRateLimiter(COMPOUND_WARN_INTERVAL_MILLIS);

    private final LogRateLimiter failureWarnLimiter = new LogRateLimiter(COMPOUND_WARN_INTERVAL_MILLIS);

    private final LogRateLimiter noPartOfSpeechInfoLimiter = new LogRateLimiter(COMPOUND_WARN_INTERVAL_MILLIS);

    /**
     * Constructor.
     */
    public DefaultContentsParser() {
        maxAnalyzedContentLength = Integer.getInteger("fess.suggest.max.analyzed.content.length", 1000);
        compoundEnabled = Boolean.parseBoolean(System.getProperty("fess.suggest.compound.enabled", "true"));
        compoundMaxTokens = atLeastOrDefault(Integer.getInteger("fess.suggest.compound.max.tokens", DEFAULT_COMPOUND_MAX_TOKENS),
                MIN_COMPOUND_MAX_TOKENS, DEFAULT_COMPOUND_MAX_TOKENS);
        compoundMaxLength = Math.min(atLeastOrDefault(Integer.getInteger("fess.suggest.compound.max.length", DEFAULT_COMPOUND_MAX_LENGTH),
                1, DEFAULT_COMPOUND_MAX_LENGTH), COMPOUND_MAX_LENGTH_LIMIT);
        compoundBreakCodePoints = System.getProperty("fess.suggest.compound.break.chars", DEFAULT_COMPOUND_BREAK_CHARS)
                .codePoints()
                .boxed()
                .collect(Collectors.toUnmodifiableSet());
        compoundBreakTokens = parseBreakTokens(System.getProperty("fess.suggest.compound.break.tokens", DEFAULT_COMPOUND_BREAK_TOKENS));
        for (final CompoundDegradation cause : CompoundDegradation.values()) {
            degradedChunkCounts.put(cause, new AtomicLong());
        }
    }

    private static Set<String> parseBreakTokens(final String value) {
        return Arrays.stream(value.split(","))
                .map(String::strip)
                .filter(surface -> !surface.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static int atLeastOrDefault(final int value, final int min, final int defaultValue) {
        return value >= min ? value : defaultValue;
    }

    /**
     * Returns the number of chunks of text for which the generation of compound words was tried.
     *
     * @return the number of chunks
     */
    public long getCompoundAnalyzedChunkCount() {
        return compoundAnalyzedChunkCount.get();
    }

    /**
     * Returns the number of chunks of text for which at least one compound word was generated.
     *
     * @return the number of chunks
     */
    public long getCompoundChunkCount() {
        return compoundChunkCount.get();
    }

    /**
     * Returns the total number of the generated compound words.
     *
     * @return the number of compound words
     */
    public long getCompoundWordCount() {
        return compoundWordCount.get();
    }

    /**
     * Returns the number of chunks of text that fell back to the fragments only, for the given cause.
     * A chunk is counted once for each cause it hits.
     *
     * @param cause the cause
     * @return the number of chunks
     */
    public long getDegradedChunkCount(final CompoundDegradation cause) {
        return degradedChunkCounts.get(cause).get();
    }

    @Override
    public SuggestItem parseSearchWords(final String[] words, final String[][] readings, final String[] fields, final String[] tags,
            final String[] roles, final long score, final ReadingConverter readingConverter, final Normalizer normalizer,
            final SuggestAnalyzer analyzer, final String[] langs) {
        try {
            final List<String> wordsList = new ArrayList<>(words.length);
            final List<String[]> readingList = new ArrayList<>(words.length);

            for (int i = 0; i < words.length; i++) {
                if (isExcludeSearchword(words[i], fields != null && fields.length > 0 ? fields[0] : "", langs, analyzer)) {
                    continue;
                }

                final String word = normalizer.normalize(words[i], fields != null && fields.length > 0 ? fields[0] : "", langs);
                final List<String> l = readingConverter.convert(word, fields != null && fields.length > 0 ? fields[0] : "", langs);
                if (readings != null && readings.length > i && readings[i].length > 0) {
                    for (final String reading : readings[i]) {
                        if (!l.contains(reading)) {
                            l.add(reading);
                        }
                    }
                }

                wordsList.add(word);
                readingList.add(l.toArray(String[]::new));
            }
            if (wordsList.isEmpty()) {
                return null;
            }
            return new SuggestItem(wordsList.toArray(String[]::new), readingList.toArray(String[][]::new), fields, 0, score, -1, tags,
                    roles, langs, SuggestItem.Kind.QUERY);
        } catch (final IOException e) {
            throw new SuggesterException("Failed to create SuggestItem from search words.", e);
        }
    }

    @Override
    public List<SuggestItem> parseQueryLog(final QueryLog queryLog, final String[] fields, final String[] tagFieldNames,
            final String roleFieldName, final ReadingConverter readingConverter, final Normalizer normalizer) {
        final String queryString = queryLog.getQueryString();
        final String filterQueryString = queryLog.getFilterQueryString();

        final String[] tags = extractTags(tagFieldNames, tagFieldName -> {
            final List<String> results = new ArrayList<>(Arrays.asList(SuggestUtil.parseQuery(queryString, tagFieldName)));
            if (filterQueryString != null) {
                results.addAll(Arrays.asList(SuggestUtil.parseQuery(filterQueryString, tagFieldName)));
            }
            return results.toArray(new String[0]);
        });
        final String[] roles = extractRolesFromQueryLog(queryString, filterQueryString, roleFieldName);

        final List<SuggestItem> items = new ArrayList<>(fields.length);
        try {
            for (final String field : fields) {
                final String[] words = SuggestUtil.parseQuery(queryString, field);
                if (words.length == 0) {
                    continue;
                }

                final String[][] readings = new String[words.length][];
                for (int j = 0; j < words.length; j++) {
                    words[j] = normalizer.normalize(words[j], field, "");
                    final List<String> l = readingConverter.convert(words[j], field);
                    readings[j] = l.toArray(String[]::new);
                }

                items.add(new SuggestItem(words, readings, new String[] { field }, 0, 1, -1, tags, roles, null, SuggestItem.Kind.QUERY));
            }
        } catch (final IOException e) {
            throw new SuggesterException("Failed to create SuggestItem from query log.", e);
        }

        return items;
    }

    @Override
    public List<SuggestItem> parseDocument(final Map<String, Object> document, final String[] fields, final String[] tagFieldNames,
            final String roleFieldName, final String langFieldName, final ReadingConverter readingConverter,
            final ReadingConverter contentsReadingConverter, final Normalizer normalizer, final SuggestAnalyzer analyzer) {
        List<SuggestItem> items = null;
        final String[] tags = extractTags(tagFieldNames, tagFieldName -> getFieldValues(document, tagFieldName));
        final String[] roles = getFieldValues(document, roleFieldName);

        for (final String field : fields) {
            final Object textObj = document.get(field);
            if (textObj == null) {
                continue;
            }
            final String text = textObj.toString();
            final String lang = document.get(langFieldName) == null ? null : document.get(langFieldName).toString();

            final List<AnalyzeToken> tokens = analyzeText(analyzer, field, text, lang);
            if (tokens == null) {
                continue;
            }
            List<AnalyzeToken> readingTokens = analyzeTextByReading(analyzer, field, text, lang);
            if (readingTokens.size() != tokens.size()) {
                readingTokens = null;
            }

            try {
                for (int i = 0; i < tokens.size(); i++) {
                    final String word = tokens.get(i).getTerm();
                    if (StringUtil.isBlank(word)) {
                        continue;
                    }
                    final String reading = readingTokens == null ? null : readingTokens.get(i).getTerm();
                    final SuggestItem item =
                            createDocumentItem(word, reading, field, lang, tags, roles, readingConverter, contentsReadingConverter);

                    if (items == null) {
                        items = new ArrayList<>(text.length() * fields.length / field.length());
                    }
                    items.add(item);
                }
            } catch (final IOException e) {
                throw new SuggesterException("Failed to create SuggestItem from the document.", e);
            }

            if (compoundEnabled) {
                final List<SuggestItem> compoundItems =
                        createCompoundItems(analyzer, field, text, lang, tags, roles, readingConverter, contentsReadingConverter);
                if (!compoundItems.isEmpty()) {
                    if (items == null) {
                        items = new ArrayList<>(compoundItems.size());
                    }
                    items.addAll(compoundItems);
                }
            }
        }

        return items == null ? new ArrayList<>() : items;
    }

    /**
     * Creates an item of a word of a document: a fragment (a token) or a compound word.
     * @param word The word
     * @param reading The reading of the word given by the analyzer, or null to convert the reading from the word
     * @param field The field of the document
     * @param lang The language of the document, or null
     * @param tags The tags
     * @param roles The roles
     * @param readingConverter The converter used when the reading is null
     * @param contentsReadingConverter The converter used for the reading given by the analyzer
     * @return The item
     * @throws IOException If the reading conversion fails
     */
    private SuggestItem createDocumentItem(final String word, final String reading, final String field, final String lang,
            final String[] tags, final String[] roles, final ReadingConverter readingConverter,
            final ReadingConverter contentsReadingConverter) throws IOException {
        final List<String> readingList =
                reading == null ? readingConverter.convert(word, field, lang) : contentsReadingConverter.convert(reading, field, lang);
        readingList.add(word);
        final String[] words = { word };
        final String[][] readings = { readingList.toArray(String[]::new) };
        final String[] langs = lang == null ? new String[] {} : new String[] { lang };
        return new SuggestItem(words, readings, new String[] { field }, 1L, 0, -1, tags, roles, langs, SuggestItem.Kind.DOCUMENT);
    }

    /**
     * Creates the items of the compound words of a field. The text is split into chunks in the same way as the fragments.
     * This method never throws an exception caused by the compound word generation: the chunk falls back to the fragments only.
     */
    private List<SuggestItem> createCompoundItems(final SuggestAnalyzer analyzer, final String field, final String text, final String lang,
            final String[] tags, final String[] roles, final ReadingConverter readingConverter,
            final ReadingConverter contentsReadingConverter) {
        final List<SuggestItem> compoundItems = new ArrayList<>();
        final Map<CompoundDegradation, Integer> degradedChunks = new EnumMap<>(CompoundDegradation.class);

        forEachChunk(text, (chunk, ignoredIsLast) -> {
            final String chunkText = chunk.trim();
            if (chunkText.isEmpty()) {
                return;
            }
            final Set<CompoundDegradation> degradations = EnumSet.noneOf(CompoundDegradation.class);
            compoundItems.addAll(createCompoundItemsOfChunk(analyzer, field, chunkText, lang, tags, roles, readingConverter,
                    contentsReadingConverter, degradations));
            for (final CompoundDegradation cause : degradations) {
                degradedChunks.merge(cause, 1, Integer::sum);
            }
        });

        warnIfDegraded(field, lang, degradedChunks);
        return compoundItems;
    }

    private List<SuggestItem> createCompoundItemsOfChunk(final SuggestAnalyzer analyzer, final String field, final String chunkText,
            final String lang, final String[] tags, final String[] roles, final ReadingConverter readingConverter,
            final ReadingConverter contentsReadingConverter, final Set<CompoundDegradation> degradations) {
        compoundAnalyzedChunkCount.incrementAndGet();
        final List<SuggestItem> chunkItems = new ArrayList<>();
        try {
            final TokenizerStageResult stageResult = analyzer.analyzeTokenizerStage(chunkText, field, lang);
            if (stageResult == null) {
                degradations.add(CompoundDegradation.STAGE_UNAVAILABLE);
            } else if (stageResult.getTokens().isEmpty()) {
                degradations.add(CompoundDegradation.NO_PART_OF_SPEECH);
            } else {
                for (final CompoundWord compoundWord : extractCompoundWords(stageResult.getTokens(), stageResult.getCharFilteredText(),
                        degradations)) {
                    chunkItems.add(createDocumentItem(compoundWord.surface, compoundWord.reading, field, lang, tags, roles,
                            readingConverter, contentsReadingConverter));
                }
            }
        } catch (final IOException | RuntimeException e) {
            // Compound words are additional candidates: a failure must not break the indexing of the fragments.
            chunkItems.clear();
            degradations.add(CompoundDegradation.FAILURE);
            logCompoundFailure(field, lang, chunkText.length(), e);
        }

        if (!chunkItems.isEmpty()) {
            compoundChunkCount.incrementAndGet();
            compoundWordCount.addAndGet(chunkItems.size());
        }
        for (final CompoundDegradation cause : degradations) {
            degradedChunkCounts.get(cause).incrementAndGet();
        }
        if (degradations.contains(CompoundDegradation.NO_PART_OF_SPEECH)) {
            logNoPartOfSpeech(field, lang);
        }
        // The chunks that only lack the part of speech are logged by logNoPartOfSpeech, and the analyzer logs the reason.
        if (!degradations.isEmpty() && !degradations.equals(EnumSet.of(CompoundDegradation.NO_PART_OF_SPEECH)) && logger.isDebugEnabled()) {
            logger.debug("Compound words degraded: field={}, lang={}, textSize={}, causes={}", field, lang, chunkText.length(),
                    degradations);
        }
        return chunkItems;
    }

    /**
     * Summarizes the chunks without part of speech at most once per minute. Without it, a Japanese environment where the part of
     * speech is no longer obtained (e.g. a changed attribute key, or a tokenizer that returns no attribute) would generate
     * no compound word without any notice. The text is not logged.
     */
    private void logNoPartOfSpeech(final String field, final String lang) {
        final String message = "Chunks without part of speech were not analyzed for compound words: field={}, lang={}, "
                + "noPartOfSpeechChunks={}, analyzedChunks={}, compoundWords={}. This is normal for a non-Japanese analyzer, "
                + "but if this log keeps appearing in a Japanese (Kuromoji) environment, the partOfSpeech attribute may not be obtained";
        final long noPartOfSpeechChunks = degradedChunkCounts.get(CompoundDegradation.NO_PART_OF_SPEECH).get();
        if (noPartOfSpeechInfoLimiter.tryAcquire()) {
            logger.info(message, field, lang, noPartOfSpeechChunks, compoundAnalyzedChunkCount.get(), compoundWordCount.get());
        } else if (logger.isDebugEnabled()) {
            logger.debug(message, field, lang, noPartOfSpeechChunks, compoundAnalyzedChunkCount.get(), compoundWordCount.get());
        }
    }

    /**
     * Logs a warning for the causes that are not expected in normal operation. Warnings are aggregated for each field of a document,
     * and limited by time, because a text without white spaces can make every chunk fall back.
     * {@link CompoundDegradation#NO_PART_OF_SPEECH} and {@link CompoundDegradation#OVER_LIMIT} are expected and not warned.
     * {@link CompoundDegradation#FAILURE} is logged separately when it happens (see {@link #createCompoundItemsOfChunk}).
     */
    private void warnIfDegraded(final String field, final String lang, final Map<CompoundDegradation, Integer> degradedChunks) {
        final boolean unexpected = degradedChunks.containsKey(CompoundDegradation.STAGE_UNAVAILABLE)
                || degradedChunks.containsKey(CompoundDegradation.INVALID_OFFSET)
                || degradedChunks.containsKey(CompoundDegradation.TEXT_MISMATCH);
        if (!unexpected) {
            return;
        }
        final Map<CompoundDegradation, Long> totals = new EnumMap<>(CompoundDegradation.class);
        degradedChunkCounts.forEach((cause, count) -> totals.put(cause, count.get()));
        logCompoundWarning(degradationWarnLimiter,
                "Compound words were not generated for some chunks (fragments only): field={}, lang={}, degradedChunks={}, "
                        + "totalDegradedChunks={}, totalAnalyzedChunks={}, totalCompoundWords={}",
                field, lang, degradedChunks, totals, compoundAnalyzedChunkCount.get(), compoundWordCount.get());
    }

    // The text is not logged. The stack trace is only at debug level, so that a frequent failure does not flood the warnings.
    private void logCompoundFailure(final String field, final String lang, final int textSize, final Exception e) {
        final String message = "Failed to create compound words: field={}, lang={}, textSize={}, error={}";
        if (failureWarnLimiter.tryAcquire()) {
            logger.warn(message, field, lang, textSize, e.toString());
            if (logger.isDebugEnabled()) {
                logger.debug("Stack trace of the failure to create compound words: field={}, lang={}, textSize={}", field, lang, textSize,
                        e);
            }
        } else if (logger.isDebugEnabled()) {
            logger.debug(message, field, lang, textSize, e.toString(), e);
        }
    }

    private void logCompoundWarning(final LogRateLimiter limiter, final String message, final Object... params) {
        if (limiter.tryAcquire()) {
            logger.warn(message, params);
        } else {
            logger.debug(message, params);
        }
    }

    /**
     * Extracts the compound words from the tokens of the tokenizer stage: the maximal runs of adjacent tokens
     * whose part of speech is a target, with 2 to {@code compoundMaxTokens} tokens and a surface up to
     * {@code compoundMaxLength} characters (all counted after the symbol tokens at both ends are removed).
     *
     * <p>A token whose surface has a break character ({@code fess.suggest.compound.break.chars}, the brackets by default), or whose
     * surface is exactly equal to a break token ({@code fess.suggest.compound.break.tokens}, {@code ら} by default), is a
     * break token. Like a token that is not a target, it is not put in any run and it ends the run before it, so the runs before
     * and after it are evaluated independently. The token is never cut: a token such as {@code (@} or {@code .)} is a break token
     * as a whole, while {@code いくら} is not a break token because only an exact match counts. Cutting a run is the normal behavior,
     * so it is not counted as a degradation.</p>
     *
     * <p>The symbol tokens at both ends of a run are removed before the run is checked, because the tokenizer gives some ASCII
     * symbols (such as {@code .}, {@code -} and {@code )}) a target part of speech, and they make noises like {@code place-} or
     * {@code (@name)}. The symbol tokens inside a run are kept (such as {@code togetter.com}). A symbol token has no letter
     * or digit in its surface.</p>
     *
     * <p>The offsets of the tokens are positions in the original text, while {@code charFilteredText} has been changed by
     * the char filters and can have a different length. So the offsets are used only for the adjacency of the tokens,
     * and never applied to {@code charFilteredText}. The existence of a compound word is checked by {@code contains},
     * which is a necessary condition only (the same string may exist at another position).</p>
     *
     * @param tokens The tokens of the tokenizer stage
     * @param charFilteredText The text after the char filters
     * @param degradations The causes why a run of tokens is not used are added to this set
     * @return The compound words
     */
    List<CompoundWord> extractCompoundWords(final List<AnalyzeToken> tokens, final String charFilteredText,
            final Set<CompoundDegradation> degradations) {
        final List<CompoundWord> compoundWords = new ArrayList<>();
        final List<AnalyzeToken> run = new ArrayList<>();
        AnalyzeToken prev = null;
        for (final AnalyzeToken token : tokens) {
            final boolean validOffsets = hasValidOffsets(token, prev);
            prev = token;
            if (!validOffsets) {
                degradations.add(CompoundDegradation.INVALID_OFFSET);
                flushRun(run, charFilteredText, compoundWords, degradations);
                continue;
            }
            if (!isCompoundTarget(token) || isBreakToken(token)) {
                flushRun(run, charFilteredText, compoundWords, degradations);
                continue;
            }
            final boolean continuesRun = run.isEmpty() || token.getStartOffset() == run.get(run.size() - 1).getEndOffset();
            if (!continuesRun) {
                flushRun(run, charFilteredText, compoundWords, degradations);
            }
            run.add(token);
        }
        flushRun(run, charFilteredText, compoundWords, degradations);
        return compoundWords;
    }

    private boolean hasValidOffsets(final AnalyzeToken token, final AnalyzeToken prev) {
        final boolean validRange = 0 <= token.getStartOffset() && token.getStartOffset() < token.getEndOffset();
        return validRange && (prev == null || prev.getEndOffset() <= token.getStartOffset());
    }

    private boolean isCompoundTarget(final AnalyzeToken token) {
        final Map<String, Object> attributes = token.getAttributes();
        final Object partOfSpeech = attributes == null ? null : attributes.get(PART_OF_SPEECH_ATTRIBUTE);
        return partOfSpeech instanceof String && COMPOUND_PARTS_OF_SPEECH.contains(partOfSpeech);
    }

    private boolean isBreakToken(final AnalyzeToken token) {
        final String term = token.getTerm();
        return compoundBreakTokens.contains(term) || term.codePoints().anyMatch(compoundBreakCodePoints::contains);
    }

    /**
     * Makes a compound word from the run if the run is valid, and clears the run.
     */
    private void flushRun(final List<AnalyzeToken> run, final String charFilteredText, final List<CompoundWord> compoundWords,
            final Set<CompoundDegradation> degradations) {
        try {
            // Trimmed before every check, so that the limits and the text check are applied to the words that are really generated.
            final List<AnalyzeToken> trimmedRun = trimSymbolTokens(run);
            if (trimmedRun.size() < 2) {
                return;
            }
            if (trimmedRun.size() > compoundMaxTokens) {
                degradations.add(CompoundDegradation.OVER_LIMIT);
                return;
            }
            final String rawSurface = trimmedRun.stream().map(AnalyzeToken::getTerm).collect(Collectors.joining());
            if (!charFilteredText.contains(rawSurface)) {
                degradations.add(CompoundDegradation.TEXT_MISMATCH);
                return;
            }
            final String surface = rawSurface.toLowerCase(Locale.ROOT);
            if (surface.length() > compoundMaxLength) {
                degradations.add(CompoundDegradation.OVER_LIMIT);
                return;
            }
            compoundWords.add(new CompoundWord(surface, joinReadings(trimmedRun)));
        } finally {
            run.clear();
        }
    }

    /**
     * Removes the symbol tokens from both ends of the run.
     * @return A view of the run without the symbol tokens at both ends. It is invalid after the run is changed.
     */
    private List<AnalyzeToken> trimSymbolTokens(final List<AnalyzeToken> run) {
        int fromIndex = 0;
        int toIndex = run.size();
        while (fromIndex < toIndex && isSymbolToken(run.get(fromIndex))) {
            fromIndex++;
        }
        while (fromIndex < toIndex && isSymbolToken(run.get(toIndex - 1))) {
            toIndex--;
        }
        return run.subList(fromIndex, toIndex);
    }

    // Checked by code points, so that a surrogate pair of a letter is a letter. The long vowel mark (U+30FC) is a letter.
    private boolean isSymbolToken(final AnalyzeToken token) {
        return token.getTerm().codePoints().noneMatch(Character::isLetterOrDigit);
    }

    /**
     * Joins the readings of the tokens as katakana. A token without reading is replaced by its lower-cased surface.
     * @return The joined reading, or null if no token has a reading
     */
    private String joinReadings(final List<AnalyzeToken> run) {
        final StringBuilder reading = new StringBuilder();
        boolean hasReading = false;
        for (final AnalyzeToken token : run) {
            final Object tokenReading = token.getAttributes() == null ? null : token.getAttributes().get(READING_ATTRIBUTE);
            if (tokenReading instanceof String && !((String) tokenReading).isEmpty()) {
                reading.append((String) tokenReading);
                hasReading = true;
            } else {
                reading.append(token.getTerm().toLowerCase(Locale.ROOT));
            }
        }
        return hasReading ? reading.toString() : null;
    }

    /**
     * A compound word.
     */
    static final class CompoundWord {

        /** The lower-cased surface. */
        final String surface;

        /** The joined readings of the tokens, or null if no token has a reading. */
        final String reading;

        CompoundWord(final String surface, final String reading) {
            this.surface = surface;
            this.reading = reading;
        }
    }

    private String[] extractTags(final String[] tagFieldNames, final Function<String, String[]> tagExtractor) {
        final List<String> tagList = new ArrayList<>();
        for (final String tagFieldName : tagFieldNames) {
            tagList.addAll(Arrays.asList(tagExtractor.apply(tagFieldName)));
        }
        return tagList.toArray(new String[tagList.size()]);
    }

    private String[] extractRolesFromQueryLog(final String queryString, final String filterQueryString, final String roleFieldName) {
        final String[] roles1 = SuggestUtil.parseQuery(queryString, roleFieldName);
        final String[] roles2 = filterQueryString == null ? new String[0] : SuggestUtil.parseQuery(filterQueryString, roleFieldName);
        return Stream.concat(Arrays.stream(roles1), Arrays.stream(roles2)).toArray(String[]::new);
    }

    /**
     * Analyze text.
     * @param analyzer Analyzer
     * @param field Field
     * @param text Text
     * @param lang Language
     * @return List of tokens
     */
    protected List<AnalyzeToken> analyzeText(final SuggestAnalyzer analyzer, final String field, final String text, final String lang) {
        return analyzeTextInternal(analyzer, field, text, lang, false);
    }

    /**
     * Analyze text by reading.
     * @param analyzer Analyzer
     * @param field Field
     * @param text Text
     * @param lang Language
     * @return List of tokens
     */
    protected List<AnalyzeToken> analyzeTextByReading(final SuggestAnalyzer analyzer, final String field, final String text,
            final String lang) {
        return analyzeTextInternal(analyzer, field, text, lang, true);
    }

    /**
     * Internal method for analyzing text with optional reading support.
     * @param analyzer Analyzer
     * @param field Field
     * @param text Text
     * @param lang Language
     * @param useReading Whether to use reading analysis
     * @return List of tokens
     */
    private List<AnalyzeToken> analyzeTextInternal(final SuggestAnalyzer analyzer, final String field, final String text, final String lang,
            final boolean useReading) {
        final List<AnalyzeToken> tokens = new ArrayList<>();
        forEachChunk(text, (chunk, isLast) -> processBuffer(analyzer, field, lang, useReading, tokens, chunk, isLast));
        return tokens;
    }

    /**
     * Splits the text into chunks and passes each chunk to the handler. The text is split by white spaces, and a chunk is
     * closed when it gets longer than {@code maxAnalyzedContentLength}. A text without white spaces is a single chunk.
     * Every analysis of the text (tokens, readings, compound words) uses this method, so that all of them see the same chunks.
     * @param text Text
     * @param chunkHandler Handler of a chunk. The chunk is not trimmed and has a trailing space.
     */
    private void forEachChunk(final String text, final ChunkHandler chunkHandler) {
        final StringBuilder buf = new StringBuilder(maxAnalyzedContentLength);
        for (final String t : text.split("\\s")) {
            buf.append(t).append(' ');
            if (buf.length() > maxAnalyzedContentLength) {
                chunkHandler.handle(buf.toString(), false);
                buf.setLength(0);
            }
        }
        if (buf.length() > 0) {
            chunkHandler.handle(buf.toString(), true);
        }
    }

    private interface ChunkHandler {
        void handle(String chunk, boolean isLast);
    }

    /**
     * Process the chunk and add analyzed tokens.
     * @param analyzer Analyzer
     * @param field Field
     * @param lang Language
     * @param useReading Whether to use reading analysis
     * @param tokens Token list to add to
     * @param chunk Chunk of text
     * @param isLast Whether this is the last chunk
     */
    private void processBuffer(final SuggestAnalyzer analyzer, final String field, final String lang, final boolean useReading,
            final List<AnalyzeToken> tokens, final String chunk, final boolean isLast) {
        try {
            final List<AnalyzeToken> result =
                    useReading ? analyzer.analyzeAndReading(chunk.trim(), field, lang) : analyzer.analyze(chunk.trim(), field, lang);
            if (result != null) {
                tokens.addAll(result);
            }
        } catch (OpenSearchStatusException | IllegalStateException e) {
            final String lastPrefix = isLast ? "last " : "";
            final String typeDesc = useReading ? "reading " : "";
            if (logger.isDebugEnabled()) {
                logger.warn("Failed to analyze {}{}text: field={}, lang={}, textSize={}", lastPrefix, typeDesc, field, lang, chunk.length(),
                        e);
            } else {
                logger.warn("Failed to analyze {}{}text: field={}, lang={}, textSize={}, error={}", lastPrefix, typeDesc, field, lang,
                        chunk.length(), e.getMessage());
            }
        }
    }

    /**
     * Get field values.
     * @param document Document
     * @param fieldName Field name
     * @return Field values
     */
    protected String[] getFieldValues(final Map<String, Object> document, final String fieldName) {
        final Object value = document.get(fieldName);
        if (value instanceof String) {
            return new String[] { value.toString() };
        }
        if (value instanceof String[]) {
            return (String[]) value;
        }
        if (value instanceof List) {
            return ((List<?>) value).stream().map(Object::toString).toArray(String[]::new);
        }
        if (value != null) {
            return new String[] { value.toString() };
        }

        return new String[0];
    }

    /**
     * Check if the search word is excluded.
     * @param searchWord Search word
     * @param field Field
     * @param langs Languages
     * @param analyzer Analyzer
     * @return True if the search word is excluded
     */
    protected boolean isExcludeSearchword(final String searchWord, final String field, final String[] langs,
            final SuggestAnalyzer analyzer) {
        if (langs == null || langs.length == 0) {
            final List<AnalyzeToken> tokens = analyzer.analyze(searchWord, "", null);
            return tokens == null || tokens.size() == 0;
        }
        for (final String lang : langs) {
            final List<AnalyzeToken> tokens = analyzer.analyze(searchWord, field, lang);
            if (tokens != null && tokens.size() > 0) {
                return false;
            }
        }
        return true;

    }
}
