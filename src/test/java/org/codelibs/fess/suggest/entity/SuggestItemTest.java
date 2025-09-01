package org.codelibs.fess.suggest.entity;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codelibs.fess.suggest.constants.FieldNames;
import org.junit.Test;

public class SuggestItemTest {

    @Test
    public void testParameterizedConstructor() {
        // Test constructor with all parameters
        String[] text = { "test", "text" };
        String[][] readings = { { "reading1" }, { "reading2" } };
        String[] fields = { "field1", "field2" };
        String[] tags = { "tag1", "tag2" };
        String[] roles = { "role1", "role2" };
        String[] languages = { "en", "ja" };
        SuggestItem.Kind kind = SuggestItem.Kind.QUERY;

        SuggestItem item = new SuggestItem(text, readings, fields, 100L, 50L, 1.5f, tags, roles, languages, kind);

        assertNotNull(item);
        assertEquals("test text", item.getText()); // Text is joined with space
        assertArrayEquals(readings, item.getReadings());
        assertArrayEquals(fields, item.getFields());
        assertArrayEquals(tags, item.getTags());
        assertArrayEquals(roles, item.getRoles());
        assertArrayEquals(languages, item.getLanguages());
        assertEquals(1, item.getKinds().length);
        assertEquals(SuggestItem.Kind.QUERY, item.getKinds()[0]);
        assertEquals(50L, item.getQueryFreq());
        assertEquals(100L, item.getDocFreq());
        assertEquals(1.5f, item.getUserBoost(), 0.001f);
        assertNotNull(item.getTimestamp());
        assertNotNull(item.getId());
    }

    @Test
    public void testConstructorWithNullArrays() {
        // Test constructor handles null arrays gracefully
        String[] text = { "text" };
        String[][] readings = { { "reading" } };

        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.DOCUMENT);

