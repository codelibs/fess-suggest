package org.codelibs.fess.suggest.settings;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.client.Client;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class AnalyzerSettings {
    public static final String readingAnalyzerName = "reading_analyzer";
    public static final String readingTermAnalyzerName = "reading_term_analyzer";
    public static final String normalizeAnalyzerName = "normalize_analyzer";
    public static final String contentsAnalyzerName = "contents_analyzer";

    protected final Client client;
    protected final String analyzerSettingsIndexName;

    public static final String[] SUPPORTED_LANGUAGES = new String[] { "en", "ja" };

    public AnalyzerSettings(final Client client, final String settingsIndexName) {
        this.client = client;
        analyzerSettingsIndexName = createAnalyzerSettingsIndexName(settingsIndexName);
    }

    public void init() {
        try {
            IndicesExistsResponse response = client.admin().indices().prepareExists(analyzerSettingsIndexName).execute().actionGet();
            if (!response.isExists()) {
                createAnalyzerSettings(defaultAnalyzerSettings());
            }
        } catch (IOException e) {
            //TODO
        }
    }

    public String getAnalyzerSettingsIndexName() {
        return analyzerSettingsIndexName;
    }

    public String getReadingAnalyzerName(final String lang) {
        return isSupportedLanguage(lang) ? readingAnalyzerName + '_' + lang : readingAnalyzerName;
    }

    public String getReadingTermAnalyzerName(final String lang) {
        return isSupportedLanguage(lang) ? readingTermAnalyzerName + '_' + lang : readingTermAnalyzerName;
    }

    public String getNormalizeAnalyzerName(final String lang) {
        return isSupportedLanguage(lang) ? normalizeAnalyzerName + '_' + lang : normalizeAnalyzerName;
    }

    public String getContentsAnalyzerName(final String lang) {
        return isSupportedLanguage(lang) ? contentsAnalyzerName + '_' + lang : contentsAnalyzerName;
    }

    public void updateAnalyzer(final Map<String, Object> settings) {
        client.admin().indices().prepareCreate(analyzerSettingsIndexName).setSettings(settings).execute().actionGet();
    }

    protected void deleteAnalyzerSettings() {
        client.admin().indices().prepareDelete(analyzerSettingsIndexName).execute().actionGet();
    }

    protected void createAnalyzerSettings(final String settings) {
        client.admin().indices().prepareCreate(analyzerSettingsIndexName).setSettings(settings).execute().actionGet();
    }

    protected void createAnalyzerSettings(final Map<String, Object> settings) {
        client.admin().indices().prepareCreate(analyzerSettingsIndexName).setSettings(settings).execute().actionGet();
    }

    protected String createAnalyzerSettingsIndexName(final String settingsIndexName) {
        return settingsIndexName + ".analyzer";
    }

    protected String defaultAnalyzerSettings() throws IOException {
        BufferedReader br = null;
        final StringBuilder sb = new StringBuilder();
        try {
            br =
                    new BufferedReader(new InputStreamReader(this.getClass().getClassLoader()
                            .getResourceAsStream("fess-suggest-default-analyzer.json")));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }
        return sb.toString();
    }

    public class DefaultContentsAnalyzer implements SuggestAnalyzer {
        public List<AnalyzeResponse.AnalyzeToken> analyze(final String text, final String lang) {
            final AnalyzeResponse analyzeResponse =
                    client.admin().indices().prepareAnalyze(analyzerSettingsIndexName, text).setAnalyzer(getContentsAnalyzerName(lang))
                            .execute().actionGet();
            return analyzeResponse.getTokens();
        }
    }

    public static boolean isSupportedLanguage(final String lang) {
        return (StringUtil.isNotBlank(lang) && Stream.of(SUPPORTED_LANGUAGES).anyMatch(lang::equals));
    }
}
