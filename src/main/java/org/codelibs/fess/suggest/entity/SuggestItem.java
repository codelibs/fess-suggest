package org.codelibs.fess.suggest.entity;

import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.elasticsearch.common.Nullable;

public class SuggestItem implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Kind {
        DOCUMENT("document"), QUERY("query"), USER("user");

        private final String kind;

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

    private SuggestItem() {
    }

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

        this.kinds = new Kind[] { kind };
        if (userBoost > 1) {
            this.userBoost = userBoost;
        } else {
            this.userBoost = 1;
        }
        this.docFreq = docFreq;
        this.queryFreq = queryFreq;
        this.timestamp = ZonedDateTime.now();
        this.emptySource = createEmptyMap();
        this.id = SuggestUtil.createSuggestTextId(this.text);
    }

    public String getText() {
        return text;
    }

    public String[][] getReadings() {
        return readings;
    }

    public String[] getTags() {
        return tags;
    }

    public String[] getRoles() {
        return roles;
    }

    public String[] getLanguages() {
        return languages;
    }

    public String[] getFields() {
        return fields;
    }

    public Kind[] getKinds() {
        return kinds;
    }

    public long getQueryFreq() {
        return queryFreq;
    }

    public long getDocFreq() {
        return docFreq;
    }

    public float getUserBoost() {
        return userBoost;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public void setTimestamp(final ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setQueryFreq(final long queryFreq) {
        this.queryFreq = queryFreq;
    }

    public void setDocFreq(final long docFreq) {
        this.docFreq = docFreq;
    }

    public void setUserBoost(final float userBoost) {
        this.userBoost = userBoost;
    }

    public void setReadings(final String[][] readings) {
        this.readings = readings;
    }

    public void setFields(final String[] fields) {
        this.fields = fields;
    }

    public void setTags(final String[] tags) {
        this.tags = tags;
    }

    public void setRoles(final String[] roles) {
        this.roles = roles;
    }

    public void setLanguages(final String[] languages) {
        this.languages = languages;
    }

    public void setKinds(final Kind[] kinds) {
        this.kinds = kinds;
    }

    public void setEmptySource(final Map<String, Object> emptySource) {
        this.emptySource = emptySource;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public Map<String, Object> toEmptyMap() {
        return emptySource;
    }

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

    public String getId() {
        return id;
    }

    public Map<String, Object> getSource() {
        final Map<String, Object> map = new HashMap<>();
        map.put(FieldNames.TEXT, text);

        for (int i = 0; i < readings.length; i++) {
            map.put(FieldNames.READING_PREFIX + i, readings[i]);
        }

        map.put(FieldNames.FIELDS, fields);
        map.put(FieldNames.TAGS, tags);
        map.put(FieldNames.ROLES, roles);
        map.put(FieldNames.LANGUAGES, languages);
        map.put(FieldNames.KINDS, Stream.of(kinds).map(kind -> kind.toString()).toArray());
        map.put(FieldNames.QUERY_FREQ, queryFreq);
        map.put(FieldNames.DOC_FREQ, docFreq);
        map.put(FieldNames.USER_BOOST, userBoost);
        map.put(FieldNames.SCORE, (queryFreq + docFreq) * userBoost);
        map.put(FieldNames.TIMESTAMP, timestamp.toInstant().toEpochMilli());
        return map;
    }

    public static SuggestItem parseSource(final Map<String, Object> source) {
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
        item.text = text;
        item.readings = readings.toArray(new String[readings.size()][]);
        item.fields = fields.toArray(new String[fields.size()]);
        item.docFreq = docFreq;
        item.queryFreq = queryFreq;
        item.userBoost = userBoost;
        item.tags = tags.toArray(new String[tags.size()]);
        item.roles = roles.toArray(new String[roles.size()]);
        item.languages = languages.toArray(new String[languages.size()]);

        item.kinds = new Kind[kinds.size()];
        for (int i = 0; i < kinds.size(); i++) {
            final String kind = kinds.get(i);
            if (kind.equals(Kind.DOCUMENT.toString())) {
                item.kinds[i] = Kind.DOCUMENT;
            } else if (kind.equals(Kind.QUERY.toString())) {
                item.kinds[i] = Kind.QUERY;
            } else if (kind.equals(Kind.USER.toString())) {
                item.kinds[i] = Kind.USER;
            }
        }

        item.id = SuggestUtil.createSuggestTextId(item.text);
        item.timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), Clock.systemDefaultZone().getZone());
        return item;
    }

    public Map<String, Object> getUpdatedSource(final Map<String, Object> existingSource) {
        final Map<String, Object> map = new HashMap<>();
        map.put(FieldNames.TEXT, text);

        for (int i = 0; i < readings.length; i++) {
            final Object readingObj = existingSource.get(FieldNames.READING_PREFIX + i);
            if (readingObj instanceof List) {
                @SuppressWarnings("unchecked")
                final List<String> existingValues = (List<String>) readingObj;
                concatValues(existingValues, readings[i]);
                map.put(FieldNames.READING_PREFIX + i, existingValues);
            } else {
                map.put(FieldNames.READING_PREFIX + i, readings[i]);
            }
        }

        final Object fieldsObj = existingSource.get(FieldNames.FIELDS);
        if (fieldsObj instanceof List) {
            @SuppressWarnings("unchecked")
            final List<String> existingValues = (List<String>) fieldsObj;
            concatValues(existingValues, fields);
            map.put(FieldNames.FIELDS, existingValues);
        } else {
            map.put(FieldNames.FIELDS, fields);
        }

        final Object tagsObj = existingSource.get(FieldNames.TAGS);
        if (tagsObj instanceof List) {
            @SuppressWarnings("unchecked")
            final List<String> existingValues = (List<String>) tagsObj;
            concatValues(existingValues, tags);
            map.put(FieldNames.TAGS, existingValues);
        } else {
            map.put(FieldNames.TAGS, tags);
        }

        final Object rolesObj = existingSource.get(FieldNames.ROLES);
        if (rolesObj instanceof List) {
            @SuppressWarnings("unchecked")
            final List<String> existingValues = (List<String>) rolesObj;
            concatValues(existingValues, roles);
            map.put(FieldNames.ROLES, existingValues);
        } else {
            map.put(FieldNames.ROLES, roles);
        }

        final Object langsObj = existingSource.get(FieldNames.LANGUAGES);
        if (langsObj instanceof List) {
            @SuppressWarnings("unchecked")
            final List<String> existingValues = (List<String>) langsObj;
            concatValues(existingValues, languages);
            map.put(FieldNames.LANGUAGES, existingValues);
        } else {
            map.put(FieldNames.LANGUAGES, languages);
        }

        final Object kindsObj = existingSource.get(FieldNames.KINDS);
        if (kindsObj instanceof List) {
            @SuppressWarnings("unchecked")
            final List<String> existingFields = (List<String>) kindsObj;
            concatValues(existingFields, Stream.of(kinds).map(kind -> kind.toString()).toArray(count -> new String[count]));
            map.put(FieldNames.KINDS, existingFields);
        } else {
            map.put(FieldNames.KINDS, Stream.of(kinds).map(kind -> kind.toString()).toArray());
        }

        final long updatedQueryFreq;
        final Object queryFreqObj = existingSource.get(FieldNames.QUERY_FREQ);
        if (queryFreqObj == null) {
            updatedQueryFreq = queryFreq;
        } else {
            final Long existingValue = Long.parseLong(queryFreqObj.toString());
            updatedQueryFreq = queryFreq + existingValue;
        }
        map.put(FieldNames.QUERY_FREQ, updatedQueryFreq);

        final long updatedDocFreq;
        final Object docFreqObj = existingSource.get(FieldNames.DOC_FREQ);
        if (docFreqObj == null) {
            updatedDocFreq = docFreq;
        } else {
            final Long existingValue = Long.parseLong(docFreqObj.toString());
            updatedDocFreq = docFreq + existingValue;
        }
        map.put(FieldNames.DOC_FREQ, updatedDocFreq);

        map.put(FieldNames.USER_BOOST, userBoost);
        map.put(FieldNames.SCORE, (updatedQueryFreq + updatedDocFreq) * userBoost);
        map.put(FieldNames.TIMESTAMP, timestamp.toInstant().toEpochMilli());
        return map;
    }

    protected static <T> void concatValues(final List<T> dest, final T... newValues) {
        for (final T value : newValues) {
            if (!dest.contains(value)) {
                dest.add(value);
            }
        }
    }

    protected static Kind[] concatKinds(final Kind[] kinds, final Kind... newKinds) {
        if (kinds == null) {
            return newKinds;
        } else if (newKinds == null) {
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

    public static SuggestItem merge(final SuggestItem item1, final SuggestItem item2) {
        if (!item1.getId().equals(item2.getId())) {
            throw new IllegalArgumentException("Item id is mismatch.");
        }

        final SuggestItem mergedItem = new SuggestItem();

        mergedItem.id = item1.getId();
        mergedItem.text = item1.getText();

        mergedItem.readings = new String[mergedItem.text.split(SuggestConstants.TEXT_SEPARATOR).length][];
        for (int i = 0; i < mergedItem.readings.length; i++) {
            final List<String> list = new ArrayList<>();
            if (item1.getReadings().length > i) {
                for (final String reading : item1.getReadings()[i]) {
                    list.add(reading);
                }
            }
            if (item2.getReadings().length > i) {
                for (final String reading : item2.getReadings()[i]) {
                    if (!list.contains(reading)) {
                        list.add(reading);
                    }
                }
            }
            mergedItem.readings[i] = list.toArray(new String[list.size()]);
        }

        final List<String> fieldList = new ArrayList<>(item1.getFields().length + item2.getFields().length);
        for (final String field : item1.getFields()) {
            fieldList.add(field);
        }
        for (final String field : item2.getFields()) {
            if (!fieldList.contains(field)) {
                fieldList.add(field);
            }
        }
        mergedItem.fields = fieldList.toArray(new String[fieldList.size()]);

        final List<String> tagList = new ArrayList<>(item1.getTags().length + item2.getTags().length);
        for (final String tag : item1.getTags()) {
            tagList.add(tag);
        }
        for (final String tag : item2.getTags()) {
            if (!tagList.contains(tag)) {
                tagList.add(tag);
            }
        }
        mergedItem.tags = tagList.toArray(new String[tagList.size()]);

        final List<String> langList = new ArrayList<>(item1.getLanguages().length + item2.getLanguages().length);
        for (final String lang : item1.getLanguages()) {
            langList.add(lang);
        }
        for (final String lang : item2.getLanguages()) {
            if (!langList.contains(lang)) {
                langList.add(lang);
            }
        }
        mergedItem.languages = langList.toArray(new String[langList.size()]);

        final List<String> roleList = new ArrayList<>(item1.getRoles().length + item2.getRoles().length);
        for (final String role : item1.getRoles()) {
            roleList.add(role);
        }
        for (final String role : item2.getRoles()) {
            if (!roleList.contains(role)) {
                roleList.add(role);
            }
        }
        mergedItem.roles = roleList.toArray(new String[roleList.size()]);

        mergedItem.kinds = concatKinds(item1.kinds, item2.kinds);
        mergedItem.timestamp = item2.timestamp;
        mergedItem.queryFreq = item1.queryFreq + item2.queryFreq;
        mergedItem.docFreq = item1.docFreq + item2.docFreq;
        mergedItem.userBoost = item2.userBoost;
        mergedItem.emptySource = item2.emptySource;

        return mergedItem;
    }

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
                + ", userBoost=" + userBoost + ", readings=" + Arrays.toString(readings) + ", fields=" + Arrays.toString(fields)
                + ", tags=" + Arrays.toString(tags) + ", roles=" + Arrays.toString(roles) + ", languages=" + Arrays.toString(languages)
                + ", kinds=" + Arrays.toString(kinds) + ", emptySource=" + emptySource + ", id=" + id + "]";
    }
}
