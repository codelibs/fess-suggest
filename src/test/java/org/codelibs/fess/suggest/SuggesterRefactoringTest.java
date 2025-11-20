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

import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensearch.action.admin.indices.alias.Alias;
import org.opensearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.transport.client.Client;

/**
 * Test class for refactoring changes made to Suggester class.
 *
 * Tests cover:
 * - Index alias helper method (getIndicesForAlias)
 * - Edge cases for switchIndex with EXPECTED_INDEX_COUNT
 * - Index lifecycle with refactored methods
 */
public class SuggesterRefactoringTest {
    static OpenSearchRunner runner;
    static Client client;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        })
                .build(newConfigs().clusterName("SuggesterRefactoringTest")
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
    public void before() throws Exception {
        runner.admin().indices().prepareDelete("_all").execute().actionGet();
        runner.refresh();
    }

    /**
     * Test getIndicesForAlias method with existing alias.
     * Verifies that the method correctly retrieves indices for a given alias.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testGetIndicesForAlias_existingAlias() throws Exception {
        final String indexName = "test-index-001";
        final String aliasName = "test-alias";

        // Create index with alias
        final CreateIndexResponse response = client.admin()
                .indices()
                .prepareCreate(indexName)
                .addAlias(new Alias(aliasName))
                .execute()
                .actionGet();
        assertTrue("Index creation should be acknowledged", response.isAcknowledged());

        final Suggester suggester = Suggester.builder().build(client, "test");

        // Use reflection to access private method
        final Method method = Suggester.class.getDeclaredMethod("getIndicesForAlias", String.class);
        method.setAccessible(true);
        final List<String> indices = (List<String>) method.invoke(suggester, aliasName);

        assertNotNull("Indices list should not be null", indices);
        assertEquals("Should find exactly one index", 1, indices.size());
        assertEquals("Should return correct index name", indexName, indices.get(0));

        // Cleanup
        client.admin().indices().prepareDelete(indexName).execute().actionGet();
        suggester.shutdown();
    }

    /**
     * Test getIndicesForAlias method with non-existing alias.
     * Verifies that the method returns an empty list for non-existing alias.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testGetIndicesForAlias_nonExistingAlias() throws Exception {
        final Suggester suggester = Suggester.builder().build(client, "test");

        // Use reflection to access private method
        final Method method = Suggester.class.getDeclaredMethod("getIndicesForAlias", String.class);
        method.setAccessible(true);
        final List<String> indices = (List<String>) method.invoke(suggester, "non-existing-alias");

        assertNotNull("Indices list should not be null", indices);
        assertEquals("Should return empty list for non-existing alias", 0, indices.size());

        suggester.shutdown();
    }

    /**
     * Test getIndicesForAlias method with multiple indices for same alias.
     * Verifies that the method retrieves all indices associated with an alias.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testGetIndicesForAlias_multipleIndices() throws Exception {
        final String index1 = "test-multi-index-001";
        final String index2 = "test-multi-index-002";
        final String aliasName = "test-multi-alias";

        // Create first index with alias
        client.admin().indices().prepareCreate(index1).addAlias(new Alias(aliasName)).execute().actionGet();

        // Create second index and add to same alias
        client.admin().indices().prepareCreate(index2).execute().actionGet();
        client.admin().indices().prepareAliases().addAlias(index2, aliasName).execute().actionGet();

        final Suggester suggester = Suggester.builder().build(client, "test");

        // Use reflection to access private method
        final Method method = Suggester.class.getDeclaredMethod("getIndicesForAlias", String.class);
        method.setAccessible(true);
        final List<String> indices = (List<String>) method.invoke(suggester, aliasName);

        assertNotNull("Indices list should not be null", indices);
        assertEquals("Should find exactly two indices", 2, indices.size());
        assertTrue("Should contain first index", indices.contains(index1));
        assertTrue("Should contain second index", indices.contains(index2));

        // Cleanup
        client.admin().indices().prepareDelete(index1, index2).execute().actionGet();
        suggester.shutdown();
    }

    /**
     * Test switchIndex with unexpected number of update indices.
     * Verifies that SuggesterException is thrown when EXPECTED_INDEX_COUNT is not met.
     */
    @Test
    public void testSwitchIndex_unexpectedUpdateIndicesCount() throws Exception {
        final Suggester suggester = Suggester.builder().build(client, "switch-test");
        suggester.createIndexIfNothing();
        Thread.sleep(100);

        // Create additional update index (making 2 total)
        final String updateAlias = suggester.getIndex() + ".update";
        final String extraIndexName = suggester.getIndex() + ".extra";

        client.admin().indices().prepareCreate(extraIndexName).execute().actionGet();
        client.admin().indices().prepareAliases().addAlias(extraIndexName, updateAlias).execute().actionGet();
        Thread.sleep(100);

        boolean exceptionThrown = false;
        String exceptionMessage = null;
        try {
            suggester.switchIndex();
            fail("Should throw SuggesterException for unexpected update indices count");
        } catch (SuggesterException e) {
            exceptionThrown = true;
            exceptionMessage = e.getMessage();
            // Check if the message contains the expected text
            // It could be either "Unexpected update indices num" or "Unexpected search indices num"
            // depending on which check fails first
            assertTrue("Exception message should mention unexpected indices: " + exceptionMessage,
                    exceptionMessage.contains("Unexpected update indices num") ||
                    exceptionMessage.contains("Unexpected search indices num"));
        } finally {
            // Cleanup
            try {
                client.admin().indices().prepareDelete(suggester.getIndex() + "*").execute().actionGet();
            } catch (Exception cleanupError) {
                // Ignore cleanup errors
            }
            suggester.shutdown();
        }

        assertTrue("SuggesterException should have been thrown", exceptionThrown);
    }

    /**
     * Test switchIndex when update and search indices are the same.
     * Verifies that the method returns early without errors.
     */
    @Test
    public void testSwitchIndex_sameUpdateAndSearchIndex() throws Exception {
        final Suggester suggester = Suggester.builder().build(client, "same-index-test");
        suggester.createIndexIfNothing();

        // Get the current indices
        final GetAliasesResponse aliasResponse = client.admin()
                .indices()
                .prepareGetAliases(suggester.getIndex())
                .execute()
                .actionGet();

        assertNotNull("Should have search alias", aliasResponse.getAliases());

        // Call switchIndex - should return early without error since they're already the same
        suggester.switchIndex();

        // Verify indices still exist and are functional
        final GetAliasesResponse afterResponse = client.admin()
                .indices()
                .prepareGetAliases(suggester.getIndex())
                .execute()
                .actionGet();

        assertNotNull("Aliases should still exist after switch", afterResponse.getAliases());

        // Cleanup
        client.admin().indices().prepareDelete(suggester.getIndex() + "*").execute().actionGet();
        suggester.shutdown();
    }

    /**
     * Test EXPECTED_INDEX_COUNT constant value.
     * Verifies that the constant is properly defined with expected value.
     */
    @Test
    public void testExpectedIndexCountConstant() throws Exception {
        final Field field = Suggester.class.getDeclaredField("EXPECTED_INDEX_COUNT");
        field.setAccessible(true);
        final int value = (int) field.get(null);

        assertEquals("EXPECTED_INDEX_COUNT should be 1", 1, value);
    }

    /**
     * Integration test for complete index lifecycle with refactored methods.
     * Tests that createNextIndex and switchIndex work together correctly.
     * Note: Full lifecycle testing is covered by SuggesterTest.test_switchIndex()
     */
    @Test
    public void testIndexLifecycle_withRefactoredMethods() throws Exception {
        final Suggester suggester = Suggester.builder().build(client, "lifecycle-test");
        suggester.createIndexIfNothing();
        Thread.sleep(100);

        // Verify initial state - should have one index
        GetAliasesResponse aliasResponse = client.admin()
                .indices()
                .prepareGetAliases(suggester.getIndex())
                .execute()
                .actionGet();
        assertTrue("Should have at least one index initially", aliasResponse.getAliases().size() >= 1);

        // Create next index - this is the main method we're testing
        Thread.sleep(1000); // Wait before creating next index (following existing test pattern)
        suggester.createNextIndex();
        Thread.sleep(100);

        // Verify that createNextIndex completed without exceptions
        // The update alias should now point to the new index
        final String updateAlias = suggester.getIndex() + ".update";
        aliasResponse = client.admin()
                .indices()
                .prepareGetAliases(updateAlias)
                .execute()
                .actionGet();
        assertNotNull("Update alias should exist after createNextIndex", aliasResponse.getAliases());

        // Switch to new index
        suggester.switchIndex();
        Thread.sleep(100);

        // Verify switch was successful - search alias should still exist
        aliasResponse = client.admin()
                .indices()
                .prepareGetAliases(suggester.getIndex())
                .execute()
                .actionGet();
        assertNotNull("Search alias should exist after switch", aliasResponse.getAliases());
        assertTrue("Search alias should point to at least one index", aliasResponse.getAliases().size() >= 1);

        // Cleanup
        try {
            client.admin().indices().prepareDelete(suggester.getIndex() + "*").execute().actionGet();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        suggester.shutdown();
    }

    /**
     * Test that resource files can be loaded successfully.
     * This indirectly tests getDefaultMappings and getDefaultIndexSettings.
     */
    @Test
    public void testResourceLoading_viaIndexCreation() throws Exception {
        final Suggester suggester = Suggester.builder().build(client, "resource-test");

        // createIndexIfNothing internally calls getDefaultMappings and getDefaultIndexSettings
        boolean created = suggester.createIndexIfNothing();

        assertTrue("Index should be created successfully", created);

        // Verify index exists
        final GetAliasesResponse aliasResponse = client.admin()
                .indices()
                .prepareGetAliases(suggester.getIndex())
                .execute()
                .actionGet();

        assertNotNull("Aliases should exist", aliasResponse.getAliases());
        assertEquals("Should have exactly one index", 1, aliasResponse.getAliases().size());

        // Cleanup
        client.admin().indices().prepareDelete(suggester.getIndex() + "*").execute().actionGet();
        suggester.shutdown();
    }
}
