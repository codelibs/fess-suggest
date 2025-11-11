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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class FullWidthToHalfWidthAlphabetNormalizerTest {

    @Test
    public void test_constructor() throws Exception {
        FullWidthToHalfWidthAlphabetNormalizer normalizer = new FullWidthToHalfWidthAlphabetNormalizer();
        assertNotNull(normalizer);
    }

    @Test
    public void test_lowercaseAlphabet() throws Exception {
        FullWidthToHalfWidthAlphabetNormalizer normalizer = new FullWidthToHalfWidthAlphabetNormalizer();

        String result = normalizer.normalize("ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ", "field");

        assertEquals("abcdefghijklmnopqrstuvwxyz", result);
    }

    @Test
    public void test_uppercaseAlphabet() throws Exception {
        FullWidthToHalfWidthAlphabetNormalizer normalizer = new FullWidthToHalfWidthAlphabetNormalizer();

        String result = normalizer.normalize("ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ", "field");

        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ", result);
    }

    @Test
    public void test_mixedAlphabet() throws Exception {
        FullWidthToHalfWidthAlphabetNormalizer normalizer = new FullWidthToHalfWidthAlphabetNormalizer();

        String result = normalizer.normalize("ＡｂＣｄＥｆ", "field");

        assertEquals("AbCdEf", result);
    }

    @Test
    public void test_emptyString() throws Exception {
        FullWidthToHalfWidthAlphabetNormalizer normalizer = new FullWidthToHalfWidthAlphabetNormalizer();

        String result = normalizer.normalize("", "field");

        assertEquals("", result);
    }

    @Test
    public void test_noConversion() throws Exception {
        FullWidthToHalfWidthAlphabetNormalizer normalizer = new FullWidthToHalfWidthAlphabetNormalizer();

        String result = normalizer.normalize("abc123", "field");

        assertEquals("abc123", result);
    }

    @Test
    public void test_mixedWithOtherCharacters() throws Exception {
        FullWidthToHalfWidthAlphabetNormalizer normalizer = new FullWidthToHalfWidthAlphabetNormalizer();

        String result = normalizer.normalize("ａｂｃ あいう 123", "field");

        assertEquals("abc あいう 123", result);
    }

    @Test
    public void test_fullWidthSpacePreserved() throws Exception {
        FullWidthToHalfWidthAlphabetNormalizer normalizer = new FullWidthToHalfWidthAlphabetNormalizer();

        String result = normalizer.normalize("ａ　ｂ", "field");

        assertEquals("a　b", result);
    }

    @Test
    public void test_withLanguages() throws Exception {
        FullWidthToHalfWidthAlphabetNormalizer normalizer = new FullWidthToHalfWidthAlphabetNormalizer();

        String result = normalizer.normalize("ａｂｃ", "field", "en", "ja");

        assertEquals("abc", result);
    }

    @Test
    public void test_japaneseCharactersUnchanged() throws Exception {
        FullWidthToHalfWidthAlphabetNormalizer normalizer = new FullWidthToHalfWidthAlphabetNormalizer();

        String result = normalizer.normalize("日本語", "field");

        assertEquals("日本語", result);
    }
}
