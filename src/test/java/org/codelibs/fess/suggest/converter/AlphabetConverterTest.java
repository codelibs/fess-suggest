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

package org.codelibs.fess.suggest.converter;

import org.codelibs.fess.suggest.converter.AlphabetConverter;
import org.codelibs.fess.suggest.converter.SuggestReadingConverter;

import junit.framework.TestCase;

public class AlphabetConverterTest extends TestCase {
    public void test_convert() {
        SuggestReadingConverter converter = new AlphabetConverter();
        converter.start();

        assertEquals("hai", converter.convert("はい").get(0));
        assertTrue(converter.convert("てつや").contains("tetuya"));
        assertTrue(converter.convert("てつや").contains("tetsuya"));
        assertTrue(converter.convert("徹夜").contains("tetuya"));
        assertTrue(converter.convert("徹夜").contains("tetsuya"));
        assertTrue(converter.convert("ﾃﾂﾔ").contains("tetuya"));
        assertTrue(converter.convert("ﾃﾂﾔ").contains("tetsuya"));

        assertEquals(2, converter.convert("ファイナンス").size());
        assertTrue(converter.convert("ファイナンス").contains("fainannsu"));
        assertTrue(converter.convert("ファイナンス").contains("fainansu"));

        assertEquals(4, converter.convert("化粧水つける").size());
        assertTrue(converter.convert("化粧水つける").contains("kesyousuitukeru"));
        assertTrue(converter.convert("化粧水つける").contains("keshousuitsukeru"));
        assertTrue(converter.convert("化粧水つける").contains("kesyousuitukeru"));
        assertTrue(converter.convert("化粧水つける").contains("keshousuitsukeru"));
    }
}
