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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.codelibs.fess.suggest.settings.SuggestSettings.TimeoutSettings;
import org.opensearch.transport.client.Client;

/**
 * Builder class for constructing SuggestSettings instances.
 */
public class SuggestSettingsBuilder {
    /**
     * Constructs a new {@link SuggestSettingsBuilder}.
     */
    public SuggestSettingsBuilder() {
        // nothing
    }

    /** The settings index name. */
    protected String settingsIndexName = "fess_suggest";

    /** The timeout settings. */
    protected TimeoutSettings timeoutSettings = new TimeoutSettings();

    /** The initial settings. */
    protected Map<String, Object> initialSettings = new HashMap<>();

    /**
     * Sets the settings index name.
     * @param settingsIndexName The settings index name.
     * @return This builder instance.
     */
    public SuggestSettingsBuilder setSettingsIndexName(final String settingsIndexName) {
        this.settingsIndexName = settingsIndexName.toLowerCase(Locale.ENGLISH);
        return this;
    }

    /**
     * Adds an initial setting.
     * @param key The key of the setting.
     * @param value The value of the setting.
     * @return This builder instance.
     */
    public SuggestSettingsBuilder addInitialSettings(final String key, final Object value) {
        initialSettings.put(key, value);
        return this;
    }

    /**
     * Sets the PIT keep alive duration.
     * @param keepAlive The PIT keep alive duration.
     * @return This builder instance.
     */
    public SuggestSettingsBuilder pitKeepAlive(final String keepAlive) {
        timeoutSettings.pitKeepAlive = keepAlive;
        return this;
    }

    /**
     * Sets the search timeout.
     * @param timeout The search timeout.
     * @return This builder instance.
     */
    public SuggestSettingsBuilder searchTimeout(final String timeout) {
        timeoutSettings.searchTimeout = timeout;
        return this;
    }

    /**
     * Sets the index timeout.
     * @param timeout The index timeout.
     * @return This builder instance.
     */
    public SuggestSettingsBuilder indexTimeout(final String timeout) {
        timeoutSettings.indexTimeout = timeout;
        return this;
    }

    /**
     * Sets the bulk timeout.
     * @param timeout The bulk timeout.
     * @return This builder instance.
     */
    public SuggestSettingsBuilder bulkTimeout(final String timeout) {
        timeoutSettings.bulkTimeout = timeout;
        return this;
    }

    /**
     * Sets the indices timeout.
     * @param timeout The indices timeout.
     * @return This builder instance.
     */
    public SuggestSettingsBuilder indicesTimeout(final String timeout) {
        timeoutSettings.indicesTimeout = timeout;
        return this;
    }

    /**
     * Sets the cluster timeout.
     * @param timeout The cluster timeout.
     * @return This builder instance.
     */
    public SuggestSettingsBuilder clusterTimeout(final String timeout) {
        timeoutSettings.clusterTimeout = timeout;
        return this;
    }

    /**
     * Builds a SuggestSettings instance.
     * @param client The OpenSearch client.
     * @param id The ID.
     * @return A SuggestSettings instance.
     */
    public SuggestSettings build(final Client client, final String id) {
        return new SuggestSettings(client, id, initialSettings, settingsIndexName, timeoutSettings);
    }
}
