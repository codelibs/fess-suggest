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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.io.ResourceUtil;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.analysis.TokenizerStageResult;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.exception.SuggestSettingsException;
import org.codelibs.fess.suggest.util.LogRateLimiter;
import org.codelibs.fess.suggest.util.PitOperationHelper;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.codelibs.fesen.opensearch.OpenSearchException;
import org.codelibs.fesen.opensearch.OpenSearchStatusException;
import org.codelibs.fesen.opensearch.action.admin.indices.analyze.AnalyzeAction;
import org.codelibs.fesen.opensearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;
import org.codelibs.fesen.opensearch.action.admin.indices.analyze.AnalyzeRequestBuilder;
import org.codelibs.fesen.opensearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.codelibs.fesen.opensearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.codelibs.fesen.opensearch.common.settings.Settings;
import org.codelibs.fesen.opensearch.common.xcontent.XContentFactory;
import org.codelibs.fesen.opensearch.common.xcontent.XContentHelper;
import org.codelibs.fesen.opensearch.common.xcontent.XContentType;
import org.codelibs.fesen.opensearch.core.common.bytes.BytesArray;
import org.codelibs.fesen.opensearch.core.common.bytes.BytesReference;
import org.codelibs.fesen.opensearch.core.xcontent.ToXContent;
import org.codelibs.fesen.opensearch.core.xcontent.XContentBuilder;
import org.codelibs.fesen.opensearch.index.query.QueryBuilders;
import org.codelibs.fesen.opensearch.transport.client.Client;

/**
 * The AnalyzerSettings class is responsible for managing and configuring analyzers for different fields and languages.
 * It interacts with the OpenSearch client to create, update, and delete analyzer settings, as well as to retrieve
 * analyzer names and mappings.
 *
 * <p>Key functionalities include:</p>
 * <ul>
 *   <li>Initializing analyzer settings and mappings.</li>
 *   <li>Retrieving analyzer names for different fields and languages.</li>
 *   <li>Updating and deleting analyzer settings.</li>
 *   <li>Loading index settings and mappings from resources.</li>
 *   <li>Checking the availability of analyzers for supported languages.</li>
 * </ul>
 *
 * <p>Supported languages are defined in the SUPPORTED_LANGUAGES array.</p>
 *
 * <p>Inner class:</p>
 * <ul>
 *   <li>DefaultContentsAnalyzer: Implements the SuggestAnalyzer interface to analyze text and retrieve tokens using
 *       the configured analyzers.</li>
 * </ul>
 *
 * <p>Protected static class:</p>
 * <ul>
 *   <li>FieldAnalyzerMapping: Holds the analyzer names for different types of analysis (reading, reading term,
 *       normalization, contents, and contents reading) for a specific field.</li>
 * </ul>
 *
 * @see SuggestAnalyzer
 * @see FieldAnalyzerMapping
 */
public class AnalyzerSettings {
    private static final Logger logger = LogManager.getLogger(AnalyzerSettings.class);

    /** Analyzer name for reading. */
    public static final String READING_ANALYZER = "reading_analyzer";
    /** Analyzer name for reading term. */
    public static final String READING_TERM_ANALYZER = "reading_term_analyzer";
    /** Analyzer name for normalization. */
    public static final String NORMALIZE_ANALYZER = "normalize_analyzer";
    /** Analyzer name for contents. */
    public static final String CONTENTS_ANALYZER = "contents_analyzer";
    /** Analyzer name for contents reading. */
    public static final String CONTENTS_READING_ANALYZER = "contents_reading_analyzer";
    /** Field name for field analyzer mapping. */
    public static final String FIELD_ANALYZER_MAPPING = "fieldAnalyzerMapping";

    /** Document type name. */
    public static final String DOC_TYPE_NAME = "_doc";

    /** Page size used to walk over the field analyzer mappings. */
    protected static final int FIELD_ANALYZER_MAPPING_PAGE_SIZE = 500;

    private static final String PART_OF_SPEECH_ATTRIBUTE = "partOfSpeech";

    private static final String READING_ATTRIBUTE = "reading";

    private static final String INFLECTION_FORM_ATTRIBUTE = "inflectionForm";

    private static final List<String> TOKENIZER_STAGE_ATTRIBUTES =
            List.of(PART_OF_SPEECH_ATTRIBUTE, READING_ATTRIBUTE, INFLECTION_FORM_ATTRIBUTE);

