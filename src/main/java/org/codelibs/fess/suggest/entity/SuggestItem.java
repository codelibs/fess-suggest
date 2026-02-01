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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.opensearch.common.Nullable;

/**
 * The SuggestItem class represents an item used for suggestions in the Fess search engine.
 * It contains various attributes such as text, readings, fields, tags, roles, languages, kinds, and frequencies.
 * The class provides methods to manipulate and retrieve these attributes, as well as to convert the item to and from different formats.
 */
public class SuggestItem {

    /**
     * The kind of suggest item.
     */
    public enum Kind {
        /** Document kind. */
        DOCUMENT("document"),
        /** Query kind. */
        QUERY("query"),
        /** User kind. */
        USER("user");

        private final String kind;

        /**
         * Constructor for Kind.
         * @param kind The kind string.
         */
        Kind(final String kind) {
            this.kind = kind;
        }

        @Override
        public String toString() {
            return kind;
        }
    }

    private String text;

    private ZonedDateTime timestamp;

    private long queryFreq;

    private long docFreq;

    private float userBoost;

    private String[][] readings;

    private String[] fields;

    private String[] tags;

    private String[] roles;

    private String[] languages;

    private Kind[] kinds;

    private Map<String, Object> emptySource;

    private String id;

    SuggestItem() {
    }

    /**
     * Constructor for SuggestItem.
     * @param text The text.
     * @param readings The readings.
     * @param fields The fields.
     * @param docFreq The document frequency.
     * @param queryFreq The query frequency.
     * @param userBoost The user boost.
     * @param tags The tags.
     * @param roles The roles.
     * @param languages The languages.
     * @param kind The kind.
     */
    public SuggestItem(final String[] text, final String[][] readings, final String[] fields, final long docFreq, final long queryFreq,
            final float userBoost, @Nullable final String[] tags, @Nullable final String[] roles, @Nullable final String[] languages,
            final Kind kind) {
        this.text = String.join(SuggestConstants.TEXT_SEPARATOR, text);
        this.readings = readings;
        this.fields = fields != null ? fields : new String[] {};
        this.tags = tags != null ? tags : new String[] {};

        if (roles == null || roles.length == 0) {
            this.roles = new String[] { SuggestConstants.DEFAULT_ROLE };
        } else {
            this.roles = new String[roles.length];
            System.arraycopy(roles, 0, this.roles, 0, roles.length);
        }

        this.languages = languages != null ? languages : new String[] {};

        kinds = new Kind[] { kind };
        if (userBoost > 1) {
            this.userBoost = userBoost;
        } else {
            this.userBoost = 1;
        }
        this.docFreq = docFreq;
        this.queryFreq = queryFreq;
        timestamp = ZonedDateTime.now();
        emptySource = createEmptyMap();
        id = SuggestUtil.createSuggestTextId(this.text);
    }

    /**
     * Returns the text of the suggest item.
     * @return The text.
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the readings of the suggest item.
     * @return The readings.
     */
    public String[][] getReadings() {
        return readings;
    }

    /**
     * Returns the tags of the suggest item.
     * @return The tags.
     */
    public String[] getTags() {
        return tags;
    }

    /**
     * Returns the roles of the suggest item.
     * @return The roles.
     */
    public String[] getRoles() {
        return roles;
    }

    /**
     * Returns the languages of the suggest item.
     * @return The languages.
     */
    public String[] getLanguages() {
        return languages;
    }

    /**
     * Returns the fields of the suggest item.
     * @return The fields.
     */
    public String[] getFields() {
        return fields;
    }

    /**
     * Returns the kinds of the suggest item.
     * @return The kinds.
     */
    public Kind[] getKinds() {
        return kinds;
    }

    /**
     * Returns the query frequency of the suggest item.
     * @return The query frequency.
     */
    public long getQueryFreq() {
        return queryFreq;
    }

    /**
     * Returns the document frequency of the suggest item.
     * @return The document frequency.
     */
    public long getDocFreq() {
        return docFreq;
    }

    /**
     * Returns the user boost of the suggest item.
     * @return The user boost.
     */
    public float getUserBoost() {
        return userBoost;
    }

    /**
     * Returns the timestamp of the suggest item.
     * @return The timestamp.
     */
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the text of the suggest item.
     * @param text The text to set.
     */
    public void setText(final String text) {
        this.text = text;
    }

