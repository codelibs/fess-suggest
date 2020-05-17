/*
 * Copyright 2009-2019 the CodeLibs Project and the Others.
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

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.exception.SuggestSettingsException;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeAction;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

public class AnalyzerSettings {
    public static final String READING_ANALYZER = "reading_analyzer";
    public static final String READING_TERM_ANALYZER = "reading_term_analyzer";
    public static final String NORMALIZE_ANALYZER = "normalize_analyzer";
    public static final String CONTENTS_ANALYZER = "contents_analyzer";
    public static final String CONTENTS_READING_ANALYZER = "contents_reading_analyzer";
    public static final String FIELD_ANALYZER_MAPPING = "fieldAnalyzerMapping";

    public static final String DOC_TYPE_NAME = "_doc";

    protected final Client client;
    protected final String analyzerSettingsIndexName;
    private final SuggestSettings settings;

    protected static Map<String, Set<String>> analyzerMap = new ConcurrentHashMap<>();
    protected static Map<String, Map<String, FieldAnalyzerMapping>> fieldAnalyzerMappingMap = new ConcurrentHashMap<>();

    protected static final String[] SUPPORTED_LANGUAGES = new String[] { "ar", "bg", "bn", "ca", "cs", "da", "de", "el", "en", "es", "et",
            "fa", "fi", "fr", "gu", "he", "hi", "hr", "hu", "id", "it", "ja", "ko", "lt", "lv", "mk", "ml", "nl", "no", "pa", "pl", "pt",
            "ro", "ru", "si", "sq", "sv", "ta", "te", "th", "tl", "tr", "uk", "ur", "vi", "zh-cn", "zh-tw" };

    public AnalyzerSettings(final Client client, final SuggestSettings settings, final String settingsIndexName) {
        this.client = client;
        this.settings = settings;
        analyzerSettingsIndexName = createAnalyzerSettingsIndexName(settingsIndexName);
    }

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

    public String getAnalyzerSettingsIndexName() {
        return analyzerSettingsIndexName;
    }

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
        } else {
            return analyzerName;
        }
    }

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
        } else {
            return analyzerName;
        }
    }

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
        } else {
            return analyzerName;
        }
    }

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
        } else {
            return analyzerName;
        }
    }

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
        } else {
            return analyzerName;
        }
    }

    public void updateAnalyzer(final Map<String, Object> settings) {
        client.admin().indices().prepareCreate(analyzerSettingsIndexName).setSettings(settings).execute()
                .actionGet(this.settings.getIndicesTimeout());
    }

    protected void deleteAnalyzerSettings() {
        client.admin().indices().prepareDelete(analyzerSettingsIndexName).execute().actionGet(settings.getIndicesTimeout());
    }

    protected void createAnalyzerSettings(final String settings, final String mappings) {
        client.admin().indices().prepareCreate(analyzerSettingsIndexName).setSettings(settings, XContentType.JSON)
                .addMapping("_doc", mappings, XContentType.JSON).execute().actionGet(this.settings.getIndicesTimeout());
    }

    protected void createAnalyzerSettings(final Map<String, Object> settings, final Map<String, Object> mappings) {
        client.admin().indices().prepareCreate(analyzerSettingsIndexName).setSettings(settings).execute()
                .actionGet(this.settings.getIndicesTimeout());
    }

    protected String createAnalyzerSettingsIndexName(final String settingsIndexName) {
        return settingsIndexName + "_analyzer";
    }

    protected String loadIndexSettings() throws IOException {
        final String dictionaryPath = System.getProperty("fess.dictionary.path", StringUtil.EMPTY);
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("suggest_indices/suggest_analyzer.json")));) {

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString().replaceAll(Pattern.quote("${fess.dictionary.path}"), dictionaryPath);
    }

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

    public class DefaultContentsAnalyzer implements SuggestAnalyzer {

        private final int maxContentLenth = settings.getAsInt(SuggestSettings.DefaultKeys.MAX_CONTENT_LENGTH, 50000);

        @Override
        public List<AnalyzeToken> analyze(final String text, final String field, final String lang) {
            if (text == null || text.length() > maxContentLenth) {
                return Collections.emptyList();
            }
            final AnalyzeAction.Response analyzeResponse = client.admin().indices().prepareAnalyze(analyzerSettingsIndexName, text)
                    .setAnalyzer(getContentsAnalyzerName(field, lang)).execute().actionGet(settings.getIndicesTimeout());
            return analyzeResponse.getTokens();
        }

        @Override
        public List<AnalyzeToken> analyzeAndReading(final String text, final String field, final String lang) {
            try {
                final String contentsReadingAnalyzerName = getContentsReadingAnalyzerName(field, lang);
                if (StringUtil.isBlank(contentsReadingAnalyzerName)) {
                    return null;
                }
                final AnalyzeAction.Response analyzeResponse = client.admin().indices().prepareAnalyze(analyzerSettingsIndexName, text)
                        .setAnalyzer(contentsReadingAnalyzerName).execute().actionGet(settings.getIndicesTimeout());
                return analyzeResponse.getTokens();
            } catch (final IllegalArgumentException e) {
                return analyze(text, field, lang);
            }
        }
    }

    public static boolean isSupportedLanguage(final String lang) {
        return (StringUtil.isNotBlank(lang) && Stream.of(SUPPORTED_LANGUAGES).anyMatch(lang::equals));
    }

    protected Set<String> getAnalyzerNames() {
        final GetSettingsResponse response =
                client.admin().indices().prepareGetSettings().setIndices(analyzerSettingsIndexName).execute().actionGet();
        final Settings settings = response.getIndexToSettings().get(analyzerSettingsIndexName);
        final Settings analyzerSettings = settings.getAsSettings("index.analysis.analyzer");
        return analyzerSettings.getAsGroups().keySet();
    }

    protected Map<String, FieldAnalyzerMapping> getFieldAnalyzerMapping() {
        final Map<String, FieldAnalyzerMapping> mappingMap = new HashMap<>();
        SearchResponse response = client.prepareSearch(analyzerSettingsIndexName)
                .setQuery(QueryBuilders.termQuery(FieldNames.ANALYZER_SETTINGS_TYPE, FIELD_ANALYZER_MAPPING))
                .setScroll(settings.getScrollTimeout()).execute().actionGet(settings.getSearchTimeout());
        String scrollId = response.getScrollId();
        try {
            while (scrollId != null) {
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
                response = client.prepareSearchScroll(scrollId).setScroll(settings.getScrollTimeout()).execute()
                        .actionGet(settings.getSearchTimeout());
                if (!scrollId.equals(response.getScrollId())) {
                    SuggestUtil.deleteScrollContext(client, scrollId);
                }
                scrollId = response.getScrollId();
            }
        } finally {
            SuggestUtil.deleteScrollContext(client, scrollId);
        }
        return mappingMap;
    }

    public Set<String> checkAnalyzer() {
        final String text = "text";
        final Set<String> undefinedAnalyzerSet = new HashSet<>();
        for (final String lang : SUPPORTED_LANGUAGES) {
            final String readingAnalyzer = getReadingAnalyzerName("", lang);
            try {
                client.admin().indices().prepareAnalyze(analyzerSettingsIndexName, text).setAnalyzer(readingAnalyzer).execute()
                        .actionGet(settings.getIndicesTimeout());
            } catch (final IllegalArgumentException e) {
                undefinedAnalyzerSet.add(readingAnalyzer);
            }

            final String readingTermAnalyzer = getReadingTermAnalyzerName("", lang);
            try {
                client.admin().indices().prepareAnalyze(analyzerSettingsIndexName, text).setAnalyzer(readingTermAnalyzer).execute()
                        .actionGet(settings.getIndicesTimeout());
            } catch (final IllegalArgumentException e) {
                undefinedAnalyzerSet.add(readingTermAnalyzer);
            }

            final String normalizeAnalyzer = getNormalizeAnalyzerName("", lang);
            try {
                client.admin().indices().prepareAnalyze(analyzerSettingsIndexName, text).setAnalyzer(normalizeAnalyzer).execute()
                        .actionGet(settings.getIndicesTimeout());
            } catch (final IllegalArgumentException e) {
                undefinedAnalyzerSet.add(normalizeAnalyzer);
            }

            final String contentsAnalyzer = getContentsAnalyzerName("", lang);
            try {
                client.admin().indices().prepareAnalyze(analyzerSettingsIndexName, text).setAnalyzer(contentsAnalyzer).execute()
                        .actionGet(settings.getIndicesTimeout());
            } catch (final IllegalArgumentException e) {
                undefinedAnalyzerSet.add(contentsAnalyzer);
            }

            final String contentsReadingAnalyzer = getContentsReadingAnalyzerName("", lang);
            try {
                client.admin().indices().prepareAnalyze(analyzerSettingsIndexName, text).setAnalyzer(contentsReadingAnalyzer).execute()
                        .actionGet(settings.getIndicesTimeout());
            } catch (final IllegalArgumentException e) {
                undefinedAnalyzerSet.add(contentsReadingAnalyzer);
            }
        }

        return undefinedAnalyzerSet;
    }

    protected static class FieldAnalyzerMapping {
        protected final String readingAnalyzer;
        protected final String readingTermAnalyzer;
        protected final String normalizeAnalyzer;
        protected final String contentsAnalyzer;
        protected final String contentsReadingAnalyzer;

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
