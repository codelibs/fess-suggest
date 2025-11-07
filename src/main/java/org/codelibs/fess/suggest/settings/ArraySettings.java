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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.CoreLibConstants;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.exception.SuggestSettingsException;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.action.search.CreatePitAction;
import org.opensearch.action.search.CreatePitRequest;
import org.opensearch.action.search.CreatePitResponse;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.index.IndexNotFoundException;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.PointInTimeBuilder;
import org.opensearch.search.sort.FieldSortBuilder;
import org.opensearch.search.sort.SortOrder;
import org.opensearch.transport.client.Client;

/**
 * The ArraySettings class provides methods to manage settings stored in an array format within an OpenSearch index.
 * It allows adding, retrieving, and deleting settings based on keys and values.
 *
 * <p>Constructor:
 * <ul>
 * <li>{@link #ArraySettings(SuggestSettings, Client, String, String)}: Initializes the ArraySettings with the provided settings, client, index name, and settings ID.</li>
 * </ul>
 *
 * <p>Public Methods:
 * <ul>
 * <li>{@link #get(String)}: Retrieves an array of values associated with the specified key.</li>
 * <li>{@link #add(String, Object)}: Adds a key-value pair to the settings array.</li>
 * <li>{@link #delete(String)}: Deletes all entries associated with the specified key.</li>
 * <li>{@link #delete(String, String)}: Deletes a specific key-value pair from the settings array.</li>
 * </ul>
 *
 * <p>Protected Methods:
 * <ul>
 * <li>{@link #createArraySettingsIndexName(String)}: Creates the name for the array settings index.</li>
 * <li>{@link #createId(String, Object)}: Creates a unique ID for a key-value pair using Base64 encoding.</li>
 * <li>{@link #getFromArrayIndex(String, String, String)}: Retrieves an array of maps from the index based on the key.</li>
 * <li>{@link #addToArrayIndex(String, String, String, Map)}: Adds a map to the array index.</li>
 * <li>{@link #deleteKeyFromArray(String, String, String)}: Deletes all entries associated with the specified key from the array index.</li>
 * <li>{@link #deleteFromArray(String, String, String)}: Deletes a specific entry from the array index based on the ID.</li>
 * <li>{@link #createMappingIfEmpty(String, String, Client)}: Creates the index mapping if it does not exist.</li>
 * <li>{@link #loadIndexSettings()}: Loads the index settings from a JSON file.</li>
 * </ul>
 *
 * <p>Fields:
 * <ul>
 * <li>{@link #logger}: Logger instance for logging debug information.</li>
 * <li>{@link #client}: OpenSearch client instance.</li>
 * <li>{@link #arraySettingsIndexName}: Name of the array settings index.</li>
 * <li>{@link #settingsId}: ID of the settings.</li>
 * <li>{@link #settings}: SuggestSettings instance containing configuration settings.</li>
 * <li>{@link #encoder}: Base64 encoder instance for encoding IDs.</li>
 * </ul>
 */
public class ArraySettings {
    private static final Logger logger = LogManager.getLogger(ArraySettings.class);

    /** OpenSearch client. */
    protected final Client client;
    /** Array settings index name. */
    protected final String arraySettingsIndexName;
    /** Settings ID. */
    protected final String settingsId;
    /** Suggest settings. */
    protected final SuggestSettings settings;

    private static final Base64.Encoder encoder = Base64.getEncoder();

    /**
     * Constructor.
     * @param settings Suggest settings
     * @param client OpenSearch client
     * @param settingsIndexName Settings index name
     * @param settingsId Settings ID
     */
    protected ArraySettings(final SuggestSettings settings, final Client client, final String settingsIndexName, final String settingsId) {
        this.settings = settings;
        this.client = client;
        arraySettingsIndexName = createArraySettingsIndexName(settingsIndexName);
        this.settingsId = settingsId;
        createMappingIfEmpty(arraySettingsIndexName, settingsId, client);
    }

    /**
     * Get values.
     * @param key Key
     * @return Values
     */
    public String[] get(final String key) {
        final Map<String, Object> sourceArray[] = getFromArrayIndex(arraySettingsIndexName, settingsId, key);

        final String[] valueArray = new String[sourceArray.length];
        for (int i = 0; i < valueArray.length; i++) {
            final Object value = sourceArray[i].get(FieldNames.ARRAY_VALUE);
            if (value != null) {
                valueArray[i] = value.toString();
            }
        }
        return valueArray;
    }

