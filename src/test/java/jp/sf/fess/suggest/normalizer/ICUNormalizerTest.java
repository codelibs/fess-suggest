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

package jp.sf.fess.suggest.normalizer;


import junit.framework.TestCase;

public class ICUNormalizerTest extends TestCase {
    public void test_FullwidthHalfwidth() {
        ICUNormalizer normalizer = new ICUNormalizer();
        normalizer.transliteratorId = "Fullwidth-Halfwidth";
        normalizer.start();


        assertEquals("abcd", normalizer.normalize("ａｂｃｄ"));
        assertEquals("みかん", normalizer.normalize("みかん"));
        assertEquals("みかん ﾘﾝｺﾞ", normalizer.normalize("みかん　リンゴ"));
    }

    public void test_AnyLower() {
        ICUNormalizer normalizer = new ICUNormalizer();
        normalizer.transliteratorId = "Any-Lower";
        normalizer.start();

        assertEquals("abcd", normalizer.normalize("ABCD"));
    }

}
