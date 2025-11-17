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

public class NormalizerChainTest {

    @Test
    public void test_constructor() throws Exception {
        NormalizerChain chain = new NormalizerChain();
        assertNotNull(chain);
    }

    @Test
    public void test_singleNormalizer() throws Exception {
        NormalizerChain chain = new NormalizerChain();
        chain.add(new FullWidthToHalfWidthAlphabetNormalizer());

        String result = chain.normalize("ａｂｃ", "field");

        assertEquals("abc", result);
    }

    @Test
    public void test_multipleNormalizers() throws Exception {
        NormalizerChain chain = new NormalizerChain();
        chain.add(new FullWidthToHalfWidthAlphabetNormalizer());
        chain.add(new HankakuKanaToZenkakuKana());

        String result = chain.normalize("ａｂｃ ｶﾞ", "field");

        assertEquals("abc ガ", result);
    }

    @Test
    public void test_normalizerOrder() throws Exception {
        NormalizerChain chain = new NormalizerChain();
        chain.add((text, field, langs) -> text.toUpperCase());
        chain.add((text, field, langs) -> text.replace("A", "X"));

        String result = chain.normalize("abc", "field");

        assertEquals("XBC", result);
    }

    @Test
    public void test_emptyChain() throws Exception {
        NormalizerChain chain = new NormalizerChain();

        String result = chain.normalize("test", "field");

        assertEquals("test", result);
    }

    @Test
    public void test_normalizeWithLanguages() throws Exception {
        NormalizerChain chain = new NormalizerChain();
        chain.add(new FullWidthToHalfWidthAlphabetNormalizer());

        String result = chain.normalize("ａｂｃ", "field", "en", "ja");

        assertEquals("abc", result);
    }

    @Test
    public void test_addMultipleNormalizersInOrder() throws Exception {
        NormalizerChain chain = new NormalizerChain();
        Normalizer normalizer1 = (text, field, langs) -> text + "-1";
        Normalizer normalizer2 = (text, field, langs) -> text + "-2";
        Normalizer normalizer3 = (text, field, langs) -> text + "-3";

        chain.add(normalizer1);
        chain.add(normalizer2);
        chain.add(normalizer3);

        String result = chain.normalize("test", "field");

        assertEquals("test-1-2-3", result);
    }

    @Test
    public void test_chainWithCustomNormalizer() throws Exception {
        NormalizerChain chain = new NormalizerChain();
        chain.add((text, field, langs) -> text.trim());
        chain.add((text, field, langs) -> text.toLowerCase());
        chain.add((text, field, langs) -> text.replace(" ", "_"));

        String result = chain.normalize("  TEST CASE  ", "field");

        assertEquals("test_case", result);
    }

    @Test
    public void test_nullInput() throws Exception {
        NormalizerChain chain = new NormalizerChain();
        chain.add(new FullWidthToHalfWidthAlphabetNormalizer());

        String result = chain.normalize(null, "field");

        assertEquals(null, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_addNullNormalizer() throws Exception {
        NormalizerChain chain = new NormalizerChain();
        chain.add(null);
    }

    @Test
    public void test_chainWithNormalizerReturningNull() throws Exception {
        NormalizerChain chain = new NormalizerChain();
        chain.add((text, field, langs) -> null);
        chain.add((text, field, langs) -> text.toUpperCase());

        String result = chain.normalize("test", "field");

        assertEquals(null, result);
    }
}
