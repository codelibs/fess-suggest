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
package org.codelibs.fess.suggest.converter;

import java.util.List;

import junit.framework.TestCase;

public class KatakanaToAlphabetConverterTest extends TestCase {

    private KatakanaToAlphabetConverter converter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        converter = new KatakanaToAlphabetConverter();
        converter.init();
    }

    public void test_convert() {
        assertTrue(converter.convert("ケンサク", null).contains("kennsaku"));
    }

    public void test_convertBasicKatakana() {
        List<String> results = converter.convert("アイウエオ", null);
        assertTrue(results.contains("aiueo"));
    }

    public void test_convertWithShi() {
        List<String> results = converter.convert("シ", null);
        assertTrue(results.contains("si"));
        assertTrue(results.contains("shi"));
    }

    public void test_convertWithChi() {
        List<String> results = converter.convert("チ", null);
        assertTrue(results.contains("ti"));
        assertTrue(results.contains("chi"));
    }

    public void test_convertWithTsu() {
        List<String> results = converter.convert("ツ", null);
        assertTrue(results.contains("tu"));
        assertTrue(results.contains("tsu"));
    }

    public void test_convertWithFu() {
        List<String> results = converter.convert("フ", null);
        assertTrue(results.contains("hu"));
        assertTrue(results.contains("fu"));
    }

    public void test_convertWithJi() {
        List<String> results = converter.convert("ジ", null);
        assertTrue(results.contains("zi"));
        assertTrue(results.contains("ji"));
    }

    public void test_convertCombinedKatakana() {
        List<String> results = converter.convert("キャ", null);
        assertTrue(results.contains("kya"));
    }

    public void test_convertSha() {
        List<String> results = converter.convert("シャ", null);
        assertTrue(results.contains("sya"));
        assertTrue(results.contains("sha"));
    }

    public void test_convertCha() {
        List<String> results = converter.convert("チャ", null);
        assertTrue(results.contains("tya"));
        assertTrue(results.contains("cha"));
    }

    public void test_convertJa() {
        List<String> results = converter.convert("ジャ", null);
        assertTrue(results.contains("zya"));
        assertTrue(results.contains("ja"));
        assertTrue(results.contains("jya"));
    }

    public void test_convertModernKatakana_Wi() {
        List<String> results = converter.convert("ウィ", null);
        assertTrue(results.contains("wi"));
    }

    public void test_convertModernKatakana_We() {
        List<String> results = converter.convert("ウェ", null);
        assertTrue(results.contains("we"));
    }

    public void test_convertModernKatakana_Wo() {
        List<String> results = converter.convert("ウォ", null);
        assertTrue(results.contains("wo"));
    }

    public void test_convertModernKatakana_Ti() {
        List<String> results = converter.convert("ティ", null);
        assertTrue(results.contains("ti"));
        assertTrue(results.contains("thi"));
    }

    public void test_convertModernKatakana_Di() {
        List<String> results = converter.convert("ディ", null);
        assertTrue(results.contains("di"));
        assertTrue(results.contains("dhi"));
    }

    public void test_convertModernKatakana_Dyu() {
        List<String> results = converter.convert("デュ", null);
        assertTrue(results.contains("dyu"));
    }

    public void test_convertModernKatakana_Tsa() {
        List<String> results = converter.convert("ツァ", null);
        assertTrue(results.contains("tsa"));
    }

    public void test_convertModernKatakana_Tse() {
        List<String> results = converter.convert("ツェ", null);
        assertTrue(results.contains("tse"));
    }

    public void test_convertFyu_BugFix() {
        // Test that フュ correctly maps to "fyu" only (not "hyu")
        List<String> results = converter.convert("フュ", null);
        assertTrue(results.contains("fyu"));
        assertFalse("Should not contain 'hyu' (that's for ヒュ)", results.contains("hyu"));
    }

    public void test_convertEmptyString() {
        List<String> results = converter.convert("", null);
        assertNotNull(results);
        assertTrue(results.size() > 0);
    }

    public void test_convertMixedText() {
        List<String> results = converter.convert("トウキョウ", null);
        assertTrue(results.contains("toukyou"));
    }

    public void test_convertWithNumbers() {
        List<String> results = converter.convert("テスト123", null);
        assertNotNull(results);
        assertTrue(results.size() > 0);
    }

    public void test_convertLowercase() {
        // All results should be lowercase
        List<String> results = converter.convert("アイウ", null);
        for (String result : results) {
            assertEquals(result, result.toLowerCase());
        }
    }

    public void test_convertHalfWidth() {
        // Test that full-width characters are converted to half-width
        List<String> results = converter.convert("アイウ", null);
        for (String result : results) {
            assertFalse(result.contains("Ａ")); // Full-width A
            assertFalse(result.contains("ａ")); // Full-width a
        }
    }
}
