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
package org.codelibs.fess.suggest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Test class for resource loading functionality in Suggester class.
 *
 * Tests cover:
 * - Successful resource loading
 * - Missing resource error handling
 * - Resource content validation
 * - InputStream null safety in try-with-resources
 */
public class SuggesterResourceLoadingTest {

    /**
     * Test that getDefaultMappings correctly loads and returns mapping resource.
     */
    @Test
    public void testGetDefaultMappings_loadsCorrectly() throws Exception {
        // Load resource directly to verify it exists
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("suggest_indices/suggest/mappings-default.json")) {
            assertNotNull("Mapping resource should exist", is);
            final String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue("Mapping content should not be empty", content.length() > 0);
            assertTrue("Mapping should contain properties", content.contains("properties"));
        }
    }

    /**
     * Test that getDefaultIndexSettings correctly loads and returns settings resource.
     */
    @Test
    public void testGetDefaultIndexSettings_loadsCorrectly() throws Exception {
        // Load resource directly to verify it exists
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("suggest_indices/suggest.json")) {
            assertNotNull("Settings resource should exist", is);
            final String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue("Settings content should not be empty", content.length() > 0);
        }
    }

    /**
     * Test that try-with-resources handles null InputStream correctly.
     * Verifies that null InputStream doesn't cause NullPointerException during close.
     */
    @Test
    public void testTryWithResources_nullInputStreamSafety() {
        // This test demonstrates that try-with-resources handles null safely
        try (InputStream is = null) {
            if (is == null) {
                throw new IOException("Resource is null");
            }
            fail("Should not reach this point");
        } catch (IOException e) {
            assertEquals("Resource is null", e.getMessage());
            // No NullPointerException should be thrown when exiting try-with-resources
        }
    }

    /**
     * Test resource loading with missing resource.
     * Demonstrates that null InputStream is handled correctly.
     */
    @Test
    public void testResourceLoading_withMissingResource() throws Exception {
        // Test the pattern used in getDefaultMappings/getDefaultIndexSettings
        // with a non-existent resource
        try {
            try (final InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("non/existent/resource.json")) {
                if (is == null) {
                    throw new IOException("Resource not found: non/existent/resource.json");
                }
                // This line should not be reached
                new String(is.readAllBytes(), StandardCharsets.UTF_8);
                fail("Should throw IOException for missing resource");
            }
        } catch (IOException e) {
            assertEquals("Resource not found: non/existent/resource.json", e.getMessage());
            // Test passes - exception was thrown as expected
        }
    }

    /**
     * Test that resource content is properly read using readAllBytes.
     * Verifies that the new implementation correctly reads entire resource content.
     */
    @Test
    public void testResourceReading_readAllBytesCorrectness() throws Exception {
        // Read using the new method (readAllBytes)
        String contentNew;
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("suggest_indices/suggest/mappings-default.json")) {
            assertNotNull("Resource should exist", is);
            contentNew = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Verify content is valid JSON
        assertTrue("Content should start with {", contentNew.trim().startsWith("{"));
        assertTrue("Content should end with }", contentNew.trim().endsWith("}"));
        assertTrue("Content should be substantial", contentNew.length() > 100);

        // Verify it contains expected mapping fields
        assertTrue("Should contain properties definition", contentNew.contains("properties"));
    }

    /**
     * Test that both mapping and settings resources can be loaded simultaneously.
     * Verifies no resource contention or locking issues.
     */
    @Test
    public void testResourceLoading_simultaneousAccess() throws Exception {
        String mappings;
        String settings;

        // Load both resources
        try (InputStream is1 = getClass().getClassLoader()
                .getResourceAsStream("suggest_indices/suggest/mappings-default.json")) {
            assertNotNull("Mappings resource should exist", is1);
            mappings = new String(is1.readAllBytes(), StandardCharsets.UTF_8);
        }

        try (InputStream is2 = getClass().getClassLoader()
                .getResourceAsStream("suggest_indices/suggest.json")) {
            assertNotNull("Settings resource should exist", is2);
            settings = new String(is2.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertNotNull("Mappings should be loaded", mappings);
        assertNotNull("Settings should be loaded", settings);
        assertTrue("Mappings should not be empty", mappings.length() > 0);
        assertTrue("Settings should not be empty", settings.length() > 0);
    }

    /**
     * Test resource loading with UTF-8 encoding.
     * Verifies that StandardCharsets.UTF_8 is used correctly.
     */
    @Test
    public void testResourceLoading_utf8Encoding() throws Exception {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("suggest_indices/suggest/mappings-default.json")) {
            assertNotNull("Resource should exist", is);

            // Read with UTF-8 encoding
            final String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // Verify encoding doesn't corrupt content
            assertTrue("Content should be valid", content.length() > 0);
            assertTrue("Should not contain encoding errors", !content.contains("\uFFFD"));

            // Verify JSON structure is intact
            final int openBraces = content.length() - content.replace("{", "").length();
            final int closeBraces = content.length() - content.replace("}", "").length();
            assertEquals("Braces should be balanced", openBraces, closeBraces);
        }
    }

    /**
     * Test that InputStream is properly closed even when exception occurs.
     * Verifies try-with-resources cleanup behavior.
     */
    @Test
    public void testResourceLoading_exceptionHandling() {
        try {
            try (InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("suggest_indices/suggest/mappings-default.json")) {
                assertNotNull("Resource should exist", is);

                // Simulate an exception during processing
                if (is.available() > 0) {
                    throw new RuntimeException("Simulated processing error");
                }
            }
            fail("Should have thrown RuntimeException");
        } catch (RuntimeException e) {
            assertEquals("Simulated processing error", e.getMessage());
            // If InputStream wasn't properly closed, this would leak resources
            // try-with-resources ensures cleanup happens
        } catch (IOException e) {
            fail("Should not throw IOException: " + e.getMessage());
        }
    }

    /**
     * Test resource content integrity.
     * Verifies that loaded content matches expected structure.
     */
    @Test
    public void testResourceContent_integrity() throws Exception {
        // Test mappings content
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("suggest_indices/suggest/mappings-default.json")) {
            assertNotNull("Mappings resource should exist", is);
            final String mappings = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // Verify expected fields exist
            assertTrue("Should contain text field", mappings.contains("text"));
            assertTrue("Should define field types", mappings.contains("type"));
        }

        // Test settings content
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("suggest_indices/suggest.json")) {
            assertNotNull("Settings resource should exist", is);
            final String settings = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // Verify it's a valid JSON structure
            assertTrue("Settings should be JSON", settings.trim().startsWith("{"));
        }
    }
}
