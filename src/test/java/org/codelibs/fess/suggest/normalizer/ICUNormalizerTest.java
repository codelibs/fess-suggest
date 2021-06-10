/*
 * Copyright 2012-2021 CodeLibs Project and the Others.
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

import junit.framework.TestCase;

public class ICUNormalizerTest extends TestCase {
    public void test_FullwidthHalfwidth() {
        ICUNormalizer normalizer = new ICUNormalizer("Fullwidth-Halfwidth");

        assertEquals("abcd", normalizer.normalize("ａｂｃｄ", null));
        assertEquals("みかん", normalizer.normalize("みかん", null));
        assertEquals("みかん ﾘﾝｺﾞ", normalizer.normalize("みかん　リンゴ", null));
    }

    public void test_AnyLower() {
        ICUNormalizer normalizer = new ICUNormalizer("Any-Lower");
        assertEquals("abcd", normalizer.normalize("ABCD", null));
    }

    public void test_HalfwidthFullwidth() {
        ICUNormalizer normalizer = new ICUNormalizer("Halfwidth-Fullwidth");
        assertEquals("ケンサク", normalizer.normalize("ｹﾝｻｸ", null));
    }

}