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

import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensearch.transport.client.Client;

public class SuggestSettingsBuilderTest {

    private static OpenSearchRunner runner;
    private static Client client;
    private SuggestSettingsBuilder builder;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        })
                .build(newConfigs().clusterName("SuggestSettingsBuilderTest")
                        .numOfNode(1)
                        .pluginTypes("org.codelibs.opensearch.extension.ExtensionPlugin"));
        runner.ensureYellow();
        client = runner.client();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        runner.close();
        runner.clean();
    }

    @Before
    public void setUp() {
        builder = new SuggestSettingsBuilder();
    }

    @Test
    public void testConstructor() {
        // Test default constructor
        SuggestSettingsBuilder testBuilder = new SuggestSettingsBuilder();
        assertNotNull(testBuilder);
        assertEquals("fess_suggest", testBuilder.settingsIndexName);
        assertNotNull(testBuilder.timeoutSettings);
        assertNotNull(testBuilder.initialSettings);
        assertTrue(testBuilder.initialSettings.isEmpty());
    }

    @Test
    public void testSetSettingsIndexName() {
        // Test setting index name with lowercase conversion
        SuggestSettingsBuilder result = builder.setSettingsIndexName("TEST_INDEX");
        assertEquals(builder, result); // Test method chaining
        assertEquals("test_index", builder.settingsIndexName);

        // Test setting index name with mixed case
        builder.setSettingsIndexName("TeSt_InDeX_NaMe");
        assertEquals("test_index_name", builder.settingsIndexName);

        // Test setting index name that's already lowercase
        builder.setSettingsIndexName("lowercase_index");
        assertEquals("lowercase_index", builder.settingsIndexName);
    }

    @Test
    public void testAddInitialSettings() {
        // Test adding single setting
        SuggestSettingsBuilder result = builder.addInitialSettings("key1", "value1");
        assertEquals(builder, result); // Test method chaining
        assertEquals(1, builder.initialSettings.size());
        assertEquals("value1", builder.initialSettings.get("key1"));

        // Test adding multiple settings
        builder.addInitialSettings("key2", 100);
        builder.addInitialSettings("key3", true);
        assertEquals(3, builder.initialSettings.size());
        assertEquals("value1", builder.initialSettings.get("key1"));
        assertEquals(100, builder.initialSettings.get("key2"));
        assertEquals(true, builder.initialSettings.get("key3"));

        // Test overwriting existing setting
        builder.addInitialSettings("key1", "newValue1");
        assertEquals(3, builder.initialSettings.size());
        assertEquals("newValue1", builder.initialSettings.get("key1"));
    }

    @Test
    public void testAddInitialSettingsWithNullValue() {
        // Test adding null value
        builder.addInitialSettings("nullKey", null);
        assertEquals(1, builder.initialSettings.size());
        assertTrue(builder.initialSettings.containsKey("nullKey"));
        assertEquals(null, builder.initialSettings.get("nullKey"));
    }

    @Test
    public void testScrollTimeout() {
        // Test setting scroll timeout
        SuggestSettingsBuilder result = builder.scrollTimeout("10s");
        assertEquals(builder, result); // Test method chaining
        assertEquals("10s", builder.timeoutSettings.scrollTimeout);

        // Test updating scroll timeout
        builder.scrollTimeout("30s");
        assertEquals("30s", builder.timeoutSettings.scrollTimeout);
    }

    @Test
    public void testSearchTimeout() {
        // Test setting search timeout
        SuggestSettingsBuilder result = builder.searchTimeout("5s");
        assertEquals(builder, result); // Test method chaining
        assertEquals("5s", builder.timeoutSettings.searchTimeout);

        // Test updating search timeout
        builder.searchTimeout("15s");
        assertEquals("15s", builder.timeoutSettings.searchTimeout);
    }

    @Test
    public void testIndexTimeout() {
        // Test setting index timeout
        SuggestSettingsBuilder result = builder.indexTimeout("20s");
        assertEquals(builder, result); // Test method chaining
        assertEquals("20s", builder.timeoutSettings.indexTimeout);

        // Test updating index timeout
        builder.indexTimeout("60s");
        assertEquals("60s", builder.timeoutSettings.indexTimeout);
    }

    @Test
    public void testBulkTimeout() {
        // Test setting bulk timeout
        SuggestSettingsBuilder result = builder.bulkTimeout("30s");
        assertEquals(builder, result); // Test method chaining
        assertEquals("30s", builder.timeoutSettings.bulkTimeout);

        // Test updating bulk timeout
        builder.bulkTimeout("120s");
        assertEquals("120s", builder.timeoutSettings.bulkTimeout);
    }

    @Test
    public void testIndicesTimeout() {
        // Test setting indices timeout
        SuggestSettingsBuilder result = builder.indicesTimeout("10s");
        assertEquals(builder, result); // Test method chaining
        assertEquals("10s", builder.timeoutSettings.indicesTimeout);

        // Test updating indices timeout
        builder.indicesTimeout("45s");
        assertEquals("45s", builder.timeoutSettings.indicesTimeout);
    }

    @Test
    public void testClusterTimeout() {
        // Test setting cluster timeout
        SuggestSettingsBuilder result = builder.clusterTimeout("15s");
        assertEquals(builder, result); // Test method chaining
        assertEquals("15s", builder.timeoutSettings.clusterTimeout);

        // Test updating cluster timeout
        builder.clusterTimeout("90s");
        assertEquals("90s", builder.timeoutSettings.clusterTimeout);
    }

    @Test
    public void testBuildWithDefaults() {
        // Test building with default values
        String id = "test-id";
        SuggestSettings settings = builder.build(client, id);

        assertNotNull(settings);
        // Verify that the settings object was created with the correct parameters
        // Note: We can't directly verify internal state of SuggestSettings without getters,
        // but we can verify it was created successfully
    }

    @Test
    public void testBuildWithCustomSettings() {
        // Test building with all custom settings
        String id = "custom-id";
        builder.setSettingsIndexName("CUSTOM_INDEX")
                .addInitialSettings("custom.key1", "value1")
                .addInitialSettings("custom.key2", 42)
                .scrollTimeout("100s")
                .searchTimeout("50s")
                .indexTimeout("200s")
                .bulkTimeout("300s")
                .indicesTimeout("150s")
                .clusterTimeout("250s");

        SuggestSettings settings = builder.build(client, id);

        assertNotNull(settings);
        assertEquals("custom_index", builder.settingsIndexName);
        assertEquals(2, builder.initialSettings.size());
        assertEquals("value1", builder.initialSettings.get("custom.key1"));
        assertEquals(42, builder.initialSettings.get("custom.key2"));
        assertEquals("100s", builder.timeoutSettings.scrollTimeout);
        assertEquals("50s", builder.timeoutSettings.searchTimeout);
        assertEquals("200s", builder.timeoutSettings.indexTimeout);
        assertEquals("300s", builder.timeoutSettings.bulkTimeout);
        assertEquals("150s", builder.timeoutSettings.indicesTimeout);
        assertEquals("250s", builder.timeoutSettings.clusterTimeout);
    }

    @Test
    public void testMethodChaining() {
        // Test that all methods return the builder instance for chaining
        String id = "chain-test";
        SuggestSettings settings = builder.setSettingsIndexName("CHAINED_INDEX")
                .addInitialSettings("chain.key1", "chainValue1")
                .addInitialSettings("chain.key2", "chainValue2")
                .scrollTimeout("11s")
                .searchTimeout("12s")
                .indexTimeout("13s")
                .bulkTimeout("14s")
                .indicesTimeout("15s")
                .clusterTimeout("16s")
                .build(client, id);

        assertNotNull(settings);
        assertEquals("chained_index", builder.settingsIndexName);
        assertEquals("11s", builder.timeoutSettings.scrollTimeout);
        assertEquals("12s", builder.timeoutSettings.searchTimeout);
        assertEquals("13s", builder.timeoutSettings.indexTimeout);
        assertEquals("14s", builder.timeoutSettings.bulkTimeout);
        assertEquals("15s", builder.timeoutSettings.indicesTimeout);
        assertEquals("16s", builder.timeoutSettings.clusterTimeout);
    }

    @Test
    public void testTimeoutSettingsIndependence() {
        // Test that timeout settings are independent for each builder instance
        SuggestSettingsBuilder builder1 = new SuggestSettingsBuilder();
        SuggestSettingsBuilder builder2 = new SuggestSettingsBuilder();

        builder1.scrollTimeout("10s");
        builder2.scrollTimeout("20s");

        assertEquals("10s", builder1.timeoutSettings.scrollTimeout);
        assertEquals("20s", builder2.timeoutSettings.scrollTimeout);
    }

    @Test
    public void testInitialSettingsIndependence() {
        // Test that initial settings are independent for each builder instance
        SuggestSettingsBuilder builder1 = new SuggestSettingsBuilder();
        SuggestSettingsBuilder builder2 = new SuggestSettingsBuilder();

        builder1.addInitialSettings("key", "value1");
        builder2.addInitialSettings("key", "value2");

        assertEquals("value1", builder1.initialSettings.get("key"));
        assertEquals("value2", builder2.initialSettings.get("key"));
    }

    @Test
    public void testBuildMultipleInstances() {
        // Test that multiple SuggestSettings can be built from the same builder
        builder.setSettingsIndexName("MULTI_INDEX").addInitialSettings("multi.key", "multiValue").scrollTimeout("25s");

        SuggestSettings settings1 = builder.build(client, "id1");
        SuggestSettings settings2 = builder.build(client, "id2");

        assertNotNull(settings1);
        assertNotNull(settings2);
        // Both should be created with the same configuration
        assertEquals("multi_index", builder.settingsIndexName);
        assertEquals("multiValue", builder.initialSettings.get("multi.key"));
        assertEquals("25s", builder.timeoutSettings.scrollTimeout);
    }

    @Test
    public void testEmptyTimeoutValues() {
        // Test setting empty timeout values
        builder.scrollTimeout("").searchTimeout("").indexTimeout("").bulkTimeout("").indicesTimeout("").clusterTimeout("");

        assertEquals("", builder.timeoutSettings.scrollTimeout);
        assertEquals("", builder.timeoutSettings.searchTimeout);
        assertEquals("", builder.timeoutSettings.indexTimeout);
        assertEquals("", builder.timeoutSettings.bulkTimeout);
        assertEquals("", builder.timeoutSettings.indicesTimeout);
        assertEquals("", builder.timeoutSettings.clusterTimeout);

        // Should still build successfully
        SuggestSettings settings = builder.build(client, "empty-timeout-id");
        assertNotNull(settings);
    }

    @Test
    public void testSpecialCharactersInIndexName() {
        // Test index name with special characters
        builder.setSettingsIndexName("Test-Index_Name.2024");
        assertEquals("test-index_name.2024", builder.settingsIndexName);

        // Test index name with spaces (should be lowercased)
        builder.setSettingsIndexName("Test Index Name");
        assertEquals("test index name", builder.settingsIndexName);
    }

    @Test
    public void testVariousTimeoutFormats() {
        // Test various timeout format strings
        builder.scrollTimeout("1000ms")
                .searchTimeout("1m")
                .indexTimeout("1h")
                .bulkTimeout("1d")
                .indicesTimeout("500")
                .clusterTimeout("10000ms");

        assertEquals("1000ms", builder.timeoutSettings.scrollTimeout);
        assertEquals("1m", builder.timeoutSettings.searchTimeout);
        assertEquals("1h", builder.timeoutSettings.indexTimeout);
        assertEquals("1d", builder.timeoutSettings.bulkTimeout);
        assertEquals("500", builder.timeoutSettings.indicesTimeout);
        assertEquals("10000ms", builder.timeoutSettings.clusterTimeout);

        SuggestSettings settings = builder.build(client, "timeout-formats-id");
        assertNotNull(settings);
    }

    @Test
    public void testNullTimeoutValues() {
        // Test setting null timeout values
        builder.scrollTimeout(null).searchTimeout(null).indexTimeout(null).bulkTimeout(null).indicesTimeout(null).clusterTimeout(null);

        assertEquals(null, builder.timeoutSettings.scrollTimeout);
        assertEquals(null, builder.timeoutSettings.searchTimeout);
        assertEquals(null, builder.timeoutSettings.indexTimeout);
        assertEquals(null, builder.timeoutSettings.bulkTimeout);
        assertEquals(null, builder.timeoutSettings.indicesTimeout);
        assertEquals(null, builder.timeoutSettings.clusterTimeout);

        // Should still build successfully
        SuggestSettings settings = builder.build(client, "null-timeout-id");
        assertNotNull(settings);
    }
}