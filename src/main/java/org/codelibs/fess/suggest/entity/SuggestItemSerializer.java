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
package org.codelibs.fess.suggest.entity;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.util.SuggestUtil;

/**
 * Utility class for serializing and deserializing SuggestItem objects.
 * Centralizes serialization logic to reduce complexity in SuggestItem.
 *
 * <p>This class provides methods for:
 * <ul>
 * <li>Converting SuggestItem to Map (for OpenSearch indexing)</li>
 * <li>Parsing Map back to SuggestItem (from OpenSearch documents)</li>
 * <li>Creating updated source maps (for document updates)</li>
 * <li>Converting SuggestItem to JSON string</li>
 * </ul>
 */
public final class SuggestItemSerializer {

    private SuggestItemSerializer() {
        // Utility class
    }

    /**
     * Converts a SuggestItem to a source map for OpenSearch indexing.
     *
     * @param item The SuggestItem to convert
     * @return A Map containing all fields for indexing
     */
    public static Map<String, Object> toSource(final SuggestItem item) {
        final Map<String, Object> map = new HashMap<>();
        map.put(FieldNames.TEXT, item.getText());

        final String[][] readings = item.getReadings();
        for (int i = 0; i < readings.length; i++) {
            final String[] values = readings[i] == null ? null : Arrays.stream(readings[i]).distinct().toArray(n -> new String[n]);
            map.put(FieldNames.READING_PREFIX + i, values);
        }

        map.put(FieldNames.FIELDS, item.getFields());
        map.put(FieldNames.TAGS, item.getTags());
        map.put(FieldNames.ROLES, item.getRoles());
        map.put(FieldNames.LANGUAGES, item.getLanguages());
        map.put(FieldNames.KINDS, Stream.of(item.getKinds()).map(SuggestItem.Kind::toString).toArray());
        map.put(FieldNames.QUERY_FREQ, item.getQueryFreq());
        map.put(FieldNames.DOC_FREQ, item.getDocFreq());
        map.put(FieldNames.USER_BOOST, item.getUserBoost());
        map.put(FieldNames.SCORE, (item.getQueryFreq() + item.getDocFreq()) * item.getUserBoost());
        map.put(FieldNames.TIMESTAMP, item.getTimestamp().toInstant().toEpochMilli());
        return map;
    }

