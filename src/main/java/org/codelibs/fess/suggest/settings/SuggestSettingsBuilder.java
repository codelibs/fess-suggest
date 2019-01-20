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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.codelibs.fess.suggest.settings.SuggestSettings.TimeoutSettings;
import org.elasticsearch.client.Client;

public class SuggestSettingsBuilder {
    protected String settingsIndexName = ".suggest";

    protected String settingsTypeName = "suggestSettings";

    protected TimeoutSettings timeoutSettings = new TimeoutSettings();

    protected Map<String, Object> initialSettings = new HashMap<>();

    public SuggestSettingsBuilder setSettingsIndexName(final String settingsIndexName) {
        this.settingsIndexName = settingsIndexName.toLowerCase(Locale.ENGLISH);
        return this;
    }

    public SuggestSettingsBuilder setSettingsTypeName(final String settingsTypeName) {
        this.settingsTypeName = settingsTypeName.toLowerCase(Locale.ENGLISH);
        return this;
    }

    public SuggestSettingsBuilder addInitialSettings(final String key, final Object value) {
        initialSettings.put(key, value);
        return this;
    }

    public SuggestSettingsBuilder scrollTimeout(final String timeout) {
        timeoutSettings.scrollTimeout = timeout;
        return this;
    }

    public SuggestSettingsBuilder searchTimeout(final String timeout) {
        timeoutSettings.searchTimeout = timeout;
        return this;
    }

    public SuggestSettingsBuilder indexTimeout(final String timeout) {
        timeoutSettings.indexTimeout = timeout;
        return this;
    }

    public SuggestSettingsBuilder bulkTimeout(final String timeout) {
        timeoutSettings.bulkTimeout = timeout;
        return this;
    }

    public SuggestSettingsBuilder indicesTimeout(final String timeout) {
        timeoutSettings.indicesTimeout = timeout;
        return this;
    }

    public SuggestSettingsBuilder clusterTimeout(final String timeout) {
        timeoutSettings.clusterTimeout = timeout;
        return this;
    }

    public SuggestSettings build(final Client client, final String id) {
        return new SuggestSettings(client, id, initialSettings, settingsIndexName, settingsTypeName, timeoutSettings);
    }
}
