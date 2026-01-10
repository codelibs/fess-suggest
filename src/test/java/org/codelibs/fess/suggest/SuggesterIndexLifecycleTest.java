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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.opensearch.action.admin.indices.get.GetIndexResponse;

/**
 * Tests for Suggester index lifecycle operations including edge cases.
 * Covers: createIndexIfNothing, createNextIndex, switchIndex, removeDisableIndices
 */
public class SuggesterIndexLifecycleTest {
    static OpenSearchRunner runner;
    static final String BASE_INDEX = "lifecycle-test";

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        }).build(newConfigs().clusterName("SuggesterIndexLifecycleTest").numOfNode(1)
                .pluginTypes("org.codelibs.opensearch.extension.ExtensionPlugin"));
        runner.ensureYellow();
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

    // ============================================================
    // Tests for createIndexIfNothing
    // ============================================================

    @Test
    public void test_createIndexIfNothing_createsNewIndex() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);

        boolean created = suggester.createIndexIfNothing();

        assertTrue("Should create index when none exists", created);

        // Verify index and aliases exist
        GetAliasesResponse aliasesResponse =
                runner.admin().indices().prepareGetAliases(BASE_INDEX).execute().actionGet();
        assertFalse("Search alias should exist", aliasesResponse.getAliases().isEmpty());

        GetAliasesResponse updateAliasResponse =
                runner.admin().indices().prepareGetAliases(BASE_INDEX + ".update").execute().actionGet();
        assertFalse("Update alias should exist", updateAliasResponse.getAliases().isEmpty());
    }

    @Test
    public void test_createIndexIfNothing_noOpWhenIndexExists() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);

        boolean firstCreate = suggester.createIndexIfNothing();
        assertTrue("First call should create index", firstCreate);

        boolean secondCreate = suggester.createIndexIfNothing();
        assertFalse("Second call should return false (already exists)", secondCreate);
    }

    @Test
    public void test_createIndexIfNothing_multipleCallsAreSafe() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);

        // Multiple concurrent-like calls should be safe
        for (int i = 0; i < 3; i++) {
            suggester.createIndexIfNothing();
        }

        // Verify only one index was created
        GetIndexResponse indexResponse =
                runner.admin().indices().prepareGetIndex().addIndices(BASE_INDEX + "*").execute().actionGet();
        assertEquals("Should have exactly one index", 1, indexResponse.getIndices().length);
    }

    // ============================================================
    // Tests for createNextIndex
    // ============================================================

    @Test
    public void test_createNextIndex_createsNewIndexAndSwitchesUpdateAlias() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);
        suggester.createIndexIfNothing();

        // Get original index name
        GetIndexResponse originalResponse =
                runner.admin().indices().prepareGetIndex().addIndices(BASE_INDEX + "*").execute().actionGet();
        String originalIndexName = originalResponse.getIndices()[0];

        // Wait a bit to ensure different timestamp in index name
        Thread.sleep(1100);

        // Create next index
        suggester.createNextIndex();

        // Verify we now have two indices
        GetIndexResponse newResponse =
                runner.admin().indices().prepareGetIndex().addIndices(BASE_INDEX + "*").execute().actionGet();
        assertEquals("Should have two indices", 2, newResponse.getIndices().length);

        // Verify update alias points to new index
        GetAliasesResponse updateAliasResponse =
                runner.admin().indices().prepareGetAliases(BASE_INDEX + ".update").execute().actionGet();
        assertEquals("Update alias should point to one index", 1, updateAliasResponse.getAliases().size());

        String newUpdateIndex = updateAliasResponse.getAliases().keySet().iterator().next();
        assertFalse("Update alias should point to different index", originalIndexName.equals(newUpdateIndex));
    }

    @Test
    public void test_createNextIndex_preservesSearchAlias() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);
        suggester.createIndexIfNothing();

        // Get original search alias index
        GetAliasesResponse originalSearchAlias =
                runner.admin().indices().prepareGetAliases(BASE_INDEX).execute().actionGet();
        String originalSearchIndex = originalSearchAlias.getAliases().keySet().iterator().next();

        Thread.sleep(1100);
        suggester.createNextIndex();

        // Search alias should still point to original index
        GetAliasesResponse newSearchAlias =
                runner.admin().indices().prepareGetAliases(BASE_INDEX).execute().actionGet();
        String newSearchIndex = newSearchAlias.getAliases().keySet().iterator().next();

        assertEquals("Search alias should still point to original index", originalSearchIndex, newSearchIndex);
    }

    // ============================================================
    // Tests for switchIndex
    // ============================================================

    @Test
    public void test_switchIndex_movesSearchAliasToUpdateIndex() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);
        suggester.createIndexIfNothing();

        Thread.sleep(1100);
        suggester.createNextIndex();

        // Get update index before switch
        GetAliasesResponse updateAlias =
                runner.admin().indices().prepareGetAliases(BASE_INDEX + ".update").execute().actionGet();
        String updateIndex = updateAlias.getAliases().keySet().iterator().next();

        // Switch index
        suggester.switchIndex();

        // Verify search alias now points to update index
        GetAliasesResponse searchAlias =
                runner.admin().indices().prepareGetAliases(BASE_INDEX).execute().actionGet();
        String searchIndex = searchAlias.getAliases().keySet().iterator().next();

        assertEquals("Search alias should now point to update index", updateIndex, searchIndex);
    }

    @Test
    public void test_switchIndex_noOpWhenSameIndex() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);
        suggester.createIndexIfNothing();

        // When search and update point to same index, switchIndex should be no-op
        suggester.switchIndex(); // Should not throw

        // Verify everything is still intact
        GetAliasesResponse searchAlias =
                runner.admin().indices().prepareGetAliases(BASE_INDEX).execute().actionGet();
        GetAliasesResponse updateAlias =
                runner.admin().indices().prepareGetAliases(BASE_INDEX + ".update").execute().actionGet();

        assertEquals("Search and update should point to same index",
                searchAlias.getAliases().keySet().iterator().next(),
                updateAlias.getAliases().keySet().iterator().next());
    }

    @Test
    public void test_switchIndex_throwsWhenMultipleUpdateIndices() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);
        suggester.createIndexIfNothing();

        // Manually create a second index with update alias to simulate edge case
        Thread.sleep(1100);
        String secondIndex = BASE_INDEX + ".manual";
        runner.admin().indices().prepareCreate(secondIndex).execute().actionGet();
        runner.admin().indices().prepareAliases()
                .addAlias(secondIndex, BASE_INDEX + ".update")
                .execute().actionGet();

        try {
            suggester.switchIndex();
            fail("Should throw when multiple indices have update alias");
        } catch (SuggesterException e) {
            assertTrue("Should mention unexpected number of indices",
                    e.getMessage().contains("Unexpected number of update indices"));
        }
    }

    @Test
    public void test_switchIndex_throwsWhenMultipleSearchIndices() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);
        suggester.createIndexIfNothing();

        // Create next index first (so update alias points to new index)
        Thread.sleep(1100);
        suggester.createNextIndex();

        // Manually add search alias to another index to simulate edge case
        String anotherIndex = BASE_INDEX + ".another";
        runner.admin().indices().prepareCreate(anotherIndex).execute().actionGet();
        runner.admin().indices().prepareAliases()
                .addAlias(anotherIndex, BASE_INDEX)
                .execute().actionGet();

        try {
            suggester.switchIndex();
            fail("Should throw when multiple indices have search alias");
        } catch (SuggesterException e) {
            assertTrue("Should mention unexpected number of indices",
                    e.getMessage().contains("Unexpected number of search indices"));
        }
    }

    // ============================================================
    // Tests for removeDisableIndices
    // ============================================================

    @Test
    public void test_removeDisableIndices_removesOrphanedIndices() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);
        suggester.createIndexIfNothing();

        Thread.sleep(1100);
        suggester.createNextIndex();
        suggester.switchIndex();

        // Now we have an orphaned index (original) without the search alias
        GetIndexResponse beforeRemove =
                runner.admin().indices().prepareGetIndex().addIndices(BASE_INDEX + "*").execute().actionGet();
        int indicesBeforeRemove = beforeRemove.getIndices().length;
        assertTrue("Should have at least 2 indices before cleanup", indicesBeforeRemove >= 2);

        // Remove disabled indices
        suggester.removeDisableIndices();

        // Verify orphaned index was removed
        GetIndexResponse afterRemove =
                runner.admin().indices().prepareGetIndex().addIndices(BASE_INDEX + "*").execute().actionGet();
        assertEquals("Should have only 1 index after cleanup", 1, afterRemove.getIndices().length);
    }

    @Test
    public void test_removeDisableIndices_preservesActiveIndices() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);
        suggester.createIndexIfNothing();

        // Index some data
        String[][] readings = new String[1][];
        readings[0] = new String[] { "test" };
        SuggestItem item = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" },
                1, 0, -1, new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        suggester.indexer().index(item);
        suggester.refresh();

        // Verify data exists
        assertEquals("Should have 1 word", 1, suggester.getAllWordsNum());

        // Remove disabled indices (should do nothing)
        suggester.removeDisableIndices();

        // Verify data still exists
        assertEquals("Should still have 1 word", 1, suggester.getAllWordsNum());
    }

    @Test
    public void test_removeDisableIndices_noOpWhenNoOrphanedIndices() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);
        suggester.createIndexIfNothing();

        GetIndexResponse before =
                runner.admin().indices().prepareGetIndex().addIndices(BASE_INDEX + "*").execute().actionGet();

        suggester.removeDisableIndices();

        GetIndexResponse after =
                runner.admin().indices().prepareGetIndex().addIndices(BASE_INDEX + "*").execute().actionGet();

        assertEquals("Should have same number of indices", before.getIndices().length, after.getIndices().length);
    }

    @Test
    public void test_removeDisableIndices_doesNotRemoveOtherIndices() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);
        suggester.createIndexIfNothing();

        // Create an unrelated index
        String unrelatedIndex = "unrelated-index";
        runner.admin().indices().prepareCreate(unrelatedIndex).execute().actionGet();

        suggester.removeDisableIndices();

        // Verify unrelated index still exists
        boolean unrelatedExists = runner.admin().indices().prepareExists(unrelatedIndex).execute().actionGet().isExists();
        assertTrue("Unrelated index should still exist", unrelatedExists);
    }

    // ============================================================
    // Tests for full lifecycle workflow
    // ============================================================

    @Test
    public void test_fullLifecycle_createIndexSwitchRemove() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);

        // Step 1: Create initial index
        assertTrue("Should create initial index", suggester.createIndexIfNothing());

        // Index some data in initial index
        String[][] readings = new String[1][];
        readings[0] = new String[] { "initial" };
        SuggestItem item = new SuggestItem(new String[] { "初期" }, readings, new String[] { "content" },
                1, 0, -1, new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        suggester.indexer().index(item);
        suggester.refresh();

        assertEquals("Should have 1 word", 1, suggester.getAllWordsNum());

        // Step 2: Create next index
        Thread.sleep(1100);
        suggester.createNextIndex();

        // Index different data in new index
        String[][] readings2 = new String[1][];
        readings2[0] = new String[] { "next" };
        SuggestItem item2 = new SuggestItem(new String[] { "次" }, readings2, new String[] { "content" },
                1, 0, -1, new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        suggester.indexer().index(item2);
        suggester.refresh();

        // Search should still see old data (search alias points to old index)
        assertEquals("Should still have 1 word (searching old index)", 1, suggester.getAllWordsNum());

        // Step 3: Switch search to new index
        suggester.switchIndex();
        suggester.refresh();

        // Search should now see new data
        assertEquals("Should now have 1 word (searching new index)", 1, suggester.getAllWordsNum());

        // Step 4: Remove old index
        suggester.removeDisableIndices();

        // Verify only new index remains
        GetIndexResponse indices =
                runner.admin().indices().prepareGetIndex().addIndices(BASE_INDEX + "*").execute().actionGet();
        assertEquals("Should have only 1 index after cleanup", 1, indices.length);

        // Data should still be accessible
        assertEquals("Should still have 1 word", 1, suggester.getAllWordsNum());
    }

    // ============================================================
    // Tests for getter methods
    // ============================================================

    @Test
    public void test_getIndex() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);

        assertEquals("Should return base index name", BASE_INDEX, suggester.getIndex());
    }

    @Test
    public void test_getAllWordsNum_onEmptyIndex() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);
        suggester.createIndexIfNothing();

        assertEquals("Empty index should have 0 words", 0, suggester.getAllWordsNum());
    }

    @Test
    public void test_getDocumentWordsNum_onEmptyIndex() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);
        suggester.createIndexIfNothing();

        assertEquals("Empty index should have 0 document words", 0, suggester.getDocumentWordsNum());
    }

    @Test
    public void test_getQueryWordsNum_onEmptyIndex() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);
        suggester.createIndexIfNothing();

        assertEquals("Empty index should have 0 query words", 0, suggester.getQueryWordsNum());
    }

    @Test
    public void test_settings_returnsNonNull() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);

        assertNotNull("Settings should not be null", suggester.settings());
    }

    @Test
    public void test_getReadingConverter_returnsNonNull() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);

        assertNotNull("ReadingConverter should not be null", suggester.getReadingConverter());
    }

    @Test
    public void test_getNormalizer_returnsNonNull() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);

        assertNotNull("Normalizer should not be null", suggester.getNormalizer());
    }

    @Test
    public void test_indexer_returnsNonNull() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);

        assertNotNull("Indexer should not be null", suggester.indexer());
    }

    @Test
    public void test_refresh_onEmptyIndex() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);
        suggester.createIndexIfNothing();

        // Should not throw
        assertNotNull(suggester.refresh());
    }

    @Test
    public void test_shutdown() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_INDEX);

        // Should not throw
        suggester.shutdown();
    }
}