    /**
     * Add a value.
     * @param key Key
     * @param value Value
     */
    public void add(final String key, final Object value) {
        if (logger.isDebugEnabled()) {
            logger.debug("Add analyzer settings. {} key: {} value: {}", arraySettingsIndexName, key, value);
        }

        final Map<String, Object> source = new HashMap<>();
        source.put(FieldNames.ARRAY_KEY, key);
        source.put(FieldNames.ARRAY_VALUE, value);
        source.put(FieldNames.TIMESTAMP, DateTimeFormatter.ISO_INSTANT.format(ZonedDateTime.now()));

        addToArrayIndex(arraySettingsIndexName, settingsId, createId(key, value), source);
    }

    /**
     * Delete values.
     * @param key Key
     */
    public void delete(final String key) {
        deleteKeyFromArray(arraySettingsIndexName, settingsId, key);
    }

    /**
     * Delete a value.
     * @param key Key
     * @param value Value
     */
    public void delete(final String key, final String value) {
        if (logger.isDebugEnabled()) {
            logger.debug("Delete analyzer settings. {} key: {} value: {}", arraySettingsIndexName, key, value);
        }
        deleteFromArray(arraySettingsIndexName, settingsId, createId(key, value));
    }

    /**
     * Create array settings index name.
     * @param settingsIndexName Settings index name
     * @return Array settings index name
     */
    protected String createArraySettingsIndexName(final String settingsIndexName) {
        return settingsIndexName + "_array";
    }

    /**
     * Create ID.
     * @param key Key
     * @param value Value
     * @return ID
     */
    protected String createId(final String key, final Object value) {
        return encoder.encodeToString(("key:" + key + "value:" + value).getBytes(CoreLibConstants.CHARSET_UTF_8));
    }

    /**
     * Get values from array index.
     * @param index Index
     * @param type Type
     * @param key Key
     * @return Values
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object>[] getFromArrayIndex(final String index, final String type, final String key) {
        final String actualIndex = index + "." + type.toLowerCase(Locale.ENGLISH);
        String pitId = null;
        try {
            // Create PIT
            final TimeValue keepAlive = TimeValue.parseTimeValue(settings.getPitKeepAlive(), "keep_alive");
            final CreatePitRequest createPitRequest = new CreatePitRequest(keepAlive, true, actualIndex);
            final CreatePitResponse createPitResponse = client.execute(CreatePitAction.INSTANCE, createPitRequest)
                    .actionGet(settings.getSearchTimeout());
            pitId = createPitResponse.getId();

            // Get total count first
            SearchResponse countResponse = client.prepareSearch()
                    .setIndices(actualIndex)
                    .setQuery(QueryBuilders.termQuery(FieldNames.ARRAY_KEY, key))
                    .setSize(0)
                    .setTrackTotalHits(true)
                    .execute()
                    .actionGet(settings.getSearchTimeout());
            final Map<String, Object>[] array = new Map[(int) countResponse.getHits().getTotalHits().value()];

            int count = 0;
            try {
                while (true) {
                    // Search with PIT
                    final PointInTimeBuilder pointInTimeBuilder = new PointInTimeBuilder(pitId);
                    pointInTimeBuilder.setKeepAlive(keepAlive);

                    SearchResponse response = client.prepareSearch()
                            .setPointInTime(pointInTimeBuilder)
                            .setQuery(QueryBuilders.termQuery(FieldNames.ARRAY_KEY, key))
                            .setSize(500)
                            .addSort(new FieldSortBuilder("_shard_doc").order(SortOrder.ASC))
                            .execute()
                            .actionGet(settings.getSearchTimeout());

                    final SearchHit[] hits = response.getHits().getHits();
                    if (hits.length == 0) {
                        break;
                    }
                    for (final SearchHit hit : hits) {
                        array[count] = hit.getSourceAsMap();
                        count++;
                    }
                }
            } finally {
                SuggestUtil.deletePitContext(client, pitId);
            }

            Arrays.sort(array, (o1, o2) -> {
                if (o1 == null && o2 == null) {
                    return 0;
                }
                if (o1 == null) {
                    return -1;
                }
                if (o2 == null) {
                    return 1;
                }

                final Object timeObj1 = o1.get(FieldNames.TIMESTAMP);
                final Object timeObj2 = o2.get(FieldNames.TIMESTAMP);
                if (timeObj1 == null && timeObj2 == null) {
                    return 0;
                }
                if (timeObj1 == null) {
                    return -1;
                }
                if (timeObj2 == null) {
                    return 1;
                }

                return o1.toString().compareTo(o2.toString());
            });
            return array;
        } catch (final IndexNotFoundException e) {
            return new Map[0];
        }
    }

    /**
     * Add a value to array index.
     * @param index Index
     * @param type Type
     * @param id ID
     * @param source Source
     */
    protected void addToArrayIndex(final String index, final String type, final String id, final Map<String, Object> source) {
        final String actualIndex = index + "." + type.toLowerCase(Locale.ENGLISH);
        try {
            final XContentBuilder builder = JsonXContent.contentBuilder().map(source);
            builder.flush();
            client.prepareUpdate()
                    .setIndex(actualIndex)
                    .setId(id)
                    .setDocAsUpsert(true)
                    .setDoc(builder)
                    .execute()
                    .actionGet(settings.getIndexTimeout());
            client.admin().indices().prepareRefresh().setIndices(actualIndex).execute().actionGet(settings.getIndicesTimeout());
        } catch (final Exception e) {
            throw new SuggestSettingsException("Failed to add to array.", e);
        }
    }

