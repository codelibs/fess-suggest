package org.codelibs.fess.suggest.settings;

import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.exception.SuggestSettingsException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ArraySettings {
    protected final Client client;
    protected final String arraySettingsIndexName;
    protected final String settingsId;

    protected ArraySettings(final Client client, final String settingsIndexName, final String settingsId) {
        this.client = client;
        this.arraySettingsIndexName = createArraySettingsIndexName(settingsIndexName);
        this.settingsId = settingsId;
    }

    public String[] get(String key) {
        Map<String, Object> sourceArray[] = getFromArrayIndex(arraySettingsIndexName, settingsId, key);

        String[] valueArray = new String[sourceArray.length];
        for (int i = 0; i < valueArray.length; i++) {
            Object value = sourceArray[i].get(FieldNames.ARRAY_VALUE);
            if (value != null) {
                valueArray[i] = value.toString();
            }
        }
        return valueArray;
    }

    public void add(final String key, final Object value) {
        Map<String, Object> source = new HashMap<>();
        source.put(FieldNames.ARRAY_KEY, key);
        source.put(FieldNames.ARRAY_VALUE, value);
        source.put(FieldNames.TIMESTAMP, LocalDateTime.now());

        addToArrayIndex(arraySettingsIndexName, settingsId, createId(key, value), source);
    }

    public void delete(final String key) {
        deleteKeyFromArray(arraySettingsIndexName, settingsId, key);
    }

    public void delete(final String key, final String value) {
        deleteFromArray(arraySettingsIndexName, settingsId, createId(key, value));
    }

    protected String createArraySettingsIndexName(final String settingsIndexName) {
        return settingsIndexName + "-array";
    }

    protected String createId(final String key, final Object value) {
        return String.valueOf(("key:" + key + "value:" + value).hashCode());
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object>[] getFromArrayIndex(final String index, final String type, String key) {
        try {
            SearchResponse response =
                    client.prepareSearch().setIndices(index).setTypes(type).setScroll(TimeValue.timeValueSeconds(10))
                            .setSearchType(SearchType.SCAN).setQuery(QueryBuilders.matchQuery(FieldNames.ARRAY_KEY, key)).setSize(1000)
                            .execute().actionGet();

            Map<String, Object>[] array = new Map[(int) response.getHits().getTotalHits()];

            String scrollId = response.getScrollId();
            int count = 0;
            SearchResponse searchResponse;
            while ((searchResponse = client.prepareSearchScroll(scrollId).setScroll(TimeValue.timeValueMinutes(10)).execute().actionGet())
                    .getHits().getHits().length > 0) {
                scrollId = searchResponse.getScrollId();
                System.out.println(scrollId);
                SearchHit[] hits = searchResponse.getHits().getHits();
                for (SearchHit hit : hits) {
                    array[count++] = hit.getSource();
                }
            }

            Arrays.sort(array, (o1, o2) -> {
                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o1 == null) {
                    return -1;
                } else if (o2 == null) {
                    return 1;
                }

                Object timeObj1 = o1.get(FieldNames.TIMESTAMP);
                Object timeObj2 = o2.get(FieldNames.TIMESTAMP);
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
        } catch (IndexMissingException e) {
            return new Map[] {};
        }
    }

    protected void addToArrayIndex(final String index, final String type, final String id, final Map<String, Object> source) {
        try {
            client.prepareUpdate().setIndex(index).setType(type).setId(id).setDocAsUpsert(true)
                    .setDoc(JsonXContent.contentBuilder().map(source)).setRefresh(true).execute().actionGet();
        } catch (Exception e) {
            throw new SuggestSettingsException("Failed to add to array.", e);
        }
    }

    protected void deleteKeyFromArray(final String index, final String type, final String key) {
        try {
            client.prepareDeleteByQuery().setIndices(index).setTypes(type).setQuery(QueryBuilders.termQuery(FieldNames.ARRAY_KEY, key))
                    .execute().actionGet();
        } catch (Exception e) {
            throw new SuggestSettingsException("Failed to delete all from array.", e);
        }
    }

    protected void deleteFromArray(final String index, final String type, final String id) {
        try {
            client.prepareDelete().setIndex(index).setType(type).setId(id).setRefresh(true).execute().actionGet();
        } catch (Exception e) {
            throw new SuggestSettingsException("Failed to delete from array.", e);
        }
    }

}
