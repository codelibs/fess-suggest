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

public class HankakuKanaToZenkakuKanaTest {

    @Test
    public void test_constructor() throws Exception {
        HankakuKanaToZenkakuKana normalizer = new HankakuKanaToZenkakuKana();
        assertNotNull(normalizer);
    }

    @Test
    public void test_basicKatakana() throws Exception {
        HankakuKanaToZenkakuKana normalizer = new HankakuKanaToZenkakuKana();

        String result = normalizer.normalize("ｱｲｳｴｵ", "field");

        assertEquals("アイウエオ", result);
    }

    @Test
    public void test_voicedSounds() throws Exception {
        HankakuKanaToZenkakuKana normalizer = new HankakuKanaToZenkakuKana();

        String result = normalizer.normalize("ｶﾞｷﾞｸﾞｹﾞｺﾞ", "field");

        assertEquals("ガギグゲゴ", result);
    }

    @Test
    public void test_semiVoicedSounds() throws Exception {
        HankakuKanaToZenkakuKana normalizer = new HankakuKanaToZenkakuKana();

        String result = normalizer.normalize("ﾊﾟﾋﾟﾌﾟﾍﾟﾎﾟ", "field");

        assertEquals("パピプペポ", result);
    }

    @Test
    public void test_mixedKatakana() throws Exception {
        HankakuKanaToZenkakuKana normalizer = new HankakuKanaToZenkakuKana();

        String result = normalizer.normalize("ｱｲｳｴｵｶﾞｷﾞﾊﾟﾋﾟ", "field");

        assertEquals("アイウエオガギパピ", result);
    }

    @Test
    public void test_emptyString() throws Exception {
        HankakuKanaToZenkakuKana normalizer = new HankakuKanaToZenkakuKana();

        String result = normalizer.normalize("", "field");

        assertEquals("", result);
    }

    @Test
    public void test_singleCharacter() throws Exception {
        HankakuKanaToZenkakuKana normalizer = new HankakuKanaToZenkakuKana();

        String result = normalizer.normalize("ｱ", "field");

        assertEquals("ア", result);
    }

    @Test
    public void test_smallKatakana() throws Exception {
        HankakuKanaToZenkakuKana normalizer = new HankakuKanaToZenkakuKana();

        String result = normalizer.normalize("ｧｨｩｪｫｬｭｮｯ", "field");

        assertEquals("ァィゥェォャュョッ", result);
    }

    @Test
    public void test_longVowelMark() throws Exception {
        HankakuKanaToZenkakuKana normalizer = new HankakuKanaToZenkakuKana();

        String result = normalizer.normalize("ｶｰﾄﾞ", "field");

        assertEquals("カード", result);
    }

    @Test
    public void test_punctuation() throws Exception {
        HankakuKanaToZenkakuKana normalizer = new HankakuKanaToZenkakuKana();

        String result = normalizer.normalize("｡｢｣､･", "field");

        assertEquals("。「」、・", result);
    }

    @Test
    public void test_n() throws Exception {
        HankakuKanaToZenkakuKana normalizer = new HankakuKanaToZenkakuKana();

        String result = normalizer.normalize("ﾆﾎﾝ", "field");

        assertEquals("ニホン", result);
    }

    @Test
    public void test_wo() throws Exception {
        HankakuKanaToZenkakuKana normalizer = new HankakuKanaToZenkakuKana();

        String result = normalizer.normalize("ｦ", "field");

        assertEquals("ヲ", result);
    }

    @Test
    public void test_mixedWithOtherCharacters() throws Exception {
        HankakuKanaToZenkakuKana normalizer = new HankakuKanaToZenkakuKana();

        String result = normalizer.normalize("abc ｱｲｳ 123", "field");

        assertEquals("abc アイウ 123", result);
    }

    @Test
    public void test_wa() throws Exception {
        HankakuKanaToZenkakuKana normalizer = new HankakuKanaToZenkakuKana();

        String result = normalizer.normalize("ﾜ", "field");

        assertEquals("ワ", result);
    }

    @Test
    public void test_withLanguages() throws Exception {
        HankakuKanaToZenkakuKana normalizer = new HankakuKanaToZenkakuKana();

        String result = normalizer.normalize("ｱｲｳ", "field", "ja");

        assertEquals("アイウ", result);
    }

    @Test
    public void test_mergeChar() throws Exception {
        assertEquals('ガ', HankakuKanaToZenkakuKana.mergeChar('ｶ', 'ﾞ'));
        assertEquals('ギ', HankakuKanaToZenkakuKana.mergeChar('ｷ', 'ﾞ'));
        assertEquals('グ', HankakuKanaToZenkakuKana.mergeChar('ｸ', 'ﾞ'));
        assertEquals('ゲ', HankakuKanaToZenkakuKana.mergeChar('ｹ', 'ﾞ'));
        assertEquals('ゴ', HankakuKanaToZenkakuKana.mergeChar('ｺ', 'ﾞ'));
        assertEquals('パ', HankakuKanaToZenkakuKana.mergeChar('ﾊ', 'ﾟ'));
        assertEquals('ピ', HankakuKanaToZenkakuKana.mergeChar('ﾋ', 'ﾟ'));
        assertEquals('プ', HankakuKanaToZenkakuKana.mergeChar('ﾌ', 'ﾟ'));
        assertEquals('ペ', HankakuKanaToZenkakuKana.mergeChar('ﾍ', 'ﾟ'));
        assertEquals('ポ', HankakuKanaToZenkakuKana.mergeChar('ﾎ', 'ﾟ'));
    }

    @Test
    public void test_mergeCharNoMatch() throws Exception {
        assertEquals('ｱ', HankakuKanaToZenkakuKana.mergeChar('ｱ', 'ﾞ'));
        assertEquals('ｱ', HankakuKanaToZenkakuKana.mergeChar('ｱ', 'ﾟ'));
        assertEquals('ｶ', HankakuKanaToZenkakuKana.mergeChar('ｶ', 'a'));
    }
}
