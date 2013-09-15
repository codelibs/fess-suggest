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

public class ReadingConverterTest {
    private final ReadingConverter readingConverter = new ReadingConverter();

    @Test
    public void convertHiragana() {
        assertThat(readingConverter.convert("あいうえお"), is("アイウエオ"));
    }

    @Test
    public void convertKatakana() {
        assertThat(readingConverter.convert("アイウエオ"), is("アイウエオ"));
        assertThat(readingConverter.convert("ｱｲｳｴｵ"), is("アイウエオ"));
    }

    @Test
    public void convertKanji() {
        assertThat(readingConverter.convert("検索"), is("ケンサク"));
        assertThat(readingConverter.convert("琥珀"), is("コハク"));
    }

    @Test
    public void convertAlphabet() {
        assertThat(readingConverter.convert("abc"), is("abc"));
        assertThat(readingConverter.convert("ABC"), is("ABC"));
        assertThat(readingConverter.convert("ａｂｃ"), is("ａｂｃ"));
        assertThat(readingConverter.convert("ＡＢＣ"), is("エイビーシー"));
    }

    @Test
    public void convertDigit() {
        assertThat(readingConverter.convert("123"), is("123"));
        assertThat(readingConverter.convert("１２３"), is("イチニサン"));
    }

    @Test
    public void convertSymbol() {
        assertThat(readingConverter.convert("@"), is("@"));
        assertThat(readingConverter.convert("＠"), is("＠"));
    }

    @Test
    public void convert() {
        assertThat(readingConverter.convert("TEST用の文字列"), is("TESTヨウノモジレツ"));
        assertThat(readingConverter.convert("メールアドレスtest@example.com"),
                is("メールアドレスtest@example.com"));
    }
}
