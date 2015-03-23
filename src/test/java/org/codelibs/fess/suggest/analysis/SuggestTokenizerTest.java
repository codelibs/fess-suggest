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

package org.codelibs.fess.suggest.analysis;

import junit.framework.TestCase;

import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.codelibs.fess.suggest.analysis.SuggestReadingAttribute;
import org.codelibs.fess.suggest.analysis.SuggestTokenizer;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

public class SuggestTokenizerTest extends TestCase {
    public void test_GenerateToken() {
        final Reader rd = new StringReader("検索エンジン");
        SuggestTokenizer tokenizer = createToenizer(rd);
        try {
            tokenizer.reset();
            int count = 0;
            while (tokenizer.incrementToken()) {
                CharTermAttribute att = tokenizer.getAttribute(CharTermAttribute.class);
                SuggestReadingAttribute reading = tokenizer.getAttribute(SuggestReadingAttribute.class);
                switch (count) {
                case 0:
                    assertEquals("検索", att.toString());
                    assertEquals("ケンサク", reading.toString());
                    break;
                case 1:
                    assertEquals("エンジン", att.toString());
                    assertEquals("エンジン", reading.toString());
                    break;
                case 2:
                    assertEquals("検索エンジン", att.toString());
                    assertEquals("ケンサクエンジン", reading.toString());
                    break;
                }
                count++;
            }
            assertEquals(3, count);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    private SuggestTokenizer createToenizer(Reader rd) {
        //TODO Factory使う
        SuggestTokenizer.TermChecker termChecker = new SuggestTokenizer.TermChecker();
        // ex. start:名詞,middle:動詞
        final String includePartOfSpeech = "start:名詞,middle:名詞,動詞";
        if (includePartOfSpeech != null) {
            for (String text : includePartOfSpeech.split(",")) {
                text = text.trim();
                if (text.length() > 0) {
                    final String[] values = text.split(":");
                    if (values.length == 2) {
                        termChecker.includePartOfSpeech(values[0].trim(), values[1].trim());
                    }
                }
            }
        }

        return new SuggestTokenizer(rd, 256, null, true, JapaneseTokenizer.Mode.NORMAL, termChecker, 1000, false);
    }
}
