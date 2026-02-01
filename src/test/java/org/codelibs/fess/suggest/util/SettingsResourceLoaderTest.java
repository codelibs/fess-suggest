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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for SettingsResourceLoader.
 */
public class SettingsResourceLoaderTest {

    @Test
    public void testLoadJsonResource_existingResource() throws IOException {
        String content = SettingsResourceLoader.loadJsonResource("suggest_indices/suggest_settings.json");

        assertNotNull(content);
        assertTrue(content.length() > 0);
        assertTrue(content.contains("number_of_shards"));
    }

    @Test(expected = IOException.class)
    public void testLoadJsonResource_nonExistingResource() throws IOException {
        SettingsResourceLoader.loadJsonResource("non_existing_resource.json");
    }

    @Test
    public void testLoadJsonResourceWithSubstitution() throws IOException {
        Map<String, String> substitutions = new HashMap<>();
        substitutions.put("${test.placeholder}", "replaced_value");

        // Test loading with substitution - using a resource that exists
        String content = SettingsResourceLoader.loadJsonResource("suggest_indices/suggest_settings.json", substitutions);
        assertNotNull(content);
    }

    @Test
    public void testLoadJsonResourceWithDictionaryPath() throws IOException {
        // Set the dictionary path for testing
        String originalPath = System.getProperty("fess.dictionary.path", "");

        try {
            System.setProperty("fess.dictionary.path", "/test/dictionary/path");

            // Load the analyzer config which contains ${fess.dictionary.path}
            String content = SettingsResourceLoader.loadJsonResourceWithDictionaryPath("suggest_indices/suggest_analyzer.json");

            assertNotNull(content);
            assertTrue(content.length() > 0);
            // The placeholder should be replaced with the system property value
            assertFalse(content.contains("${fess.dictionary.path}"));
        } finally {
            // Restore original property
            if (originalPath.isEmpty()) {
                System.clearProperty("fess.dictionary.path");
            } else {
                System.setProperty("fess.dictionary.path", originalPath);
            }
        }
    }

    @Test
    public void testLoadJsonResourceWithDictionaryPath_emptyPath() throws IOException {
        // Clear the dictionary path property
        String originalPath = System.getProperty("fess.dictionary.path", "");

        try {
            System.clearProperty("fess.dictionary.path");

            String content = SettingsResourceLoader.loadJsonResourceWithDictionaryPath("suggest_indices/suggest_analyzer.json");

            assertNotNull(content);
            assertTrue(content.length() > 0);
            // The placeholder should be replaced with empty string
            assertFalse(content.contains("${fess.dictionary.path}"));
        } finally {
            // Restore original property
            if (!originalPath.isEmpty()) {
                System.setProperty("fess.dictionary.path", originalPath);
            }
        }
    }

    @Test
    public void testLoadJsonResource_emptySubstitutions() throws IOException {
        Map<String, String> emptySubstitutions = new HashMap<>();

        String content = SettingsResourceLoader.loadJsonResource("suggest_indices/suggest_settings.json", emptySubstitutions);

        assertNotNull(content);
        assertTrue(content.length() > 0);
    }
}
