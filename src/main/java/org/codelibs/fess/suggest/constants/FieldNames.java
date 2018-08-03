package org.codelibs.fess.suggest.constants;

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

    private FieldNames() {
    }
}