    /** Interval of the warning logs of a failed analyze request of the tokenizer stage, to avoid flooding the log. */
    private static final long ANALYZE_FAILURE_WARN_INTERVAL_MILLIS = 60_000L;

    /** OpenSearch client. */
    protected final Client client;
    /** Analyzer settings index name. */
    protected final String analyzerSettingsIndexName;
    private final SuggestSettings settings;

    /** Analyzer map. */
    protected static Map<String, Set<String>> analyzerMap = new ConcurrentHashMap<>();
    /** Field analyzer mapping map. */
    protected static Map<String, Map<String, FieldAnalyzerMapping>> fieldAnalyzerMappingMap = new ConcurrentHashMap<>();

    /** Supported languages. */
    protected static final String[] SUPPORTED_LANGUAGES = { "ar", "bg", "bn", "ca", "cs", "da", "de", "el", "en", "es", "et", "fa", "fi",
            "fr", "gu", "he", "hi", "hr", "hu", "id", "it", "ja", "ko", "lt", "lv", "mk", "ml", "nl", "no", "pa", "pl", "pt", "ro", "ru",
            "si", "sq", "sv", "ta", "te", "th", "tl", "tr", "uk", "ur", "vi", "zh-cn", "zh-tw" };

    /**
     * Constructor for AnalyzerSettings.
     * @param client The OpenSearch client.
     * @param settings The SuggestSettings instance.
     * @param settingsIndexName The name of the settings index.
     */
    public AnalyzerSettings(final Client client, final SuggestSettings settings, final String settingsIndexName) {
        this.client = client;
        this.settings = settings;
        analyzerSettingsIndexName = createAnalyzerSettingsIndexName(settingsIndexName);
    }

    /**
     * Initializes the analyzer settings.
     * If the analyzer settings index does not exist, it creates it with default settings and mappings.
     * It also loads analyzer names and field analyzer mappings.
     */
    public synchronized void init() {
        try {
            final IndicesExistsResponse response =
                    client.admin().indices().prepareExists(analyzerSettingsIndexName).execute().actionGet(settings.getIndicesTimeout());
            if (!response.isExists()) {
                try {
                    createAnalyzerSettings(loadIndexSettings(), loadIndexMapping());
                } catch (final OpenSearchStatusException e) {
                    if (!SuggestUtil.isResourceAlreadyExistsException(e)) {
                        throw e;
                    }
                    // another process sharing this index name created it after the lookup above
                    if (logger.isInfoEnabled()) {
                        logger.info("Analyzer settings index was created by another process: index={}", analyzerSettingsIndexName);
                    }
                }
            }
            // an index that another process has just created exists before its primary shard can be searched
            client.admin()
                    .cluster()
                    .prepareHealth(analyzerSettingsIndexName)
                    .setWaitForYellowStatus()
                    .execute()
                    .actionGet(settings.getClusterTimeout());
            analyzerMap.put(analyzerSettingsIndexName, getAnalyzerNames());
            // getFieldAnalyzerMapping reads through a PIT, which can still fail right after the yellow wait above
            // returns (see PitOperationHelper#readWithRetry); getAnalyzerNames reads index settings from the
            // cluster state instead, so it is not affected and does not need the same retry.
            fieldAnalyzerMappingMap.put(analyzerSettingsIndexName,
                    PitOperationHelper.readWithRetry(client, settings, analyzerSettingsIndexName, this::getFieldAnalyzerMapping));
        } catch (final IOException e) {
            throw new SuggestSettingsException("Failed to create mappings.");
        }
    }

    /**
     * Returns the name of the analyzer settings index.
     * @return The analyzer settings index name.
     */
    public String getAnalyzerSettingsIndexName() {
        return analyzerSettingsIndexName;
    }

