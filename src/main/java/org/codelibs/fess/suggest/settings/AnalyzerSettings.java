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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.codelibs.core.io.ResourceUtil;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.exception.SuggestSettingsException;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.opensearch.action.admin.indices.analyze.AnalyzeAction;
import org.opensearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;
import org.opensearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.opensearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.opensearch.action.search.CreatePitAction;
import org.opensearch.action.search.CreatePitRequest;
import org.opensearch.action.search.CreatePitResponse;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.PointInTimeBuilder;
import org.opensearch.search.sort.FieldSortBuilder;
import org.opensearch.search.sort.SortOrder;
import org.opensearch.transport.client.Client;

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
                createAnalyzerSettings(loadIndexSettings(), loadIndexMapping());
            }
            analyzerMap.put(analyzerSettingsIndexName, getAnalyzerNames());
            fieldAnalyzerMappingMap.put(analyzerSettingsIndexName, getFieldAnalyzerMapping());
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
     * Returns the name of the reading analyzer for a given field and language.
     * @param field The field name.
     * @param lang The language.
     * @return The reading analyzer name.
     */
    public String getReadingAnalyzerName(final String field, final String lang) {
        final Map<String, FieldAnalyzerMapping> fieldAnalyzerMapping = fieldAnalyzerMappingMap.get(analyzerSettingsIndexName);
        final Set<String> analyzerNames = analyzerMap.get(analyzerSettingsIndexName);
        final String analyzerName;
        if (StringUtil.isNotBlank(field) && fieldAnalyzerMapping.containsKey(field)
                && fieldAnalyzerMapping.get(field).readingAnalyzer != null) {
            analyzerName = fieldAnalyzerMapping.get(field).readingAnalyzer;
        } else {
            analyzerName = READING_ANALYZER;
        }
        final String analyzerNameWithlang = isSupportedLanguage(lang) ? analyzerName + '_' + lang : analyzerName;
        if (analyzerNames.contains(analyzerNameWithlang)) {
            return analyzerNameWithlang;
        }
        return analyzerName;
    }

    /**
     * Returns the name of the reading term analyzer for a given field and language.
     * @param field The field name.
     * @param lang The language.
     * @return The reading term analyzer name.
     */
    public String getReadingTermAnalyzerName(final String field, final String lang) {
        final Map<String, FieldAnalyzerMapping> fieldAnalyzerMapping = fieldAnalyzerMappingMap.get(analyzerSettingsIndexName);
        final Set<String> analyzerNames = analyzerMap.get(analyzerSettingsIndexName);
        final String analyzerName;
        if (StringUtil.isNotBlank(field) && fieldAnalyzerMapping.containsKey(field)
                && fieldAnalyzerMapping.get(field).readingTermAnalyzer != null) {
            analyzerName = fieldAnalyzerMapping.get(field).readingTermAnalyzer;
        } else {
            analyzerName = READING_TERM_ANALYZER;
        }
        final String analyzerNameWithlang = isSupportedLanguage(lang) ? analyzerName + '_' + lang : analyzerName;
        if (analyzerNames.contains(analyzerNameWithlang)) {
            return analyzerNameWithlang;
        }
        return analyzerName;
    }

    /**
     * Returns the name of the normalize analyzer for a given field and language.
     * @param field The field name.
     * @param lang The language.
     * @return The normalize analyzer name.
     */
    public String getNormalizeAnalyzerName(final String field, final String lang) {
        final Map<String, FieldAnalyzerMapping> fieldAnalyzerMapping = fieldAnalyzerMappingMap.get(analyzerSettingsIndexName);
        final Set<String> analyzerNames = analyzerMap.get(analyzerSettingsIndexName);
        final String analyzerName;
        if (StringUtil.isNotBlank(field) && fieldAnalyzerMapping.containsKey(field)
                && fieldAnalyzerMapping.get(field).normalizeAnalyzer != null) {
            analyzerName = fieldAnalyzerMapping.get(field).normalizeAnalyzer;
        } else {
            analyzerName = NORMALIZE_ANALYZER;
        }
        final String analyzerNameWithlang = isSupportedLanguage(lang) ? analyzerName + '_' + lang : analyzerName;
        if (analyzerNames.contains(analyzerNameWithlang)) {
            return analyzerNameWithlang;
        }
        return analyzerName;
    }

    /**
     * Returns the name of the contents analyzer for a given field and language.
     * @param field The field name.
     * @param lang The language.
     * @return The contents analyzer name.
     */
    public String getContentsAnalyzerName(final String field, final String lang) {
        final Map<String, FieldAnalyzerMapping> fieldAnalyzerMapping = fieldAnalyzerMappingMap.get(analyzerSettingsIndexName);
        final Set<String> analyzerNames = analyzerMap.get(analyzerSettingsIndexName);
        final String analyzerName;
        if (StringUtil.isNotBlank(field) && fieldAnalyzerMapping.containsKey(field)
                && fieldAnalyzerMapping.get(field).contentsAnalyzer != null) {
            analyzerName = fieldAnalyzerMapping.get(field).contentsAnalyzer;
        } else {
            analyzerName = CONTENTS_ANALYZER;
        }
        final String analyzerNameWithlang = isSupportedLanguage(lang) ? analyzerName + '_' + lang : analyzerName;
        if (analyzerNames.contains(analyzerNameWithlang)) {
            return analyzerNameWithlang;
        }
        return analyzerName;
    }

    /**
     * Returns the name of the contents reading analyzer for a given field and language.
     * @param field The field name.
     * @param lang The language.
     * @return The contents reading analyzer name.
     */
    public String getContentsReadingAnalyzerName(final String field, final String lang) {
        final Map<String, FieldAnalyzerMapping> fieldAnalyzerMapping = fieldAnalyzerMappingMap.get(analyzerSettingsIndexName);
        final Set<String> analyzerNames = analyzerMap.get(analyzerSettingsIndexName);
        final String analyzerName;
        if (StringUtil.isNotBlank(field) && fieldAnalyzerMapping.containsKey(field)
                && fieldAnalyzerMapping.get(field).contentsReadingAnalyzer != null) {
            analyzerName = fieldAnalyzerMapping.get(field).contentsReadingAnalyzer;
        } else {
            analyzerName = CONTENTS_READING_ANALYZER;
        }
        final String analyzerNameWithlang = isSupportedLanguage(lang) ? analyzerName + '_' + lang : analyzerName;
        if (analyzerNames.contains(analyzerNameWithlang)) {
            return analyzerNameWithlang;
        }
        return analyzerName;
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
        String pitId = null;
        try {
            // Create PIT
            final TimeValue keepAlive = TimeValue.parseTimeValue(settings.getPitKeepAlive(), "keep_alive");
            final CreatePitRequest createPitRequest = new CreatePitRequest(keepAlive, analyzerSettingsIndexName);
            final CreatePitResponse createPitResponse = client.execute(CreatePitAction.INSTANCE, createPitRequest)
                    .actionGet(settings.getSearchTimeout());
            pitId = createPitResponse.getId();

            try {
                while (true) {
                    // Search with PIT
                    final PointInTimeBuilder pointInTimeBuilder = new PointInTimeBuilder(pitId);
                    pointInTimeBuilder.setKeepAlive(keepAlive);

                    SearchResponse response = client.prepareSearch()
                            .setPointInTime(pointInTimeBuilder)
                            .setQuery(QueryBuilders.termQuery(FieldNames.ANALYZER_SETTINGS_TYPE, FIELD_ANALYZER_MAPPING))
                            .setSize(500)
                            .addSort(new FieldSortBuilder("_shard_doc").order(SortOrder.ASC))
                            .execute()
                            .actionGet(settings.getSearchTimeout());

                    final SearchHit[] hits = response.getHits().getHits();
                    if (hits.length == 0) {
                        break;
                    }
                    for (final SearchHit hit : hits) {
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
                    }
                }
            } finally {
                SuggestUtil.deletePitContext(client, pitId);
            }
        } catch (final Exception e) {
            SuggestUtil.deletePitContext(client, pitId);
            throw e;
        }
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
