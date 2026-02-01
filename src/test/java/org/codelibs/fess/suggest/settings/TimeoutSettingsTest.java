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
package org.codelibs.fess.suggest.settings;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for TimeoutSettings.
 */
public class TimeoutSettingsTest {

    @Test
    public void testDefaultValues() {
        TimeoutSettings settings = new TimeoutSettings();

        assertEquals("15s", settings.getSearchTimeout());
        assertEquals("1m", settings.getIndexTimeout());
        assertEquals("1m", settings.getBulkTimeout());
        assertEquals("1m", settings.getIndicesTimeout());
        assertEquals("1m", settings.getClusterTimeout());
        assertEquals("1m", settings.getScrollTimeout());
    }

    @Test
    public void testSetSearchTimeout() {
        TimeoutSettings settings = new TimeoutSettings();
        settings.setSearchTimeout("30s");
        assertEquals("30s", settings.getSearchTimeout());
    }

    @Test
    public void testSetIndexTimeout() {
        TimeoutSettings settings = new TimeoutSettings();
        settings.setIndexTimeout("2m");
        assertEquals("2m", settings.getIndexTimeout());
    }

    @Test
    public void testSetBulkTimeout() {
        TimeoutSettings settings = new TimeoutSettings();
        settings.setBulkTimeout("5m");
        assertEquals("5m", settings.getBulkTimeout());
    }

    @Test
    public void testSetIndicesTimeout() {
        TimeoutSettings settings = new TimeoutSettings();
        settings.setIndicesTimeout("3m");
        assertEquals("3m", settings.getIndicesTimeout());
    }

    @Test
    public void testSetClusterTimeout() {
        TimeoutSettings settings = new TimeoutSettings();
        settings.setClusterTimeout("10m");
        assertEquals("10m", settings.getClusterTimeout());
    }

    @Test
    public void testSetScrollTimeout() {
        TimeoutSettings settings = new TimeoutSettings();
        settings.setScrollTimeout("2m");
        assertEquals("2m", settings.getScrollTimeout());
    }

    @Test
    public void testAllSetters() {
        TimeoutSettings settings = new TimeoutSettings();

        settings.setSearchTimeout("10s");
        settings.setIndexTimeout("20s");
        settings.setBulkTimeout("30s");
        settings.setIndicesTimeout("40s");
        settings.setClusterTimeout("50s");
        settings.setScrollTimeout("60s");

        assertEquals("10s", settings.getSearchTimeout());
        assertEquals("20s", settings.getIndexTimeout());
        assertEquals("30s", settings.getBulkTimeout());
        assertEquals("40s", settings.getIndicesTimeout());
        assertEquals("50s", settings.getClusterTimeout());
        assertEquals("60s", settings.getScrollTimeout());
    }

    @Test
    public void testNestedTimeoutSettingsBackwardCompatibility() {
        // Test that the deprecated nested class still works
        SuggestSettings.TimeoutSettings nestedSettings = new SuggestSettings.TimeoutSettings();

        // Should have same default values
        assertEquals("15s", nestedSettings.getSearchTimeout());
        assertEquals("1m", nestedSettings.getIndexTimeout());

        // Should be able to set values
        nestedSettings.setSearchTimeout("25s");
        assertEquals("25s", nestedSettings.getSearchTimeout());
    }

    @Test
    public void testNestedTimeoutSettingsInheritance() {
        // Verify that nested class extends top-level class
        SuggestSettings.TimeoutSettings nestedSettings = new SuggestSettings.TimeoutSettings();
        assertTrue(nestedSettings instanceof TimeoutSettings);
    }
}
