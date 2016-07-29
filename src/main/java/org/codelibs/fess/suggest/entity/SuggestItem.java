package org.codelibs.fess.suggest.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private LocalDateTime timestamp;

    private long queryFreq;

    private long docFreq;

    private float userBoost;

    private String[][] readings;

    private String[] fields;

    private String[] tags;

    private String[] roles;

    private Kind kind;

    private Map<String, Object> emptySource;

    private String id;

    private SuggestItem() {
    }

    public SuggestItem(final String[] text, final String[][] readings, final String[] fields, final long score, final float userBoost,
            @Nullable final String[] tags, @Nullable final String[] roles, final Kind kind) {
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

        this.kind = kind;
        if (kind == Kind.QUERY) {
            this.queryFreq = score;
            this.docFreq = 0;
            this.userBoost = 1;
        } else if (kind == Kind.USER) {
            this.queryFreq = 0;
            this.docFreq = 1;
            this.userBoost = userBoost;
        } else {
            this.queryFreq = 0;
            this.docFreq = score;
            this.userBoost = 1;
        }

        this.timestamp = LocalDateTime.now();
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

    public String[] getFields() {
        return fields;
    }

    public Kind getKind() {
        return kind;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
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
        map.put(FieldNames.KINDS, new String[] {});
        map.put(FieldNames.SCORE, 1.0F);
        map.put(FieldNames.QUERY_FREQ, 0L);
        map.put(FieldNames.DOC_FREQ, 0L);
        map.put(FieldNames.USER_BOOST, 1.0F);
        map.put(FieldNames.TIMESTAMP, LocalDateTime.now());
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
        map.put(FieldNames.KINDS, new String[] { kind.toString() });
        map.put(FieldNames.QUERY_FREQ, queryFreq);
        map.put(FieldNames.DOC_FREQ, docFreq);
        map.put(FieldNames.USER_BOOST, userBoost);
        map.put(FieldNames.SCORE, (queryFreq + docFreq) * userBoost);
        map.put(FieldNames.TIMESTAMP, timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        return map;
    }

    public Map<String, Object> getUpdatedSource(final Map<String, Object> existingSource) {
        final Map<String, Object> map = new HashMap<>();
        map.put(FieldNames.TEXT, text);

        for (int i = 0; i < readings.length; i++) {
            final Object readingObj = existingSource.get(FieldNames.READING_PREFIX + i);
            if (readingObj == null) {
                map.put(FieldNames.READING_PREFIX + i, readings[i]);
            } else {
                final List<String> existingValues = (List) readingObj;
                concatValues(existingValues, readings[i]);
                map.put(FieldNames.READING_PREFIX + i, existingValues);
            }
        }

        final Object fieldsObj = existingSource.get(FieldNames.FIELDS);
        if (fieldsObj == null) {
            map.put(FieldNames.FIELDS, fields);
        } else {
            @SuppressWarnings("unchecked")
            final List<String> existingValues = (List) fieldsObj;
            concatValues(existingValues, fields);
            map.put(FieldNames.FIELDS, existingValues);
        }

        final Object tagsObj = existingSource.get(FieldNames.TAGS);
        if (tagsObj == null) {
            map.put(FieldNames.TAGS, tags);
        } else {
            @SuppressWarnings("unchecked")
            final List<String> existingValues = (List) tagsObj;
            concatValues(existingValues, tags);
            map.put(FieldNames.TAGS, existingValues);
        }

        final Object rolesObj = existingSource.get(FieldNames.ROLES);
        if (rolesObj == null) {
            map.put(FieldNames.ROLES, rolesObj);
        } else {
            @SuppressWarnings("unchecked")
            final List<String> existingFields = (List) rolesObj;
            concatValues(existingFields, roles);
            map.put(FieldNames.ROLES, existingFields);
        }

        final Object kindsObj = existingSource.get(FieldNames.KINDS);
        if (kindsObj == null) {
            map.put(FieldNames.KINDS, new String[] { kind.toString() });
        } else {
            @SuppressWarnings("unchecked")
            final List<String> existingFields = (List) kindsObj;
            concatValues(existingFields, kind.toString());
            map.put(FieldNames.KINDS, existingFields);
        }

        final long updatedQueryFreq;
        final Object queryFreqObj = existingSource.get(FieldNames.QUERY_FREQ);
        if (queryFreqObj == null) {
            updatedQueryFreq = queryFreq;
        } else {
            @SuppressWarnings("unchecked")
            final Long existingValue = Long.parseLong(queryFreqObj.toString());
            updatedQueryFreq = queryFreq + existingValue;
        }
        map.put(FieldNames.QUERY_FREQ, updatedQueryFreq);

        final long updatedDocFreq;
        final Object docFreqObj = existingSource.get(FieldNames.DOC_FREQ);
        if (docFreqObj == null) {
            updatedDocFreq = docFreq;
        } else {
            @SuppressWarnings("unchecked")
            final Long existingValue = Long.parseLong(docFreqObj.toString());
            updatedDocFreq = docFreq + existingValue;
        }
        map.put(FieldNames.DOC_FREQ, updatedDocFreq);

        map.put(FieldNames.USER_BOOST, userBoost);
        map.put(FieldNames.SCORE, (updatedQueryFreq + updatedDocFreq) * userBoost);
        map.put(FieldNames.TIMESTAMP, timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        return map;
    }

    protected void concatValues(final List<String> dest, final String... newValues) {
        for (final String value : newValues) {
            if (!dest.contains(value)) {
                dest.add(value);
            }
        }
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

        mergedItem.kind = item2.kind;
        mergedItem.timestamp = item2.timestamp;
        mergedItem.queryFreq = item1.queryFreq + item2.queryFreq;
        mergedItem.docFreq = item1.docFreq + item2.docFreq;
        mergedItem.userBoost = item2.userBoost;
        mergedItem.emptySource = item2.emptySource;

        return mergedItem;
    }

    public String getScript() {
        final StringBuilder script = new StringBuilder(1000);

        // define vars
        script.append("def source=ctx._source;");

        //text
        script.append("source.text=text;");

        //readings
        for (int i = 0; i < readings.length; i++) {
            script.append("source[\"reading_").append(i).append("\"]").append("=reading").append(i).append(';');
        }

        //fields
        script.append("sourceFields=source.fields; fields.each{ if(!sourceFields.contains(it)) sourceFields.add(it);};");

        //score
        script.append("source.queryFreq+=queryFreq;");
        script.append("source.docFreq+=docFreq;");
        script.append("if(userBoost >= 0) source.userBoost=userBoost;");
        script.append("source.score=(source.queryFreq + source.docFreq) * source.userBoost;");

        //tags
        script.append("sourceTags=source.tags; tags.each{ if(!sourceTags.contains(it)) sourceTags.add(it);};");

        //roles
        script.append("sourceRoles=source.roles; roles.each{ if(!sourceRoles.contains(it)) sourceRoles.add(it);};");

        //kind
        script.append("if(!source.kinds.contains(kind)) {source.kinds.add(kind);};");

        //timestamp
        script.append("source['@timestamp']=timestamp;");

        return script.toString();
    }

    public Map<String, Object> getScriptParams() {
        final Map<String, Object> params = new HashMap<>();
        params.put("text", text);
        for (int i = 0; i < readings.length; i++) {
            params.put("reading" + i, readings[i]);
        }
        params.put("fields", fields);
        params.put("queryFreq", queryFreq);
        params.put("docFreq", docFreq);
        params.put("userBoost", userBoost);
        params.put("tags", tags);
        params.put("roles", roles);
        params.put("kind", kind.toString());
        params.put("timestamp", timestamp);

        return params;
    }

    public boolean isNgWord(final String[] badWords) {
        for (final String badWord : badWords) {
            if (text.contains(badWord)) {
                return true;
            }
        }
        return false;
    }

}
