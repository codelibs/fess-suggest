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
 * This class contains constants used in the Fess Suggest module.
 * It is a utility class and should not be instantiated.
 */
public final class SuggestConstants {
    // Private constructor to prevent instantiation
    private SuggestConstants() {
    }

    /** An empty string constant. */
    public static final String EMPTY_STRING = "";

    /** The system property name for user dictionary encoding. */
    public static final String USER_DICT_ENCODING = "fess.user.dict.encoding";

    /** The system property name for user dictionary path. */
    public static final String USER_DICT_PATH = "fess.user.dict.path";

    /** The text separator. */
    public static final String TEXT_SEPARATOR = " ";

    /** The default role for guest users. */
    public static final String DEFAULT_ROLE = "_guest_";

    /** The default document type. */
    public static final String DEFAULT_TYPE = "_doc";
}
