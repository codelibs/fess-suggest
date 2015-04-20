package org.codelibs.fess.suggest.entity;

import java.io.Serializable;
import java.util.List;

public class ElevateWord implements Serializable {
    protected final String elevateWord;
    protected final float boost;
    protected final List<String> readings;
    protected final List<String> fields;

    public ElevateWord(final String elevateWord, final float boost, final List<String> readings, final List<String> fields) {
        this.elevateWord = elevateWord;
        this.boost = boost;
        this.readings = readings;
        this.fields = fields;
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

    public SuggestItem toSuggestItem() {
        return new SuggestItem(new String[] { this.getElevateWord() }, new String[][] { this.getReadings().toArray(
                new String[this.getReadings().size()]) }, fields.toArray(new String[fields.size()]), 1, this.getBoost(), null, null,
                SuggestItem.Kind.USER);
    }
}
