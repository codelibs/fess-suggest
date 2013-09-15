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

import java.util.Enumeration;

import org.junit.Test;

import com.ibm.icu.text.Transliterator;

public class ICUConverterTest {

    @Test
    public void convertFullwidthHalfwidth() {
        final ICUConverter icuConverter = new ICUConverter(
                "Fullwidth-Halfwidth");
        assertThat(icuConverter.convert("123"), is("123"));
        assertThat(icuConverter.convert("１２３"), is("123"));
        assertThat(icuConverter.convert("abc"), is("abc"));
        assertThat(icuConverter.convert("ａｂｃ"), is("abc"));
        assertThat(icuConverter.convert("ABC"), is("ABC"));
        assertThat(icuConverter.convert("ＡＢＣ"), is("ABC"));
        assertThat(icuConverter.convert("!\"#$%&'()~"), is("!\"#$%&'()~"));
        assertThat(icuConverter.convert("！”＃＄％＆’（）〜"), is("!”#$%&’()〜"));
        assertThat(icuConverter.convert("-^\\=~|"), is("-^\\=~|"));
        assertThat(icuConverter.convert("ー＾￥＝〜｜"), is("ｰ^¥=〜|"));
        assertThat(icuConverter.convert("@[;:]`{+*}"), is("@[;:]`{+*}"));
        assertThat(icuConverter.convert("＠「；：」｀｛＋＊｝"), is("@｢;:｣`{+*}"));
        assertThat(icuConverter.convert(",./<>?_"), is(",./<>?_"));
        assertThat(icuConverter.convert("、。・＜＞？＿"), is("､｡･<>?_"));
        assertThat(icuConverter.convert("あいうえお"), is("あいうえお"));
        assertThat(icuConverter.convert("アイウエオ"), is("ｱｲｳｴｵ"));
        assertThat(icuConverter.convert("ｱｲｳｴｵ"), is("ｱｲｳｴｵ"));
        assertThat(icuConverter.convert("漢字"), is("漢字"));
        assertThat(icuConverter.convert(" 　"), is("  "));

    }

    @Test
    public void convertHalfwidthFullwidth() {
        final ICUConverter icuConverter = new ICUConverter(
                "Halfwidth-Fullwidth");
        assertThat(icuConverter.convert("123"), is("１２３"));
        assertThat(icuConverter.convert("１２３"), is("１２３"));
        assertThat(icuConverter.convert("abc"), is("ａｂｃ"));
        assertThat(icuConverter.convert("ａｂｃ"), is("ａｂｃ"));
        assertThat(icuConverter.convert("ABC"), is("ＡＢＣ"));
        assertThat(icuConverter.convert("ＡＢＣ"), is("ＡＢＣ"));
        assertThat(icuConverter.convert("!\"#$%&'()~"), is("！＂＃＄％＆＇（）～"));
        assertThat(icuConverter.convert("！”＃＄％＆’（）〜"), is("！”＃＄％＆’（）〜"));
        assertThat(icuConverter.convert("-^\\=~|"), is("－＾＼＝～｜"));
        assertThat(icuConverter.convert("ー＾￥＝〜｜"), is("ー＾￥＝〜｜"));
        assertThat(icuConverter.convert("@[;:]`{+*}"), is("＠［；：］｀｛＋＊｝"));
        assertThat(icuConverter.convert("＠「；：」｀｛＋＊｝"), is("＠「；：」｀｛＋＊｝"));
        assertThat(icuConverter.convert(",./<>?_"), is("，．／＜＞？＿"));
        assertThat(icuConverter.convert("、。・＜＞？＿"), is("、。・＜＞？＿"));
        assertThat(icuConverter.convert("あいうえお"), is("あいうえお"));
        assertThat(icuConverter.convert("アイウエオ"), is("アイウエオ"));
        assertThat(icuConverter.convert("ｱｲｳｴｵ"), is("アイウエオ"));
        assertThat(icuConverter.convert("漢字"), is("漢字"));
        assertThat(icuConverter.convert(" 　"), is("　　"));

    }

