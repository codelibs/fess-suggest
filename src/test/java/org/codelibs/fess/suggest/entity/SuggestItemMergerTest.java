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
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * Unit tests for SuggestItemMerger.
 */
public class SuggestItemMergerTest {

    @Test
    public void testMerge_frequencies() {
        SuggestItem item1 = createItem("text", 10L, 5L, 1.0f);
        SuggestItem item2 = createItem("text", 20L, 15L, 2.0f);

        SuggestItem merged = SuggestItemMerger.merge(item1, item2);

        assertNotNull(merged);
        assertEquals(30L, merged.getDocFreq()); // 10 + 20
        assertEquals(20L, merged.getQueryFreq()); // 5 + 15
        assertEquals(2.0f, merged.getUserBoost(), 0.001f); // max(1.0, 2.0)
    }

    @Test
    public void testMerge_tags() {
        SuggestItem item1 = createItemWithTags("text", "tag1", "tag2");
        SuggestItem item2 = createItemWithTags("text", "tag2", "tag3");

        SuggestItem merged = SuggestItemMerger.merge(item1, item2);

        String[] tags = merged.getTags();
        Set<String> tagSet = new HashSet<>(Arrays.asList(tags));

        assertEquals(3, tagSet.size());
        assertTrue(tagSet.contains("tag1"));
        assertTrue(tagSet.contains("tag2"));
        assertTrue(tagSet.contains("tag3"));
    }

    @Test
    public void testMerge_roles() {
        SuggestItem item1 = createItemWithRoles("text", "role1", "role2");
        SuggestItem item2 = createItemWithRoles("text", "role2", "role3");

        SuggestItem merged = SuggestItemMerger.merge(item1, item2);

        String[] roles = merged.getRoles();
        Set<String> roleSet = new HashSet<>(Arrays.asList(roles));

        assertEquals(3, roleSet.size());
        assertTrue(roleSet.contains("role1"));
        assertTrue(roleSet.contains("role2"));
        assertTrue(roleSet.contains("role3"));
    }

    @Test
    public void testMerge_fields() {
        SuggestItem item1 = createItemWithFields("text", "field1", "field2");
        SuggestItem item2 = createItemWithFields("text", "field2", "field3");

        SuggestItem merged = SuggestItemMerger.merge(item1, item2);

        String[] fields = merged.getFields();
        Set<String> fieldSet = new HashSet<>(Arrays.asList(fields));

        assertEquals(3, fieldSet.size());
        assertTrue(fieldSet.contains("field1"));
        assertTrue(fieldSet.contains("field2"));
        assertTrue(fieldSet.contains("field3"));
    }

    @Test
    public void testMerge_languages() {
        SuggestItem item1 = createItemWithLanguages("text", "en", "ja");
        SuggestItem item2 = createItemWithLanguages("text", "ja", "zh");

        SuggestItem merged = SuggestItemMerger.merge(item1, item2);

        String[] languages = merged.getLanguages();
        Set<String> langSet = new HashSet<>(Arrays.asList(languages));

        assertEquals(3, langSet.size());
        assertTrue(langSet.contains("en"));
        assertTrue(langSet.contains("ja"));
        assertTrue(langSet.contains("zh"));
    }

    @Test
    public void testMerge_preservesText() {
        SuggestItem item1 = createItem("test text", 10L, 5L, 1.0f);
        SuggestItem item2 = createItem("test text", 20L, 15L, 2.0f);

        SuggestItem merged = SuggestItemMerger.merge(item1, item2);

        assertEquals("test text", merged.getText());
    }

    @Test
    public void testMerge_preservesId() {
        SuggestItem item1 = createItem("text", 10L, 5L, 1.0f);
        SuggestItem item2 = createItem("text", 20L, 15L, 2.0f);

        SuggestItem merged = SuggestItemMerger.merge(item1, item2);

        // ID is auto-generated from text, so both items and merged should have same ID
        assertEquals(item1.getId(), merged.getId());
    }

    private SuggestItem createItem(String text, long docFreq, long queryFreq, float userBoost) {
        String[] texts = { text };
        String[][] readings = { { "reading" } };
        String[] fields = { "field" };

        return new SuggestItem(texts, readings, fields, docFreq, queryFreq, userBoost, new String[0], new String[0], new String[0],
                SuggestItem.Kind.DOCUMENT);
    }

    private SuggestItem createItemWithTags(String text, String... tags) {
        String[] texts = { text };
        String[][] readings = { { "reading" } };
        String[] fields = { "field" };

        return new SuggestItem(texts, readings, fields, 1L, 1L, 1.0f, tags, new String[0], new String[0], SuggestItem.Kind.DOCUMENT);
    }

    private SuggestItem createItemWithRoles(String text, String... roles) {
        String[] texts = { text };
        String[][] readings = { { "reading" } };
        String[] fields = { "field" };

        return new SuggestItem(texts, readings, fields, 1L, 1L, 1.0f, new String[0], roles, new String[0], SuggestItem.Kind.DOCUMENT);
    }

    private SuggestItem createItemWithFields(String text, String... fields) {
        String[] texts = { text };
        String[][] readings = { { "reading" } };

        return new SuggestItem(texts, readings, fields, 1L, 1L, 1.0f, new String[0], new String[0], new String[0],
                SuggestItem.Kind.DOCUMENT);
    }

    private SuggestItem createItemWithLanguages(String text, String... languages) {
        String[] texts = { text };
        String[][] readings = { { "reading" } };
        String[] fields = { "field" };

        return new SuggestItem(texts, readings, fields, 1L, 1L, 1.0f, new String[0], new String[0], languages, SuggestItem.Kind.DOCUMENT);
    }
}