    /**
     * Delete values from array index.
     * @param index Index
     * @param type Type
     * @param key Key
     */
    protected void deleteKeyFromArray(final String index, final String type, final String key) {
        final String actualIndex = index + "." + type.toLowerCase(Locale.ENGLISH);
        try {
            SuggestUtil.deleteByQuery(client, settings, actualIndex, QueryBuilders.termQuery(FieldNames.ARRAY_KEY, key));
        } catch (final Exception e) {
            throw new SuggestSettingsException("Failed to delete all from array.", e);
        }
    }

    /**
     * Delete a value from array index.
     * @param index Index
     * @param type Type
     * @param id ID
     */
    protected void deleteFromArray(final String index, final String type, final String id) {
        final String actualIndex = index + "." + type.toLowerCase(Locale.ENGLISH);
        try {
            client.prepareDelete().setIndex(actualIndex).setId(id).execute().actionGet(settings.getIndexTimeout());
            client.admin().indices().prepareRefresh().setIndices(actualIndex).execute().actionGet(settings.getIndicesTimeout());
        } catch (final Exception e) {
            throw new SuggestSettingsException("Failed to delete from array.", e);
        }
    }

    /**
     * Create mapping if empty.
     * @param index Index
     * @param type Type
     * @param client OpenSearch client
     */
    protected void createMappingIfEmpty(final String index, final String type, final Client client) {
        final String actualIndex = index + "." + type.toLowerCase(Locale.ENGLISH);
        try {
            boolean empty;
            try {
                empty = client.admin()
                        .indices()
                        .prepareGetMappings(actualIndex)
                        .execute()
                        .actionGet(settings.getIndicesTimeout())
                        .getMappings()
                        .isEmpty();
            } catch (final IndexNotFoundException e) {
                empty = true;
                final CreateIndexResponse response = client.admin()
                        .indices()
                        .prepareCreate(actualIndex)
                        .setSettings(loadIndexSettings(), XContentType.JSON)
                        .execute()
                        .actionGet(settings.getIndicesTimeout());
                if (!response.isAcknowledged()) {
                    throw new SuggestSettingsException("Failed to create " + actualIndex + "/" + type + " index.", e);
                }
                client.admin()
                        .cluster()
                        .prepareHealth(actualIndex)
                        .setWaitForYellowStatus()
                        .execute()
                        .actionGet(settings.getClusterTimeout());
            }
            if (empty) {
                client.admin()
                        .indices()
                        .preparePutMapping(actualIndex)
                        .setSource(XContentFactory.jsonBuilder()
                                .startObject()
                                .startObject("properties")
                                .startObject(FieldNames.ARRAY_KEY)
                                .field("type", "keyword")
                                .endObject()
                                .endObject()
                                .endObject())
                        .execute()
                        .actionGet(settings.getIndicesTimeout());
            }
        } catch (final IOException e) {
            throw new SuggestSettingsException("Failed to create mappings.");
        }
    }

    /**
     * Load index settings.
     * @return Index settings
     * @throws IOException I/O exception
     */
    protected String loadIndexSettings() throws IOException {
        final String dictionaryPath = System.getProperty("fess.dictionary.path", StringUtil.EMPTY);
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                this.getClass().getClassLoader().getResourceAsStream("suggest_indices/suggest_settings_array.json")))) {

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString().replaceAll(Pattern.quote("${fess.dictionary.path}"), dictionaryPath);
    }

}