    /**
     * Returns the analyzer name for a given field and language using the specified mapping extractor.
     * @param field The field name.
     * @param lang The language.
     * @param mappingExtractor Function to extract the analyzer from FieldAnalyzerMapping.
     * @param defaultAnalyzer The default analyzer name to use.
     * @return The resolved analyzer name.
     */
    private String getAnalyzerName(final String field, final String lang, final Function<FieldAnalyzerMapping, String> mappingExtractor,
            final String defaultAnalyzer) {
        final Map<String, FieldAnalyzerMapping> fieldAnalyzerMapping = fieldAnalyzerMappingMap.get(analyzerSettingsIndexName);
        final Set<String> analyzerNames = analyzerMap.get(analyzerSettingsIndexName);
        final String analyzerName;
        if (StringUtil.isNotBlank(field) && fieldAnalyzerMapping.containsKey(field)) {
            final String mappedAnalyzer = mappingExtractor.apply(fieldAnalyzerMapping.get(field));
            analyzerName = mappedAnalyzer != null ? mappedAnalyzer : defaultAnalyzer;
        } else {
            analyzerName = defaultAnalyzer;
        }
        final String analyzerNameWithLang = isSupportedLanguage(lang) ? analyzerName + '_' + lang : analyzerName;
        if (analyzerNames.contains(analyzerNameWithLang)) {
            return analyzerNameWithLang;
        }
        return analyzerName;
    }

    /**
     * Returns the name of the reading analyzer for a given field and language.
     * @param field The field name.
     * @param lang The language.
     * @return The reading analyzer name.
     */
    public String getReadingAnalyzerName(final String field, final String lang) {
        return getAnalyzerName(field, lang, m -> m.readingAnalyzer, READING_ANALYZER);
    }

    /**
     * Returns the name of the reading term analyzer for a given field and language.
     * @param field The field name.
     * @param lang The language.
     * @return The reading term analyzer name.
     */
    public String getReadingTermAnalyzerName(final String field, final String lang) {
        return getAnalyzerName(field, lang, m -> m.readingTermAnalyzer, READING_TERM_ANALYZER);
    }

    /**
     * Returns the name of the normalize analyzer for a given field and language.
     * @param field The field name.
     * @param lang The language.
     * @return The normalize analyzer name.
     */
    public String getNormalizeAnalyzerName(final String field, final String lang) {
        return getAnalyzerName(field, lang, m -> m.normalizeAnalyzer, NORMALIZE_ANALYZER);
    }

    /**
     * Returns the name of the contents analyzer for a given field and language.
     * @param field The field name.
     * @param lang The language.
     * @return The contents analyzer name.
     */
    public String getContentsAnalyzerName(final String field, final String lang) {
        return getAnalyzerName(field, lang, m -> m.contentsAnalyzer, CONTENTS_ANALYZER);
    }

    /**
     * Returns the name of the contents reading analyzer for a given field and language.
     * @param field The field name.
     * @param lang The language.
     * @return The contents reading analyzer name.
     */
    public String getContentsReadingAnalyzerName(final String field, final String lang) {
        return getAnalyzerName(field, lang, m -> m.contentsReadingAnalyzer, CONTENTS_READING_ANALYZER);
    }

    /**
     * Updates the analyzer settings.
     * @param settings The settings to update.
     */
    public void updateAnalyzer(final Map<String, Object> settings) {
        client.admin()
                .indices()
                .prepareCreate(analyzerSettingsIndexName)
                .setSettings(settings)
                .execute()
                .actionGet(this.settings.getIndicesTimeout());
    }

    /**
     * Deletes the analyzer settings.
     */
    protected void deleteAnalyzerSettings() {
        client.admin().indices().prepareDelete(analyzerSettingsIndexName).execute().actionGet(settings.getIndicesTimeout());
    }

    /**
     * Creates analyzer settings with string settings and mappings.
     * @param settings The settings in string format.
     * @param mappings The mappings in string format.
     */
    protected void createAnalyzerSettings(final String settings, final String mappings) {
        client.admin()
                .indices()
                .prepareCreate(analyzerSettingsIndexName)
                .setSettings(settings, XContentType.JSON)
                .setMapping(mappings)
                .execute()
                .actionGet(this.settings.getIndicesTimeout());
    }

    /**
     * Creates analyzer settings with map settings and mappings.
     * @param settings The settings in map format.
     * @param mappings The mappings in map format.
     */
    protected void createAnalyzerSettings(final Map<String, Object> settings, final Map<String, Object> mappings) {
        client.admin()
                .indices()
                .prepareCreate(analyzerSettingsIndexName)
                .setSettings(settings)
                .execute()
                .actionGet(this.settings.getIndicesTimeout());
    }

    /**
     * Creates the analyzer settings index name.
     * @param settingsIndexName The base settings index name.
     * @return The created analyzer settings index name.
     */
    protected String createAnalyzerSettingsIndexName(final String settingsIndexName) {
        return settingsIndexName + "_analyzer";
    }

