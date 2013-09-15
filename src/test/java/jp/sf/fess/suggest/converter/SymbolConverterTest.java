/*
 * Copyright 2009-2013 the Fess Project and the Others.
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

package jp.sf.fess.suggest.converter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class SymbolConverterTest {

    @Test
    public void convert() {
        final SymbolConverter symbolConveter = new SymbolConverter();
        assertThat(symbolConveter.convert("123abcあいうえおアイウエオ"),
                is("123abcあいうえおアイウエオ"));
        symbolConveter.addSymbol(new String[] { "あ", "ア" });
        assertThat(symbolConveter.convert("123abcあいうえおアイウエオ"),
                is("123abc__ID0__いうえお__ID1__イウエオ"));
        symbolConveter.addSymbol(new String[] { "a" });
        assertThat(symbolConveter.convert("123abcあいうえおアイウエオ"),
                is("123__ID2__bc__ID0__いうえお__ID1__イウエオ"));
    }

}
