/*
 * Copyright 2012-2024 CodeLibs Project and the Others.
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

/**
 * Represents an elevate word entity with associated properties such as boost, readings, fields, tags, and roles.
 */
public class ElevateWord {

    /**
     * The elevate word.
     */
    protected final String elevateWord;

    /**
     * The boost value associated with the elevate word.
     */
    protected final float boost;

    /**
     * The list of readings associated with the elevate word.
     */
    protected final List<String> readings;

    /**
     * The list of fields associated with the elevate word.
     */
    protected final List<String> fields;

    /**
     * The list of tags associated with the elevate word.
     */
    protected final List<String> tags;

    /**
     * The list of roles associated with the elevate word.
     */
    protected final List<String> roles;

    /**
     * Constructs an ElevateWord instance with the specified properties.
     *
     * @param elevateWord the elevate word
     * @param boost the boost value
     * @param readings the list of readings
     * @param fields the list of fields
     * @param tags the list of tags
     * @param roles the list of roles
     */
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

    /**
     * Returns the boost value.
     *
     * @return the boost value
     */
    public float getBoost() {
        return boost;
    }

    /**
     * Returns the list of readings.
     *
     * @return the list of readings
     */
    public List<String> getReadings() {
        return readings;
    }

    /**
     * Returns the list of fields.
     *
     * @return the list of fields
     */
    public List<String> getFields() {
        return fields;
    }

    /**
     * Returns the list of tags.
     *
     * @return the list of tags
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Returns the list of roles.
     *
     * @return the list of roles
     */
    public List<String> getRoles() {
        return roles;
    }

    /**
     * Converts this ElevateWord instance to a SuggestItem.
     *
     * @return a SuggestItem representing this ElevateWord
     */
    public SuggestItem toSuggestItem() {
        final String[][] readingArray =
                this.getReadings().stream().map(reading -> new String[] { reading }).toArray(count -> new String[count][]);
        return new SuggestItem(new String[] { this.getElevateWord() }, readingArray, fields.toArray(new String[fields.size()]), 1, 0,
                this.getBoost(), this.getTags().toArray(new String[this.getTags().size()]),
                this.getRoles().toArray(new String[this.getRoles().size()]), null, SuggestItem.Kind.USER);
    }
}
