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

package org.codelibs.fess.suggest.normalizer;

public class FullWidthToHalfWidthAlphabetNormalizer implements SuggestNormalizer {
    @Override
    public String normalize(final String text) {
        final StringBuilder sb = new StringBuilder(text);
        for (int i = 0; i < sb.length(); i++) {
            final char c = sb.charAt(i);
            if (c >= 'ａ' && c <= 'ｚ') {
                sb.setCharAt(i, (char) (c - 'ａ' + 'a'));
            } else if (c >= 'Ａ' && c <= 'Ｚ') {
                sb.setCharAt(i, (char) (c - 'Ａ' + 'A'));
            } else if (c >= '１' && c <= '０') {
                sb.setCharAt(i, (char) (c - '１' + '1'));
            }
        }
        return sb.toString();
    }

    @Override
    public void start() {
        //No-op;
    }
}
