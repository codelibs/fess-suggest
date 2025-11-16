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
 * Normalizes full-width alphanumeric characters to half-width alphanumeric characters.
 */
public class FullWidthToHalfWidthAlphabetNormalizer implements Normalizer {
    /**
     * Constructs a new {@link FullWidthToHalfWidthAlphabetNormalizer}.
     */
    public FullWidthToHalfWidthAlphabetNormalizer() {
        // nothing
    }

    @Override
    public String normalize(final String text, final String field, final String... langs) {
        if (text == null) {
            return null;
        }
        final char[] chars = new char[text.length()];
        for (int i = 0; i < chars.length; i++) {
            final char c = text.charAt(i);
            if (c >= 'ａ' && c <= 'ｚ') {
                chars[i] = (char) (c - 'ａ' + 'a');
            } else if (c >= 'Ａ' && c <= 'Ｚ') {
                chars[i] = (char) (c - 'Ａ' + 'A');
            } else if (c >= '０' && c <= '９') {
                chars[i] = (char) (c - '０' + '0');
            } else {
                chars[i] = c;
            }
        }
        return new String(chars);
    }
}
