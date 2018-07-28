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

import org.codelibs.core.CoreLibConstants;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.exception.SuggestSettingsException;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

public class ArraySettings {
    protected final Client client;
    protected final String arraySettingsIndexName;
    protected final String settingsId;
    protected final SuggestSettings settings;

    private static final Base64.Encoder encoder = Base64.getEncoder();

    protected ArraySettings(final SuggestSettings settings, final Client client, final String settingsIndexName, final String settingsId) {
        this.settings = settings;
        this.client = client;
        this.arraySettingsIndexName = createArraySettingsIndexName(settingsIndexName);
        this.settingsId = settingsId;
        createMappingIfEmpty(arraySettingsIndexName, settingsId, client);
    }

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

    public void add(final String key, final Object value) {
        final Map<String, Object> source = new HashMap<>();
        source.put(FieldNames.ARRAY_KEY, key);
        source.put(FieldNames.ARRAY_VALUE, value);
        source.put(FieldNames.TIMESTAMP, DateTimeFormatter.ISO_INSTANT.format(ZonedDateTime.now()));

        addToArrayIndex(arraySettingsIndexName, settingsId, createId(key, value), source);
    }

    public void delete(final String key) {
        deleteKeyFromArray(arraySettingsIndexName, settingsId, key);
    }

    public void delete(final String key, final String value) {
        deleteFromArray(arraySettingsIndexName, settingsId, createId(key, value));
    }

    protected String createArraySettingsIndexName(final String settingsIndexName) {
        return settingsIndexName + "_array";
    }

    protected String createId(final String key, final Object value) {
        return encoder.encodeToString(("key:" + key + "value:" + value).getBytes(CoreLibConstants.CHARSET_UTF_8));
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object>[] getFromArrayIndex(final String index, final String type, final String key) {
        final String actualIndex = index + "." + type.toLowerCase(Locale.ENGLISH);
        try {
            SearchResponse response =
                    client.prepareSearch().setIndices(actualIndex).setTypes(type).setScroll(settings.getScrollTimeout())
                            .setQuery(QueryBuilders.termQuery(FieldNames.ARRAY_KEY, key)).setSize(1000).execute()
                            .actionGet(settings.getSearchTimeout());

            final Map<String, Object>[] array = new Map[(int) response.getHits().getTotalHits()];

            int count = 0;
            while (response.getHits().getHits().length > 0) {
                final String scrollId = response.getScrollId();
                final SearchHit[] hits = response.getHits().getHits();
                for (final SearchHit hit : hits) {
                    array[count++] = hit.getSourceAsMap();
                }
                response =
                        client.prepareSearchScroll(scrollId).setScroll(settings.getScrollTimeout()).execute()
                                .actionGet(settings.getSearchTimeout());
            }

            Arrays.sort(array, (o1, o2) -> {
                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o1 == null) {
                    return -1;
                } else if (o2 == null) {
                    return 1;
                }

                final Object timeObj1 = o1.get(FieldNames.TIMESTAMP);
                final Object timeObj2 = o2.get(FieldNames.TIMESTAMP);
                if (timeObj1 == null && timeObj2 == null) {
                    return 0;
                } else if (timeObj1 == null) {
                    return -1;
                } else if (timeObj2 == null) {
                    return 1;
                }

                return o1.toString().compareTo(o2.toString());
            });
            return array;
        } catch (final IndexNotFoundException e) {
            return new Map[0];
        }
    }

    protected void addToArrayIndex(final String index, final String type, final String id, final Map<String, Object> source) {
        final String actualIndex = index + "." + type.toLowerCase(Locale.ENGLISH);
        try {
            client.prepareUpdate().setIndex(actualIndex).setType(type).setId(id).setDocAsUpsert(true)
                    .setDoc(JsonXContent.contentBuilder().map(source)).execute().actionGet(settings.getIndexTimeout());
            client.admin().indices().prepareRefresh().setIndices(actualIndex).execute().actionGet(settings.getIndicesTimeout());
        } catch (final Exception e) {
            throw new SuggestSettingsException("Failed to add to array.", e);
        }
    }

    protected void deleteKeyFromArray(final String index, final String type, final String key) {
        final String actualIndex = index + "." + type.toLowerCase(Locale.ENGLISH);
        try {
            SuggestUtil.deleteByQuery(client, settings, actualIndex, type, QueryBuilders.termQuery(FieldNames.ARRAY_KEY, key));
        } catch (final Exception e) {
            throw new SuggestSettingsException("Failed to delete all from array.", e);
        }
    }

    protected void deleteFromArray(final String index, final String type, final String id) {
        final String actualIndex = index + "." + type.toLowerCase(Locale.ENGLISH);
        try {
            client.prepareDelete().setIndex(actualIndex).setType(type).setId(id).execute().actionGet(settings.getIndexTimeout());
            client.admin().indices().prepareRefresh().setIndices(actualIndex).execute().actionGet(settings.getIndicesTimeout());
        } catch (final Exception e) {
            throw new SuggestSettingsException("Failed to delete from array.", e);
        }
    }

    protected void createMappingIfEmpty(final String index, final String type, final Client client) {
        final String actualIndex = index + "." + type.toLowerCase(Locale.ENGLISH);
        try {
            boolean empty;
            try {
                empty =
                        client.admin().indices().prepareGetMappings(actualIndex).setTypes(type).execute()
                                .actionGet(settings.getIndicesTimeout()).getMappings().isEmpty();
            } catch (final IndexNotFoundException e) {
                empty = true;
                final CreateIndexResponse response =
                        client.admin().indices().prepareCreate(actualIndex).setSettings(loadIndexSettings(), XContentType.JSON).execute()
                                .actionGet(settings.getIndicesTimeout());
                if (!response.isAcknowledged()) {
                    throw new SuggestSettingsException("Failed to create " + actualIndex + "/" + type + " index.", e);
                }
                client.admin().cluster().prepareHealth(actualIndex).setWaitForYellowStatus().execute()
                        .actionGet(settings.getClusterTimeout());
            }
            if (empty) {
                client.admin()
                        .indices()
                        .preparePutMapping(actualIndex)
                        .setType(type)
                        .setSource(
                                XContentFactory.jsonBuilder().startObject().startObject(settingsId).startObject("properties")
                                        .startObject(FieldNames.ARRAY_KEY).field("type", "keyword").endObject().endObject().endObject()
                                        .endObject()).execute().actionGet(settings.getIndicesTimeout());
            }
        } catch (final IOException e) {
            throw new SuggestSettingsException("Failed to create mappings.");
        }
    }

    protected String loadIndexSettings() throws IOException {
        final String dictionaryPath = System.getProperty("fess.dictionary.path", StringUtil.EMPTY);
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader br =
                new BufferedReader(new InputStreamReader(this.getClass().getClassLoader()
                        .getResourceAsStream("suggest_indices/suggest_settings_array.json")));) {

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString().replaceAll(Pattern.quote("${fess.dictionary.path}"), dictionaryPath);
    }

}
