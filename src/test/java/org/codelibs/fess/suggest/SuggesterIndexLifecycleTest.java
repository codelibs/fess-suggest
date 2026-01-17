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

import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensearch.action.admin.indices.get.GetIndexResponse;

/**
 * Tests for Suggester index lifecycle operations including edge cases.
 * Covers: createIndexIfNothing, createNextIndex, switchIndex, removeDisableIndices
 */
public class SuggesterIndexLifecycleTest {
    static OpenSearchRunner runner;
    static final String BASE_ID = "lifecycle-test";
    // The actual index/alias name is {id}.suggest
    static final String INDEX_NAME = BASE_ID + ".suggest";

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        })
                .build(newConfigs().clusterName("SuggesterIndexLifecycleTest")
                        .numOfNode(1)
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
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);

        boolean created = suggester.createIndexIfNothing();

        assertTrue("Should create index when none exists", created);

        // Verify the index was created by checking we can query it
        suggester.refresh();
        assertEquals("Empty index should have 0 words", 0, suggester.getAllWordsNum());
    }

    @Test
    public void test_createIndexIfNothing_noOpWhenIndexExists() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);

        boolean firstCreate = suggester.createIndexIfNothing();
        assertTrue("First call should create index", firstCreate);

        boolean secondCreate = suggester.createIndexIfNothing();
        assertFalse("Second call should return false (already exists)", secondCreate);
    }

    @Test
    public void test_createIndexIfNothing_multipleCallsAreSafe() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);

        // Multiple consecutive calls should be safe
        suggester.createIndexIfNothing();
        suggester.createIndexIfNothing();
        suggester.createIndexIfNothing();

        // Should still be able to use the index
        suggester.refresh();
        assertEquals("Index should work normally", 0, suggester.getAllWordsNum());
    }

    // ============================================================
    // Tests for data persistence and index lifecycle
    // ============================================================

    @Test
    public void test_dataPersistedAfterCreateIndex() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);
        suggester.createIndexIfNothing();

        // Index some data
        String[][] readings = new String[1][];
        readings[0] = new String[] { "test" };
        SuggestItem item = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 1, 0, -1, new String[] { "tag1" },
                new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        suggester.indexer().index(item);
        suggester.refresh();

        assertEquals("Should have 1 word after indexing", 1, suggester.getAllWordsNum());
    }

    @Test
    public void test_createNextIndex_createsSecondIndex() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);
        suggester.createIndexIfNothing();

        // Wait a bit to ensure different timestamp in index name
        Thread.sleep(1100);

        // Create next index
        suggester.createNextIndex();

        // Verify we now have two indices
        GetIndexResponse response = runner.admin().indices().prepareGetIndex().addIndices(INDEX_NAME + "*").execute().actionGet();
        assertEquals("Should have two indices", 2, response.getIndices().length);
    }

    @Test
    public void test_switchIndex_works() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);
        suggester.createIndexIfNothing();

        // Index data in initial index
        String[][] readings = new String[1][];
        readings[0] = new String[] { "initial" };
        SuggestItem item = new SuggestItem(new String[] { "初期" }, readings, new String[] { "content" }, 1, 0, -1, new String[] { "tag1" },
                new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        suggester.indexer().index(item);
        suggester.refresh();

        assertEquals("Should have 1 word in initial index", 1, suggester.getAllWordsNum());

        Thread.sleep(1100);
        suggester.createNextIndex();

        // Index different data in new index (via update alias)
        String[][] readings2 = new String[1][];
        readings2[0] = new String[] { "next" };
        SuggestItem item2 = new SuggestItem(new String[] { "次" }, readings2, new String[] { "content" }, 1, 0, -1, new String[] { "tag1" },
                new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        suggester.indexer().index(item2);
        suggester.refresh();

        // Search still sees old data (search alias points to old index)
        assertEquals("Should still see 1 word from old index", 1, suggester.getAllWordsNum());

        // Switch search to new index
        suggester.switchIndex();
        suggester.refresh();

        // Now search sees new data
        assertEquals("Should see 1 word from new index", 1, suggester.getAllWordsNum());
    }

    @Test
    public void test_removeDisableIndices_cleansUpOrphanedIndex() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);
        suggester.createIndexIfNothing();

        Thread.sleep(1100);
        suggester.createNextIndex();
        suggester.switchIndex();

        // Before cleanup
        GetIndexResponse beforeRemove = runner.admin().indices().prepareGetIndex().addIndices(INDEX_NAME + "*").execute().actionGet();
        assertTrue("Should have at least 2 indices before cleanup", beforeRemove.getIndices().length >= 2);

        // Remove orphaned indices
        suggester.removeDisableIndices();

        // After cleanup
        GetIndexResponse afterRemove = runner.admin().indices().prepareGetIndex().addIndices(INDEX_NAME + "*").execute().actionGet();
        assertEquals("Should have only 1 index after cleanup", 1, afterRemove.getIndices().length);
    }

    @Test
    public void test_removeDisableIndices_preservesActiveIndices() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);
        suggester.createIndexIfNothing();

        // Index some data
        String[][] readings = new String[1][];
        readings[0] = new String[] { "test" };
        SuggestItem item = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 1, 0, -1, new String[] { "tag1" },
                new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        suggester.indexer().index(item);
        suggester.refresh();

        assertEquals("Should have 1 word", 1, suggester.getAllWordsNum());

        // Remove disabled indices (should do nothing)
        suggester.removeDisableIndices();

        // Verify data still exists
        assertEquals("Should still have 1 word", 1, suggester.getAllWordsNum());
    }

    @Test
    public void test_fullLifecycle() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);

        // Step 1: Create initial index
        assertTrue("Should create initial index", suggester.createIndexIfNothing());

        // Index some data
        String[][] readings = new String[1][];
        readings[0] = new String[] { "initial" };
        SuggestItem item = new SuggestItem(new String[] { "初期" }, readings, new String[] { "content" }, 1, 0, -1, new String[] { "tag1" },
                new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        suggester.indexer().index(item);
        suggester.refresh();

        assertEquals("Should have 1 word", 1, suggester.getAllWordsNum());

        // Step 2: Create next index
        Thread.sleep(1100);
        suggester.createNextIndex();

        // Index different data in new index
        String[][] readings2 = new String[1][];
        readings2[0] = new String[] { "next" };
        SuggestItem item2 = new SuggestItem(new String[] { "次" }, readings2, new String[] { "content" }, 1, 0, -1, new String[] { "tag1" },
                new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        suggester.indexer().index(item2);
        suggester.refresh();

        // Search still sees old data
        assertEquals("Should still see 1 word from old index", 1, suggester.getAllWordsNum());

        // Step 3: Switch search to new index
        suggester.switchIndex();
        suggester.refresh();

        // Now search sees new data
        assertEquals("Should see 1 word from new index", 1, suggester.getAllWordsNum());

        // Step 4: Remove old index
        suggester.removeDisableIndices();

        // Verify only new index remains
        GetIndexResponse indices = runner.admin().indices().prepareGetIndex().addIndices(INDEX_NAME + "*").execute().actionGet();
        assertEquals("Should have only 1 index after cleanup", 1, indices.getIndices().length);

        // Data should still be accessible
        assertEquals("Should still have 1 word", 1, suggester.getAllWordsNum());
    }

    // ============================================================
    // Tests for getter methods
    // ============================================================

    @Test
    public void test_getIndex() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);

        // The index name should be {id}.suggest
        assertEquals("Should return correct index name", INDEX_NAME, suggester.getIndex());
    }

    @Test
    public void test_getAllWordsNum_onEmptyIndex() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);
        suggester.createIndexIfNothing();

        assertEquals("Empty index should have 0 words", 0, suggester.getAllWordsNum());
    }

    @Test
    public void test_getDocumentWordsNum_onEmptyIndex() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);
        suggester.createIndexIfNothing();

        assertEquals("Empty index should have 0 document words", 0, suggester.getDocumentWordsNum());
    }

    @Test
    public void test_getQueryWordsNum_onEmptyIndex() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);
        suggester.createIndexIfNothing();

        assertEquals("Empty index should have 0 query words", 0, suggester.getQueryWordsNum());
    }

    @Test
    public void test_settings_returnsNonNull() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);

        assertNotNull("Settings should not be null", suggester.settings());
    }

    @Test
    public void test_getReadingConverter_returnsNonNull() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);

        assertNotNull("ReadingConverter should not be null", suggester.getReadingConverter());
    }

    @Test
    public void test_getNormalizer_returnsNonNull() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);

        assertNotNull("Normalizer should not be null", suggester.getNormalizer());
    }

    @Test
    public void test_indexer_returnsNonNull() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);

        assertNotNull("Indexer should not be null", suggester.indexer());
    }

    @Test
    public void test_refresh_onEmptyIndex() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);
        suggester.createIndexIfNothing();

        // Should not throw
        assertNotNull(suggester.refresh());
    }

    @Test
    public void test_shutdown() throws Exception {
        Suggester suggester = Suggester.builder().build(runner.client(), BASE_ID);

        // Should not throw
        suggester.shutdown();
    }
}
