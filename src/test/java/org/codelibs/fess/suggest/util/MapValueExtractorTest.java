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
package org.codelibs.fess.suggest.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for MapValueExtractor.
 */
public class MapValueExtractorTest {

    @Test
    public void testGetString_withStringValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");

        assertEquals("value", MapValueExtractor.getString(map, "key"));
    }

    @Test
    public void testGetString_withNullValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", null);

        assertNull(MapValueExtractor.getString(map, "key"));
    }

    @Test
    public void testGetString_withMissingKey() {
        Map<String, Object> map = new HashMap<>();

        assertNull(MapValueExtractor.getString(map, "missing"));
    }

    @Test
    public void testGetString_withDefaultValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");

        assertEquals("value", MapValueExtractor.getString(map, "key", "default"));
        assertEquals("default", MapValueExtractor.getString(map, "missing", "default"));
    }

    @Test
    public void testGetLong_withLongValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("long", 123L);
        map.put("int", 456);
        map.put("string", "789");

        assertEquals(Long.valueOf(123L), MapValueExtractor.getLong(map, "long"));
        assertEquals(Long.valueOf(456L), MapValueExtractor.getLong(map, "int"));
        assertEquals(Long.valueOf(789L), MapValueExtractor.getLong(map, "string"));
    }

    @Test
    public void testGetLong_withNullAndMissingKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("null", null);

        assertNull(MapValueExtractor.getLong(map, "null"));
        assertNull(MapValueExtractor.getLong(map, "missing"));
    }

    @Test
    public void testGetFloat_withFloatValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("float", 1.5f);
        map.put("double", 2.5d);
        map.put("int", 3);
        map.put("string", "4.5");

        assertEquals(1.5f, MapValueExtractor.getFloat(map, "float"), 0.001f);
        assertEquals(2.5f, MapValueExtractor.getFloat(map, "double"), 0.001f);
        assertEquals(3.0f, MapValueExtractor.getFloat(map, "int"), 0.001f);
        assertEquals(4.5f, MapValueExtractor.getFloat(map, "string"), 0.001f);
    }

    @Test
    public void testGetFloat_withNullAndMissingKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("null", null);

        assertNull(MapValueExtractor.getFloat(map, "null"));
        assertNull(MapValueExtractor.getFloat(map, "missing"));
    }

    @Test
    public void testGetStringList() {
        Map<String, Object> map = new HashMap<>();
        map.put("list", Arrays.asList("a", "b", "c"));
        map.put("empty", Arrays.asList());

        List<String> result = MapValueExtractor.getStringList(map, "list");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));

        List<String> empty = MapValueExtractor.getStringList(map, "empty");
        assertTrue(empty.isEmpty());

        assertTrue(MapValueExtractor.getStringList(map, "missing").isEmpty());
    }

    @Test
    public void testGetMap() {
        Map<String, Object> inner = new HashMap<>();
        inner.put("innerKey", "innerValue");

        Map<String, Object> map = new HashMap<>();
        map.put("nested", inner);

        Map<String, Object> result = MapValueExtractor.getMap(map, "nested");
        assertNotNull(result);
        assertEquals("innerValue", result.get("innerKey"));

        assertNull(MapValueExtractor.getMap(map, "missing"));
    }

    @Test
    public void testGetInteger() {
        Map<String, Object> map = new HashMap<>();
        map.put("int", 123);
        map.put("long", 456L);
        map.put("string", "789");

        assertEquals(Integer.valueOf(123), MapValueExtractor.getInteger(map, "int"));
        assertEquals(Integer.valueOf(456), MapValueExtractor.getInteger(map, "long"));
        assertEquals(Integer.valueOf(789), MapValueExtractor.getInteger(map, "string"));
        assertNull(MapValueExtractor.getInteger(map, "missing"));
    }

    @Test
    public void testGetDouble() {
        Map<String, Object> map = new HashMap<>();
        map.put("double", 1.5d);
        map.put("float", 2.5f);
        map.put("string", "3.5");

        assertEquals(1.5d, MapValueExtractor.getDouble(map, "double"), 0.001d);
        assertEquals(2.5d, MapValueExtractor.getDouble(map, "float"), 0.001d);
        assertEquals(3.5d, MapValueExtractor.getDouble(map, "string"), 0.001d);
        assertNull(MapValueExtractor.getDouble(map, "missing"));
    }

    @Test
    public void testGetBoolean() {
        Map<String, Object> map = new HashMap<>();
        map.put("true", true);
        map.put("false", false);
        map.put("stringTrue", "true");
        map.put("stringFalse", "false");

        assertEquals(Boolean.TRUE, MapValueExtractor.getBoolean(map, "true"));
        assertEquals(Boolean.FALSE, MapValueExtractor.getBoolean(map, "false"));
        assertEquals(Boolean.TRUE, MapValueExtractor.getBoolean(map, "stringTrue"));
        assertEquals(Boolean.FALSE, MapValueExtractor.getBoolean(map, "stringFalse"));
        assertNull(MapValueExtractor.getBoolean(map, "missing"));
    }

    @Test
    public void testHasValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        map.put("nullKey", null);

        assertTrue(MapValueExtractor.hasValue(map, "key"));
        assertFalse(MapValueExtractor.hasValue(map, "nullKey"));
        assertFalse(MapValueExtractor.hasValue(map, "missing"));
    }
}
