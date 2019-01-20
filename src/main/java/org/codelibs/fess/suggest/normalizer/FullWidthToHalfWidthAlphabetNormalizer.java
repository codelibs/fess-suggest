/*
 * Copyright 2009-2019 the CodeLibs Project and the Others.
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

public class FullWidthToHalfWidthAlphabetNormalizer implements Normalizer {
    @Override
    public String normalize(final String text, final String field, final String... langs) {
        final char[] chars = new char[text.length()];
        for (int i = 0; i < chars.length; i++) {
            final char c = text.charAt(i);
            if (c >= 'ａ' && c <= 'ｚ') {
                chars[i] = (char) (c - 'ａ' + 'a');
            } else if (c >= 'Ａ' && c <= 'Ｚ') {
                chars[i] = (char) (c - 'Ａ' + 'A');
            } else if (c >= '１' && c <= '０') {
                chars[i] = (char) (c - '１' + '1');
            } else {
                chars[i] = c;
            }
        }
        return new String(chars);
    }
}
