package org.codelibs.fess.suggest.settings;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.client.Client;

public class SuggestSettingsBuilder {
    protected String settingsIndexName = ".suggest";

    protected String settingsTypeName = "suggestSettings";

    protected Map<String, Object> initialSettings = new HashMap<>();

    public SuggestSettingsBuilder() {
    }

    public SuggestSettingsBuilder setSettingsIndexName(final String settingsIndexName) {
        this.settingsIndexName = settingsIndexName.toLowerCase();
        return this;
    }

    public SuggestSettingsBuilder setSettingsTypeName(final String settingsTypeName) {
        this.settingsTypeName = settingsTypeName.toLowerCase();
        return this;
    }

    public SuggestSettingsBuilder addInitialSettings(final String key, final Object value) {
        initialSettings.put(key, value);
        return this;
    }

    public SuggestSettings build(final Client client, final String id) {
        return new SuggestSettings(client, id, initialSettings, settingsIndexName, settingsTypeName);
    }
}
