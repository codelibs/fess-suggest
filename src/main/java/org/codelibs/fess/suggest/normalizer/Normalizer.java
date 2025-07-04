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
package org.codelibs.fess.suggest.normalizer;

/**
 * An interface for normalizing text.
 * Implementations of this interface should provide a method to normalize
 * a given text based on the specified field and optional language parameters.
 */
public interface Normalizer {
    /**
     * Normalizes the given text.
     * @param text The text to normalize
     * @param field The field name
     * @param langs The language
     * @return The normalized text
     */
    String normalize(String text, String field, String... langs);
}
