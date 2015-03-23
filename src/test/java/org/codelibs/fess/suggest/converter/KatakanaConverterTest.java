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

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

import org.codelibs.fess.suggest.analysis.SuggestTokenizerFactory;
import org.codelibs.fess.suggest.converter.KatakanaConverter;

public class KatakanaConverterTest extends TestCase {
    public void test_toKatakana() {
        KatakanaConverter converter = new KatakanaConverter();
        converter.start();
        try {
            assertEquals("アイウエオ", converter.convert("あいうえお").get(0));
            assertEquals("アイウエオ", converter.convert("ｱｲｳｴｵ").get(0));
            assertEquals("ザジズゼゾ", converter.convert("ｻﾞｼﾞｽﾞｾﾞｿﾞ").get(0));
            assertEquals("ケンサクエンジン", converter.convert("検索エンジン").get(0));
            assertEquals("apple", converter.convert("apple").get(0));
            assertEquals("ミカン リンゴ", converter.convert("みかん りんご").get(0));
            assertEquals("appleジュース", converter.convert("appleじゅーす").get(0));
            assertEquals("1234ジャ1234", converter.convert("1234じゃ1234").get(0));
            assertEquals("スモモモモモモモモノウチ", converter.convert("すもももももももものうち").get(0));
            assertEquals("トナリノキャクハヨクカキクウキャクダ。イヌモアルケバボウニアタル。", converter.convert("隣の客はよく柿くう客だ。犬もあるけば棒にあたる。").get(0));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void test_toKatakanaWithSuggestTokenizer() {
        Map<String, String> args = new HashMap<String, String>();
        args.put("discardPunctuation", "false");
        SuggestTokenizerFactory suggestTokenizerFactory = new SuggestTokenizerFactory(args);
        KatakanaConverter converter = new KatakanaConverter();
        converter.setTokenizerFactory(suggestTokenizerFactory);
        converter.start();
        try {
            assertEquals("アイウエオ", converter.convert("あいうえお").get(0));
            assertEquals("アイウエオ", converter.convert("ｱｲｳｴｵ").get(0));
            assertEquals("ザジズゼゾ", converter.convert("ｻﾞｼﾞｽﾞｾﾞｿﾞ").get(0));
            assertEquals("ケンサクエンジン", converter.convert("検索エンジン").get(0));
            assertEquals("apple", converter.convert("apple").get(0));
            assertEquals("ミカン リンゴ", converter.convert("みかん りんご").get(0));
            assertEquals("appleジュース", converter.convert("appleじゅーす").get(0));
            assertEquals("1234ジャ1234", converter.convert("1234じゃ1234").get(0));
            assertEquals("スモモモモモモモモノウチ", converter.convert("すもももももももものうち").get(0));
            assertEquals("トナリノキャクハヨクカキクウキャクダ。イヌモアルケバボウニアタル。", converter.convert("隣の客はよく柿くう客だ。犬もあるけば棒にあたる。").get(0));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

}
