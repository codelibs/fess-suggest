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
package org.codelibs.fess.suggest.pit;

import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.SuggestIndexer;
import org.codelibs.fess.suggest.index.contents.document.ESSourceReader;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensearch.action.bulk.BulkRequestBuilder;
import org.opensearch.action.index.IndexAction;
import org.opensearch.action.index.IndexRequestBuilder;
import org.opensearch.action.search.CreatePitRequest;
import org.opensearch.action.search.CreatePitResponse;
import org.opensearch.action.search.SearchRequestBuilder;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.index.IndexNotFoundException;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.PointInTimeBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;
import org.opensearch.transport.client.Client;

/**
 * Tests for Point in Time (PIT) API implementation using OpenSearchRunner.
 * These tests verify that the Scroll API has been successfully replaced with PIT API.
 */
public class PitApiTest {
    static Suggester suggester;
    static OpenSearchRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        })
                .build(newConfigs().clusterName("PitApiTest")
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
        try {
            runner.admin().indices().prepareDelete("_all").execute().actionGet();
        } catch (IndexNotFoundException ignore) {
        }
        runner.refresh();
        suggester = Suggester.builder().build(runner.client(), "PitApiTest");
        suggester.createIndexIfNothing();
    }

    @Test
    public void testPitCreationAndUsage() throws Exception {
        String indexName = "test-pit-index";
        Client client = runner.client();
        SuggestSettings settings = suggester.settings();

        // Create and populate index
        addDocuments(indexName, client, 100);

        // Create PIT
        CreatePitRequest createPitRequest = new CreatePitRequest(
                TimeValue.parseTimeValue(settings.getScrollTimeout(), "keep_alive"),
                indexName);
        CreatePitResponse createPitResponse = client.createPit(createPitRequest)
                .actionGet(settings.getSearchTimeout());
        String pitId = createPitResponse.getId();

        assertNotNull("PIT ID should not be null", pitId);

        try {
            // Use PIT to search
            SearchRequestBuilder searchBuilder = client.prepareSearch()
                    .setQuery(QueryBuilders.matchAllQuery())
                    .setSize(10)
                    .setPointInTime(new PointInTimeBuilder(pitId));

            SearchResponse response = searchBuilder.execute().actionGet(settings.getSearchTimeout());

            assertEquals("Should return 10 documents", 10, response.getHits().getHits().length);
        } finally {
            // Clean up PIT
            SuggestUtil.deletePitContext(client, pitId);
        }
    }

    @Test
    public void testESSourceReaderWithPit() throws Exception {
        String indexName = "test-reader-pit";
        Client client = runner.client();
        SuggestSettings settings = suggester.settings();
        int numDocs = 1000;

        addDocuments(indexName, client, numDocs);

        // Test ESSourceReader which now uses PIT internally
        ESSourceReader reader = new ESSourceReader(client, settings, indexName);
        reader.setBatchSize(100);
        reader.addSort(SortBuilders.fieldSort("field2").order(SortOrder.ASC));

        int count = 0;
        Set<String> valueSet = new HashSet<>();
        Map<String, Object> source;
        while ((source = reader.read()) != null) {
            assertTrue(source.get("field1").toString().startsWith("test"));
            valueSet.add(source.get("field1").toString());
            count++;
        }

        assertEquals("Should read all documents", numDocs, count);
        assertEquals("Should have unique documents", numDocs, valueSet.size());

        reader.close();
    }

    @Test
    public void testPitWithSearchAfterPagination() throws Exception {
        String indexName = "test-pit-pagination";
        Client client = runner.client();
        SuggestSettings settings = suggester.settings();
        int numDocs = 500;

        addDocuments(indexName, client, numDocs);

        // Create PIT
        CreatePitRequest createPitRequest = new CreatePitRequest(
                TimeValue.parseTimeValue(settings.getScrollTimeout(), "keep_alive"),
                indexName);
        CreatePitResponse createPitResponse = client.createPit(createPitRequest)
                .actionGet(settings.getSearchTimeout());
        String pitId = createPitResponse.getId();

        try {
            List<Integer> allValues = new ArrayList<>();
            Object[] searchAfter = null;
            int pageSize = 50;

            while (true) {
                SearchRequestBuilder searchBuilder = client.prepareSearch()
                        .setQuery(QueryBuilders.matchAllQuery())
                        .setSize(pageSize)
                        .addSort(SortBuilders.fieldSort("field2").order(SortOrder.ASC))
                        .setPointInTime(new PointInTimeBuilder(pitId));

                if (searchAfter != null) {
                    searchBuilder.searchAfter(searchAfter);
                }

                SearchResponse response = searchBuilder.execute().actionGet(settings.getSearchTimeout());
                SearchHit[] hits = response.getHits().getHits();

                if (hits.length == 0) {
                    break;
                }

                for (SearchHit hit : hits) {
                    Map<String, Object> source = hit.getSourceAsMap();
                    allValues.add((Integer) source.get("field2"));
                }

                searchAfter = hits[hits.length - 1].getSortValues();
            }

            assertEquals("Should fetch all documents", numDocs, allValues.size());

            // Verify order is maintained
            for (int i = 0; i < allValues.size(); i++) {
                assertEquals("Documents should be in correct order", Integer.valueOf(i), allValues.get(i));
            }

        } finally {
            SuggestUtil.deletePitContext(client, pitId);
        }
    }

    @Test
    public void testDeleteByQueryWithPit() throws Exception {
        String indexName = suggester.settings().index().get(SuggestSettings.DefaultKeys.INDEX);
        Client client = runner.client();
        SuggestSettings settings = suggester.settings();

        // Add documents to suggest index
        List<SuggestItem> items = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            SuggestItem item = new SuggestItem();
            item.setText("test" + i);
            item.setFields(new String[] { "field1" });
            item.setDocFreq(i % 2 == 0 ? 1 : 0); // Half with doc_freq=1, half with 0
            item.setQueryFreq(0);
            item.setKinds(new SuggestItem.Kind[] { SuggestItem.Kind.DOCUMENT });
            items.add(item);
        }

        suggester.indexer().index(items.toArray(new SuggestItem[0]));
        runner.refresh();

        // Count documents before deletion
        long countBefore = client.prepareSearch(indexName)
                .setQuery(QueryBuilders.rangeQuery(FieldNames.DOC_FREQ).gte(1))
                .setSize(0)
                .setTrackTotalHits(true)
                .execute()
                .actionGet(settings.getSearchTimeout())
                .getHits()
                .getTotalHits()
                .value();

        assertEquals("Should have 50 documents with doc_freq >= 1", 50, countBefore);

        // Delete using PIT-based deleteByQuery
        SuggestUtil.deleteByQuery(client, settings, indexName,
                QueryBuilders.rangeQuery(FieldNames.DOC_FREQ).gte(1));

        runner.refresh();

        // Count documents after deletion
        long countAfter = client.prepareSearch(indexName)
                .setQuery(QueryBuilders.rangeQuery(FieldNames.DOC_FREQ).gte(1))
                .setSize(0)
                .setTrackTotalHits(true)
                .execute()
                .actionGet(settings.getSearchTimeout())
                .getHits()
                .getTotalHits()
                .value();

        assertEquals("Should have deleted all documents with doc_freq >= 1", 0, countAfter);

        // Verify remaining documents
        long totalAfter = client.prepareSearch(indexName)
                .setQuery(QueryBuilders.matchAllQuery())
                .setSize(0)
                .setTrackTotalHits(true)
                .execute()
                .actionGet(settings.getSearchTimeout())
                .getHits()
                .getTotalHits()
                .value();

        assertEquals("Should have 50 documents remaining", 50, totalAfter);
    }

    @Test
    public void testPitConsistency() throws Exception {
        String indexName = "test-pit-consistency";
        Client client = runner.client();
        SuggestSettings settings = suggester.settings();

        // Add initial documents
        addDocuments(indexName, client, 100);

        // Create PIT
        CreatePitRequest createPitRequest = new CreatePitRequest(
                TimeValue.parseTimeValue(settings.getScrollTimeout(), "keep_alive"),
                indexName);
        CreatePitResponse createPitResponse = client.createPit(createPitRequest)
                .actionGet(settings.getSearchTimeout());
        String pitId = createPitResponse.getId();

        try {
            // First search with PIT
            SearchRequestBuilder searchBuilder = client.prepareSearch()
                    .setQuery(QueryBuilders.matchAllQuery())
                    .setSize(100)
                    .setTrackTotalHits(true)
                    .setPointInTime(new PointInTimeBuilder(pitId));

            SearchResponse response1 = searchBuilder.execute().actionGet(settings.getSearchTimeout());
            long count1 = response1.getHits().getTotalHits().value();

            assertEquals("First search should return 100 documents", 100, count1);

            // Add more documents
            addDocuments(indexName, client, 50, 100);

            // Second search with same PIT should still return 100 documents
            SearchResponse response2 = searchBuilder.execute().actionGet(settings.getSearchTimeout());
            long count2 = response2.getHits().getTotalHits().value();

            assertEquals("Second search with PIT should still return 100 documents (frozen in time)", 100, count2);

            // Search without PIT should return 150 documents
            SearchResponse response3 = client.prepareSearch(indexName)
                    .setQuery(QueryBuilders.matchAllQuery())
                    .setSize(0)
                    .setTrackTotalHits(true)
                    .execute()
                    .actionGet(settings.getSearchTimeout());
            long count3 = response3.getHits().getTotalHits().value();

            assertEquals("Search without PIT should return all 150 documents", 150, count3);

        } finally {
            SuggestUtil.deletePitContext(client, pitId);
        }
    }

    @Test
    public void testSuggestIndexerDeleteMethods() throws Exception {
        Client client = runner.client();
        SuggestSettings settings = suggester.settings();
        SuggestIndexer indexer = suggester.indexer();

        // Add documents with different kinds
        List<SuggestItem> items = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            SuggestItem item = new SuggestItem();
            item.setText("doc" + i);
            item.setFields(new String[] { "field1" });
            item.setDocFreq(1);
            item.setQueryFreq(0);
            item.setKinds(new SuggestItem.Kind[] { SuggestItem.Kind.DOCUMENT });
            items.add(item);
        }

        for (int i = 0; i < 50; i++) {
            SuggestItem item = new SuggestItem();
            item.setText("query" + i);
            item.setFields(new String[] { "field1" });
            item.setDocFreq(0);
            item.setQueryFreq(1);
            item.setKinds(new SuggestItem.Kind[] { SuggestItem.Kind.QUERY });
            items.add(item);
        }

        indexer.index(items.toArray(new SuggestItem[0]));
        runner.refresh();

        // Test deleteDocumentWords (uses PIT internally)
        indexer.deleteDocumentWords();
        runner.refresh();

        // Verify document words are deleted/updated
        String indexName = settings.index().get(SuggestSettings.DefaultKeys.INDEX);
        long docFreqCount = client.prepareSearch(indexName)
                .setQuery(QueryBuilders.rangeQuery(FieldNames.DOC_FREQ).gte(1))
                .setSize(0)
                .setTrackTotalHits(true)
                .execute()
                .actionGet(settings.getSearchTimeout())
                .getHits()
                .getTotalHits()
                .value();

        assertEquals("Document frequency should be cleared", 0, docFreqCount);

        // Test deleteQueryWords (uses PIT internally)
        indexer.deleteQueryWords();
        runner.refresh();

        // Verify query words are deleted/updated
        long queryFreqCount = client.prepareSearch(indexName)
                .setQuery(QueryBuilders.rangeQuery(FieldNames.QUERY_FREQ).gte(1))
                .setSize(0)
                .setTrackTotalHits(true)
                .execute()
                .actionGet(settings.getSearchTimeout())
                .getHits()
                .getTotalHits()
                .value();

        assertEquals("Query frequency should be cleared", 0, queryFreqCount);
    }

    private void addDocuments(String indexName, Client client, int num) {
        addDocuments(indexName, client, num, 0);
    }

    private void addDocuments(String indexName, Client client, int num, int startId) {
        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
        for (int i = 0; i < num; i++) {
            int id = startId + i;
            final Map<String, Object> source = new HashMap<>();
            source.put("field1", "test" + id);
            source.put("field2", id);
            IndexRequestBuilder indexRequestBuilder = new IndexRequestBuilder(client, IndexAction.INSTANCE);
            indexRequestBuilder.setIndex(indexName).setId(String.valueOf(id)).setCreate(true).setSource(source);
            bulkRequestBuilder.add(indexRequestBuilder);
        }
        bulkRequestBuilder.execute().actionGet();
        runner.refresh();
    }
}
