package org.codelibs.fess.suggest.settings;

import org.codelibs.fess.suggest.exception.SuggesterException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.indices.IndexMissingException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SuggestSettings {
    protected final String id;

    protected final Client client;

    protected final String settingsIndexName;

    protected final String settingsTypeName;

    protected final Map<String, Object> initialSettings;

    protected boolean initialized = false;


    public SuggestSettings(final Client client,
                           final String id,
                           final Map<String, Object> initialSettings,
                           final String settingsIndexName,
                           final String settingsTypeName) {
        this.client = client;
        this.id = id;
        this.settingsIndexName = settingsIndexName;
        this.settingsTypeName = settingsTypeName;
        this.initialSettings = initialSettings;
    }

    public void init() {
        if(initialized) {
            return;
        }
        initialized = true;
        initialize(initialSettings);
    }

    private void initialize(Map<String, Object> initialSettings) {
        boolean doCreate = false;
        try {
            GetResponse getResponse = client.prepareGet()
                .setIndex(settingsIndexName)
                .setType(settingsTypeName)
                .setId(id)
                .execute()
                .actionGet();

            if (!getResponse.isExists()) {
                doCreate = true;
            }
        } catch (IndexMissingException e) {
            doCreate = true;
        }

        if (doCreate) {
            Map<String, Object> defaultSettings = defaultSettings();
            for (String key : initialSettings.keySet()) {
                defaultSettings.put(key, initialSettings.get(key));
            }
            set(defaultSettings);
        }
    }

    public Object get(String key) {
        GetResponse getResponse = client.prepareGet()
            .setIndex(settingsIndexName)
            .setType(settingsTypeName)
            .setId(id)
            .execute()
            .actionGet();
        if(!getResponse.isExists()) {
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

    public String[] getAsArray(String key) {
        Object obj = get(key);
        if (obj instanceof List) {
            List list = (List) obj;
            String[] array = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                array[i] = list.get(i).toString();
            }
            return array;
        } else {
            return new String[0];
        }
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
            client.prepareUpdate()
                .setIndex(settingsIndexName)
                .setType(settingsTypeName)
                .setId(id)
                .setDocAsUpsert(true)
                .setDoc(key, value)
                .setRefresh(true)
                .execute().actionGet();
        } catch (Exception e) {
            throw new SuggesterException("Failed to update settings.", e);
        }
    }

    public void set(Map<String, Object> map) {
        try {
            client.prepareUpdate()
                .setIndex(settingsIndexName)
                .setType(settingsTypeName)
                .setId(id)
                .setDocAsUpsert(true)
                .setRefresh(true)
                .setDoc(JsonXContent.contentBuilder().map(map))
                .execute().actionGet();
        } catch (Exception e) {
            throw new SuggesterException("Failed to update settings.", e);
        }
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

    public String getId() {
        return id;
    }

    private Map<String, Object> defaultSettings() {
        Map<String, Object> defaultSettings = new HashMap<>();
        defaultSettings.put(DefaultKeys.INDEX, (id + "-suggest").toLowerCase());
        defaultSettings.put(DefaultKeys.TYPE, "doc");
        defaultSettings.put(DefaultKeys.SUPPORTED_FIELDS, new String[]{"content"});
        defaultSettings.put(DefaultKeys.TAG_FIELD_NAME, "label");
        defaultSettings.put(DefaultKeys.ROLE_FIELD_NAME, "role");
        return defaultSettings;
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