    /**
     * Loads the index settings from a resource file.
     * @return The index settings as a string.
     * @throws IOException If an I/O error occurs.
     */
    protected String loadIndexSettings() throws IOException {
        final String dictionaryPath = System.getProperty("fess.dictionary.path", StringUtil.EMPTY);
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader br =
                new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(getSuggestAnalyzerPath())))) {

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString().replaceAll(Pattern.quote("${fess.dictionary.path}"), dictionaryPath);
    }

    /**
     * Returns the path to the suggest analyzer configuration file.
     * @return The path to the suggest analyzer configuration file.
     */
    protected String getSuggestAnalyzerPath() {
        final Object typeObj = settings.get("search_engine.type");
        if (typeObj != null) {
            final String path = "suggest_indices/_" + typeObj.toString() + "/suggest_analyzer.json";
            if (ResourceUtil.getResourceNoException(path) != null) {
                return path;
            }
        }
        return "suggest_indices/suggest_analyzer.json";
    }

    /**
     * Loads the index mapping from a resource file.
     * @return The index mapping as a string.
     * @throws IOException If an I/O error occurs.
     */
    protected String loadIndexMapping() throws IOException {
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                this.getClass().getClassLoader().getResourceAsStream("suggest_indices/analyzer/mapping-default.json")));) {

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * Default contents analyzer.
     */
    public class DefaultContentsAnalyzer implements SuggestAnalyzer {

        /**
         * Constructs a new DefaultContentsAnalyzer.
         */
        public DefaultContentsAnalyzer() {
            // nothing
        }

        private final int maxContentLenth = settings.getAsInt(SuggestSettings.DefaultKeys.MAX_CONTENT_LENGTH, 50000);

        private final LogRateLimiter analyzeFailureWarnLimiter = new LogRateLimiter(ANALYZE_FAILURE_WARN_INTERVAL_MILLIS);

        /**
         * Analyzes the given text.
         * @param text The text to analyze.
         * @param field The field name.
         * @param lang The language.
         * @return A list of analyzed tokens.
         */
        @Override
        public List<AnalyzeToken> analyze(final String text, final String field, final String lang) {
            if (text == null || text.length() > maxContentLenth) {
                return Collections.emptyList();
            }
            final AnalyzeAction.Response analyzeResponse = client.admin()
                    .indices()
                    .prepareAnalyze(analyzerSettingsIndexName, text)
                    .setAnalyzer(getContentsAnalyzerName(field, lang))
                    .execute()
                    .actionGet(settings.getIndicesTimeout());
            return analyzeResponse.getTokens();
        }

        /**
         * Analyzes the given text and its reading.
         * @param text The text to analyze.
         * @param field The field name.
         * @param lang The language.
         * @return A list of analyzed tokens, or null if the contents reading analyzer name is blank.
         */
        @Override
        public List<AnalyzeToken> analyzeAndReading(final String text, final String field, final String lang) {
            try {
                final String contentsReadingAnalyzerName = getContentsReadingAnalyzerName(field, lang);
                if (StringUtil.isBlank(contentsReadingAnalyzerName)) {
                    return null;
                }
                final AnalyzeAction.Response analyzeResponse = client.admin()
                        .indices()
                        .prepareAnalyze(analyzerSettingsIndexName, text)
                        .setAnalyzer(contentsReadingAnalyzerName)
                        .execute()
                        .actionGet(settings.getIndicesTimeout());
                return analyzeResponse.getTokens();
            } catch (final IllegalArgumentException e) {
                return analyze(text, field, lang);
            }
        }

        /**
         * Analyzes the given text and returns the output of the tokenizer stage, using the explain mode of the analyze API.
         *
         * <p>
         * If the response is valid but the tokenizer provides no part of speech (e.g. a non-Japanese analyzer), a result
         * without tokens is returned, because this is a normal state of such an analyzer. {@code null} is returned only when
         * the tokenizer stage cannot be obtained: the text is too long, the analyze request failed (e.g. the explain response
         * exceeding the allowed number of tokens for a long text without whitespace; logged as a warning at most once
         * per minute), or the response lacks the tokenizer stage or the char-filtered text.
         * </p>
         * @param text The text to analyze.
         * @param field The field name.
         * @param lang The language.
         * @return The tokenizer stage result, or null if it is not available.
         */
        @Override
        public TokenizerStageResult analyzeTokenizerStage(final String text, final String field, final String lang) {
            if (text == null || text.length() > maxContentLenth) {
                return null;
            }
            final String analyzerName = getContentsAnalyzerName(field, lang);
            final String target = "analyzer=" + analyzerName + ", field=" + field + ", lang=" + lang + ", textSize=" + text.length();
            try {
                // AnalyzeRequestBuilder has no setter for explain, and the response has no accessor for the detail.
                final AnalyzeRequestBuilder builder =
                        client.admin().indices().prepareAnalyze(analyzerSettingsIndexName, text).setAnalyzer(analyzerName);
                builder.request().explain(true);
                // Limits the response size: without it every token of every filter stage carries all attributes.
                builder.request().attributes(TOKENIZER_STAGE_ATTRIBUTES);
                final AnalyzeAction.Response response = builder.execute().actionGet(settings.getIndicesTimeout());
                return parseTokenizerStage(convertToMap(response), target);
            } catch (OpenSearchException | IOException | IllegalArgumentException | IllegalStateException e) {
                logAnalyzeFailure(target, e);
                return null;
            }
        }

        // The text is not logged. Without the limit, every text without whitespace that is too long would log a warning.
        private void logAnalyzeFailure(final String target, final Exception e) {
            if (analyzeFailureWarnLimiter.tryAcquire()) {
                logger.warn("Failed to analyze the tokenizer stage: {}, error={}", target, e.getMessage());
            } else {
                logger.debug("Failed to analyze the tokenizer stage (not warned: at most once per minute): {}, error={}", target,
                        e.getMessage(), e);
            }
        }

        // getTokens() is null for an explain response, so the response is read through its JSON form.
        private Map<String, Object> convertToMap(final AnalyzeAction.Response response) throws IOException {
            final XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
            response.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);
            final String json = BytesReference.bytes(jsonBuilder).utf8ToString();
            return XContentHelper.convertToMap(new BytesArray(json), true, XContentType.JSON).v2();
        }

        // Each null return logs its own reason at debug level, so that the causes can be told apart.
        // Static and package-private so that the parsing of the response can be tested without an analyze request.
        static TokenizerStageResult parseTokenizerStage(final Map<String, Object> responseMap, final String target) {
            if (!(responseMap.get("detail") instanceof final Map<?, ?> detail)) {
                return unavailable("the response has no detail", target);
            }
            if (!(detail.get("tokenizer") instanceof final Map<?, ?> tokenizer)
                    || !(tokenizer.get("tokens") instanceof final List<?> rawTokens)) {
                return unavailable("the response has no tokenizer tokens", target);
            }
            final List<AnalyzeToken> tokens = new ArrayList<>(rawTokens.size());
            boolean hasPartOfSpeech = false;
            for (final Object rawToken : rawTokens) {
                final AnalyzeToken token = rawToken instanceof final Map<?, ?> tokenMap ? toAnalyzeToken(tokenMap) : null;
                if (token == null) {
                    return unavailable("a token of the response has an unexpected form", target);
                }
                hasPartOfSpeech |= token.getAttributes().containsKey(PART_OF_SPEECH_ATTRIBUTE);
                tokens.add(token);
            }

            // Judged before the char filters: an analyzer without part of speech (e.g. a non-Japanese one) may have no char filter,
            // and the caller needs neither tokens nor text from it.
            final String charFilteredText = extractCharFilteredText(detail);
            if (!hasPartOfSpeech) {
                if (logger.isDebugEnabled()) {
                    logger.debug("The tokenizer provides no part of speech: {}", target);
                }
                return new TokenizerStageResult(List.of(), charFilteredText == null ? "" : charFilteredText);
            }
            if (charFilteredText == null) {
                // With part of speech but without the char-filtered text, the analyzer settings or the response key is wrong.
                return unavailable("the response has no filtered_text of the char filters", target);
            }
            return new TokenizerStageResult(tokens, charFilteredText);
        }

        private static TokenizerStageResult unavailable(final String reason, final String target) {
            if (logger.isDebugEnabled()) {
                logger.debug("Tokenizer stage is not available ({}): {}", reason, target);
            }
            return null;
        }

        // The key is the singular "filtered_text" and its value is an array of strings.
        // The last char filter gives the text that the tokenizer actually receives. A missing "charfilters" is not
        // replaced by the original text, so that a wrong key or analyzer configuration is detected instead of hidden.
        private static String extractCharFilteredText(final Map<?, ?> detail) {
            if (!(detail.get("charfilters") instanceof final List<?> charFilters) || charFilters.isEmpty()) {
                return null;
            }
            if (!(charFilters.get(charFilters.size() - 1) instanceof final Map<?, ?> charFilter)
                    || !(charFilter.get("filtered_text") instanceof final List<?> filteredTexts) || filteredTexts.isEmpty()) {
                return null;
            }
            return filteredTexts.get(0) instanceof final String filteredText ? filteredText : null;
        }

        private static AnalyzeToken toAnalyzeToken(final Map<?, ?> tokenMap) {
            if (!(tokenMap.get("token") instanceof final String term) || !(tokenMap.get("start_offset") instanceof final Number startOffset)
                    || !(tokenMap.get("end_offset") instanceof final Number endOffset)) {
                return null;
            }
            final int position = tokenMap.get("position") instanceof final Number number ? number.intValue() : 0;
            final String type = tokenMap.get("type") instanceof final String typeValue ? typeValue : null;
            final Map<String, Object> attributes = new HashMap<>();
            for (final String attributeName : TOKENIZER_STAGE_ATTRIBUTES) {
                final Object value = tokenMap.get(attributeName);
                // A non-string inflectionForm is treated as absent, so that a consumer can cast the value safely.
                final boolean isInvalidInflectionForm = INFLECTION_FORM_ATTRIBUTE.equals(attributeName) && !(value instanceof String);
                if (value != null && !isInvalidInflectionForm) {
                    attributes.put(attributeName, value);
                }
            }
            return new AnalyzeToken(term, position, startOffset.intValue(), endOffset.intValue(), 1, type, attributes);
        }
    }

    /**
     * Check if the language is supported.
     * @param lang Language
     * @return True if the language is supported
     */
    public static boolean isSupportedLanguage(final String lang) {
        return StringUtil.isNotBlank(lang) && Stream.of(SUPPORTED_LANGUAGES).anyMatch(lang::equals);
    }

    /**
     * Get analyzer names.
     * @return Analyzer names
     */
    protected Set<String> getAnalyzerNames() {
        final GetSettingsResponse response =
                client.admin().indices().prepareGetSettings().setIndices(analyzerSettingsIndexName).execute().actionGet();
        final Settings settings = response.getIndexToSettings().get(analyzerSettingsIndexName);
        final Settings analyzerSettings = settings.getAsSettings("index.analysis.analyzer");
        return analyzerSettings.getAsGroups().keySet();
    }

    /**
     * Get field analyzer mapping.
     * @return Field analyzer mapping
     */
    protected Map<String, FieldAnalyzerMapping> getFieldAnalyzerMapping() {
        final Map<String, FieldAnalyzerMapping> mappingMap = new HashMap<>();
        PitOperationHelper.searchWithCallback(client, settings, analyzerSettingsIndexName,
                QueryBuilders.termQuery(FieldNames.ANALYZER_SETTINGS_TYPE, FIELD_ANALYZER_MAPPING), FIELD_ANALYZER_MAPPING_PAGE_SIZE,
                hit -> {
                    final Map<String, Object> source = hit.getSourceAsMap();
                    final String fieldReadingAnalyzer = source.get(FieldNames.ANALYZER_SETTINGS_READING_ANALYZER) == null ? null
                            : source.get(FieldNames.ANALYZER_SETTINGS_READING_ANALYZER).toString();
                    final String fieldReadingTermAnalyzer = source.get(FieldNames.ANALYZER_SETTINGS_READING_TERM_ANALYZER) == null ? null
                            : source.get(FieldNames.ANALYZER_SETTINGS_READING_TERM_ANALYZER).toString();
                    final String fieldNormalizeAnalyzer = source.get(FieldNames.ANALYZER_SETTINGS_NORMALIZE_ANALYZER) == null ? null
                            : source.get(FieldNames.ANALYZER_SETTINGS_NORMALIZE_ANALYZER).toString();
                    final String fieldContentsAnalyzer = source.get(FieldNames.ANALYZER_SETTINGS_CONTENTS_ANALYZER) == null ? null
                            : source.get(FieldNames.ANALYZER_SETTINGS_CONTENTS_ANALYZER).toString();
                    final String fieldContentsReadingAnalyzer =
                            source.get(FieldNames.ANALYZER_SETTINGS_CONTENTS_READING_ANALYZER) == null ? null
                                    : source.get(FieldNames.ANALYZER_SETTINGS_CONTENTS_READING_ANALYZER).toString();

                    mappingMap.put(source.get(FieldNames.ANALYZER_SETTINGS_FIELD_NAME).toString(),
                            new FieldAnalyzerMapping(fieldReadingAnalyzer, fieldReadingTermAnalyzer, fieldNormalizeAnalyzer,
                                    fieldContentsAnalyzer, fieldContentsReadingAnalyzer));
                });
        return mappingMap;
    }

    /**
     * Check analyzers.
     * @return Undefined analyzer set
     */
    public Set<String> checkAnalyzer() {
        final String text = "text";
        final Set<String> undefinedAnalyzerSet = new HashSet<>();
        for (final String lang : SUPPORTED_LANGUAGES) {
            final String readingAnalyzer = getReadingAnalyzerName("", lang);
            try {
                client.admin()
                        .indices()
                        .prepareAnalyze(analyzerSettingsIndexName, text)
                        .setAnalyzer(readingAnalyzer)
                        .execute()
                        .actionGet(settings.getIndicesTimeout());
            } catch (final IllegalArgumentException e) {
                undefinedAnalyzerSet.add(readingAnalyzer);
            }

            final String readingTermAnalyzer = getReadingTermAnalyzerName("", lang);
            try {
                client.admin()
                        .indices()
                        .prepareAnalyze(analyzerSettingsIndexName, text)
                        .setAnalyzer(readingTermAnalyzer)
                        .execute()
                        .actionGet(settings.getIndicesTimeout());
            } catch (final IllegalArgumentException e) {
                undefinedAnalyzerSet.add(readingTermAnalyzer);
            }

            final String normalizeAnalyzer = getNormalizeAnalyzerName("", lang);
            try {
                client.admin()
                        .indices()
                        .prepareAnalyze(analyzerSettingsIndexName, text)
                        .setAnalyzer(normalizeAnalyzer)
                        .execute()
                        .actionGet(settings.getIndicesTimeout());
            } catch (final IllegalArgumentException e) {
                undefinedAnalyzerSet.add(normalizeAnalyzer);
            }

            final String contentsAnalyzer = getContentsAnalyzerName("", lang);
            try {
                client.admin()
                        .indices()
                        .prepareAnalyze(analyzerSettingsIndexName, text)
                        .setAnalyzer(contentsAnalyzer)
                        .execute()
                        .actionGet(settings.getIndicesTimeout());
            } catch (final IllegalArgumentException e) {
                undefinedAnalyzerSet.add(contentsAnalyzer);
            }

            final String contentsReadingAnalyzer = getContentsReadingAnalyzerName("", lang);
            try {
                client.admin()
                        .indices()
                        .prepareAnalyze(analyzerSettingsIndexName, text)
                        .setAnalyzer(contentsReadingAnalyzer)
                        .execute()
                        .actionGet(settings.getIndicesTimeout());
            } catch (final IllegalArgumentException e) {
                undefinedAnalyzerSet.add(contentsReadingAnalyzer);
            }
        }

        return undefinedAnalyzerSet;
    }

    /**
     * Field analyzer mapping.
     */
    protected static class FieldAnalyzerMapping {
        /** Reading analyzer name. */
        protected final String readingAnalyzer;
        /** Reading term analyzer name. */
        protected final String readingTermAnalyzer;
        /** Normalize analyzer name. */
        protected final String normalizeAnalyzer;
        /** Contents analyzer name. */
        protected final String contentsAnalyzer;
        /** Contents reading analyzer name. */
        protected final String contentsReadingAnalyzer;

        /**
         * Constructor.
         * @param readingAnalyzer Reading analyzer name
         * @param readingTermAnalyzer Reading term analyzer name
         * @param normalizeAnalyzer Normalize analyzer name
         * @param contentsAnalyzer Contents analyzer name
         * @param contentsReadingAnalyzer Contents reading analyzer name
         */
        public FieldAnalyzerMapping(final String readingAnalyzer, final String readingTermAnalyzer, final String normalizeAnalyzer,
                final String contentsAnalyzer, final String contentsReadingAnalyzer) {
            this.readingAnalyzer = readingAnalyzer;
            this.readingTermAnalyzer = readingTermAnalyzer;
            this.normalizeAnalyzer = normalizeAnalyzer;
            this.contentsAnalyzer = contentsAnalyzer;
            this.contentsReadingAnalyzer = contentsReadingAnalyzer;
        }
    }
}