        assertNotNull(item);
        assertEquals("text", item.getText());
        assertArrayEquals(readings, item.getReadings());
        assertNotNull(item.getFields());
        assertEquals(0, item.getFields().length);
        assertNotNull(item.getTags());
        assertEquals(0, item.getTags().length);
        assertNotNull(item.getRoles());
        assertEquals(1, item.getRoles().length); // Default role is added
        assertNotNull(item.getLanguages());
        assertEquals(0, item.getLanguages().length);
        assertNotNull(item.getKinds());
        assertEquals(1, item.getKinds().length);
        assertEquals(SuggestItem.Kind.DOCUMENT, item.getKinds()[0]);
    }

    @Test
    public void testGettersAndSetters() {
        // Test all getter and setter methods
        String[] text = { "initial" };
        String[][] readings = { { "read" } };
        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.USER);

        // Test text
        item.setText("sample text");
        assertEquals("sample text", item.getText());

        // Test timestamp
        ZonedDateTime now = ZonedDateTime.now();
        item.setTimestamp(now);
        assertEquals(now, item.getTimestamp());

        // Test query frequency
        item.setQueryFreq(200L);
        assertEquals(200L, item.getQueryFreq());

        // Test document frequency
        item.setDocFreq(150L);
        assertEquals(150L, item.getDocFreq());

        // Test user boost
        item.setUserBoost(2.0f);
        assertEquals(2.0f, item.getUserBoost(), 0.001f);

        // Test readings
        String[][] newReadings = { { "read1", "read2" }, { "read3" } };
        item.setReadings(newReadings);
        assertArrayEquals(newReadings, item.getReadings());

        // Test fields
        String[] fields = { "field1", "field2" };
        item.setFields(fields);
        assertArrayEquals(fields, item.getFields());

        // Test tags
        String[] tags = { "tag1", "tag2" };
        item.setTags(tags);
        assertArrayEquals(tags, item.getTags());

        // Test roles
        String[] roles = { "admin", "user" };
        item.setRoles(roles);
        assertArrayEquals(roles, item.getRoles());

        // Test languages
        String[] languages = { "en", "fr" };
        item.setLanguages(languages);
        assertArrayEquals(languages, item.getLanguages());

        // Test kinds
        SuggestItem.Kind[] kinds = { SuggestItem.Kind.DOCUMENT, SuggestItem.Kind.QUERY };
        item.setKinds(kinds);
        assertArrayEquals(kinds, item.getKinds());

        // Test ID
        item.setId("custom-id");
        assertEquals("custom-id", item.getId());
    }

    @Test
    public void testIsBadWord() {
        // Test isBadWord method
        String[] text = { "test", "text" };
        String[][] readings = { { "test" }, { "text" } };
        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        // Test with no bad words
        String[] badWords = { "spam", "illegal" };
        assertFalse(item.isBadWord(badWords));

        // Test with matching bad word
        String[] badWords2 = { "test", "bad" };
        assertTrue(item.isBadWord(badWords2));

        // Test with partial match
        item.setText("testing something");
        String[] badWords3 = { "test" };
        assertTrue(item.isBadWord(badWords3));
    }

    @Test
    public void testToEmptyMap() {
        // Test toEmptyMap method
        String[] text = { "test" };
        String[][] readings = { { "test" } };
        SuggestItem item = new SuggestItem(text, readings, null, 10L, 5L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        Map<String, Object> map = item.toEmptyMap();

        assertNotNull(map);
        assertEquals("", map.get(FieldNames.TEXT)); // Empty string for empty map
        assertEquals(0L, map.get(FieldNames.QUERY_FREQ));
        assertEquals(0L, map.get(FieldNames.DOC_FREQ));
        assertEquals(1.0f, map.get(FieldNames.USER_BOOST));
        assertNotNull(map.get(FieldNames.READING_PREFIX + "0"));
        assertNotNull(map.get(FieldNames.FIELDS));
        assertNotNull(map.get(FieldNames.TAGS));
        assertNotNull(map.get(FieldNames.ROLES));
        assertNotNull(map.get(FieldNames.LANGUAGES));
        assertNotNull(map.get(FieldNames.KINDS));
        assertNotNull(map.get(FieldNames.TIMESTAMP));
    }

    @Test
    public void testGetSource() {
        // Test getSource method
        String[] text = { "source", "test" };
        String[][] readings = { { "source" }, { "test" } };
        String[] fields = { "field1" };
        String[] tags = { "tag1" };
        String[] roles = { "admin" };
        String[] languages = { "en" };

        SuggestItem item = new SuggestItem(text, readings, fields, 25L, 50L, 2.0f, tags, roles, languages, SuggestItem.Kind.QUERY);

        Map<String, Object> source = item.getSource();

        assertNotNull(source);
        assertEquals("source test", source.get(FieldNames.TEXT));
        assertEquals(50L, source.get(FieldNames.QUERY_FREQ));
        assertEquals(25L, source.get(FieldNames.DOC_FREQ));
        assertEquals(2.0f, source.get(FieldNames.USER_BOOST));
        assertEquals(150.0f, source.get(FieldNames.SCORE)); // (50 + 25) * 2.0 = 150

        assertNotNull(source.get(FieldNames.READING_PREFIX + "0"));
        assertNotNull(source.get(FieldNames.READING_PREFIX + "1"));
        assertArrayEquals(fields, (String[]) source.get(FieldNames.FIELDS));
        assertArrayEquals(tags, (String[]) source.get(FieldNames.TAGS));
        assertArrayEquals(roles, (String[]) source.get(FieldNames.ROLES));
        assertArrayEquals(languages, (String[]) source.get(FieldNames.LANGUAGES));

        Object[] kinds = (Object[]) source.get(FieldNames.KINDS);
        assertNotNull(kinds);
        assertEquals(1, kinds.length);
        assertEquals("query", kinds[0]);
    }

    @Test
    public void testParseSource() {
        // Test parseSource method
        Map<String, Object> source = new HashMap<>();
        source.put(FieldNames.TEXT, "parsed text");
        source.put(FieldNames.QUERY_FREQ, 75L);
        source.put(FieldNames.DOC_FREQ, 35L);
        source.put(FieldNames.USER_BOOST, 2.0f);
        source.put(FieldNames.READING_PREFIX + "0", Arrays.asList("reading1", "reading2"));
        source.put(FieldNames.READING_PREFIX + "1", Arrays.asList("reading3"));
        source.put(FieldNames.FIELDS, Arrays.asList("field1"));
        source.put(FieldNames.TAGS, Arrays.asList("tag1", "tag2"));
        source.put(FieldNames.ROLES, Arrays.asList("user"));
        source.put(FieldNames.LANGUAGES, Arrays.asList("ja"));
        source.put(FieldNames.KINDS, Arrays.asList("document"));
        source.put(FieldNames.TIMESTAMP, System.currentTimeMillis());

        SuggestItem item = SuggestItem.parseSource(source);

        assertNotNull(item);
        assertEquals("parsed text", item.getText());
        assertEquals(75L, item.getQueryFreq());
        assertEquals(35L, item.getDocFreq());
        assertEquals(2.0f, item.getUserBoost(), 0.001f);
        assertEquals(2, item.getReadings().length);
        assertArrayEquals(new String[] { "reading1", "reading2" }, item.getReadings()[0]);
        assertArrayEquals(new String[] { "reading3" }, item.getReadings()[1]);
        assertEquals(1, item.getFields().length);
        assertEquals("field1", item.getFields()[0]);
        assertEquals(2, item.getTags().length);
        assertEquals(1, item.getRoles().length);
        assertEquals(1, item.getLanguages().length);
        assertEquals(1, item.getKinds().length);
        assertEquals(SuggestItem.Kind.DOCUMENT, item.getKinds()[0]);
        assertNotNull(item.getTimestamp());
    }

    @Test
    public void testMerge() {
        // Test merge static method
        String[] text1 = { "item1" };
        String[][] readings1 = { { "read1" } };
        String[] tags1 = { "tag1" };
        String[] roles1 = { "role1" };
        String[] languages1 = { "en" };

        SuggestItem item1 = new SuggestItem(text1, readings1, null, 50L, 100L, 2.0f, tags1, roles1, languages1, SuggestItem.Kind.QUERY);

        String[] text2 = { "item1" }; // Same text for same ID
        String[][] readings2 = { { "read2" } };
        String[] tags2 = { "tag2" };
        String[] roles2 = { "role2" };
        String[] languages2 = { "ja" };

        SuggestItem item2 = new SuggestItem(text2, readings2, null, 25L, 50L, 1.5f, tags2, roles2, languages2, SuggestItem.Kind.DOCUMENT);

        SuggestItem merged = SuggestItem.merge(item1, item2);

        assertNotNull(merged);
        assertEquals("item1", merged.getText());
        assertEquals(150L, merged.getQueryFreq()); // Sum of frequencies
        assertEquals(75L, merged.getDocFreq()); // Sum of frequencies
        assertEquals(1.5f, merged.getUserBoost(), 0.001f); // Takes item2's boost

        // Check merged arrays contain both items' values
        assertTrue(merged.getTags().length >= 2);
        assertTrue(merged.getRoles().length >= 2);
        assertTrue(merged.getLanguages().length >= 2);
        assertEquals(2, merged.getKinds().length);
    }

    @Test
    public void testToJsonString() {
        // Test toJsonString method
        String[] text = { "json", "test" };
        String[][] readings = { { "json" }, { "test" } };
        String[] tags = { "tag1" };

        SuggestItem item = new SuggestItem(text, readings, null, 0L, 10L, 1.0f, tags, null, null, SuggestItem.Kind.QUERY);

        String json = item.toJsonString();

        assertNotNull(json);
        assertTrue(json.contains("\"" + FieldNames.TEXT + "\":\"json test\""));
        assertTrue(json.contains("\"" + FieldNames.QUERY_FREQ + "\":10"));
        assertTrue(json.contains("\"" + FieldNames.TAGS + "\""));
        assertTrue(json.contains("tag1"));
    }

    @Test
    public void testKindEnum() {
        // Test Kind enum
        assertEquals("query", SuggestItem.Kind.QUERY.toString());
        assertEquals("document", SuggestItem.Kind.DOCUMENT.toString());
        assertEquals("user", SuggestItem.Kind.USER.toString());
    }

    @Test
    public void testGetUpdatedSource() {
        // Test getUpdatedSource method
        String[] text = { "test" };
        String[][] readings = { { "test" } };
        SuggestItem item = new SuggestItem(text, readings, null, 10L, 20L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        Map<String, Object> existingSource = new HashMap<>();
        existingSource.put(FieldNames.QUERY_FREQ, 5L);
        existingSource.put(FieldNames.DOC_FREQ, 10L);
        existingSource.put(FieldNames.TAGS, Arrays.asList("existing"));

        Map<String, Object> updated = item.getUpdatedSource(existingSource);

        assertNotNull(updated);
        assertEquals("test", updated.get(FieldNames.TEXT));
        assertEquals(25L, updated.get(FieldNames.QUERY_FREQ)); // 20 + 5
        assertEquals(20L, updated.get(FieldNames.DOC_FREQ)); // 10 + 10

        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) updated.get(FieldNames.TAGS);
        assertTrue(tags.contains("existing"));
    }

    @Test
    public void testConcatValues() {
        // Test static concatValues method
        List<String> dest = new java.util.ArrayList<>();
        dest.add("existing");

        SuggestItem.concatValues(dest, "new1", "new2", "existing");

        assertEquals(3, dest.size());
        assertTrue(dest.contains("existing"));
        assertTrue(dest.contains("new1"));
        assertTrue(dest.contains("new2"));
        // Should not have duplicate "existing"
        assertEquals(1, dest.stream().filter(s -> s.equals("existing")).count());
    }

    @Test
    public void testConcatKinds() {
        // Test static concatKinds method
        SuggestItem.Kind[] kinds1 = { SuggestItem.Kind.QUERY };
        SuggestItem.Kind[] kinds2 = { SuggestItem.Kind.DOCUMENT, SuggestItem.Kind.QUERY };

        SuggestItem.Kind[] result = SuggestItem.concatKinds(kinds1, kinds2);

        assertNotNull(result);
        assertEquals(2, result.length); // Should not have duplicates
        assertTrue(Arrays.asList(result).contains(SuggestItem.Kind.QUERY));
        assertTrue(Arrays.asList(result).contains(SuggestItem.Kind.DOCUMENT));
    }

    @Test
    public void testEmptySource() {
        // Test setEmptySource and related methods
        String[] text = { "test" };
        String[][] readings = { { "test" } };
        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        Map<String, Object> emptySource = new HashMap<>();
        emptySource.put("custom", "value");
        item.setEmptySource(emptySource);

        Map<String, Object> result = item.toEmptyMap();
        assertEquals(emptySource, result);
    }

    @Test
    public void testUserBoostMinimum() {
        // Test that user boost is at least 1
        String[] text = { "test" };
        String[][] readings = { { "test" } };

        // Test with boost less than 1
        SuggestItem item = new SuggestItem(text, readings, null, 0L, 0L, 0.5f, null, null, null, SuggestItem.Kind.QUERY);
        assertEquals(1.0f, item.getUserBoost(), 0.001f); // Should be set to 1

        // Test with boost greater than 1
        SuggestItem item2 = new SuggestItem(text, readings, null, 0L, 0L, 2.5f, null, null, null, SuggestItem.Kind.QUERY);
        assertEquals(2.5f, item2.getUserBoost(), 0.001f); // Should keep the value
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMergeDifferentIds() {
        // Test that merge throws exception for different IDs
        String[] text1 = { "item1" };
        String[][] readings1 = { { "read1" } };
        SuggestItem item1 = new SuggestItem(text1, readings1, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        String[] text2 = { "item2" }; // Different text -> different ID
        String[][] readings2 = { { "read2" } };
        SuggestItem item2 = new SuggestItem(text2, readings2, null, 0L, 0L, 1.0f, null, null, null, SuggestItem.Kind.QUERY);

        SuggestItem.merge(item1, item2); // Should throw IllegalArgumentException
    }
}