    /**
     * Parses a source map from OpenSearch and creates a SuggestItem.
     *
     * @param source The source map from OpenSearch
     * @return A new SuggestItem instance
     */
    public static SuggestItem fromSource(final Map<String, Object> source) {
        final String text = source.get(FieldNames.TEXT).toString();
        final List<String[]> readings = new ArrayList<>();
        for (int i = 0;; i++) {
            final Object readingObj = source.get(FieldNames.READING_PREFIX + i);
            if (!(readingObj instanceof List)) {
                break;
            }
            @SuppressWarnings("unchecked")
            final List<String> list = (List<String>) readingObj;
            readings.add(list.toArray(new String[list.size()]));
        }
        final List<String> fields = SuggestUtil.getAsList(source.get(FieldNames.FIELDS));
        final long docFreq = Long.parseLong(source.get(FieldNames.DOC_FREQ).toString());
        final long queryFreq = Long.parseLong(source.get(FieldNames.QUERY_FREQ).toString());
        final float userBoost = Float.parseFloat(source.get(FieldNames.USER_BOOST).toString());
        final List<String> tags = SuggestUtil.getAsList(source.get(FieldNames.TAGS));
        final List<String> roles = SuggestUtil.getAsList(source.get(FieldNames.ROLES));
        final List<String> languages = SuggestUtil.getAsList(source.get(FieldNames.LANGUAGES));
        final List<String> kinds = SuggestUtil.getAsList(source.get(FieldNames.KINDS));
        final long timestamp = Long.parseLong(source.get(FieldNames.TIMESTAMP).toString());

        final SuggestItem item = new SuggestItem();
        item.setText(text);
        item.setReadings(readings.toArray(new String[readings.size()][]));
        item.setFields(fields.toArray(new String[fields.size()]));
        item.setDocFreq(docFreq);
        item.setQueryFreq(queryFreq);
        item.setUserBoost(userBoost);
        item.setTags(tags.toArray(new String[tags.size()]));
        item.setRoles(roles.toArray(new String[roles.size()]));
        item.setLanguages(languages.toArray(new String[languages.size()]));

        final SuggestItem.Kind[] itemKinds = new SuggestItem.Kind[kinds.size()];
        for (int i = 0; i < kinds.size(); i++) {
            final String kind = kinds.get(i);
            if (kind.equals(SuggestItem.Kind.DOCUMENT.toString())) {
                itemKinds[i] = SuggestItem.Kind.DOCUMENT;
            } else if (kind.equals(SuggestItem.Kind.QUERY.toString())) {
                itemKinds[i] = SuggestItem.Kind.QUERY;
            } else if (kind.equals(SuggestItem.Kind.USER.toString())) {
                itemKinds[i] = SuggestItem.Kind.USER;
            }
        }
        item.setKinds(itemKinds);

        item.setId(SuggestUtil.createSuggestTextId(text));
        item.setTimestamp(ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), Clock.systemDefaultZone().getZone()));
        return item;
    }

    /**
     * Creates an updated source map by merging the item with existing source.
     *
     * @param item The SuggestItem with new data
     * @param existingSource The existing source map from OpenSearch
     * @return A merged source map
     */
    public static Map<String, Object> toUpdatedSource(final SuggestItem item, final Map<String, Object> existingSource) {
        final Map<String, Object> map = new HashMap<>();
        map.put(FieldNames.TEXT, item.getText());

        final String[][] readings = item.getReadings();
        for (int i = 0; i < readings.length; i++) {
            final Object readingObj = existingSource.get(FieldNames.READING_PREFIX + i);
            if (readingObj instanceof List) {
                @SuppressWarnings("unchecked")
                final List<String> existingValues = (List<String>) readingObj;
                concatValues(existingValues, readings[i]);
                map.put(FieldNames.READING_PREFIX + i, existingValues.stream().distinct().toList());
            } else {
                final String[] values = readings[i] == null ? null : Arrays.stream(readings[i]).distinct().toArray(n -> new String[n]);
                map.put(FieldNames.READING_PREFIX + i, values);
            }
        }

        mergeListField(map, existingSource, FieldNames.FIELDS, item.getFields());
        mergeListField(map, existingSource, FieldNames.TAGS, item.getTags());
        mergeListField(map, existingSource, FieldNames.ROLES, item.getRoles());
        mergeListField(map, existingSource, FieldNames.LANGUAGES, item.getLanguages());

        final Object kindsObj = existingSource.get(FieldNames.KINDS);
        if (kindsObj instanceof List) {
            @SuppressWarnings("unchecked")
            final List<String> existingKinds = (List<String>) kindsObj;
            concatValues(existingKinds, Stream.of(item.getKinds()).map(SuggestItem.Kind::toString).toArray(count -> new String[count]));
            map.put(FieldNames.KINDS, existingKinds);
        } else {
            map.put(FieldNames.KINDS, Stream.of(item.getKinds()).map(SuggestItem.Kind::toString).toArray());
        }

        final long updatedQueryFreq = mergeFrequency(existingSource, FieldNames.QUERY_FREQ, item.getQueryFreq());
        map.put(FieldNames.QUERY_FREQ, updatedQueryFreq);

        final long updatedDocFreq = mergeFrequency(existingSource, FieldNames.DOC_FREQ, item.getDocFreq());
        map.put(FieldNames.DOC_FREQ, updatedDocFreq);

        map.put(FieldNames.USER_BOOST, item.getUserBoost());
        map.put(FieldNames.SCORE, (updatedQueryFreq + updatedDocFreq) * item.getUserBoost());
        map.put(FieldNames.TIMESTAMP, item.getTimestamp().toInstant().toEpochMilli());
        return map;
    }

    /**
     * Converts a SuggestItem to a JSON string.
     *
     * @param item The SuggestItem to convert
     * @return A JSON string representation
     */
    public static String toJson(final SuggestItem item) {
        final StringBuilder buf = new StringBuilder(100);
        buf.append('{').append('"').append(FieldNames.TEXT).append("\":").append(escapeJsonString(item.getText()));

        final String[][] readings = item.getReadings();
        for (int i = 0; i < readings.length; i++) {
            final String[] values = readings[i] == null ? null : Arrays.stream(readings[i]).distinct().toArray(n -> new String[n]);
            buf.append(',').append('"').append(FieldNames.READING_PREFIX + i).append("\":").append(toJsonArray(values));
        }

        buf.append(',').append('"').append(FieldNames.FIELDS).append("\":").append(toJsonArray(item.getFields()));
        buf.append(',').append('"').append(FieldNames.TAGS).append("\":").append(toJsonArray(item.getTags()));
        buf.append(',').append('"').append(FieldNames.ROLES).append("\":").append(toJsonArray(item.getRoles()));
        buf.append(',').append('"').append(FieldNames.LANGUAGES).append("\":").append(toJsonArray(item.getLanguages()));
        buf.append(',')
                .append('"')
                .append(FieldNames.KINDS)
                .append("\":")
                .append(toJsonArray(Stream.of(item.getKinds()).map(SuggestItem.Kind::toString).toArray(n -> new String[n])));
        buf.append(',').append('"').append(FieldNames.QUERY_FREQ).append("\":").append(item.getQueryFreq());
        buf.append(',').append('"').append(FieldNames.DOC_FREQ).append("\":").append(item.getDocFreq());
        buf.append(',').append('"').append(FieldNames.USER_BOOST).append("\":").append(item.getUserBoost());
        buf.append(',')
                .append('"')
                .append(FieldNames.SCORE)
                .append("\":")
                .append((item.getQueryFreq() + item.getDocFreq()) * item.getUserBoost());
        buf.append(',').append('"').append(FieldNames.TIMESTAMP).append("\":").append(item.getTimestamp().toInstant().toEpochMilli());
        return buf.append('}').toString();
    }

    /**
     * Merges a list field from existing source with new values.
     */
    private static void mergeListField(final Map<String, Object> map, final Map<String, Object> existingSource, final String fieldName,
            final String[] newValues) {
        final Object existingObj = existingSource.get(fieldName);
        if (existingObj instanceof List) {
            @SuppressWarnings("unchecked")
            final List<String> existingValues = (List<String>) existingObj;
            concatValues(existingValues, newValues);
            map.put(fieldName, existingValues);
        } else {
            map.put(fieldName, newValues);
        }
    }

    /**
     * Merges a frequency value from existing source with new value.
     */
    private static long mergeFrequency(final Map<String, Object> existingSource, final String fieldName, final long newValue) {
        final Object freqObj = existingSource.get(fieldName);
        if (freqObj == null) {
            return newValue;
        }
        return newValue + Long.parseLong(freqObj.toString());
    }

    /**
     * Concatenates new values into an existing list, avoiding duplicates.
     */
    private static void concatValues(final List<String> existingValues, final String[] newValues) {
        if (newValues == null) {
            return;
        }
        for (final String value : newValues) {
            if (!existingValues.contains(value)) {
                existingValues.add(value);
            }
        }
    }

    /**
     * Escapes a string for JSON output.
     */
    private static String escapeJsonString(final String value) {
        if (value == null) {
            return "null";
        }
        final StringBuilder buf = new StringBuilder(value.length() + 2);
        buf.append('"');
        for (final char c : value.toCharArray()) {
            switch (c) {
            case '"':
                buf.append("\\\"");
                break;
            case '\\':
                buf.append("\\\\");
                break;
            case '\n':
                buf.append("\\n");
                break;
            case '\r':
                buf.append("\\r");
                break;
            case '\t':
                buf.append("\\t");
                break;
            default:
                buf.append(c);
            }
        }
        buf.append('"');
        return buf.toString();
    }

    /**
     * Converts a string array to JSON array format.
     */
    private static String toJsonArray(final String[] values) {
        if (values == null) {
            return "[]";
        }
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                buf.append(',');
            }
            buf.append(escapeJsonString(values[i]));
        }
        buf.append(']');
        return buf.toString();
    }
}
