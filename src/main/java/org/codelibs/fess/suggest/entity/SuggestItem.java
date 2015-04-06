package org.codelibs.fess.suggest.entity;

import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.elasticsearch.common.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SuggestItem {
    public enum Kind {
        DOCUMENT("document"), QUERY("query"), USER("user");

        private final String kind;

        private Kind(String kind) {
            this.kind = kind;
        }

        @Override
        public String toString() {
            return kind;
        }
    }

    private final String text;

    private final long queryFreq;

    private final long docFreq;

    private final long userBoost;

    private final String[][] readings;

    private final String[] tags;

    private final String[] roles;

    private final Kind kind;

    public SuggestItem(final String[] text, final String[][] readings, final long score, @Nullable final String[] tags,
            @Nullable final String[] roles, final Kind kind) {
        this.text = String.join(SuggestConstants.TEXT_SEPARATOR, text);
        this.readings = readings;
        this.tags = tags != null ? tags : new String[] {};

        if (roles == null) {
            this.roles = new String[] { SuggestConstants.DEFAULT_ROLE };
        } else {
            this.roles = new String[roles.length + 1];
            this.roles[0] = SuggestConstants.DEFAULT_ROLE;
            for (int i = 0; i < roles.length; i++) {
                this.roles[i + 1] = roles[i];
            }
        }

        this.kind = kind;
        if (kind == Kind.QUERY) {
            this.queryFreq = score;
            this.docFreq = 0;
            this.userBoost = -1;
        } else if (kind == Kind.USER) {
            this.queryFreq = 0;
            this.docFreq = 1;
            this.userBoost = score;
        } else {
            this.queryFreq = 0;
            this.docFreq = score;
            this.userBoost = -1;
        }
    }

    public String getText() {
        return text;
    }

    public long getScore() {
        if (kind == Kind.QUERY) {
            return queryFreq;
        } else if (kind == Kind.USER) {
            return userBoost;
        } else {
            return docFreq;
        }
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

    public Kind getKind() {
        return kind;
    }

    //TODO 最初に一度つくればいい
    public Map<String, Object> toEmptyMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(FieldNames.TEXT, "");

        for (int i = 0; i < readings.length; i++) {
            map.put(FieldNames.READING_PREFIX + i, new String[] {});
        }

        map.put(FieldNames.TAGS, new String[] {});
        map.put(FieldNames.ROLES, new String[] {});
        map.put(FieldNames.KINDS, new String[] {});
        map.put(FieldNames.SCORE, 0L);
        map.put(FieldNames.QUERY_FREQ, 0L);
        map.put(FieldNames.DOC_FREQ, 0L);
        map.put(FieldNames.USER_BOOST, 1L);
        return map;
    }

    public String getId() {
        return String.valueOf(text.hashCode());
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
        script.append("if(!source.kinds.contains(kind)) {source.kinds.add(kind);}");

        return script.toString();
    }

    public Map<String, Object> getScriptParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("text", text);
        for (int i = 0; i < readings.length; i++) {
            params.put("reading" + i, readings[i]);
        }
        params.put("queryFreq", queryFreq);
        params.put("docFreq", docFreq);
        params.put("userBoost", userBoost);
        params.put("tags", tags);
        params.put("roles", roles);
        params.put("kind", kind.toString());

        return params;
    }

}
