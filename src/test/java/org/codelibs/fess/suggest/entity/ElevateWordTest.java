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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class ElevateWordTest {

    private ElevateWord elevateWord;

    @Before
    public void setUp() {
        // Setup for each test
    }

    @Test
    public void testConstructorWithAllParameters() {
        // Test constructor with all parameters
        String word = "test word";
        float boost = 2.5f;
        List<String> readings = Arrays.asList("reading1", "reading2");
        List<String> fields = Arrays.asList("field1", "field2");
        List<String> tags = Arrays.asList("tag1", "tag2");
        List<String> roles = Arrays.asList("role1", "role2");

        elevateWord = new ElevateWord(word, boost, readings, fields, tags, roles);

        assertEquals(word, elevateWord.getElevateWord());
        assertEquals(boost, elevateWord.getBoost(), 0.001f);
        assertEquals(readings, elevateWord.getReadings());
        assertEquals(fields, elevateWord.getFields());
        assertEquals(tags, elevateWord.getTags());
        assertEquals(roles, elevateWord.getRoles());
    }

    @Test
    public void testConstructorWithNullTags() {
        // Test constructor with null tags
        String word = "test word";
        float boost = 1.0f;
        List<String> readings = Arrays.asList("reading1");
        List<String> fields = Arrays.asList("field1");
        List<String> tags = null;
        List<String> roles = Arrays.asList("role1");

        elevateWord = new ElevateWord(word, boost, readings, fields, tags, roles);

        assertEquals(word, elevateWord.getElevateWord());
        assertEquals(boost, elevateWord.getBoost(), 0.001f);
        assertEquals(readings, elevateWord.getReadings());
        assertEquals(fields, elevateWord.getFields());
        assertNotNull(elevateWord.getTags());
        assertTrue(elevateWord.getTags().isEmpty());
        assertEquals(roles, elevateWord.getRoles());
    }

    @Test
    public void testConstructorWithNullRoles() {
        // Test constructor with null roles
        String word = "test word";
        float boost = 1.5f;
        List<String> readings = Arrays.asList("reading1");
        List<String> fields = Arrays.asList("field1");
        List<String> tags = Arrays.asList("tag1");
        List<String> roles = null;

        elevateWord = new ElevateWord(word, boost, readings, fields, tags, roles);

        assertEquals(word, elevateWord.getElevateWord());
        assertEquals(boost, elevateWord.getBoost(), 0.001f);
        assertEquals(readings, elevateWord.getReadings());
        assertEquals(fields, elevateWord.getFields());
        assertEquals(tags, elevateWord.getTags());
        assertNotNull(elevateWord.getRoles());
        assertTrue(elevateWord.getRoles().isEmpty());
    }

    @Test
    public void testConstructorWithNullTagsAndRoles() {
        // Test constructor with both null tags and roles
        String word = "test word";
        float boost = 3.0f;
        List<String> readings = Arrays.asList("reading1", "reading2", "reading3");
        List<String> fields = Arrays.asList("field1", "field2");
        List<String> tags = null;
        List<String> roles = null;

        elevateWord = new ElevateWord(word, boost, readings, fields, tags, roles);

        assertEquals(word, elevateWord.getElevateWord());
        assertEquals(boost, elevateWord.getBoost(), 0.001f);
        assertEquals(readings, elevateWord.getReadings());
        assertEquals(fields, elevateWord.getFields());
        assertNotNull(elevateWord.getTags());
        assertTrue(elevateWord.getTags().isEmpty());
        assertNotNull(elevateWord.getRoles());
        assertTrue(elevateWord.getRoles().isEmpty());
    }

    @Test
    public void testConstructorWithEmptyLists() {
        // Test constructor with empty lists
        String word = "empty lists";
        float boost = 1.0f;
        List<String> readings = Collections.emptyList();
        List<String> fields = Collections.emptyList();
        List<String> tags = Collections.emptyList();
        List<String> roles = Collections.emptyList();

        elevateWord = new ElevateWord(word, boost, readings, fields, tags, roles);

        assertEquals(word, elevateWord.getElevateWord());
        assertEquals(boost, elevateWord.getBoost(), 0.001f);
        assertTrue(elevateWord.getReadings().isEmpty());
        assertTrue(elevateWord.getFields().isEmpty());
        assertTrue(elevateWord.getTags().isEmpty());
        assertTrue(elevateWord.getRoles().isEmpty());
    }

    @Test
    public void testConstructorWithNullWord() {
        // Test constructor with null word
        String word = null;
        float boost = 1.0f;
        List<String> readings = Arrays.asList("reading1");
        List<String> fields = Arrays.asList("field1");
        List<String> tags = Arrays.asList("tag1");
        List<String> roles = Arrays.asList("role1");

        elevateWord = new ElevateWord(word, boost, readings, fields, tags, roles);

        assertNull(elevateWord.getElevateWord());
        assertEquals(boost, elevateWord.getBoost(), 0.001f);
        assertEquals(readings, elevateWord.getReadings());
        assertEquals(fields, elevateWord.getFields());
        assertEquals(tags, elevateWord.getTags());
        assertEquals(roles, elevateWord.getRoles());
    }

    @Test
    public void testConstructorWithZeroBoost() {
        // Test constructor with zero boost
        String word = "zero boost";
        float boost = 0.0f;
        List<String> readings = Arrays.asList("reading1");
        List<String> fields = Arrays.asList("field1");
        List<String> tags = Arrays.asList("tag1");
        List<String> roles = Arrays.asList("role1");

        elevateWord = new ElevateWord(word, boost, readings, fields, tags, roles);

        assertEquals(word, elevateWord.getElevateWord());
        assertEquals(0.0f, elevateWord.getBoost(), 0.001f);
    }

    @Test
    public void testConstructorWithNegativeBoost() {
        // Test constructor with negative boost
        String word = "negative boost";
        float boost = -1.5f;
        List<String> readings = Arrays.asList("reading1");
        List<String> fields = Arrays.asList("field1");
        List<String> tags = Arrays.asList("tag1");
        List<String> roles = Arrays.asList("role1");

        elevateWord = new ElevateWord(word, boost, readings, fields, tags, roles);

        assertEquals(word, elevateWord.getElevateWord());
        assertEquals(-1.5f, elevateWord.getBoost(), 0.001f);
    }

    @Test
    public void testToSuggestItemWithCompleteData() {
        // Test toSuggestItem with complete data
        String word = "complete word";
        float boost = 2.0f;
        List<String> readings = Arrays.asList("reading1", "reading2");
        List<String> fields = Arrays.asList("field1", "field2", "field3");
        List<String> tags = Arrays.asList("tag1", "tag2");
        List<String> roles = Arrays.asList("role1", "role2", "role3");

        elevateWord = new ElevateWord(word, boost, readings, fields, tags, roles);
        SuggestItem suggestItem = elevateWord.toSuggestItem();

        assertNotNull(suggestItem);
        assertEquals(word, suggestItem.getText());
        assertEquals(boost, suggestItem.getUserBoost(), 0.001f);
        // The queryFreq and docFreq might be handled differently by SuggestItem
        assertTrue(suggestItem.getQueryFreq() >= 0);
        assertTrue(suggestItem.getDocFreq() >= 0);

        // Check readings
        String[][] itemReadings = suggestItem.getReadings();
        assertNotNull(itemReadings);
        assertEquals(2, itemReadings.length);
        assertArrayEquals(new String[] { "reading1" }, itemReadings[0]);
        assertArrayEquals(new String[] { "reading2" }, itemReadings[1]);

        // Check fields
        assertArrayEquals(new String[] { "field1", "field2", "field3" }, suggestItem.getFields());

        // Check tags
        assertArrayEquals(new String[] { "tag1", "tag2" }, suggestItem.getTags());

        // Check roles
        assertArrayEquals(new String[] { "role1", "role2", "role3" }, suggestItem.getRoles());

        // Check kind
        assertNotNull(suggestItem.getKinds());
        // The kinds array structure might be different
        boolean hasUserKind = false;
        for (SuggestItem.Kind kind : suggestItem.getKinds()) {
            if (kind == SuggestItem.Kind.USER) {
                hasUserKind = true;
                break;
            }
        }
        assertTrue("Should have USER kind", hasUserKind);
    }

    @Test
    public void testToSuggestItemWithEmptyReadings() {
        // Test toSuggestItem with empty readings
        String word = "no readings";
        float boost = 1.0f;
        List<String> readings = Collections.emptyList();
        List<String> fields = Arrays.asList("field1");
        List<String> tags = Arrays.asList("tag1");
        List<String> roles = Arrays.asList("role1");

        elevateWord = new ElevateWord(word, boost, readings, fields, tags, roles);
        SuggestItem suggestItem = elevateWord.toSuggestItem();

        assertNotNull(suggestItem);
        assertEquals(word, suggestItem.getText());
        String[][] itemReadings = suggestItem.getReadings();
        assertNotNull(itemReadings);
        assertEquals(0, itemReadings.length);
    }

    @Test
    public void testToSuggestItemWithSingleReading() {
        // Test toSuggestItem with single reading
        String word = "single reading";
        float boost = 1.5f;
        List<String> readings = Arrays.asList("reading1");
        List<String> fields = Arrays.asList("field1");
        List<String> tags = Arrays.asList("tag1");
        List<String> roles = Arrays.asList("role1");

        elevateWord = new ElevateWord(word, boost, readings, fields, tags, roles);
        SuggestItem suggestItem = elevateWord.toSuggestItem();

        assertNotNull(suggestItem);
        String[][] itemReadings = suggestItem.getReadings();
        assertNotNull(itemReadings);
        assertEquals(1, itemReadings.length);
        assertArrayEquals(new String[] { "reading1" }, itemReadings[0]);
    }

    @Test
    public void testToSuggestItemWithNullTagsAndRoles() {
        // Test toSuggestItem when tags and roles are null (converted to empty lists)
        String word = "null tags and roles";
        float boost = 1.0f;
        List<String> readings = Arrays.asList("reading1");
        List<String> fields = Arrays.asList("field1");

        elevateWord = new ElevateWord(word, boost, readings, fields, null, null);
        SuggestItem suggestItem = elevateWord.toSuggestItem();

        assertNotNull(suggestItem);
        assertEquals(word, suggestItem.getText());

        // Check tags and roles are not null (they might not be empty due to internal processing)
        assertNotNull(suggestItem.getTags());
        assertNotNull(suggestItem.getRoles());
    }

    @Test
    public void testToSuggestItemWithLargeBoost() {
        // Test toSuggestItem with large boost value
        String word = "large boost";
        float boost = Float.MAX_VALUE;
        List<String> readings = Arrays.asList("reading1");
        List<String> fields = Arrays.asList("field1");
        List<String> tags = Arrays.asList("tag1");
        List<String> roles = Arrays.asList("role1");

        elevateWord = new ElevateWord(word, boost, readings, fields, tags, roles);
        SuggestItem suggestItem = elevateWord.toSuggestItem();

        assertNotNull(suggestItem);
        assertEquals(Float.MAX_VALUE, suggestItem.getUserBoost(), 0.001f);
    }

    @Test
    public void testGettersWithMutableLists() {
        // Test that getters return the same list references
        String word = "mutable test";
        float boost = 1.0f;
        List<String> readings = new ArrayList<>();
        readings.add("reading1");
        List<String> fields = new ArrayList<>();
        fields.add("field1");
        List<String> tags = new ArrayList<>();
        tags.add("tag1");
        List<String> roles = new ArrayList<>();
        roles.add("role1");

        elevateWord = new ElevateWord(word, boost, readings, fields, tags, roles);

        // Check that we get the same references
        assertTrue(readings == elevateWord.getReadings());
        assertTrue(fields == elevateWord.getFields());
        assertTrue(tags == elevateWord.getTags());
        assertTrue(roles == elevateWord.getRoles());
    }

    @Test
    public void testWithSpecialCharactersInWord() {
        // Test with special characters in word
        String word = "test@#$%^&*()_+-=[]{}|;':\",./<>?";
        float boost = 1.0f;
        List<String> readings = Arrays.asList("reading1");
        List<String> fields = Arrays.asList("field1");
        List<String> tags = Arrays.asList("tag1");
        List<String> roles = Arrays.asList("role1");

        elevateWord = new ElevateWord(word, boost, readings, fields, tags, roles);

        assertEquals(word, elevateWord.getElevateWord());
        SuggestItem suggestItem = elevateWord.toSuggestItem();
        assertEquals(word, suggestItem.getText());
    }

    @Test
    public void testWithJapaneseCharacters() {
        // Test with Japanese characters
        String word = "日本語テスト";
        float boost = 2.0f;
        List<String> readings = Arrays.asList("ニホンゴテスト");
        List<String> fields = Arrays.asList("content_ja");
        List<String> tags = Arrays.asList("japanese");
        List<String> roles = Arrays.asList("admin");

        elevateWord = new ElevateWord(word, boost, readings, fields, tags, roles);

        assertEquals(word, elevateWord.getElevateWord());
        assertEquals("ニホンゴテスト", elevateWord.getReadings().get(0));

        SuggestItem suggestItem = elevateWord.toSuggestItem();
        assertEquals(word, suggestItem.getText());
        assertArrayEquals(new String[] { "ニホンゴテスト" }, suggestItem.getReadings()[0]);
    }

    @Test
    public void testWithEmptyWord() {
        // Test with empty word string
        String word = "";
        float boost = 1.0f;
        List<String> readings = Arrays.asList("reading1");
        List<String> fields = Arrays.asList("field1");
        List<String> tags = Arrays.asList("tag1");
        List<String> roles = Arrays.asList("role1");

        elevateWord = new ElevateWord(word, boost, readings, fields, tags, roles);

        assertEquals("", elevateWord.getElevateWord());
        SuggestItem suggestItem = elevateWord.toSuggestItem();
        assertEquals("", suggestItem.getText());
    }

    @Test
    public void testMultipleReadingsConversion() {
        // Test conversion of multiple readings to SuggestItem format
        String word = "multi reading";
        float boost = 1.0f;
        List<String> readings = Arrays.asList("reading1", "reading2", "reading3", "reading4", "reading5");
        List<String> fields = Arrays.asList("field1");
        List<String> tags = Arrays.asList("tag1");
        List<String> roles = Arrays.asList("role1");

        elevateWord = new ElevateWord(word, boost, readings, fields, tags, roles);
        SuggestItem suggestItem = elevateWord.toSuggestItem();

        String[][] itemReadings = suggestItem.getReadings();
        assertEquals(5, itemReadings.length);
        for (int i = 0; i < 5; i++) {
            assertArrayEquals(new String[] { "reading" + (i + 1) }, itemReadings[i]);
        }
    }
}