    @Test
    public void convertKatakanaHiragana() {
        final ICUConverter icuConverter = new ICUConverter("Katakana-Hiragana");
        assertThat(icuConverter.convert("123"), is("123"));
        assertThat(icuConverter.convert("１２３"), is("１２３"));
        assertThat(icuConverter.convert("abc"), is("abc"));
        assertThat(icuConverter.convert("ａｂｃ"), is("ａｂｃ"));
        assertThat(icuConverter.convert("ABC"), is("ABC"));
        assertThat(icuConverter.convert("ＡＢＣ"), is("ＡＢＣ"));
        assertThat(icuConverter.convert("!\"#$%&'()~"), is("!\"#$%&'()~"));
        assertThat(icuConverter.convert("！”＃＄％＆’（）〜"), is("！”＃＄％＆’（）〜"));
        assertThat(icuConverter.convert("-^\\=~|"), is("-^\\=~|"));
        assertThat(icuConverter.convert("ー＾￥＝〜｜"), is("ー＾￥＝〜｜"));
        assertThat(icuConverter.convert("@[;:]`{+*}"), is("@[;:]`{+*}"));
        assertThat(icuConverter.convert("＠「；：」｀｛＋＊｝"), is("＠「；：」｀｛＋＊｝"));
        assertThat(icuConverter.convert(",./<>?_"), is(",./<>?_"));
        assertThat(icuConverter.convert("、。・＜＞？＿"), is("、。・＜＞？＿"));
        assertThat(icuConverter.convert("あいうえお"), is("あいうえお"));
        assertThat(icuConverter.convert("アイウエオ"), is("あいうえお"));
        assertThat(icuConverter.convert("ｱｲｳｴｵ"), is("あいうえお"));
        assertThat(icuConverter.convert("漢字"), is("漢字"));
        assertThat(icuConverter.convert(" 　"), is(" 　"));

    }

    @Test
    public void convertHiraganaKatakana() {
        final ICUConverter icuConverter = new ICUConverter("Hiragana-Katakana");
        assertThat(icuConverter.convert("123"), is("123"));
        assertThat(icuConverter.convert("１２３"), is("１２３"));
        assertThat(icuConverter.convert("abc"), is("abc"));
        assertThat(icuConverter.convert("ａｂｃ"), is("ａｂｃ"));
        assertThat(icuConverter.convert("ABC"), is("ABC"));
        assertThat(icuConverter.convert("ＡＢＣ"), is("ＡＢＣ"));
        assertThat(icuConverter.convert("!\"#$%&'()~"), is("!\"#$%&'()~"));
        assertThat(icuConverter.convert("！”＃＄％＆’（）〜"), is("！”＃＄％＆’（）〜"));
        assertThat(icuConverter.convert("-^\\=~|"), is("-^\\=~|"));
        assertThat(icuConverter.convert("ー＾￥＝〜｜"), is("ー＾￥＝〜｜"));
        assertThat(icuConverter.convert("@[;:]`{+*}"), is("@[;:]`{+*}"));
        assertThat(icuConverter.convert("＠「；：」｀｛＋＊｝"), is("＠「；：」｀｛＋＊｝"));
        assertThat(icuConverter.convert(",./<>?_"), is(",./<>?_"));
        assertThat(icuConverter.convert("、。・＜＞？＿"), is("、。・＜＞？＿"));
        assertThat(icuConverter.convert("あいうえお"), is("アイウエオ"));
        assertThat(icuConverter.convert("アイウエオ"), is("アイウエオ"));
        assertThat(icuConverter.convert("ｱｲｳｴｵ"), is("アイウエオ"));
        assertThat(icuConverter.convert("漢字"), is("漢字"));
        assertThat(icuConverter.convert(" 　"), is(" 　"));

    }

    private void printTransliteratorIDs() {
        final Enumeration<String> availableIDs = Transliterator
                .getAvailableIDs();
        while (availableIDs.hasMoreElements()) {
            System.out.println("ID: " + availableIDs.nextElement());
        }
    }

}
