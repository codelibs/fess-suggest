package org.codelibs.fess.suggest.entity;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.DateUtil;
import org.codelibs.fess.suggest.SuggestConstants;
import org.codelibs.fess.suggest.entity.SuggestItem;

public class SuggestItemTest extends TestCase {
    public void test_equals_True() {
        final SuggestItem item1 = new SuggestItem();
        item1.setText("test");
        item1.addReading("テスト");
        item1.addFieldName("content");

        final SuggestItem item2 = new SuggestItem();
        item2.setText("test");
        item2.addReading("テスト");
        item2.addFieldName("content");
        item2.setLabels(new ArrayList<String>());

        assertTrue(item1.equals(item2));
    }

    public void test_equals_False() {
        final SuggestItem item1 = new SuggestItem();
        item1.setText("test");
        item1.addReading("テスト");
        item1.addFieldName("content");

        final SuggestItem item2 = new SuggestItem();
        item2.setText("test2");
        item2.addReading("テスト");
        item2.addFieldName("content");
        assertFalse(item1.equals(item2));

        item2.setText("test");
        item2.addReading("テスト");
        item2.addFieldName("content2");
        assertTrue(item1.equals(item2));
    }

    public void test_toSolrInputDocument() {
        final SuggestItem item1 = new SuggestItem();
        item1.setText("test");
        item1.addReading("テスト");
        item1.addReading("テスト2");
        item1.addFieldName("content");
        final String date = DateUtil.getThreadLocalDateFormat().format(new Date());
        item1.setExpires(date);
        item1.setExpiresField(SuggestConstants.SuggestFieldNames.EXPIRES);
        item1.setCount(10);
        final List<String> labels = Arrays.asList(new String[] { "label1", "label2" });
        item1.setLabels(labels);

        final SolrInputDocument doc = item1.toSolrInputDocument();
        assertEquals("test", doc.getFieldValue(SuggestConstants.SuggestFieldNames.TEXT));
        assertEquals("テスト", doc.getFieldValue(SuggestConstants.SuggestFieldNames.READING));
        assertEquals("content", doc.getFieldValue(SuggestConstants.SuggestFieldNames.FIELD_NAME));
        assertEquals(date, doc.getFieldValue(SuggestConstants.SuggestFieldNames.EXPIRES));
        assertEquals(10, Integer.parseInt(doc.getFieldValue(SuggestConstants.SuggestFieldNames.COUNT).toString()));
        assertTrue(labels.equals(doc.getFieldValues(SuggestConstants.SuggestFieldNames.LABELS)));
        assertEquals("test", doc.getFieldValue("id"));
    }
}