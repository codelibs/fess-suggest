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

package jp.sf.fess.suggest.converter;


import junit.framework.TestCase;
import org.apache.lucene.analysis.util.TokenizerFactory;

import java.util.ArrayList;
import java.util.List;

public class SuggestIntegrateConverterTest extends TestCase {
    public void test_convert() {
        SuggestIntegrateConverter converter = new SuggestIntegrateConverter();
        converter.addConverter(new SuggestReadingConverter() {
            @Override
            public List<String> convert(String text) {
                List<String> list = new ArrayList<String>();
                list.add("abc");
                list.add("123");
                return list;
            }

            @Override
            public void start() {
            }

            @Override
            public void setTokenizerFactory(TokenizerFactory tokenizerFactory) {
            }
        });

        converter.addConverter(new SuggestReadingConverter() {
            @Override
            public List<String> convert(String text) {
                List<String> list = new ArrayList<String>();
                list.add("あああ");
                list.add("いいい");
                return list;
            }

            @Override
            public void start() {
            }

            @Override
            public void setTokenizerFactory(TokenizerFactory tokenizerFactory) {
            }
        });

        converter.start();

        List<String> list = converter.convert("");
        assertEquals("abc", list.get(0));
        assertEquals("123", list.get(1));
        assertEquals("あああ", list.get(2));
        assertEquals("いいい", list.get(3));
    }
}
