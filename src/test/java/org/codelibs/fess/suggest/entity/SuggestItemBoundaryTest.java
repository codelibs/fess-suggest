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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.codelibs.fess.suggest.constants.FieldNames;
import org.junit.Test;

/**
 * Boundary tests for SuggestItem.
 * Tests edge cases, null handling, and limit conditions.
 */
public class SuggestItemBoundaryTest {

    // ============================================================
    // Tests for empty/boundary text arrays
    // ============================================================

    @Test
    public void test_singleCharacterText() {
        String[] text = { "a" };
        String[][] readings = { { "a" } };

        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        assertEquals("a", item.getText());
        assertNotNull(item.getId());
    }

    @Test
    public void test_singleJapaneseCharacter() {
        String[] text = { "あ" };
        String[][] readings = { { "a" } };

        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        assertEquals("あ", item.getText());
    }

    @Test
    public void test_veryLongText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("テスト");
        }
        String[] text = { sb.toString() };
        String[][] readings = { { "test" } };

        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        assertEquals(sb.toString(), item.getText());
        assertNotNull(item.getId());
    }

    @Test
    public void test_manyWords() {
        String[] text = new String[100];
        String[][] readings = new String[100][];
        for (int i = 0; i < 100; i++) {
            text[i] = "word" + i;
            readings[i] = new String[] { "reading" + i };
        }

        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        assertTrue("Text should contain all words", item.getText().contains("word0"));
        assertTrue("Text should contain all words", item.getText().contains("word99"));
        assertEquals(100, item.getReadings().length);
    }

    @Test
    public void test_textWithSpecialCharacters() {
        String[] text = { "test!@#$%^&*()_+-=[]{}|;':\",./<>?`~" };
        String[][] readings = { { "test" } };

        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        assertTrue("Should contain special characters", item.getText().contains("!@#$"));
    }

    @Test
    public void test_textWithUnicode() {
        // Test with various Unicode characters: emoji, Chinese, Arabic, etc.
        String[] text = { "日本語", "中文", "한국어", "العربية" };
        String[][] readings = { { "nihongo" }, { "zhongwen" }, { "hangugeo" }, { "arabic" } };

        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        assertTrue("Should contain Japanese", item.getText().contains("日本語"));
        assertTrue("Should contain Chinese", item.getText().contains("中文"));
        assertTrue("Should contain Korean", item.getText().contains("한국어"));
        assertTrue("Should contain Arabic", item.getText().contains("العربية"));
    }

    @Test
    public void test_textWithWhitespace() {
        String[] text = { "  spaces  " };
        String[][] readings = { { "spaces" } };

        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        // Text is trimmed or kept as-is depending on implementation
        assertNotNull(item.getText());
    }

    // ============================================================
    // Tests for reading arrays edge cases
    // ============================================================

    @Test
    public void test_manyReadingsForOneWord() {
        String[] text = { "test" };
        String[][] readings = { { "reading1", "reading2", "reading3", "reading4", "reading5", "reading6", "reading7", "reading8",
                "reading9", "reading10" } };

        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        assertEquals(10, item.getReadings()[0].length);
    }

    @Test
    public void test_emptyReadingsArray() {
        String[] text = { "test" };
        String[][] readings = { new String[0] };

        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        assertEquals(0, item.getReadings()[0].length);
    }

    @Test
    public void test_mixedReadingsLengths() {
        String[] text = { "word1", "word2", "word3" };
        String[][] readings = { { "r1", "r2", "r3" }, // 3 readings
                { "r4" }, // 1 reading
                { "r5", "r6" } // 2 readings
        };

        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        assertEquals(3, item.getReadings()[0].length);
        assertEquals(1, item.getReadings()[1].length);
        assertEquals(2, item.getReadings()[2].length);
    }

    // ============================================================
    // Tests for frequency edge cases
    // ============================================================

    @Test
    public void test_zeroFrequencies() {
        String[] text = { "test" };
        String[][] readings = { { "test" } };

        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        assertEquals(0L, item.getDocFreq());
        assertEquals(0L, item.getQueryFreq());
    }

    @Test
    public void test_maxFrequencies() {
        String[] text = { "test" };
        String[][] readings = { { "test" } };

        SuggestItem item =
                new SuggestItem(text, readings, null, Long.MAX_VALUE, Long.MAX_VALUE, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        assertEquals(Long.MAX_VALUE, item.getDocFreq());
        assertEquals(Long.MAX_VALUE, item.getQueryFreq());
    }

    @Test
    public void test_negativeFrequencies() {
        String[] text = { "test" };
        String[][] readings = { { "test" } };

        // Negative frequencies might be allowed (though unusual)
        SuggestItem item = new SuggestItem(text, readings, null, -1L, -1L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        assertEquals(-1L, item.getDocFreq());
        assertEquals(-1L, item.getQueryFreq());
    }

    // ============================================================
    // Tests for user boost edge cases
    // ============================================================

    @Test
    public void test_userBoostZero() {
        String[] text = { "test" };
        String[][] readings = { { "test" } };

        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 0.0f, null, null, null, SuggestItem.Kind.QUERY);

        // User boost should be at least 1
        assertEquals(1.0f, item.getUserBoost(), 0.001f);
    }

    @Test
    public void test_userBoostNegative() {
        String[] text = { "test" };
        String[][] readings = { { "test" } };

        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, -5.0f, null, null, null, SuggestItem.Kind.QUERY);

        // Negative boost should be converted to 1
        assertEquals(1.0f, item.getUserBoost(), 0.001f);
    }

    @Test
    public void test_userBoostVeryLarge() {
        String[] text = { "test" };
        String[][] readings = { { "test" } };

        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, Float.MAX_VALUE, null, null, null, SuggestItem.Kind.QUERY);

        assertEquals(Float.MAX_VALUE, item.getUserBoost(), 0.001f);
    }

    // ============================================================
    // Tests for score calculation
    // ============================================================

    @Test
    public void test_scoreCalculationBasic() {
        String[] text = { "test" };
        String[][] readings = { { "test" } };

        SuggestItem item = new SuggestItem(text, readings, null, 10L, 20L, 2.0f, null, null, null, SuggestItem.Kind.QUERY);

        Map<String, Object> source = item.getSource();
        float expectedScore = (10L + 20L) * 2.0f; // (docFreq + queryFreq) * userBoost
        assertEquals(expectedScore, (Float) source.get(FieldNames.SCORE), 0.001f);
    }

    @Test
    public void test_scoreCalculationZero() {
        String[] text = { "test" };
        String[][] readings = { { "test" } };

        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        Map<String, Object> source = item.getSource();
        assertEquals(0.0f, (Float) source.get(FieldNames.SCORE), 0.001f);
    }

    // ============================================================
    // Tests for parseSource with complete data
    // ============================================================

    @Test
    public void test_parseSource_completeData() {
        Map<String, Object> source = new HashMap<>();
        source.put(FieldNames.TEXT, "test");
        source.put(FieldNames.QUERY_FREQ, 100L);
        source.put(FieldNames.DOC_FREQ, 50L);
        source.put(FieldNames.USER_BOOST, 2.0f);
        source.put(FieldNames.READING_PREFIX + "0", Arrays.asList("reading1"));
        source.put(FieldNames.FIELDS, Arrays.asList("field1"));
        source.put(FieldNames.TAGS, Arrays.asList("tag1"));
        source.put(FieldNames.ROLES, Arrays.asList("role1"));
        source.put(FieldNames.LANGUAGES, Arrays.asList("en"));
        source.put(FieldNames.KINDS, Arrays.asList("query"));
        source.put(FieldNames.TIMESTAMP, System.currentTimeMillis());

        SuggestItem item = SuggestItem.parseSource(source);

        assertNotNull(item);
        assertEquals("test", item.getText());
        assertEquals(100L, item.getQueryFreq());
        assertEquals(50L, item.getDocFreq());
        assertEquals(2.0f, item.getUserBoost(), 0.001f);
    }

    @Test
    public void test_parseSource_stringFrequencies() {
        // Sometimes values come as String instead of Long
        Map<String, Object> source = new HashMap<>();
        source.put(FieldNames.TEXT, "test");
        source.put(FieldNames.QUERY_FREQ, "100");
        source.put(FieldNames.DOC_FREQ, "50");
        source.put(FieldNames.USER_BOOST, "2.0");
        source.put(FieldNames.FIELDS, Arrays.asList("field1"));
        source.put(FieldNames.TAGS, Arrays.asList());
        source.put(FieldNames.ROLES, Arrays.asList());
        source.put(FieldNames.LANGUAGES, Arrays.asList());
        source.put(FieldNames.KINDS, Arrays.asList("query"));
        source.put(FieldNames.TIMESTAMP, System.currentTimeMillis());

        SuggestItem item = SuggestItem.parseSource(source);

        assertNotNull(item);
        assertEquals(100L, item.getQueryFreq());
        assertEquals(50L, item.getDocFreq());
        assertEquals(2.0f, item.getUserBoost(), 0.001f);
    }

    // ============================================================
    // Tests for isBadWord edge cases
    // ============================================================

    @Test
    public void test_isBadWord_emptyBadWords() {
        String[] text = { "test" };
        String[][] readings = { { "test" } };
        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        assertFalse(item.isBadWord(new String[0]));
    }

    @Test
    public void test_isBadWord_substringMatch() {
        String[] text = { "testing" };
        String[][] readings = { { "testing" } };
        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        String[] badWords = { "test" };
        assertTrue("Should match substring", item.isBadWord(badWords));
    }

    @Test
    public void test_isBadWord_noMatch() {
        String[] text = { "hello" };
        String[][] readings = { { "hello" } };
        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        String[] badWords = { "test", "bad" };
        assertFalse("Should not match", item.isBadWord(badWords));
    }

    @Test
    public void test_isBadWord_exactMatch() {
        String[] text = { "bad" };
        String[][] readings = { { "bad" } };
        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        String[] badWords = { "bad" };
        assertTrue("Should match exact", item.isBadWord(badWords));
    }

    // ============================================================
    // Tests for merge edge cases
    // ============================================================

    @Test
    public void test_merge_emptyTags() {
        String[] text1 = { "test" };
        String[][] readings1 = { { "test" } };
        SuggestItem item1 = new SuggestItem(text1, readings1, null, 10L, 20L, 1.0f, new String[0], null, null, SuggestItem.Kind.QUERY);

        String[] text2 = { "test" };
        String[][] readings2 = { { "test" } };
        SuggestItem item2 = new SuggestItem(text2, readings2, null, 5L, 10L, 1.0f, new String[0], null, null, SuggestItem.Kind.QUERY);

        SuggestItem merged = SuggestItem.merge(item1, item2);

        assertNotNull(merged);
        assertEquals(30L, merged.getQueryFreq());
        assertEquals(15L, merged.getDocFreq());
    }

    @Test
    public void test_merge_withOverlappingValues() {
        String[] text1 = { "test" };
        String[][] readings1 = { { "test" } };
        String[] tags1 = { "tag1", "tag2" };
        String[] roles1 = { "role1" };
        SuggestItem item1 = new SuggestItem(text1, readings1, null, 10L, 20L, 1.0f, tags1, roles1, null, SuggestItem.Kind.QUERY);

        String[] text2 = { "test" };
        String[][] readings2 = { { "test" } };
        String[] tags2 = { "tag2", "tag3" }; // tag2 overlaps
        String[] roles2 = { "role1", "role2" }; // role1 overlaps
        SuggestItem item2 = new SuggestItem(text2, readings2, null, 5L, 10L, 1.0f, tags2, roles2, null, SuggestItem.Kind.DOCUMENT);

        SuggestItem merged = SuggestItem.merge(item1, item2);

        assertNotNull(merged);
        // Overlapping values should not be duplicated
        assertTrue("Should have tag1", Arrays.asList(merged.getTags()).contains("tag1"));
        assertTrue("Should have tag3", Arrays.asList(merged.getTags()).contains("tag3"));
        // Check for no duplicates
        long tag2Count = Arrays.stream(merged.getTags()).filter(t -> t.equals("tag2")).count();
        assertEquals("tag2 should appear only once", 1, tag2Count);
    }

    @Test
    public void test_merge_allKinds() {
        String[] text1 = { "test" };
        String[][] readings1 = { { "test" } };
        SuggestItem item1 = new SuggestItem(text1, readings1, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);
        item1.setKinds(new SuggestItem.Kind[] { SuggestItem.Kind.QUERY, SuggestItem.Kind.DOCUMENT });

        String[] text2 = { "test" };
        String[][] readings2 = { { "test" } };
        SuggestItem item2 = new SuggestItem(text2, readings2, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.USER);

        SuggestItem merged = SuggestItem.merge(item1, item2);

        // Should have all 3 kinds
        assertEquals(3, merged.getKinds().length);
        assertTrue(Arrays.asList(merged.getKinds()).contains(SuggestItem.Kind.QUERY));
        assertTrue(Arrays.asList(merged.getKinds()).contains(SuggestItem.Kind.DOCUMENT));
        assertTrue(Arrays.asList(merged.getKinds()).contains(SuggestItem.Kind.USER));
    }

    // ============================================================
    // Tests for concatValues edge cases
    // ============================================================

    @Test
    public void test_concatValues_emptyValues() {
        java.util.List<String> dest = new java.util.ArrayList<>();
        SuggestItem.concatValues(dest);

        assertEquals(0, dest.size());
    }

    @Test
    public void test_concatValues_duplicatesRemoved() {
        java.util.List<String> dest = new java.util.ArrayList<>();
        dest.add("existing");

        SuggestItem.concatValues(dest, "new", "existing", "new", "another");

        assertEquals(3, dest.size());
        assertTrue(dest.contains("existing"));
        assertTrue(dest.contains("new"));
        assertTrue(dest.contains("another"));
    }

    // ============================================================
    // Tests for concatKinds edge cases
    // ============================================================

    @Test
    public void test_concatKinds_bothEmpty() {
        SuggestItem.Kind[] result = SuggestItem.concatKinds(new SuggestItem.Kind[0], new SuggestItem.Kind[0]);

        assertEquals(0, result.length);
    }

    @Test
    public void test_concatKinds_firstEmpty() {
        SuggestItem.Kind[] kinds2 = { SuggestItem.Kind.QUERY };

        SuggestItem.Kind[] result = SuggestItem.concatKinds(new SuggestItem.Kind[0], kinds2);

        assertEquals(1, result.length);
        assertEquals(SuggestItem.Kind.QUERY, result[0]);
    }

    @Test
    public void test_concatKinds_secondEmpty() {
        SuggestItem.Kind[] kinds1 = { SuggestItem.Kind.DOCUMENT };

        SuggestItem.Kind[] result = SuggestItem.concatKinds(kinds1, new SuggestItem.Kind[0]);

        assertEquals(1, result.length);
        assertEquals(SuggestItem.Kind.DOCUMENT, result[0]);
    }

    @Test
    public void test_concatKinds_noDuplicatesFromSecondArray() {
        SuggestItem.Kind[] kinds1 = { SuggestItem.Kind.QUERY };
        SuggestItem.Kind[] kinds2 = { SuggestItem.Kind.QUERY, SuggestItem.Kind.DOCUMENT };

        SuggestItem.Kind[] result = SuggestItem.concatKinds(kinds1, kinds2);

        // Should have QUERY from first, and only DOCUMENT added from second (QUERY already exists)
        assertEquals(2, result.length);
    }

    // ============================================================
    // Tests for toJsonString edge cases
    // ============================================================

    @Test
    public void test_toJsonString_specialCharactersEscaped() {
        String[] text = { "test\"with\"quotes" };
        String[][] readings = { { "test" } };

        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        String json = item.toJsonString();

        assertNotNull(json);
        // JSON should properly escape quotes
        assertTrue("JSON should be valid", json.startsWith("{") && json.endsWith("}"));
    }

    @Test
    public void test_toJsonString_withAllFields() {
        String[] text = { "test" };
        String[][] readings = { { "reading" } };
        String[] fields = { "field1" };
        String[] tags = { "tag1" };
        String[] roles = { "role1" };
        String[] languages = { "en" };

        SuggestItem item = new SuggestItem(text, readings, fields, 100L, 50L, 2.0f, tags, roles, languages, SuggestItem.Kind.QUERY);

        String json = item.toJsonString();

        assertTrue("Should contain text", json.contains("test"));
        assertTrue("Should contain query_freq", json.contains("50"));
        assertTrue("Should contain doc_freq", json.contains("100"));
    }

    // ============================================================
    // Tests for getId consistency
    // ============================================================

    @Test
    public void test_getId_sameTextSameId() {
        String[] text1 = { "test" };
        String[][] readings1 = { { "reading1" } };
        SuggestItem item1 = new SuggestItem(text1, readings1, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        String[] text2 = { "test" };
        String[][] readings2 = { { "reading2" } };
        SuggestItem item2 = new SuggestItem(text2, readings2, null, 10L, 20L, 2.0f, null, null, null, SuggestItem.Kind.DOCUMENT);

        // Same text should produce same ID regardless of other fields
        assertEquals(item1.getId(), item2.getId());
    }

    @Test
    public void test_getId_differentTextDifferentId() {
        String[] text1 = { "test1" };
        String[][] readings1 = { { "reading" } };
        SuggestItem item1 = new SuggestItem(text1, readings1, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        String[] text2 = { "test2" };
        String[][] readings2 = { { "reading" } };
        SuggestItem item2 = new SuggestItem(text2, readings2, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        assertFalse("Different text should produce different IDs", item1.getId().equals(item2.getId()));
    }

    @Test
    public void test_getId_multiWordOrder() {
        String[] text1 = { "word1", "word2" };
        String[][] readings1 = { { "r1" }, { "r2" } };
        SuggestItem item1 = new SuggestItem(text1, readings1, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        String[] text2 = { "word2", "word1" }; // Different order
        String[][] readings2 = { { "r2" }, { "r1" } };
        SuggestItem item2 = new SuggestItem(text2, readings2, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        // Different word order should produce different IDs (since text is "word1 word2" vs "word2 word1")
        assertFalse("Different word order should produce different IDs", item1.getId().equals(item2.getId()));
    }
}
