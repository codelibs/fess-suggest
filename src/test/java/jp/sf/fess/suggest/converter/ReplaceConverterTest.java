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

public class ReplaceConverterTest {

    @Test
    public void convert() {
        final ReplaceConverter replaceConverter = new ReplaceConverter();
        assertThat(replaceConverter.convert("123abcあいうえおアイウエオ"),
                is("123abcあいうえおアイウエオ"));
        replaceConverter.addReplaceString("abc", "ABC");
        assertThat(replaceConverter.convert("123abcあいうえおアイウエオ"),
                is("123ABCあいうえおアイウエオ"));
    }

}
