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
package org.codelibs.fess.suggest.index.operations;

/**
 * Encapsulates the context parameters commonly passed to content indexing operations.
 * Groups together index name, field configuration, and bad words to simplify method signatures.
 */
public class ContentIndexingContext {

    private final String index;
    private final String[] supportedFields;
    private final String[] tagFieldNames;
    private final String roleFieldName;
    private final String langFieldName;
    private final String[] badWords;

    /**
     * Constructor.
     *
     * @param index The index name
     * @param supportedFields The supported fields
     * @param tagFieldNames The tag field names
     * @param roleFieldName The role field name
     * @param langFieldName The language field name
     * @param badWords The bad words array
     */
    public ContentIndexingContext(final String index, final String[] supportedFields, final String[] tagFieldNames,
            final String roleFieldName, final String langFieldName, final String[] badWords) {
        this.index = index;
        this.supportedFields = supportedFields;
        this.tagFieldNames = tagFieldNames;
        this.roleFieldName = roleFieldName;
        this.langFieldName = langFieldName;
        this.badWords = badWords;
    }

    /**
     * Returns the index name.
     * @return The index name.
     */
    public String getIndex() {
        return index;
    }

    /**
     * Returns the supported fields.
     * @return The supported fields.
     */
    public String[] getSupportedFields() {
        return supportedFields;
    }

    /**
     * Returns the tag field names.
     * @return The tag field names.
     */
    public String[] getTagFieldNames() {
        return tagFieldNames;
    }

    /**
     * Returns the role field name.
     * @return The role field name.
     */
    public String getRoleFieldName() {
        return roleFieldName;
    }

    /**
     * Returns the language field name.
     * @return The language field name.
     */
    public String getLangFieldName() {
        return langFieldName;
    }

    /**
     * Returns the bad words array.
     * @return The bad words array.
     */
    public String[] getBadWords() {
        return badWords;
    }
}
