package org.codelibs.fess.suggest.settings;

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.client.Client;

import java.io.*;
import java.util.Map;

public class AnalyzerSettings {
    public static final String readingAnalyzerName = "reading_analyzer";
    public static final String readingTermAnalyzerName = "reading_term_analyzer";
    public static final String normalizeAnalyzerName = "normalize_analyzer";

    protected final Client client;
    protected final String analyzerSettingsIndexName;

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

    public String getReadingAnalyzerName() {
        return readingAnalyzerName;
    }

    public String getReadingTermAnalyzerName() {
        return readingTermAnalyzerName;
    }

    public String getNormalizeAnalyzerName() {
        return normalizeAnalyzerName;
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
}