    /**
     * Sets the timestamp of the suggest item.
     * @param timestamp The timestamp to set.
     */
    public void setTimestamp(final ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Sets the query frequency of the suggest item.
     * @param queryFreq The query frequency to set.
     */
    public void setQueryFreq(final long queryFreq) {
        this.queryFreq = queryFreq;
    }

    /**
     * Sets the document frequency of the suggest item.
     * @param docFreq The document frequency to set.
     */
    public void setDocFreq(final long docFreq) {
        this.docFreq = docFreq;
    }

    /**
     * Sets the user boost of the suggest item.
     * @param userBoost The user boost to set.
     */
    public void setUserBoost(final float userBoost) {
        this.userBoost = userBoost;
    }

    /**
     * Sets the readings of the suggest item.
     * @param readings The readings to set.
     */
    public void setReadings(final String[][] readings) {
        this.readings = readings;
    }

    /**
     * Sets the fields of the suggest item.
     * @param fields The fields to set.
     */
    public void setFields(final String[] fields) {
        this.fields = fields;
    }

    /**
     * Sets the tags of the suggest item.
     * @param tags The tags to set.
     */
    public void setTags(final String[] tags) {
        this.tags = tags;
    }

    /**
     * Sets the roles of the suggest item.
     * @param roles The roles to set.
     */
    public void setRoles(final String[] roles) {
        this.roles = roles;
    }

    /**
     * Sets the languages of the suggest item.
     * @param languages The languages to set.
     */
    public void setLanguages(final String[] languages) {
        this.languages = languages;
    }

    /**
     * Sets the kinds of the suggest item.
     * @param kinds The kinds to set.
     */
    public void setKinds(final Kind[] kinds) {
        this.kinds = kinds;
    }

    /**
     * Sets the empty source map.
     * @param emptySource The empty source map to set.
     */
    public void setEmptySource(final Map<String, Object> emptySource) {
        this.emptySource = emptySource;
    }

    /**
     * Sets the ID of the suggest item.
     * @param id The ID to set.
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Returns an empty map with default values for suggest item fields.
     * @return An empty map.
     */
    public Map<String, Object> toEmptyMap() {
        return emptySource;
    }

    /**
     * Creates an empty map with default values for suggest item fields.
     * @return An empty map.
     */
    protected Map<String, Object> createEmptyMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put(FieldNames.TEXT, StringUtil.EMPTY);

        for (int i = 0; i < readings.length; i++) {
            map.put(FieldNames.READING_PREFIX + i, new String[] {});
        }

        map.put(FieldNames.FIELDS, new String[] {});
        map.put(FieldNames.TAGS, new String[] {});
        map.put(FieldNames.ROLES, new String[] {});
        map.put(FieldNames.LANGUAGES, new String[] {});
        map.put(FieldNames.KINDS, new String[] {});
        map.put(FieldNames.SCORE, 1.0F);
        map.put(FieldNames.QUERY_FREQ, 0L);
        map.put(FieldNames.DOC_FREQ, 0L);
        map.put(FieldNames.USER_BOOST, 1.0F);
        map.put(FieldNames.TIMESTAMP, DateTimeFormatter.ISO_INSTANT.format(ZonedDateTime.now()));
        return map;
    }

    /**
     * Returns the ID of the suggest item.
     * @return The ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the source map of the suggest item.
     * @return The source map.
     */
    public Map<String, Object> getSource() {
        return SuggestItemSerializer.toSource(this);
    }

    /**
     * Parses a source map and returns a SuggestItem instance.
     * @param source The source map.
     * @return A SuggestItem instance.
     */
    public static SuggestItem parseSource(final Map<String, Object> source) {
        return SuggestItemSerializer.fromSource(source);
    }

    /**
     * Returns the updated source map by merging with existing source.
     * @param existingSource The existing source map.
     * @return The updated source map.
     */
    public Map<String, Object> getUpdatedSource(final Map<String, Object> existingSource) {
        return SuggestItemSerializer.toUpdatedSource(this, existingSource);
    }

    /**
     * Concatenates values to a destination list, avoiding duplicates.
     * @param dest The destination list.
     * @param newValues The new values to add.
     * @param <T> The type of the values.
     */
    protected static <T> void concatValues(final List<T> dest, final T... newValues) {
        for (final T value : newValues) {
            if (!dest.contains(value)) {
                dest.add(value);
            }
        }
    }

    /**
     * Concatenates kind arrays, avoiding duplicates.
     * @param kinds The initial kind array.
     * @param newKinds The new kind array to add.
     * @return The concatenated kind array.
     */
    protected static Kind[] concatKinds(final Kind[] kinds, final Kind... newKinds) {
        if (kinds == null) {
            return newKinds;
        }
        if (newKinds == null) {
            return kinds;
        }

        final List<Kind> list = new ArrayList<>(kinds.length + newKinds.length);
        list.addAll(Arrays.asList(kinds));
        for (final Kind kind : newKinds) {
            if (!list.contains(kind)) {
                list.add(kind);
            }
        }
        return list.toArray(new Kind[list.size()]);
    }

    /**
     * Merges two suggest items.
     * @param item1 The first suggest item.
     * @param item2 The second suggest item.
     * @return The merged suggest item.
     */
    public static SuggestItem merge(final SuggestItem item1, final SuggestItem item2) {
        return SuggestItemMerger.merge(item1, item2);
    }

    /**
     * Checks if the suggest item contains any of the given bad words.
     * @param badWords The array of bad words.
     * @return True if the item contains a bad word, false otherwise.
     */
    public boolean isBadWord(final String[] badWords) {
        for (final String badWord : badWords) {
            if (text.contains(badWord)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "SuggestItem [text=" + text + ", timestamp=" + timestamp + ", queryFreq=" + queryFreq + ", docFreq=" + docFreq
                + ", userBoost=" + userBoost + ", readings=" + Arrays.toString(readings) + ", fields=" + Arrays.toString(fields) + ", tags="
                + Arrays.toString(tags) + ", roles=" + Arrays.toString(roles) + ", languages=" + Arrays.toString(languages) + ", kinds="
                + Arrays.toString(kinds) + ", emptySource=" + emptySource + ", id=" + id + "]";
    }

    private String convertJsonString(final String value) {
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }

    private String convertJsonStrings(final String[] values) {
        if (values == null) {
            return "[]";
        }
        return "[" + Arrays.stream(values).map(this::convertJsonString).collect(Collectors.joining(",")) + "]";
    }

    /**
     * Converts the suggest item to a JSON string.
     * @return The JSON string representation of the suggest item.
     */
    public String toJsonString() {
        return SuggestItemSerializer.toJson(this);
    }
}
