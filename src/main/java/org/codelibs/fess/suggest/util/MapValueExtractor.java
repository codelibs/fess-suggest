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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for type-safe value extraction from Map objects.
 * Centralizes map access patterns to reduce code duplication and improve type safety.
 *
 * <p>This class provides methods to safely extract typed values from Map&lt;String, Object&gt;
 * commonly used when parsing OpenSearch document sources.
 */
public final class MapValueExtractor {

    private MapValueExtractor() {
        // Utility class
    }

    /**
     * Gets a String value from the map.
     *
     * @param map The source map
     * @param key The key to look up
     * @return The string value, or null if the key doesn't exist or value is null
     */
    public static String getString(final Map<String, Object> map, final String key) {
        return getString(map, key, null);
    }

    /**
     * Gets a String value from the map with a default value.
     *
     * @param map The source map
     * @param key The key to look up
     * @param defaultValue The default value to return if key doesn't exist or value is null
     * @return The string value, or defaultValue if not found
     */
    public static String getString(final Map<String, Object> map, final String key, final String defaultValue) {
        final Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    /**
     * Gets a Long value from the map.
     *
     * @param map The source map
     * @param key The key to look up
     * @return The Long value, or null if the key doesn't exist or value is null
     * @throws NumberFormatException if the value cannot be parsed as a Long
     */
    public static Long getLong(final Map<String, Object> map, final String key) {
        return getLong(map, key, null);
    }

    /**
     * Gets a Long value from the map with a default value.
     *
     * @param map The source map
     * @param key The key to look up
     * @param defaultValue The default value to return if key doesn't exist or value is null
     * @return The Long value, or defaultValue if not found
     * @throws NumberFormatException if the value cannot be parsed as a Long
     */
    public static Long getLong(final Map<String, Object> map, final String key, final Long defaultValue) {
        final Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number numValue) {
            return numValue.longValue();
        }
        return Long.parseLong(value.toString());
    }

    /**
     * Gets an Integer value from the map.
     *
     * @param map The source map
     * @param key The key to look up
     * @return The Integer value, or null if the key doesn't exist or value is null
     * @throws NumberFormatException if the value cannot be parsed as an Integer
     */
    public static Integer getInteger(final Map<String, Object> map, final String key) {
        return getInteger(map, key, null);
    }

    /**
     * Gets an Integer value from the map with a default value.
     *
     * @param map The source map
     * @param key The key to look up
     * @param defaultValue The default value to return if key doesn't exist or value is null
     * @return The Integer value, or defaultValue if not found
     * @throws NumberFormatException if the value cannot be parsed as an Integer
     */
    public static Integer getInteger(final Map<String, Object> map, final String key, final Integer defaultValue) {
        final Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number numValue) {
            return numValue.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    /**
     * Gets a Float value from the map.
     *
     * @param map The source map
     * @param key The key to look up
     * @return The Float value, or null if the key doesn't exist or value is null
     * @throws NumberFormatException if the value cannot be parsed as a Float
     */
    public static Float getFloat(final Map<String, Object> map, final String key) {
        return getFloat(map, key, null);
    }

    /**
     * Gets a Float value from the map with a default value.
     *
     * @param map The source map
     * @param key The key to look up
     * @param defaultValue The default value to return if key doesn't exist or value is null
     * @return The Float value, or defaultValue if not found
     * @throws NumberFormatException if the value cannot be parsed as a Float
     */
    public static Float getFloat(final Map<String, Object> map, final String key, final Float defaultValue) {
        final Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number numValue) {
            return numValue.floatValue();
        }
        return Float.parseFloat(value.toString());
    }

    /**
     * Gets a Double value from the map.
     *
     * @param map The source map
     * @param key The key to look up
     * @return The Double value, or null if the key doesn't exist or value is null
     * @throws NumberFormatException if the value cannot be parsed as a Double
     */
    public static Double getDouble(final Map<String, Object> map, final String key) {
        return getDouble(map, key, null);
    }

    /**
     * Gets a Double value from the map with a default value.
     *
     * @param map The source map
     * @param key The key to look up
     * @param defaultValue The default value to return if key doesn't exist or value is null
     * @return The Double value, or defaultValue if not found
     * @throws NumberFormatException if the value cannot be parsed as a Double
     */
    public static Double getDouble(final Map<String, Object> map, final String key, final Double defaultValue) {
        final Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number numValue) {
            return numValue.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    /**
     * Gets a Boolean value from the map.
     *
     * @param map The source map
     * @param key The key to look up
     * @return The Boolean value, or null if the key doesn't exist or value is null
     */
    public static Boolean getBoolean(final Map<String, Object> map, final String key) {
        return getBoolean(map, key, null);
    }

    /**
     * Gets a Boolean value from the map with a default value.
     *
     * @param map The source map
     * @param key The key to look up
     * @param defaultValue The default value to return if key doesn't exist or value is null
     * @return The Boolean value, or defaultValue if not found
     */
    public static Boolean getBoolean(final Map<String, Object> map, final String key, final Boolean defaultValue) {
        final Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Gets a List of Strings from the map.
     * This method handles both String and List&lt;String&gt; values:
     * - If the value is null, returns an empty list
     * - If the value is a String, returns a list containing that string
     * - If the value is a List, casts and returns it
     *
     * @param map The source map
     * @param key The key to look up
     * @return A List of strings, never null
     * @throws IllegalArgumentException if the value is not a String or List
     */
    @SuppressWarnings("unchecked")
    public static List<String> getStringList(final Map<String, Object> map, final String key) {
        final Object value = map.get(key);
        if (value == null) {
            return new ArrayList<>();
        }
        if (value instanceof String strValue) {
            final List<String> list = new ArrayList<>();
            list.add(strValue);
            return list;
        }
        if (value instanceof List<?> listValue) {
            return (List<String>) listValue;
        }
        throw new IllegalArgumentException("The value for key '" + key + "' should be String or List, but was " + value.getClass());
    }

    /**
     * Gets a nested Map from the map.
     *
     * @param map The source map
     * @param key The key to look up
     * @return The nested Map, or null if the key doesn't exist or value is not a Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(final Map<String, Object> map, final String key) {
        final Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    /**
     * Checks if the map contains the specified key and the value is not null.
     *
     * @param map The source map
     * @param key The key to check
     * @return true if the key exists and its value is not null
     */
    public static boolean hasValue(final Map<String, Object> map, final String key) {
        return map.containsKey(key) && map.get(key) != null;
    }
}
