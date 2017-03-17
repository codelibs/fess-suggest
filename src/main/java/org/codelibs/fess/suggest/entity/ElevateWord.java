package org.codelibs.fess.suggest.entity;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class ElevateWord implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final String elevateWord;
    protected final float boost;
    protected final List<String> readings;
    protected final List<String> fields;
    protected final List<String> tags;
    protected final List<String> roles;

    public ElevateWord(final String elevateWord, final float boost, final List<String> readings, final List<String> fields,
            final List<String> tags, final List<String> roles) {
        this.elevateWord = elevateWord;
        this.boost = boost;
        this.readings = readings;
        this.fields = fields;
        if (tags == null) {
            this.tags = Collections.emptyList();
        } else {
            this.tags = tags;
        }
        if (roles == null) {
            this.roles = Collections.emptyList();
        } else {
            this.roles = roles;
        }
    }

    public String getElevateWord() {
        return elevateWord;
    }

    public float getBoost() {
        return boost;
    }

    public List<String> getReadings() {
        return readings;
    }

    public List<String> getFields() {
        return fields;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<String> getRoles() {
        return roles;
    }

    public SuggestItem toSuggestItem() {
        final String[][] readingArray =
                this.getReadings().stream().map(reading -> new String[] { reading }).toArray((count) -> new String[count][]);
        return new SuggestItem(new String[] { this.getElevateWord() }, readingArray, fields.toArray(new String[fields.size()]), 1, 0,
                this.getBoost(), this.getTags().toArray(new String[this.getTags().size()]), this.getRoles().toArray(
                        new String[this.getRoles().size()]), null, SuggestItem.Kind.USER);
    }
}
