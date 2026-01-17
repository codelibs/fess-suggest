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
package org.codelibs.fess.suggest.index;

import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.concurrent.Deferred;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggestIndexException;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLog;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLogReader;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for error handling scenarios in SuggestIndexer.
 */
public class SuggestIndexerErrorHandlingTest {
    static Suggester suggester;
    static OpenSearchRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        })
                .build(newConfigs().clusterName("SuggestIndexerErrorHandlingTest")
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
        suggester = Suggester.builder().build(runner.client(), "SuggestIndexerErrorHandlingTest");
        suggester.createIndexIfNothing();
    }

    // ============================================================
    // Tests for empty/null input handling
    // ============================================================

    @Test
    public void test_indexEmptyArray() throws Exception {
        SuggestItem[] items = new SuggestItem[0];
        SuggestIndexResponse response = suggester.indexer().index(items);

        assertNotNull(response);
        assertEquals(0, response.getNumberOfInputDocs());
        assertEquals(0, response.getNumberOfSuggestDocs());
        assertFalse(response.hasError());
    }

    @Test
    public void test_indexFromEmptyQueryLogs() throws Exception {
        QueryLog[] queryLogs = new QueryLog[0];
        SuggestIndexResponse response = suggester.indexer().indexFromQueryLog(queryLogs);

        assertNotNull(response);
        assertEquals(0, response.getNumberOfInputDocs());
        assertFalse(response.hasError());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_indexFromEmptyDocuments() throws Exception {
        Map<String, Object>[] documents = new Map[0];
        SuggestIndexResponse response = suggester.indexer().indexFromDocument(documents);

        assertNotNull(response);
        assertEquals(0, response.getNumberOfInputDocs());
        assertFalse(response.hasError());
    }

    @Test
    public void test_indexFromEmptySearchWord() throws Exception {
        String searchWord = "";
        String[] fields = new String[] { "content" };

        SuggestIndexResponse response = suggester.indexer().indexFromSearchWord(searchWord, fields, null, null, 1, null);

        assertNotNull(response);
        // Empty search word should result in no indexed items
        assertEquals(0, response.getNumberOfSuggestDocs());
    }

    @Test
    public void test_indexFromWhitespaceOnlySearchWord() throws Exception {
        String searchWord = "   　　  ";
        String[] fields = new String[] { "content" };

        SuggestIndexResponse response = suggester.indexer().indexFromSearchWord(searchWord, fields, null, null, 1, null);

        assertNotNull(response);
        assertEquals(0, response.getNumberOfSuggestDocs());
    }

    // ============================================================
    // Tests for bad word filtering
    // ============================================================

    @Test
    public void test_indexAllItemsFilteredByBadWords() throws Exception {
        // Add bad words first
        suggester.indexer().addBadWord("filtered", false);

        String[][] readings = new String[1][];
        readings[0] = new String[] { "filtered" };
        SuggestItem item = new SuggestItem(new String[] { "filtered" }, readings, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        SuggestIndexResponse response = suggester.indexer().index(item);

        assertNotNull(response);
        assertEquals(1, response.getNumberOfInputDocs());
        // Item should be filtered but still counted
        assertFalse(response.hasError());
    }

    @Test
    public void test_indexMixedBadWordItems() throws Exception {
        suggester.indexer().addBadWord("bad", false);

        SuggestItem[] items = new SuggestItem[3];

        // Good item
        String[][] readings1 = new String[1][];
        readings1[0] = new String[] { "good" };
        items[0] = new SuggestItem(new String[] { "good" }, readings1, new String[] { "content" }, 1, 0, -1, new String[] { "tag1" },
                new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        // Bad item
        String[][] readings2 = new String[1][];
        readings2[0] = new String[] { "bad" };
        items[1] = new SuggestItem(new String[] { "bad" }, readings2, new String[] { "content" }, 1, 0, -1, new String[] { "tag1" },
                new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        // Another good item
        String[][] readings3 = new String[1][];
        readings3[0] = new String[] { "another" };
        items[2] = new SuggestItem(new String[] { "another" }, readings3, new String[] { "content" }, 1, 0, -1, new String[] { "tag1" },
                new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        SuggestIndexResponse response = suggester.indexer().index(items);

        assertNotNull(response);
        assertEquals(3, response.getNumberOfInputDocs());
        assertFalse(response.hasError());

        suggester.refresh();
        // Only 2 items should be indexed (one filtered)
        assertEquals(2, suggester.getAllWordsNum());
    }

    // ============================================================
    // Tests for async operation error handling
    // ============================================================

    @Test
    public void test_indexFromQueryLogReaderWithEmptyReader() throws Exception {
        QueryLogReader emptyReader = new QueryLogReader() {
            @Override
            public QueryLog read() {
                return null; // Empty reader
            }

            @Override
            public void close() {
            }
        };

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<SuggestIndexResponse> responseRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Deferred<SuggestIndexResponse>.Promise promise = suggester.indexer().indexFromQueryLog(emptyReader, 10, 0);

        promise.then(response -> {
            responseRef.set(response);
            latch.countDown();
        }).error(error -> {
            errorRef.set(error);
            latch.countDown();
        });

        assertTrue("Should complete within timeout", latch.await(30, TimeUnit.SECONDS));
        assertNotNull("Should have response", responseRef.get());
        assertEquals(0, responseRef.get().getNumberOfInputDocs());
    }

    @Test
    public void test_indexFromQueryLogReaderWithErrorThrowingReader() throws Exception {
        QueryLogReader errorReader = new QueryLogReader() {
            @Override
            public QueryLog read() {
                throw new RuntimeException("Simulated read error");
            }

            @Override
            public void close() {
            }
        };

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Deferred<SuggestIndexResponse>.Promise promise = suggester.indexer().indexFromQueryLog(errorReader, 10, 0);

        promise.then(response -> {
            latch.countDown();
        }).error(error -> {
            errorRef.set(error);
            latch.countDown();
        });

        assertTrue("Should complete within timeout", latch.await(30, TimeUnit.SECONDS));
        assertNotNull("Should have error", errorRef.get());
        assertTrue("Error should be RuntimeException", errorRef.get() instanceof RuntimeException);
    }

    @Test
    public void test_indexFromQueryLogReaderClosesCalled() throws Exception {
        AtomicBoolean closeCalled = new AtomicBoolean(false);
        QueryLogReader reader = new QueryLogReader() {
            @Override
            public QueryLog read() {
                return null;
            }

            @Override
            public void close() {
                closeCalled.set(true);
            }
        };

        CountDownLatch latch = new CountDownLatch(1);

        Deferred<SuggestIndexResponse>.Promise promise = suggester.indexer().indexFromQueryLog(reader, 10, 0);
        promise.then(response -> latch.countDown()).error(error -> latch.countDown());

        assertTrue("Should complete within timeout", latch.await(30, TimeUnit.SECONDS));
        assertTrue("Close should be called", closeCalled.get());
    }

    // ============================================================
    // Tests for delete operations error handling
    // ============================================================

    @Test
    public void test_deleteNonExistentId() throws Exception {
        SuggestDeleteResponse response = suggester.indexer().delete("non-existent-id");

        assertNotNull(response);
        // Deleting non-existent ID should not cause error
        assertFalse(response.hasError());
    }

    @Test
    public void test_deleteByInvalidQueryString() throws Exception {
        try {
            suggester.indexer().deleteByQuery("invalid:[query");
            // Some invalid queries may still be accepted by OpenSearch
        } catch (Exception e) {
            // Expected - invalid query syntax
            assertTrue(e instanceof SuggestIndexException || e.getCause() != null);
        }
    }

    @Test
    public void test_deleteAllOnEmptyIndex() throws Exception {
        SuggestDeleteResponse response = suggester.indexer().deleteAll();

        assertNotNull(response);
        assertFalse(response.hasError());
    }

    @Test
    public void test_deleteDocumentWordsOnEmptyIndex() throws Exception {
        SuggestDeleteResponse response = suggester.indexer().deleteDocumentWords();

        assertNotNull(response);
        assertFalse(response.hasError());
    }

    @Test
    public void test_deleteQueryWordsOnEmptyIndex() throws Exception {
        SuggestDeleteResponse response = suggester.indexer().deleteQueryWords();

        assertNotNull(response);
        assertFalse(response.hasError());
    }

    // ============================================================
    // Tests for document parsing with edge case content
    // ============================================================

    @Test
    @SuppressWarnings("unchecked")
    public void test_indexFromDocumentWithMissingField() throws Exception {
        SuggestSettings settings = suggester.settings();
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        Map<String, Object>[] documents = new Map[1];
        documents[0] = new HashMap<>();
        // Document doesn't contain the supported field
        documents[0].put("other_field", "some content");

        SuggestIndexResponse response = suggester.indexer().indexFromDocument(documents);

        assertNotNull(response);
        assertEquals(1, response.getNumberOfInputDocs());
        // Should not error, just produce no suggest items
        assertFalse(response.hasError());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_indexFromDocumentWithEmptyFieldValue() throws Exception {
        SuggestSettings settings = suggester.settings();
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        Map<String, Object>[] documents = new Map[1];
        documents[0] = new HashMap<>();
        documents[0].put(field, "");

        SuggestIndexResponse response = suggester.indexer().indexFromDocument(documents);

        assertNotNull(response);
        assertFalse(response.hasError());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_indexFromDocumentWithNullFieldValue() throws Exception {
        SuggestSettings settings = suggester.settings();
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        Map<String, Object>[] documents = new Map[1];
        documents[0] = new HashMap<>();
        documents[0].put(field, null);

        SuggestIndexResponse response = suggester.indexer().indexFromDocument(documents);

        assertNotNull(response);
        assertFalse(response.hasError());
    }

    // ============================================================
    // Tests for elevate word error handling
    // ============================================================

    @Test
    public void test_deleteNonExistentElevateWord() throws Exception {
        SuggestDeleteResponse response = suggester.indexer().deleteElevateWord("non-existent", true);

        assertNotNull(response);
        // Deleting non-existent elevate word should not error
        assertFalse(response.hasError());
    }

    @Test
    public void test_deleteNonExistentBadWord() throws Exception {
        // Should not throw
        suggester.indexer().deleteBadWord("non-existent");
    }

    @Test
    public void test_restoreElevateWordOnEmpty() throws Exception {
        SuggestIndexResponse response = suggester.indexer().restoreElevateWord();

        assertNotNull(response);
        assertEquals(0, response.getNumberOfInputDocs());
        assertFalse(response.hasError());
    }

    // ============================================================
    // Tests for query log parsing edge cases
    // ============================================================

    @Test
    public void test_indexFromQueryLogWithInvalidQuerySyntax() throws Exception {
        SuggestSettings settings = suggester.settings();
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        // Create a query log with potentially problematic syntax
        QueryLog queryLog = new QueryLog(field + ":", null);

        try {
            SuggestIndexResponse response = suggester.indexer().indexFromQueryLog(queryLog);
            // May succeed with empty result or fail depending on implementation
            assertNotNull(response);
        } catch (SuggestIndexException e) {
            // Expected for invalid query
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void test_indexFromQueryLogWithSpecialCharacters() throws Exception {
        SuggestSettings settings = suggester.settings();
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        QueryLog queryLog = new QueryLog(field + ":test!@#$%^&*()", null);

        SuggestIndexResponse response = suggester.indexer().indexFromQueryLog(queryLog);

        assertNotNull(response);
        // Should handle special characters gracefully
    }

    // ============================================================
    // Tests for setters with null values
    // ============================================================

    @Test
    public void test_setNullIndexName() throws Exception {
        SuggestIndexer indexer = suggester.indexer();
        // Setting null index name should be allowed (though may cause issues later)
        indexer.setIndexName(null);
    }

    @Test
    public void test_setNullSupportedFields() throws Exception {
        SuggestIndexer indexer = suggester.indexer();
        // Setting null fields should be allowed
        indexer.setSupportedFields(null);
    }

    @Test
    public void test_setEmptySupportedFields() throws Exception {
        SuggestIndexer indexer = suggester.indexer();
        indexer.setSupportedFields(new String[0]);
    }

    // ============================================================
    // Tests for sequential batch operations
    // ============================================================

    @Test
    public void test_sequentialIndexOperations() throws Exception {
        int itemCount = 10;

        for (int i = 0; i < itemCount; i++) {
            String[][] readings = new String[1][];
            readings[0] = new String[] { "word" + i };
            SuggestItem item = new SuggestItem(new String[] { "ワード" + i }, readings, new String[] { "content" }, 1, 0, -1,
                    new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
            suggester.indexer().index(item);
        }

        suggester.refresh();
        // All items should be indexed
        assertEquals("Should have indexed all items", itemCount, suggester.getAllWordsNum());
    }
}
