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
package org.codelibs.fess.suggest.converter;

import java.io.IOException;
import java.util.List;

/**
 * Interface for converting text into its reading form.
 */
public interface ReadingConverter {

    /**
     * Returns the maximum number of readings.
     *
     * @return the maximum number of readings, default is 10.
     */
    default int getMaxReadingNum() {
        return 10;
    }

    /**
     * Initializes the converter.
     *
     * @throws IOException if an I/O error occurs during initialization.
     */
    void init() throws IOException;

    /**
     * Converts the given text into a list of readings based on the specified field and languages.
     *
     * @param text the text to be converted.
     * @param field the field to be used for conversion.
     * @param langs the languages to be used for conversion.
     * @return a list of readings for the given text.
     * @throws IOException if an I/O error occurs during conversion.
     */
    List<String> convert(String text, final String field, String... langs) throws IOException;
}
