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
package org.codelibs.fess.suggest.entity;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.codelibs.fess.suggest.constants.FieldNames;
import org.junit.Test;

/**
 * Unit tests for SuggestItemSerializer.
 */
public class SuggestItemSerializerTest {

    @Test
    public void testToSource_basicFields() {
        String[] text = { "test text" };
        String[][] readings = { { "reading1" } };
        String[] fields = { "field1" };
        String[] tags = { "tag1" };
        String[] roles = { "role1" };
        String[] languages = { "en" };
        long docFreq = 10L;
        long queryFreq = 5L;
        float userBoost = 1.5f;

        SuggestItem item =
                new SuggestItem(text, readings, fields, docFreq, queryFreq, userBoost, tags, roles, languages, SuggestItem.Kind.DOCUMENT);

        Map<String, Object> source = SuggestItemSerializer.toSource(item);

        assertNotNull(source);
        assertEquals("test text", source.get(FieldNames.TEXT));
        assertEquals(docFreq, source.get(FieldNames.DOC_FREQ));
        assertEquals(queryFreq, source.get(FieldNames.QUERY_FREQ));
        assertEquals(userBoost, ((Number) source.get(FieldNames.USER_BOOST)).floatValue(), 0.001f);
        assertNotNull(source.get(FieldNames.TIMESTAMP));
    }

    @Test
    public void testFromSource_requiresAllFields() {
        // Create a complete source map with all required fields
        Map<String, Object> source = new HashMap<>();
        source.put(FieldNames.TEXT, "test text");
        source.put(FieldNames.DOC_FREQ, 10L);
        source.put(FieldNames.QUERY_FREQ, 5L);
        source.put(FieldNames.USER_BOOST, 1.5f);
        source.put(FieldNames.FIELDS, Arrays.asList("field1"));
        source.put(FieldNames.TAGS, Arrays.asList("tag1"));
        source.put(FieldNames.ROLES, Arrays.asList("role1"));
        source.put(FieldNames.LANGUAGES, Arrays.asList("en"));
        source.put(FieldNames.KINDS, Arrays.asList("document"));
        source.put(FieldNames.TIMESTAMP, System.currentTimeMillis());

        SuggestItem item = SuggestItemSerializer.fromSource(source);

        assertNotNull(item);
        assertEquals("test text", item.getText());
        assertEquals(10L, item.getDocFreq());
        assertEquals(5L, item.getQueryFreq());
        assertEquals(1.5f, item.getUserBoost(), 0.001f);
    }

    @Test
    public void testToJson() {
        String[] text = { "json test" };
        String[][] readings = { { "reading" } };
        String[] fields = { "field" };

        SuggestItem item = new SuggestItem(text, readings, fields, 1L, 1L, 1.0f, new String[0], new String[0], new String[0],
                SuggestItem.Kind.DOCUMENT);

        String json = SuggestItemSerializer.toJson(item);

        assertNotNull(json);
        assertTrue(json.contains("json test"));
    }

    @Test
    public void testToUpdatedSource() {
        String[] text = { "updated" };
        String[][] readings = { { "reading" } };
        String[] fields = { "field" };

        SuggestItem item = new SuggestItem(text, readings, fields, 10L, 5L, 2.0f, new String[0], new String[0], new String[0],
                SuggestItem.Kind.DOCUMENT);

        Map<String, Object> existing = new HashMap<>();
        existing.put(FieldNames.DOC_FREQ, 5L);
        existing.put(FieldNames.QUERY_FREQ, 3L);
        existing.put(FieldNames.TIMESTAMP, System.currentTimeMillis());

        Map<String, Object> updated = SuggestItemSerializer.toUpdatedSource(item, existing);

        assertNotNull(updated);
        assertTrue(updated.containsKey(FieldNames.DOC_FREQ));
        assertTrue(updated.containsKey(FieldNames.QUERY_FREQ));
    }

    @Test
    public void testSourceContainsExpectedFields() {
        String[] text = { "test" };
        String[][] readings = { { "reading" } };
        String[] fields = { "field" };

        SuggestItem item = new SuggestItem(text, readings, fields, 1L, 1L, 1.0f, new String[] { "tag" }, new String[] { "role" },
                new String[] { "en" }, SuggestItem.Kind.QUERY);

        Map<String, Object> source = SuggestItemSerializer.toSource(item);

        // Verify all expected fields are present
        assertTrue(source.containsKey(FieldNames.TEXT));
        assertTrue(source.containsKey(FieldNames.FIELDS));
        assertTrue(source.containsKey(FieldNames.TAGS));
        assertTrue(source.containsKey(FieldNames.ROLES));
        assertTrue(source.containsKey(FieldNames.LANGUAGES));
        assertTrue(source.containsKey(FieldNames.KINDS));
        assertTrue(source.containsKey(FieldNames.DOC_FREQ));
        assertTrue(source.containsKey(FieldNames.QUERY_FREQ));
        assertTrue(source.containsKey(FieldNames.USER_BOOST));
        assertTrue(source.containsKey(FieldNames.SCORE));
        assertTrue(source.containsKey(FieldNames.TIMESTAMP));
    }
}
