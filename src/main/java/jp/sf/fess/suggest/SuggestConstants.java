package jp.sf.fess.suggest;

public class SuggestConstants {
    public static final String USER_DICT_ENCODING = "fess.user.dict.encoding";

    public static final String USER_DICT_PATH = "fess.user.dict.path";

    public static class SuggestFieldNames {
        public static final String TEXT = "text_s";

        public static final String READING = "reading_s_m";

        public static final String COUNT = "count_i";

        public static final String FIELD_NAME = "fieldname_s_m";

        public static final String LABELS = "label_s_m";

        public static final String ROLES = "role_s_m";
    }

    private SuggestConstants() {
    }
}
