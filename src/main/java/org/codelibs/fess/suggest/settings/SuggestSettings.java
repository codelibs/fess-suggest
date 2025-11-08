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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.exception.SuggestSettingsException;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.opensearch.action.get.GetResponse;
import org.opensearch.common.collect.Tuple;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.index.IndexNotFoundException;
import org.opensearch.transport.client.Client;

/**
 * The SuggestSettings class is responsible for managing the settings related to suggestions.
 * It interacts with an OpenSearch client to store and retrieve settings.
 *
 * <p>This class provides methods to initialize settings, get and set individual settings,
 * and manage various types of settings such as array settings, analyzer settings, bad word settings,
 * and elevate word settings.</p>
 *
 * <p>It also includes a nested TimeoutSettings class to manage various timeout configurations.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * Client client = ...;
 * Map<String, Object> initialSettings = ...;
 * SuggestSettings.TimeoutSettings timeoutSettings = new SuggestSettings.TimeoutSettings();
 * SuggestSettings suggestSettings = new SuggestSettings(client, "settingsId", initialSettings, "settingsIndexName", timeoutSettings);
 * suggestSettings.init();
 * }
 * </pre>
 *
 * <p>Key methods:</p>
 * <ul>
 *   <li>{@link #init()} - Initializes the settings.</li>
 *   <li>{@link #get(String)} - Retrieves a setting value by key.</li>
 *   <li>{@link #set(String, Object)} - Sets a setting value by key.</li>
 *   <li>{@link #array()} - Returns an instance of ArraySettings.</li>
 *   <li>{@link #analyzer()} - Returns an instance of AnalyzerSettings.</li>
 *   <li>{@link #badword()} - Returns an instance of BadWordSettings.</li>
 *   <li>{@link #elevateWord()} - Returns an instance of ElevateWordSettings.</li>
 * </ul>
 *
 * <p>Timeout settings can be accessed via:</p>
 * <ul>
 *   <li>{@link #getPitKeepAlive()}</li>
 *   <li>{@link #getSearchTimeout()}</li>
 *   <li>{@link #getIndexTimeout()}</li>
 *   <li>{@link #getIndicesTimeout()}</li>
 *   <li>{@link #getBulkTimeout()}</li>
 *   <li>{@link #getClusterTimeout()}</li>
 * </ul>
 *
 * <p>Default settings and array settings can be customized using:</p>
 * <ul>
 *   <li>{@link #defaultSettings()}</li>
 *   <li>{@link #defaultArraySettings()}</li>
 * </ul>
 *
 * <p>Index settings can be loaded from a JSON file using:</p>
 * <ul>
 *   <li>{@link #loadIndexSettings()}</li>
 * </ul>
 *
 * <p>A builder for SuggestSettings can be obtained using:</p>
 * <ul>
 *   <li>{@link #builder()}</li>
 * </ul>
 *
 * @see Client
 * @see GetResponse
 * @see IndexNotFoundException
 * @see SuggesterException
 * @see SuggestSettingsException
 * @see ArraySettings
 * @see AnalyzerSettings
 * @see BadWordSettings
 * @see ElevateWordSettings
 */
public class SuggestSettings {
    private static final Logger logger = LogManager.getLogger(SuggestSettings.class);

    /** The settings ID. */
    protected final String settingsId;

    /** The OpenSearch client. */
    protected final Client client;

    /** The settings index name. */
    protected final String settingsIndexName;

    /** The initial settings. */
    protected final Map<String, Object> initialSettings;

    /** Flag indicating if the settings are initialized. */
    protected boolean initialized = false;

    /** The bad word index name. */
    protected final String badWordIndexName;
    /** The elevate word index name. */
    protected final String elevateWordIndexName;

    /** The timeout settings. */
    protected TimeoutSettings timeoutSettings;

    /**
     * Timeout settings for various operations.
     */
    public static class TimeoutSettings {
        /**
         * Constructs a new {@link TimeoutSettings}.
         */
        public TimeoutSettings() {
            // nothing
        }

