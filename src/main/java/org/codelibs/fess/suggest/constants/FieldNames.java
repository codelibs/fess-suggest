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
package org.codelibs.fess.suggest.constants;

/**
 * This class contains constants for field names used in the Fess Suggest system.
 * These constants represent various field names that are used throughout the application
 * to ensure consistency and avoid hardcoding strings.
 *
 * <ul>
 * <li>{@link #ID} - The unique identifier field.</li>
 * <li>{@link #TEXT} - The text content field.</li>
 * <li>{@link #READING_PREFIX} - The prefix for reading fields.</li>
 * <li>{@link #SCORE} - The score field.</li>
 * <li>{@link #QUERY_FREQ} - The query frequency field.</li>
 * <li>{@link #DOC_FREQ} - The document frequency field.</li>
 * <li>{@link #USER_BOOST} - The user boost field.</li>
 * <li>{@link #KINDS} - The kinds field.</li>
 * <li>{@link #TIMESTAMP} - The timestamp field.</li>
 * <li>{@link #TAGS} - The tags field.</li>
 * <li>{@link #ROLES} - The roles field.</li>
 * <li>{@link #FIELDS} - The fields field.</li>
 * <li>{@link #LANGUAGES} - The languages field.</li>
 * <li>{@link #ARRAY_KEY} - The key for array elements.</li>
 * <li>{@link #ARRAY_VALUE} - The value for array elements.</li>
 * <li>{@link #ANALYZER_SETTINGS_TYPE} - The analyzer settings type field.</li>
 * <li>{@link #ANALYZER_SETTINGS_FIELD_NAME} - The analyzer settings field name.</li>
 * <li>{@link #ANALYZER_SETTINGS_READING_ANALYZER} - The reading analyzer settings field.</li>
 * <li>{@link #ANALYZER_SETTINGS_READING_TERM_ANALYZER} - The reading term analyzer settings field.</li>
 * <li>{@link #ANALYZER_SETTINGS_NORMALIZE_ANALYZER} - The normalize analyzer settings field.</li>
 * <li>{@link #ANALYZER_SETTINGS_CONTENTS_ANALYZER} - The contents analyzer settings field.</li>
 * <li>{@link #ANALYZER_SETTINGS_CONTENTS_READING_ANALYZER} - The contents reading analyzer settings field.</li>
 * </ul>
 *
 * This class cannot be instantiated.
 */
public final class FieldNames {
    public static final String ID = "_id";
    public static final String TEXT = "text";
    public static final String READING_PREFIX = "reading_";
    public static final String SCORE = "score";
    public static final String QUERY_FREQ = "queryFreq";
    public static final String DOC_FREQ = "docFreq";
    public static final String USER_BOOST = "userBoost";
    public static final String KINDS = "kinds";
    public static final String TIMESTAMP = "@timestamp";
    public static final String TAGS = "tags";
    public static final String ROLES = "roles";
    public static final String FIELDS = "fields";
    public static final String LANGUAGES = "languages";

    public static final String ARRAY_KEY = "key";
    public static final String ARRAY_VALUE = "value";

    public static final String ANALYZER_SETTINGS_TYPE = "settingsType";
    public static final String ANALYZER_SETTINGS_FIELD_NAME = "fieldName";
    public static final String ANALYZER_SETTINGS_READING_ANALYZER = "readingAnalyzer";
    public static final String ANALYZER_SETTINGS_READING_TERM_ANALYZER = "readingTermAnalyzer";
    public static final String ANALYZER_SETTINGS_NORMALIZE_ANALYZER = "normalizeAnalyzer";
    public static final String ANALYZER_SETTINGS_CONTENTS_ANALYZER = "contentsAnalyzer";
    public static final String ANALYZER_SETTINGS_CONTENTS_READING_ANALYZER = "contentsReadingAnalyzer";

    // Private constructor to prevent instantiation
    private FieldNames() {
    }
}
