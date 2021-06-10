/*
 * Copyright 2009-2021 the CodeLibs Project and the Others.
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

import java.util.Collections;
import java.util.List;

public class ElevateWord {

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
                this.getReadings().stream().map(reading -> new String[] { reading }).toArray(count -> new String[count][]);
        return new SuggestItem(new String[] { this.getElevateWord() }, readingArray, fields.toArray(new String[fields.size()]), 1, 0,
                this.getBoost(), this.getTags().toArray(new String[this.getTags().size()]),
                this.getRoles().toArray(new String[this.getRoles().size()]), null, SuggestItem.Kind.USER);
    }
}
