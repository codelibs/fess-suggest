package org.codelibs.fess.suggest.settings;

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.client.Client;

import java.util.Map;

public class AnalyzerSettings {
    protected final Client client;
    protected final String analyzerSettingsIndexName;

    protected boolean initialized = false;

    public AnalyzerSettings(final Client client, final String settingsIndexName) {
        this.client = client;
        analyzerSettingsIndexName = createAnalyzerSettingsIndexName(settingsIndexName);
    }

    public void init() {
        IndicesExistsResponse response = client.admin().indices().prepareExists(analyzerSettingsIndexName).execute().actionGet();
        if (!response.isExists()) {

        }
    }

    public void updateAnalyzer(final Map<String, Object> settings) {

    }

    protected void deleteAnalyzerSettings() {
        client.admin().indices().prepareDelete(analyzerSettingsIndexName).execute().actionGet();
    }

    protected void createAnalyzerSettings(final Map<String, Object> settings) {
        client.admin().indices().prepareCreate(analyzerSettingsIndexName).execute().actionGet();
    }

    protected String createAnalyzerSettingsIndexName(final String settingsIndexName) {
        return settingsIndexName + ".analyzer";
    }
}
