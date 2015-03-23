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

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

import org.codelibs.fess.suggest.normalizer.SuggestIntegrateNormalizer;
import org.codelibs.fess.suggest.normalizer.SuggestNormalizer;
import org.codelibs.fess.suggest.util.SuggestUtil;

public class SuggestIntegrateNormalizerTest extends TestCase {
    public void test_normalize() {
        try {
            SuggestIntegrateNormalizer normalizer = new SuggestIntegrateNormalizer();
            Map<String, String> map = new HashMap<String, String>();

            map.put("transliteratorId", "Fullwidth-Halfwidth");
            SuggestNormalizer icuNormalizer = SuggestUtil.createNormalizer("org.codelibs.fess.suggest.normalizer.ICUNormalizer", map);
            normalizer.addNormalizer(icuNormalizer);

            map = new HashMap<String, String>();
            map.put("transliteratorId", "Any-Lower");
            icuNormalizer = SuggestUtil.createNormalizer("org.codelibs.fess.suggest.normalizer.ICUNormalizer", map);
            normalizer.addNormalizer(icuNormalizer);
            normalizer.start();

            assertEquals("abcd", normalizer.normalize("ABCD"));
            assertEquals("abcd aaa", normalizer.normalize("abcd　ＡＡＡ"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
