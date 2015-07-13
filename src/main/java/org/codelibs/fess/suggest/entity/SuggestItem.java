package org.codelibs.fess.suggest.entity;

import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.elasticsearch.common.Nullable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class SuggestItem implements Serializable {
    public enum Kind {
        DOCUMENT("document"), QUERY("query"), USER("user");

        private final String kind;

        Kind(String kind) {
            this.kind = kind;
        }

        @Override
        public String toString() {
            return kind;
        }
    }

    private final String text;

    private final LocalDateTime timestamp;

    private final long queryFreq;

    private final long docFreq;

    private final float userBoost;

    private final String[][] readings;

    private final String[] fields;

    private final String[] tags;

    private final String[] roles;

    private final Kind kind;

    private final Map<String, Object> emptySource;

    public SuggestItem(final String[] text, final String[][] readings, final String[] fields, final long score, final float userBoost,
            @Nullable final String[] tags, @Nullable final String[] roles, final Kind kind) {
        this.text = String.join(SuggestConstants.TEXT_SEPARATOR, text);
        this.readings = readings;
        this.fields = fields;
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

        timestamp = LocalDateTime.now();
        emptySource = createEmptyMap();
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
        Map<String, Object> map = new HashMap<>();
        map.put(FieldNames.TEXT, "");

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
        return SuggestUtil.createSuggestTextId(text);
    }

    public String getScript() {
        StringBuilder script = new StringBuilder(100);

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
        Map<String, Object> params = new HashMap<>();
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

    public boolean isNgWord(String[] ngWords) {
        for (String ngWord : ngWords) {
            if (text.contains(ngWord)) {
                return true;
            }
        }
        return false;
    }

}
