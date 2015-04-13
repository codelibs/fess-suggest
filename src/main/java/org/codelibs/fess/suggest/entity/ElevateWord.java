package org.codelibs.fess.suggest.entity;

import java.io.Serializable;
import java.util.List;

public class ElevateWord implements Serializable {
    protected final String elevateWord;
    protected final float boost;
    protected final List<String> readings;

    public ElevateWord(final String elevateWord, final float boost, final List<String> readings) {
        this.elevateWord = elevateWord;
        this.boost = boost;
        this.readings = readings;
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

    public SuggestItem toSuggestItem() {
        return new SuggestItem(new String[] { this.getElevateWord() }, new String[][] { this.getReadings().toArray(
                new String[this.getReadings().size()]) }, 1, this.getBoost(), null, null, SuggestItem.Kind.USER);
    }
}