        /** Search timeout. */
        protected String searchTimeout = "15s";
        /** Index timeout. */
        protected String indexTimeout = "1m";
        /** Bulk timeout. */
        protected String bulkTimeout = "1m";
        /** Indices timeout. */
        protected String indicesTimeout = "1m";
        /** Cluster timeout. */
        protected String clusterTimeout = "1m";
        /** PIT keep alive duration. */
        protected String pitKeepAlive = "1m";
    }

    /**
     * Constructor for SuggestSettings.
     * @param client The OpenSearch client.
     * @param settingsId The settings ID.
     * @param initialSettings The initial settings.
     * @param settingsIndexName The settings index name.
     * @param timeoutSettings The timeout settings.
     */
    public SuggestSettings(final Client client, final String settingsId, final Map<String, Object> initialSettings,
            final String settingsIndexName, final TimeoutSettings timeoutSettings) {
        this.client = client;
        this.settingsId = settingsId;
        this.settingsIndexName = settingsIndexName;
        this.initialSettings = initialSettings;
        this.timeoutSettings = timeoutSettings;

        badWordIndexName = settingsIndexName + "-badword";
        elevateWordIndexName = settingsIndexName + "-elevateword";
    }

    /**
     * Initializes the settings.
     */
    public void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        initialize(initialSettings);
        new AnalyzerSettings(client, this, settingsIndexName).init();
    }

    private void initialize(final Map<String, Object> initialSettings) {
        boolean doIndexCreate = false;
        boolean doCreate = false;
        try {
            final GetResponse getResponse =
                    client.prepareGet().setIndex(settingsIndexName).setId(settingsId).execute().actionGet(getSearchTimeout());

            if (!getResponse.isExists()) {
                doCreate = true;
            }
        } catch (final IndexNotFoundException e) {
            doIndexCreate = true;
            doCreate = true;
        }

        if (doCreate) {
            if (doIndexCreate) {
                try {
                    client.admin()
                            .indices()
                            .prepareCreate(settingsIndexName)
                            .setSettings(loadIndexSettings(), XContentType.JSON)
                            .execute()
                            .actionGet(getIndicesTimeout());
                } catch (final IOException e) {
                    throw new SuggesterException(e);
                }
            }

            final List<Tuple<String, Object>> arraySettings = new ArrayList<>();
            final Map<String, Object> defaultSettings = defaultSettings();
            initialSettings.forEach((key, value) -> {
                if (value instanceof Collection) {
                    @SuppressWarnings("unchecked")
                    final Collection<Object> collection = (Collection<Object>) value;
                    collection.forEach(element -> arraySettings.add(new Tuple<>(key, element)));
                } else if (value instanceof Object[]) {
                    for (final Object element : (Object[]) value) {
                        arraySettings.add(new Tuple<>(key, element));
                    }
                } else {
                    defaultSettings.put(key, value);
                }
            });
            set(defaultSettings);

            final List<Tuple<String, Object>> defaultArraySettings = defaultArraySettings();
            defaultArraySettings.addAll(arraySettings);
            defaultArraySettings.forEach(t -> array().add(t.v1(), t.v2()));
        }
    }

    /**
     * Retrieves a setting value by key.
     * @param key The key of the setting.
     * @return The setting value, or null if not found.
     */
    public Object get(final String key) {
        final GetResponse getResponse =
                client.prepareGet().setIndex(settingsIndexName).setId(settingsId).execute().actionGet(getSearchTimeout());
        if (!getResponse.isExists()) {
            return null;
        }
        final Map<String, Object> map = getResponse.getSource();
        return map.get(key);
    }

    /**
     * Retrieves a setting value as a String.
     * @param key The key of the setting.
     * @param defaultValue The default value if the setting is not found.
     * @return The setting value as a String.
     */
    public String getAsString(final String key, final String defaultValue) {
        final Object obj = get(key);

        final String value;
        if (obj == null) {
            value = defaultValue;
        } else {
            value = obj.toString();
        }

        return value;
    }

    /**
     * Retrieves a setting value as an int.
     * @param key The key of the setting.
     * @param defaultValue The default value if the setting is not found.
     * @return The setting value as an int.
     */
    public int getAsInt(final String key, final int defaultValue) {
        final Object obj = get(key);

        final int value;
        if (obj == null) {
            value = defaultValue;
        } else {
            value = Integer.parseInt(obj.toString());
        }

        return value;
    }

    /**
     * Retrieves a setting value as a long.
     * @param key The key of the setting.
     * @param defaultValue The default value if the setting is not found.
     * @return The setting value as a long.
     */
    public long getAsLong(final String key, final long defaultValue) {
        final Object obj = get(key);

        final long value;
        if (obj == null) {
            value = defaultValue;
        } else {
            value = Long.parseLong(obj.toString());
        }

        return value;
    }

    /**
     * Retrieves a setting value as a float.
     * @param key The key of the setting.
     * @param defaultValue The default value if the setting is not found.
     * @return The setting value as a float.
     */
    public float getAsFloat(final String key, final float defaultValue) {
        final Object obj = get(key);

        final float value;
        if (obj == null) {
            value = defaultValue;
        } else {
            value = Float.parseFloat(obj.toString());
        }

        return value;
    }

    /**
     * Retrieves a setting value as a boolean.
     * @param key The key of the setting.
     * @param defaultValue The default value if the setting is not found.
     * @return The setting value as a boolean.
     */
    public boolean getAsBoolean(final String key, final boolean defaultValue) {
        final Object obj = get(key);

        final boolean value;
        if (obj == null) {
            value = defaultValue;
        } else {
            value = Boolean.parseBoolean(obj.toString());
        }

        return value;
    }

    /**
     * Sets a setting value.
     * @param key The key of the setting.
     * @param value The value to set.
     */
    public void set(final String key, final Object value) {
        if (logger.isDebugEnabled()) {
            logger.debug("Set suggest settings. {} key: {} value: {}", settingsIndexName, key, value);
        }
        try {
            client.prepareUpdate()
                    .setIndex(settingsIndexName)
                    .setId(settingsId)
                    .setDocAsUpsert(true)
                    .setDoc(key, value)
                    .setRetryOnConflict(5)
                    .execute()
                    .actionGet(getIndexTimeout());
            client.admin().indices().prepareRefresh().setIndices(settingsIndexName).execute().actionGet(getIndicesTimeout());
        } catch (final Exception e) {
            throw new SuggestSettingsException("Failed to update suggestSettings.", e);
        }
    }

    /**
     * Sets multiple settings from a map.
     * @param map The map of settings to set.
     */
    public void set(final Map<String, Object> map) {
        if (logger.isDebugEnabled()) {
            logger.debug("Set suggest settings. {} {}", settingsIndexName, map.toString());
        }
        try {
            final XContentBuilder builder = JsonXContent.contentBuilder().map(map);
            builder.flush();
            client.prepareUpdate()
                    .setIndex(settingsIndexName)
                    .setId(settingsId)
                    .setDocAsUpsert(true)
                    .setDoc(builder)
                    .setRetryOnConflict(5)
                    .execute()
                    .actionGet(getIndexTimeout());
            client.admin().indices().prepareRefresh().setIndices(settingsIndexName).execute().actionGet(getIndicesTimeout());
        } catch (final Exception e) {
            throw new SuggestSettingsException("Failed to update suggestSettings.", e);
        }
    }

    /**
     * Returns an ArraySettings instance.
     * @return An ArraySettings instance.
     */
    public ArraySettings array() {
        return new ArraySettings(this, client, settingsIndexName, settingsId);
    }

    /**
     * Returns an AnalyzerSettings instance.
     * @return An AnalyzerSettings instance.
     */
    public AnalyzerSettings analyzer() {
        return new AnalyzerSettings(client, this, settingsIndexName);
    }

    /**
     * Returns a BadWordSettings instance.
     * @return A BadWordSettings instance.
     */
    public BadWordSettings badword() {
        return new BadWordSettings(this, client, settingsIndexName, settingsId);
    }

    /**
     * Returns an ElevateWordSettings instance.
     * @return An ElevateWordSettings instance.
     */
    public ElevateWordSettings elevateWord() {
        return new ElevateWordSettings(this, client, settingsIndexName, settingsId);
    }

    /**
     * Returns a SuggestSettingsBuilder instance.
     * @return A SuggestSettingsBuilder instance.
     */
    public static SuggestSettingsBuilder builder() {
        return new SuggestSettingsBuilder();
    }

    /**
     * Returns the PIT keep alive duration.
     * @return The PIT keep alive duration.
     */
    public String getPitKeepAlive() {
        return timeoutSettings.pitKeepAlive;
    }

    /**
     * Returns the search timeout.
     * @return The search timeout.
     */
    public String getSearchTimeout() {
        return timeoutSettings.searchTimeout;
    }

    /**
     * Returns the index timeout.
     * @return The index timeout.
     */
    public String getIndexTimeout() {
        return timeoutSettings.indexTimeout;
    }

    /**
     * Returns the indices timeout.
     * @return The indices timeout.
     */
    public String getIndicesTimeout() {
        return timeoutSettings.indicesTimeout;
    }

    /**
     * Returns the bulk timeout.
     * @return The bulk timeout.
     */
    public String getBulkTimeout() {
        return timeoutSettings.bulkTimeout;
    }

    /**
     * Returns the cluster timeout.
     * @return The cluster timeout.
     */
    public String getClusterTimeout() {
        return timeoutSettings.clusterTimeout;
    }

    /**
     * Returns the settings ID.
     * @return The settings ID.
     */
    public String getSettingsId() {
        return settingsId;
    }

    /**
     * Returns the settings index name.
     * @return The settings index name.
     */
    public String getSettingsIndexName() {
        return settingsIndexName;
    }

    /**
     * Checks if the settings are initialized.
     * @return True if initialized, false otherwise.
     */
    public boolean isInitialized() {
        return initialized;
    }

    private Map<String, Object> defaultSettings() {
        final Map<String, Object> defaultSettings = new HashMap<>();
        defaultSettings.put(DefaultKeys.INDEX, (settingsId + ".suggest").toLowerCase());
        defaultSettings.put(DefaultKeys.TAG_FIELD_NAME, "label,virtual_host");
        defaultSettings.put(DefaultKeys.ROLE_FIELD_NAME, "role");
        defaultSettings.put(DefaultKeys.LANG_FIELD_NAME, "lang");
        defaultSettings.put(DefaultKeys.PARALLEL_PROCESSING, Boolean.FALSE);
        return defaultSettings;
    }

    private List<Tuple<String, Object>> defaultArraySettings() {
        final List<Tuple<String, Object>> tuples = new ArrayList<>();
        tuples.add(new Tuple<>(DefaultKeys.SUPPORTED_FIELDS, "content"));
        return tuples;
    }

    /**
     * Loads the index settings from a resource file.
     * @return The index settings as a string.
     * @throws IOException If an I/O error occurs.
     */
    protected String loadIndexSettings() throws IOException {
        final String dictionaryPath = System.getProperty("fess.dictionary.path", StringUtil.EMPTY);
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("suggest_indices/suggest_settings.json")));) {

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString().replaceAll(Pattern.quote("${fess.dictionary.path}"), dictionaryPath);
    }

    /**
     * Default keys for suggest settings.
     */
    public static class DefaultKeys {
        /** Index key. */
        public static final String INDEX = "index";
        /** Supported fields key. */
        public static final String SUPPORTED_FIELDS = "supportedFields";
        /** Tag field name key. */
        public static final String TAG_FIELD_NAME = "tagFieldName";
        /** Role field name key. */
        public static final String ROLE_FIELD_NAME = "roleFieldName";
        /** Language field name key. */
        public static final String LANG_FIELD_NAME = "langFieldName";
        /** Parallel processing key. */
        public static final String PARALLEL_PROCESSING = "parallel";
        /** Max content length key. */
        public static final String MAX_CONTENT_LENGTH = "maxContextLength";

        private DefaultKeys() {
        }
    }
}
