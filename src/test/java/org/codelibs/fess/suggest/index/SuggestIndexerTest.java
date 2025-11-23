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

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.entity.ElevateWord;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLog;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensearch.index.query.QueryBuilders;

public class SuggestIndexerTest {
    static Suggester suggester;
    static OpenSearchRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        }).build(newConfigs().clusterName("SuggestIndexerTest").numOfNode(1)
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
        // Delete only the test index instead of "_all" for faster cleanup
        try {
            runner.admin().indices().prepareDelete("SuggestIndexerTest*").execute().actionGet();
        } catch (Exception e) {
            // Index might not exist, ignore
        }
        suggester = Suggester.builder().build(runner.client(), "SuggestIndexerTest");
        suggester.createIndexIfNothing();
    }

    @Test
    public void test_indexSingleItem() throws Exception {
        String[][] readings = new String[1][];
        readings[0] = new String[] { "test" };
        SuggestItem item = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        SuggestIndexResponse response = suggester.indexer().index(item);

        assertNotNull(response);
        assertEquals(1, response.getNumberOfInputDocs());
        assertEquals(1, response.getNumberOfSuggestDocs());
        assertFalse(response.hasError());
    }

    @Test
    public void test_indexMultipleItems() throws Exception {
        SuggestItem[] items = new SuggestItem[3];

        for (int i = 0; i < 3; i++) {
            String[][] readings = new String[1][];
            readings[0] = new String[] { "test" + i };
            items[i] = new SuggestItem(new String[] { "テスト" + i }, readings, new String[] { "content" }, 1, 0, -1,
                    new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        }

        SuggestIndexResponse response = suggester.indexer().index(items);

        assertNotNull(response);
        assertEquals(3, response.getNumberOfInputDocs());
        assertEquals(3, response.getNumberOfSuggestDocs());
        assertFalse(response.hasError());
    }

    @Test
    public void test_deleteById() throws Exception {
        String[][] readings = new String[1][];
        readings[0] = new String[] { "test" };
        SuggestItem item = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        suggester.indexer().index(item);
        suggester.refresh();

        SuggestDeleteResponse deleteResponse = suggester.indexer().delete(item.getId());

        assertNotNull(deleteResponse);
        assertFalse(deleteResponse.hasError());
    }

    @Test
    public void test_deleteByQueryString() throws Exception {
        String[][] readings = new String[1][];
        readings[0] = new String[] { "test" };
        SuggestItem item = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        suggester.indexer().index(item);
        suggester.refresh();

        SuggestDeleteResponse deleteResponse = suggester.indexer().deleteByQuery(FieldNames.TEXT + ":テスト");

        assertNotNull(deleteResponse);
        assertFalse(deleteResponse.hasError());
    }

    @Test
    public void test_deleteByQueryBuilder() throws Exception {
        String[][] readings = new String[1][];
        readings[0] = new String[] { "test" };
        SuggestItem item = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        suggester.indexer().index(item);
        suggester.refresh();

        SuggestDeleteResponse deleteResponse = suggester.indexer().deleteByQuery(QueryBuilders.matchQuery(FieldNames.TEXT, "テスト"));

        assertNotNull(deleteResponse);
        assertFalse(deleteResponse.hasError());
    }

    @Test
    public void test_deleteAll() throws Exception {
        SuggestItem[] items = new SuggestItem[3];
        for (int i = 0; i < 3; i++) {
            String[][] readings = new String[1][];
            readings[0] = new String[] { "test" + i };
            items[i] = new SuggestItem(new String[] { "テスト" + i }, readings, new String[] { "content" }, 1, 0, -1,
                    new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        }

        suggester.indexer().index(items);
        suggester.refresh();

        assertEquals(3, suggester.getAllWordsNum());

        SuggestDeleteResponse deleteResponse = suggester.indexer().deleteAll();

        assertNotNull(deleteResponse);
        assertFalse(deleteResponse.hasError());

        suggester.refresh();
        assertEquals(0, suggester.getAllWordsNum());
    }

    @Test
    public void test_deleteDocumentWords() throws Exception {
        SuggestItem[] items = new SuggestItem[2];

        String[][] readings1 = new String[1][];
        readings1[0] = new String[] { "doc" };
        items[0] = new SuggestItem(new String[] { "ドキュメント" }, readings1, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        String[][] readings2 = new String[1][];
        readings2[0] = new String[] { "query" };
        items[1] = new SuggestItem(new String[] { "クエリ" }, readings2, new String[] { "content" }, 0, 1, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.QUERY);

        suggester.indexer().index(items);
        suggester.refresh();

        assertEquals(2, suggester.getAllWordsNum());
        assertEquals(1, suggester.getDocumentWordsNum());
        assertEquals(1, suggester.getQueryWordsNum());

        SuggestDeleteResponse deleteResponse = suggester.indexer().deleteDocumentWords();

        assertNotNull(deleteResponse);
        assertFalse(deleteResponse.hasError());

        suggester.refresh();
        assertEquals(1, suggester.getAllWordsNum());
        assertEquals(0, suggester.getDocumentWordsNum());
        assertEquals(1, suggester.getQueryWordsNum());
    }

    @Test
    public void test_deleteQueryWords() throws Exception {
        SuggestItem[] items = new SuggestItem[2];

        String[][] readings1 = new String[1][];
        readings1[0] = new String[] { "doc" };
        items[0] = new SuggestItem(new String[] { "ドキュメント" }, readings1, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        String[][] readings2 = new String[1][];
        readings2[0] = new String[] { "query" };
        items[1] = new SuggestItem(new String[] { "クエリ" }, readings2, new String[] { "content" }, 0, 1, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.QUERY);

        suggester.indexer().index(items);
        suggester.refresh();

        assertEquals(2, suggester.getAllWordsNum());
        assertEquals(1, suggester.getDocumentWordsNum());
        assertEquals(1, suggester.getQueryWordsNum());

        SuggestDeleteResponse deleteResponse = suggester.indexer().deleteQueryWords();

        assertNotNull(deleteResponse);
        assertFalse(deleteResponse.hasError());

        suggester.refresh();
        assertEquals(1, suggester.getAllWordsNum());
        assertEquals(1, suggester.getDocumentWordsNum());
        assertEquals(0, suggester.getQueryWordsNum());
    }

    @Test
    public void test_indexFromSingleQueryLog() throws Exception {
        SuggestSettings settings = suggester.settings();
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        QueryLog queryLog = new QueryLog(field + ":検索", null);
        SuggestIndexResponse response = suggester.indexer().indexFromQueryLog(queryLog);

        assertNotNull(response);
        assertEquals(1, response.getNumberOfInputDocs());
        assertFalse(response.hasError());
    }

    @Test
    public void test_indexFromMultipleQueryLogs() throws Exception {
        SuggestSettings settings = suggester.settings();
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        QueryLog[] queryLogs = new QueryLog[3];
        queryLogs[0] = new QueryLog(field + ":検索", null);
        queryLogs[1] = new QueryLog(field + ":fess", null);
        queryLogs[2] = new QueryLog(field + ":エンジン", null);

        SuggestIndexResponse response = suggester.indexer().indexFromQueryLog(queryLogs);

        assertNotNull(response);
        assertEquals(3, response.getNumberOfInputDocs());
        assertFalse(response.hasError());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_indexFromDocument() throws Exception {
        SuggestSettings settings = suggester.settings();
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        Map<String, Object>[] documents = new Map[2];
        documents[0] = new HashMap<>();
        documents[0].put(field, "これはテストです");

        documents[1] = new HashMap<>();
        documents[1].put(field, "検索エンジン");

        SuggestIndexResponse response = suggester.indexer().indexFromDocument(documents);

        assertNotNull(response);
        assertEquals(2, response.getNumberOfInputDocs());
        assertFalse(response.hasError());
    }

    @Test
    public void test_indexFromSearchWord() throws Exception {
        String searchWord = "検索 エンジン";
        String[] fields = new String[] { "content" };
        String[] tags = new String[] { "tag1" };
        String[] roles = new String[] { SuggestConstants.DEFAULT_ROLE };

        SuggestIndexResponse response = suggester.indexer().indexFromSearchWord(searchWord, fields, tags, roles, 1, null);

        assertNotNull(response);
        assertEquals(1, response.getNumberOfInputDocs());
        assertEquals(1, response.getNumberOfSuggestDocs());
        assertFalse(response.hasError());
    }

    @Test
    public void test_indexFromSearchWordWithWhitespace() throws Exception {
        String searchWord = "検索　　 エンジン  ";
        String[] fields = new String[] { "content" };

        SuggestIndexResponse response = suggester.indexer().indexFromSearchWord(searchWord, fields, null, null, 1, null);

        assertNotNull(response);
        assertEquals(1, response.getNumberOfInputDocs());
        assertFalse(response.hasError());
    }

    @Test
    public void test_addBadWord() throws Exception {
        String[][] readings = new String[1][];
        readings[0] = new String[] { "badword" };
        SuggestItem item = new SuggestItem(new String[] { "バッドワード" }, readings, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        suggester.indexer().index(item);
        suggester.refresh();

        assertEquals(1, suggester.getAllWordsNum());

        SuggestDeleteResponse deleteResponse = suggester.indexer().addBadWord("バッドワード", true);

        assertNotNull(deleteResponse);
        assertFalse(deleteResponse.hasError());

        suggester.refresh();
        assertEquals(0, suggester.getAllWordsNum());
    }

    @Test
    public void test_addBadWordWithoutApply() throws Exception {
        String[][] readings = new String[1][];
        readings[0] = new String[] { "badword" };
        SuggestItem item = new SuggestItem(new String[] { "バッドワード" }, readings, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        suggester.indexer().index(item);
        suggester.refresh();

        assertEquals(1, suggester.getAllWordsNum());

        SuggestDeleteResponse deleteResponse = suggester.indexer().addBadWord("バッドワード", false);

        assertNotNull(deleteResponse);
        assertFalse(deleteResponse.hasError());

        suggester.refresh();
        assertEquals(1, suggester.getAllWordsNum());
    }

    @Test
    public void test_deleteBadWord() throws Exception {
        suggester.indexer().addBadWord("badword", false);

        String[] badWords = suggester.settings().badword().get(false);
        assertTrue(badWords.length > 0);

        suggester.indexer().deleteBadWord("badword");

        badWords = suggester.settings().badword().get(false);
        assertEquals(0, badWords.length);
    }

    @Test
    public void test_addElevateWord() throws Exception {
        ElevateWord elevateWord = new ElevateWord("test", 2.0f, Collections.singletonList("test"),
                Collections.singletonList("content"), null, null);

        SuggestIndexResponse response = suggester.indexer().addElevateWord(elevateWord, true);

        assertNotNull(response);
        assertFalse(response.hasError());

        suggester.refresh();

        ElevateWord[] elevateWords = suggester.settings().elevateWord().get();
        assertEquals(1, elevateWords.length);
        assertEquals("test", elevateWords[0].getElevateWord());
    }

    @Test
    public void test_addElevateWordWithoutApply() throws Exception {
        ElevateWord elevateWord = new ElevateWord("test", 2.0f, Collections.singletonList("test"),
                Collections.singletonList("content"), null, null);

        SuggestIndexResponse response = suggester.indexer().addElevateWord(elevateWord, false);

        assertNotNull(response);
        assertEquals(0, response.getNumberOfInputDocs());
        assertEquals(0, response.getNumberOfSuggestDocs());

        ElevateWord[] elevateWords = suggester.settings().elevateWord().get();
        assertEquals(1, elevateWords.length);
    }

    @Test
    public void test_deleteElevateWord() throws Exception {
        ElevateWord elevateWord = new ElevateWord("test", 2.0f, Collections.singletonList("test"),
                Collections.singletonList("content"), null, null);

        suggester.indexer().addElevateWord(elevateWord, true);
        suggester.refresh();

        ElevateWord[] elevateWords = suggester.settings().elevateWord().get();
        assertEquals(1, elevateWords.length);

        SuggestDeleteResponse deleteResponse = suggester.indexer().deleteElevateWord("test", true);

        assertNotNull(deleteResponse);
        assertFalse(deleteResponse.hasError());

        elevateWords = suggester.settings().elevateWord().get();
        assertEquals(0, elevateWords.length);
    }

    @Test
    public void test_restoreElevateWord() throws Exception {
        ElevateWord elevateWord1 = new ElevateWord("test1", 2.0f, Collections.singletonList("test1"),
                Collections.singletonList("content"), null, null);
        ElevateWord elevateWord2 = new ElevateWord("test2", 3.0f, Collections.singletonList("test2"),
                Collections.singletonList("content"), null, null);

        suggester.settings().elevateWord().add(elevateWord1);
        suggester.settings().elevateWord().add(elevateWord2);

        SuggestIndexResponse response = suggester.indexer().restoreElevateWord();

        assertNotNull(response);
        assertEquals(2, response.getNumberOfInputDocs());
        assertEquals(2, response.getNumberOfSuggestDocs());
        assertFalse(response.hasError());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_deleteOldWords() throws Exception {
        SuggestSettings settings = suggester.settings();
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        // Index old data first
        Map<String, Object> document = new HashMap<>();
        document.put(field, "この柿は美味しい。");
        suggester.indexer().indexFromDocument(new Map[] { document });
        suggester.refresh();

        long oldWordsCount = suggester.getAllWordsNum();
        assertTrue(oldWordsCount > 0);

        // Short sleep to ensure timestamp separation (reduced from 1000ms to 150ms for 85% performance gain)
        Thread.sleep(150);
        ZonedDateTime threshold = ZonedDateTime.now();
        Thread.sleep(150);

        // Index new data after threshold
        document = new HashMap<>();
        document.put(field, "検索エンジン");
        suggester.indexer().indexFromDocument(new Map[] { document });
        suggester.refresh();

        long totalWordsCount = suggester.getAllWordsNum();
        assertTrue(totalWordsCount > oldWordsCount);

        SuggestDeleteResponse deleteResponse = suggester.indexer().deleteOldWords(threshold);

        assertNotNull(deleteResponse);
        assertFalse(deleteResponse.hasError());

        suggester.refresh();
        long remainingWordsCount = suggester.getAllWordsNum();
        assertTrue(remainingWordsCount < totalWordsCount);
        assertTrue(remainingWordsCount > 0);
    }

    @Test
    public void test_setters() throws Exception {
        SuggestIndexer indexer = suggester.indexer();

        assertNotNull(indexer.setIndexName("test-index"));
        assertNotNull(indexer.setSupportedFields(new String[] { "field1", "field2" }));
        assertNotNull(indexer.setTagFieldNames(new String[] { "tag1", "tag2" }));
        assertNotNull(indexer.setRoleFieldName("role"));
        assertNotNull(indexer.setReadingConverter(null));
        assertNotNull(indexer.setNormalizer(null));
        assertNotNull(indexer.setAnalyzer(null));
        assertNotNull(indexer.setContentsParser(null));
        assertNotNull(indexer.setSuggestWriter(null));
    }

    @Test
    public void test_indexBadWordItem() throws Exception {
        suggester.indexer().addBadWord("bad", false);

        String[][] readings = new String[1][];
        readings[0] = new String[] { "bad" };
        SuggestItem item = new SuggestItem(new String[] { "bad" }, readings, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        SuggestIndexResponse response = suggester.indexer().index(item);

        assertNotNull(response);
        assertEquals(1, response.getNumberOfInputDocs());
        assertEquals(1, response.getNumberOfSuggestDocs());
        assertFalse(response.hasError());
    }

    @Test
    public void test_indexFromQueryLogWithParallel() throws Exception {
        SuggestSettings settings = suggester.settings();
        settings.set(SuggestSettings.DefaultKeys.PARALLEL_PROCESSING, "true");
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        QueryLog[] queryLogs = new QueryLog[10];
        for (int i = 0; i < 10; i++) {
            queryLogs[i] = new QueryLog(field + ":検索" + i, null);
        }

        SuggestIndexResponse response = suggester.indexer().indexFromQueryLog(queryLogs);

        assertNotNull(response);
        assertEquals(10, response.getNumberOfInputDocs());
        assertFalse(response.hasError());

        settings.set(SuggestSettings.DefaultKeys.PARALLEL_PROCESSING, "false");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_indexFromDocumentWithParallel() throws Exception {
        SuggestSettings settings = suggester.settings();
        settings.set(SuggestSettings.DefaultKeys.PARALLEL_PROCESSING, "true");
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        Map<String, Object>[] documents = new Map[10];
        for (int i = 0; i < 10; i++) {
            documents[i] = new HashMap<>();
            documents[i].put(field, "テストドキュメント" + i);
        }

        SuggestIndexResponse response = suggester.indexer().indexFromDocument(documents);

        assertNotNull(response);
        assertEquals(10, response.getNumberOfInputDocs());
        assertFalse(response.hasError());

        settings.set(SuggestSettings.DefaultKeys.PARALLEL_PROCESSING, "false");
    }

    @Test
    public void test_deleteDocumentWordsWithMixedKinds() throws Exception {
        SuggestItem[] items = new SuggestItem[3];

        // Document only
        String[][] readings1 = new String[1][];
        readings1[0] = new String[] { "doc1" };
        items[0] = new SuggestItem(new String[] { "ドキュメント1" }, readings1, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        // Query only
        String[][] readings2 = new String[1][];
        readings2[0] = new String[] { "query1" };
        items[1] = new SuggestItem(new String[] { "クエリ1" }, readings2, new String[] { "content" }, 0, 1, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.QUERY);

        // Both Document and Query (simulated by setting both frequencies)
        String[][] readings3 = new String[1][];
        readings3[0] = new String[] { "both" };
        items[2] = new SuggestItem(new String[] { "両方" }, readings3, new String[] { "content" }, 1L, 1L, -1.0f,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        items[2].setKinds(new SuggestItem.Kind[] { SuggestItem.Kind.DOCUMENT, SuggestItem.Kind.QUERY });

        suggester.indexer().index(items);
        suggester.refresh();

        assertEquals(3, suggester.getAllWordsNum());

        SuggestDeleteResponse deleteResponse = suggester.indexer().deleteDocumentWords();

        assertNotNull(deleteResponse);
        assertFalse(deleteResponse.hasError());

        suggester.refresh();

        // Document-only should be deleted, Query-only should remain, Both should remain but as Query only
        assertEquals(2, suggester.getAllWordsNum());
        assertEquals(0, suggester.getDocumentWordsNum());
        assertEquals(2, suggester.getQueryWordsNum());
    }

    @Test
    public void test_deleteQueryWordsWithMixedKinds() throws Exception {
        SuggestItem[] items = new SuggestItem[3];

        // Document only
        String[][] readings1 = new String[1][];
        readings1[0] = new String[] { "doc1" };
        items[0] = new SuggestItem(new String[] { "ドキュメント1" }, readings1, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        // Query only
        String[][] readings2 = new String[1][];
        readings2[0] = new String[] { "query1" };
        items[1] = new SuggestItem(new String[] { "クエリ1" }, readings2, new String[] { "content" }, 0, 1, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.QUERY);

        // Both Document and Query
        String[][] readings3 = new String[1][];
        readings3[0] = new String[] { "both" };
        items[2] = new SuggestItem(new String[] { "両方" }, readings3, new String[] { "content" }, 1L, 1L, -1.0f,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        items[2].setKinds(new SuggestItem.Kind[] { SuggestItem.Kind.DOCUMENT, SuggestItem.Kind.QUERY });

        suggester.indexer().index(items);
        suggester.refresh();

        assertEquals(3, suggester.getAllWordsNum());

        SuggestDeleteResponse deleteResponse = suggester.indexer().deleteQueryWords();

        assertNotNull(deleteResponse);
        assertFalse(deleteResponse.hasError());

        suggester.refresh();

        // Query-only should be deleted, Document-only should remain, Both should remain but as Document only
        assertEquals(2, suggester.getAllWordsNum());
        assertEquals(2, suggester.getDocumentWordsNum());
        assertEquals(0, suggester.getQueryWordsNum());
    }

    @Test
    public void test_deleteDocumentWordsWithUserKind() throws Exception {
        SuggestItem[] items = new SuggestItem[2];

        // Document
        String[][] readings1 = new String[1][];
        readings1[0] = new String[] { "doc1" };
        items[0] = new SuggestItem(new String[] { "ドキュメント1" }, readings1, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        // User
        String[][] readings2 = new String[1][];
        readings2[0] = new String[] { "user1" };
        items[1] = new SuggestItem(new String[] { "ユーザー1" }, readings2, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.USER);

        suggester.indexer().index(items);
        suggester.refresh();

        assertEquals(2, suggester.getAllWordsNum());

        SuggestDeleteResponse deleteResponse = suggester.indexer().deleteDocumentWords();

        assertNotNull(deleteResponse);
        assertFalse(deleteResponse.hasError());

        suggester.refresh();

        // Only Document should be deleted, User should remain
        assertEquals(1, suggester.getAllWordsNum());
    }

    @Test
    public void test_deleteQueryWordsWithUserKind() throws Exception {
        SuggestItem[] items = new SuggestItem[2];

        // Query
        String[][] readings1 = new String[1][];
        readings1[0] = new String[] { "query1" };
        items[0] = new SuggestItem(new String[] { "クエリ1" }, readings1, new String[] { "content" }, 0, 1, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.QUERY);

        // User
        String[][] readings2 = new String[1][];
        readings2[0] = new String[] { "user1" };
        items[1] = new SuggestItem(new String[] { "ユーザー1" }, readings2, new String[] { "content" }, 0, 1, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.USER);

        suggester.indexer().index(items);
        suggester.refresh();

        assertEquals(2, suggester.getAllWordsNum());

        SuggestDeleteResponse deleteResponse = suggester.indexer().deleteQueryWords();

        assertNotNull(deleteResponse);
        assertFalse(deleteResponse.hasError());

        suggester.refresh();

        // Only Query should be deleted, User should remain
        assertEquals(1, suggester.getAllWordsNum());
    }
}
