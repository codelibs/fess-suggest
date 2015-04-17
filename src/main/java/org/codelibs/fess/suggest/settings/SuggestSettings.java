package org.codelibs.fess.suggest.settings;

import org.codelibs.fess.suggest.exception.SuggestSettingsException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.indices.IndexMissingException;

import java.util.*;

public class SuggestSettings {
    protected final String settingsId;

    protected final Client client;

    protected final String settingsIndexName;

    protected final String settingsTypeName;

    protected final Map<String, Object> initialSettings;

    protected boolean initialized = false;

    protected final String ngWordIndexName;
    protected final String elevateWordIndexName;
    protected final String arraySettingsIndexName;

    public SuggestSettings(final Client client, final String settingsId, final Map<String, Object> initialSettings,
            final String settingsIndexName, final String settingsTypeName) {
        this.client = client;
        this.settingsId = settingsId;
        this.settingsIndexName = settingsIndexName;
        this.settingsTypeName = settingsTypeName;
        this.initialSettings = initialSettings;

        this.ngWordIndexName = settingsIndexName + "-ngword";
        this.elevateWordIndexName = settingsIndexName + "-elevateword";
        this.arraySettingsIndexName = settingsIndexName + "-array";
    }

    public void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        initialize(initialSettings);
    }

    private void initialize(Map<String, Object> initialSettings) {
        boolean doCreate = false;
        try {
            GetResponse getResponse =
                    client.prepareGet().setIndex(settingsIndexName).setType(settingsTypeName).setId(settingsId).execute().actionGet();

            if (!getResponse.isExists()) {
                doCreate = true;
            }
        } catch (IndexMissingException e) {
            doCreate = true;
        }

        if (doCreate) {
            List<Tuple<String, Object>> arraySettings = new ArrayList<>();

            Map<String, Object> defaultSettings = defaultSettings();
            for (String key : initialSettings.keySet()) {
                Object value = initialSettings.get(key);
                if (value instanceof Collection) {
                    ((Collection<Object>) value).forEach(element -> arraySettings.add(new Tuple<>(key, element)));
                } else if (value instanceof Object[]) {
                    for (Object element : (Object[]) value) {
                        arraySettings.add(new Tuple<>(key, element));
                    }
                } else {
                    defaultSettings.put(key, initialSettings.get(key));
                }
            }
            set(defaultSettings);

            List<Tuple<String, Object>> defaultArraySettings = defaultArraySettings();
            defaultArraySettings.addAll(arraySettings);
            defaultArraySettings.forEach(t -> array().add(t.v1(), t.v2()));
        }
    }

    public Object get(String key) {
        GetResponse getResponse =
                client.prepareGet().setIndex(settingsIndexName).setType(settingsTypeName).setId(settingsId).execute().actionGet();
        if (!getResponse.isExists()) {
            return null;
        }
        Map<String, Object> map = getResponse.getSource();
        return map.get(key);
    }

    public String getAsString(String key, String defaultValue) {
        final Object obj = get(key);

        final String value;
        if (obj == null) {
            value = defaultValue;
        } else {
            value = obj.toString();
        }

        return value;
    }

    public int getAsInt(String key, int defaultValue) {
        final Object obj = get(key);

        final int value;
        if (obj == null) {
            value = defaultValue;
        } else {
            value = Integer.parseInt(obj.toString());
        }

        return value;
    }

    public long getAsLong(String key, long defaultValue) {
        final Object obj = get(key);

        final long value;
        if (obj == null) {
            value = defaultValue;
        } else {
            value = Long.parseLong(obj.toString());
        }

        return value;
    }

    public float getAsFloat(String key, float defaultValue) {
        final Object obj = get(key);

        final float value;
        if (obj == null) {
            value = defaultValue;
        } else {
            value = Float.parseFloat(obj.toString());
        }

        return value;
    }

    public boolean getAsBoolean(String key, boolean defaultValue) {
        final Object obj = get(key);

        final boolean value;
        if (obj == null) {
            value = defaultValue;
        } else {
            value = Boolean.parseBoolean(obj.toString());
        }

        return value;
    }

    public void set(String key, Object value) {
        try {
            client.prepareUpdate().setIndex(settingsIndexName).setType(settingsTypeName).setId(settingsId).setDocAsUpsert(true)
                    .setDoc(key, value).setRefresh(true).setRetryOnConflict(5).execute().actionGet();
        } catch (Exception e) {
            throw new SuggestSettingsException("Failed to update settings.", e);
        }
    }

    public void set(Map<String, Object> map) {
        try {
            client.prepareUpdate().setIndex(settingsIndexName).setType(settingsTypeName).setId(settingsId).setDocAsUpsert(true)
                    .setRefresh(true).setDoc(JsonXContent.contentBuilder().map(map)).setRetryOnConflict(5).execute().actionGet();
        } catch (Exception e) {
            throw new SuggestSettingsException("Failed to update settings.", e);
        }
    }

    public ArraySettings array() {
        return new ArraySettings(client, settingsIndexName, settingsId);
    }

    public NgWordSettings ngword() {
        return new NgWordSettings(client, settingsIndexName, settingsId);
    }

    public ElevateWordSettings elevateWord() {
        return new ElevateWordSettings(client, settingsIndexName, settingsId);
    }

    public String getSettingsIndexName() {
        return settingsIndexName;
    }

    public String getSettingsTypeName() {
        return settingsTypeName;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public String getSettingsId() {
        return settingsId;
    }

    private Map<String, Object> defaultSettings() {
        Map<String, Object> defaultSettings = new HashMap<>();
        defaultSettings.put(DefaultKeys.INDEX, (settingsId + "-suggest").toLowerCase());
        defaultSettings.put(DefaultKeys.TYPE, "doc");
        defaultSettings.put(DefaultKeys.SUPPORTED_FIELDS, new String[] { "content" });
        defaultSettings.put(DefaultKeys.TAG_FIELD_NAME, "label");
        defaultSettings.put(DefaultKeys.ROLE_FIELD_NAME, "role");
        return defaultSettings;
    }

    private List<Tuple<String, Object>> defaultArraySettings() {
        List<Tuple<String, Object>> tuples = new ArrayList<>();
        tuples.add(new Tuple<>(DefaultKeys.SUPPORTED_FIELDS, "content"));
        return tuples;
    }

    public static SuggestSettingsBuilder builder() {
        return new SuggestSettingsBuilder();
    }

    public static class DefaultKeys {
        public static final String INDEX = "index";
        public static final String TYPE = "type";
        public static final String SUPPORTED_FIELDS = "supportedFields";
        public static final String TAG_FIELD_NAME = "tagFieldName";
        public static final String ROLE_FIELD_NAME = "roleFieldName";
    }
}
