/*
 * Copyright 2009-2014 the CodeLibs Project and the Others.
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

package jp.sf.fess.suggest;

public final class SuggestConstants {
    public static final String UTF_8 = "UTF-8";

    public static final String USER_DICT_ENCODING = "fess.user.dict.encoding";

    public static final String USER_DICT_PATH = "fess.user.dict.path";

    public static final String SEGMENT_QUERY = "1";

    public static final String SEGMENT_ELEVATE = "0";

    public static final String BADWORD_FILENAME = "badword.txt";

    public static class SuggestFieldNames {
        public static final String ID = "id";

        public static final String TEXT = "text_s";

        public static final String READING = "reading_s_m";

        public static final String COUNT = "count_i";

        public static final String FIELD_NAME = "fieldname_s_m";

        public static final String LABELS = "label_s_m";

        public static final String ROLES = "role_s_m";

        public static final String EXPIRES = "expires_dt";

        public static final String SEGMENT = "segment";

        public static final String BOOST = "boost_l";

        private SuggestFieldNames() {
        }
    }

    private SuggestConstants() {
    }
}
