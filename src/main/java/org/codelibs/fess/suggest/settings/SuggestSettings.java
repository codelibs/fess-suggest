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

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.exception.SuggestSettingsException;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.IndexNotFoundException;

public class SuggestSettings {
    protected final String settingsId;

    protected final Client client;

    protected final String settingsIndexName;

    protected final String settingsTypeName;

    protected final Map<String, Object> initialSettings;

    protected boolean initialized = false;

    protected final String badWordIndexName;
    protected final String elevateWordIndexName;

    protected TimeoutSettings timeoutSettings;

    public static class TimeoutSettings {
        protected String searchTimeout = "15s";
        protected String indexTimeout = "1m";
        protected String bulkTimeout = "1m";
        protected String indicesTimeout = "1m";
        protected String clusterTimeout = "1m";
        protected String scrollTimeout = "1m";
    }

    public SuggestSettings(final Client client, final String settingsId, final Map<String, Object> initialSettings,
            final String settingsIndexName, final String settingsTypeName, final TimeoutSettings timeoutSettings) {
        this.client = client;
        this.settingsId = settingsId;
        this.settingsIndexName = settingsIndexName;
        this.settingsTypeName = settingsTypeName;
        this.initialSettings = initialSettings;
        this.timeoutSettings = timeoutSettings;

        this.badWordIndexName = settingsIndexName + "-badword";
        this.elevateWordIndexName = settingsIndexName + "-elevateword";
    }

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
                    client.prepareGet().setIndex(settingsIndexName).setType(settingsTypeName).setId(settingsId).execute()
                            .actionGet(getSearchTimeout());

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
                    client.admin().indices().prepareCreate(settingsIndexName).setSettings(loadIndexSettings(), XContentType.JSON).execute()
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

    public Object get(final String key) {
        final GetResponse getResponse =
                client.prepareGet().setIndex(settingsIndexName).setType(settingsTypeName).setId(settingsId).execute()
                        .actionGet(getSearchTimeout());
        if (!getResponse.isExists()) {
            return null;
        }
        final Map<String, Object> map = getResponse.getSource();
        return map.get(key);
    }

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

    public void set(final String key, final Object value) {
        try {
            client.prepareUpdate().setIndex(settingsIndexName).setType(settingsTypeName).setId(settingsId).setDocAsUpsert(true)
                    .setDoc(key, value).setRetryOnConflict(5).execute().actionGet(getIndexTimeout());
            client.admin().indices().prepareRefresh().setIndices(settingsIndexName).execute().actionGet(getIndicesTimeout());
        } catch (final Exception e) {
            throw new SuggestSettingsException("Failed to update suggestSettings.", e);
        }
    }

    public void set(final Map<String, Object> map) {
        try {
            client.prepareUpdate().setIndex(settingsIndexName).setType(settingsTypeName).setId(settingsId).setDocAsUpsert(true)
                    .setDoc(JsonXContent.contentBuilder().map(map)).setRetryOnConflict(5).execute().actionGet(getIndexTimeout());
            client.admin().indices().prepareRefresh().setIndices(settingsIndexName).execute().actionGet(getIndicesTimeout());
        } catch (final Exception e) {
            throw new SuggestSettingsException("Failed to update suggestSettings.", e);
        }
    }

    public ArraySettings array() {
        return new ArraySettings(this, client, settingsIndexName, settingsId);
    }

    public AnalyzerSettings analyzer() {
        return new AnalyzerSettings(client, this, settingsIndexName);
    }

    public BadWordSettings badword() {
        return new BadWordSettings(this, client, settingsIndexName, settingsId);
    }

    public ElevateWordSettings elevateWord() {
        return new ElevateWordSettings(this, client, settingsIndexName, settingsId);
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
        final Map<String, Object> defaultSettings = new HashMap<>();
        defaultSettings.put(DefaultKeys.INDEX, (settingsId + ".suggest").toLowerCase());
        defaultSettings.put(DefaultKeys.TYPE, "doc");
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

    protected String loadIndexSettings() throws IOException {
        final String dictionaryPath = System.getProperty("fess.dictionary.path", StringUtil.EMPTY);
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader br =
                new BufferedReader(new InputStreamReader(this.getClass().getClassLoader()
                        .getResourceAsStream("suggest_indices/suggest_settings.json")));) {

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString().replaceAll(Pattern.quote("${fess.dictionary.path}"), dictionaryPath);
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
        public static final String LANG_FIELD_NAME = "langFieldName";
        public static final String PARALLEL_PROCESSING = "parallel";

        private DefaultKeys() {
        }
    }

    public String getScrollTimeout() {
        return timeoutSettings.scrollTimeout;
    }

    public String getSearchTimeout() {
        return timeoutSettings.searchTimeout;
    }

    public String getIndexTimeout() {
        return timeoutSettings.indexTimeout;
    }

    public String getIndicesTimeout() {
        return timeoutSettings.indicesTimeout;
    }

    public String getBulkTimeout() {
        return timeoutSettings.bulkTimeout;
    }

    public String getClusterTimeout() {
        return timeoutSettings.clusterTimeout;
    }
}
