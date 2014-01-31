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

package jp.sf.fess.suggest;


import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SuggesterTest extends TestCase {
    public void test_buildQuery() {
        Suggester suggester = new Suggester();
        suggester.setNormalizer(new SuggestNormalizer() {
            @Override
            public String normalize(String text) {
                return text.replace("りんご", "リンゴ");
            }

            @Override
            public void start() {
            }
        });

        suggester.setConverter(new SuggestReadingConverter() {
            @Override
            public List<String> convert(String text) {
                List<String> list = new ArrayList<String>();
                list.add(text);
                list.add(text.replace("みかん", "ミカン"));
                return list;
            }

            @Override
            public void start() {
            }
        });

        List<String> targetFields = Arrays.asList(new String[]{"field1", "field2"});
        List<String> labels = Arrays.asList(new String[]{"label1", "label2"});
        List<String> roles = Arrays.asList(new String[]{"role1", "role2"});

        String readingField = SuggestConstants.SuggestFieldNames.READING;
        String field = SuggestConstants.SuggestFieldNames.FIELD_NAME;
        String labelField = SuggestConstants.SuggestFieldNames.LABELS;
        String roleField = SuggestConstants.SuggestFieldNames.ROLES;
        String query = suggester.buildSuggestQuery("りんごとみかん", targetFields, labels, roles);
        assertEquals("(" + readingField + ":リンゴとみかん* OR " + readingField + ":リンゴとミカン*) AND " +
                "(" + field + ":field1 OR " + field + ":field2) AND (" +
                labelField + ":label1 OR " + labelField + ":label2) AND (" +
                roleField + ":role1 OR " + roleField + ":role2)",
                query);
    }

    public void test_buildQueryWithSpace() {
        Suggester suggester = new Suggester();
        String readingField = SuggestConstants.SuggestFieldNames.READING;
        String query = suggester.buildQuery("りんご　みかん");
        assertEquals(readingField + ":りんご\\　みかん*", query);
    }

    public void test_buildQueryWithEscape() {
        Suggester suggester = new Suggester();
        String readingField = SuggestConstants.SuggestFieldNames.READING;
        String query = suggester.buildQuery("りんご+-&&||!(){}[]^\"~*?:");
        assertEquals(readingField + ":りんご\\+\\-\\&\\&\\|\\|\\!\\(\\)\\{\\}\\[\\]\\^\\\"\\~\\*\\?\\:*", query);
    }

}
