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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpHost;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.SuggestIndexer;
import org.codelibs.fess.suggest.index.contents.document.ESSourceReader;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.CreatePitRequest;
import org.opensearch.action.search.CreatePitResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.PointInTimeBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration tests for Point in Time (PIT) API implementation.
 * This test uses TestContainers to start an OpenSearch 3.3.2 instance.
 */
public class PitApiIntegrationTest {

    @ClassRule
    public static GenericContainer<?> opensearchContainer = new GenericContainer<>(
            DockerImageName.parse("opensearchproject/opensearch:3.0.0"))
                    .withEnv("discovery.type", "single-node")
                    .withEnv("OPENSEARCH_INITIAL_ADMIN_PASSWORD", "Admin123!")
                    .withEnv("DISABLE_SECURITY_PLUGIN", "true")
                    .withExposedPorts(9200)
                    .waitingFor(Wait.forHttp("/").forPort(9200).forStatusCode(200));

    private RestHighLevelClient client;

    @Before
    public void setUp() throws Exception {
        String host = opensearchContainer.getHost();
        Integer port = opensearchContainer.getMappedPort(9200);

        client = new RestHighLevelClient(
                RestClient.builder(new HttpHost(host, port, "http")));
    }

    @After
    public void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @Test
    public void testPitCreationAndDeletion() throws Exception {
        String indexName = "test-pit-index";

        // Create index
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);
        client.indices().create(createIndexRequest, RequestOptions.DEFAULT);

        // Index some documents
        BulkRequest bulkRequest = new BulkRequest();
        for (int i = 0; i < 100; i++) {
            Map<String, Object> source = new HashMap<>();
            source.put("field1", "value" + i);
            source.put("field2", i);
            bulkRequest.add(new IndexRequest(indexName).id(String.valueOf(i)).source(source));
        }
        client.bulk(bulkRequest, RequestOptions.DEFAULT);
        client.indices().refresh(new org.opensearch.action.admin.indices.refresh.RefreshRequest(indexName),
                RequestOptions.DEFAULT);

        // Create PIT
        CreatePitRequest createPitRequest = new CreatePitRequest(TimeValue.timeValueMinutes(1), indexName);
        CreatePitResponse createPitResponse = client.createPit(createPitRequest, RequestOptions.DEFAULT);
        String pitId = createPitResponse.getId();

        assertNotNull("PIT ID should not be null", pitId);

        // Use PIT to search
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.matchAllQuery())
                .size(10)
                .pointInTimeBuilder(new PointInTimeBuilder(pitId));

        SearchRequest searchRequest = new SearchRequest().source(searchSourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        assertEquals("Should return 10 documents", 10, searchResponse.getHits().getHits().length);

        // Delete PIT
        org.opensearch.action.search.DeletePitRequest deletePitRequest = new org.opensearch.action.search.DeletePitRequest(pitId);
        client.deletePit(deletePitRequest, RequestOptions.DEFAULT);

        // Clean up
        client.indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
    }

    @Test
    public void testPitWithSearchAfter() throws Exception {
        String indexName = "test-pit-search-after";

        // Create index
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);
        client.indices().create(createIndexRequest, RequestOptions.DEFAULT);

        // Index documents
        BulkRequest bulkRequest = new BulkRequest();
        for (int i = 0; i < 1000; i++) {
            Map<String, Object> source = new HashMap<>();
            source.put("field1", "value" + i);
            source.put("field2", i);
            bulkRequest.add(new IndexRequest(indexName).id(String.valueOf(i)).source(source));
        }
        client.bulk(bulkRequest, RequestOptions.DEFAULT);
        client.indices().refresh(new org.opensearch.action.admin.indices.refresh.RefreshRequest(indexName),
                RequestOptions.DEFAULT);

        // Create PIT
        CreatePitRequest createPitRequest = new CreatePitRequest(TimeValue.timeValueMinutes(1), indexName);
        CreatePitResponse createPitResponse = client.createPit(createPitRequest, RequestOptions.DEFAULT);
        String pitId = createPitResponse.getId();

        try {
            Set<String> allIds = new HashSet<>();
            Object[] searchAfter = null;
            int batchSize = 100;
            int totalFetched = 0;

            while (true) {
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                        .query(QueryBuilders.matchAllQuery())
                        .size(batchSize)
                        .sort(SortBuilders.fieldSort("field2").order(SortOrder.ASC))
                        .pointInTimeBuilder(new PointInTimeBuilder(pitId));

                if (searchAfter != null) {
                    searchSourceBuilder.searchAfter(searchAfter);
                }

                SearchRequest searchRequest = new SearchRequest().source(searchSourceBuilder);
                SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

                SearchHit[] hits = searchResponse.getHits().getHits();
                if (hits.length == 0) {
                    break;
                }

                for (SearchHit hit : hits) {
                    allIds.add(hit.getId());
                }

                totalFetched += hits.length;
                searchAfter = hits[hits.length - 1].getSortValues();
            }

            assertEquals("Should fetch all 1000 documents", 1000, totalFetched);
            assertEquals("Should have 1000 unique document IDs", 1000, allIds.size());

        } finally {
            // Delete PIT
            org.opensearch.action.search.DeletePitRequest deletePitRequest = new org.opensearch.action.search.DeletePitRequest(pitId);
            client.deletePit(deletePitRequest, RequestOptions.DEFAULT);

            // Clean up
            client.indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
        }
    }

    @Test
    public void testPitConsistency() throws Exception {
        String indexName = "test-pit-consistency";

        // Create index
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);
        client.indices().create(createIndexRequest, RequestOptions.DEFAULT);

        // Index initial documents
        BulkRequest bulkRequest = new BulkRequest();
        for (int i = 0; i < 100; i++) {
            Map<String, Object> source = new HashMap<>();
            source.put("field1", "value" + i);
            source.put("field2", i);
            bulkRequest.add(new IndexRequest(indexName).id(String.valueOf(i)).source(source));
        }
        client.bulk(bulkRequest, RequestOptions.DEFAULT);
        client.indices().refresh(new org.opensearch.action.admin.indices.refresh.RefreshRequest(indexName),
                RequestOptions.DEFAULT);

        // Create PIT
        CreatePitRequest createPitRequest = new CreatePitRequest(TimeValue.timeValueMinutes(1), indexName);
        CreatePitResponse createPitResponse = client.createPit(createPitRequest, RequestOptions.DEFAULT);
        String pitId = createPitResponse.getId();

        try {
            // First search with PIT
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                    .query(QueryBuilders.matchAllQuery())
                    .size(100)
                    .pointInTimeBuilder(new PointInTimeBuilder(pitId));

            SearchRequest searchRequest = new SearchRequest().source(searchSourceBuilder);
            SearchResponse searchResponse1 = client.search(searchRequest, RequestOptions.DEFAULT);

            long firstCount = searchResponse1.getHits().getTotalHits().value();
            assertEquals("First search should return 100 documents", 100, firstCount);

            // Add more documents to index
            BulkRequest bulkRequest2 = new BulkRequest();
            for (int i = 100; i < 200; i++) {
                Map<String, Object> source = new HashMap<>();
                source.put("field1", "value" + i);
                source.put("field2", i);
                bulkRequest2.add(new IndexRequest(indexName).id(String.valueOf(i)).source(source));
            }
            client.bulk(bulkRequest2, RequestOptions.DEFAULT);
            client.indices().refresh(new org.opensearch.action.admin.indices.refresh.RefreshRequest(indexName),
                    RequestOptions.DEFAULT);

            // Second search with same PIT should still return 100 documents
            SearchResponse searchResponse2 = client.search(searchRequest, RequestOptions.DEFAULT);
            long secondCount = searchResponse2.getHits().getTotalHits().value();
            assertEquals("Second search with PIT should still return 100 documents (frozen in time)", 100, secondCount);

            // Search without PIT should return 200 documents
            SearchSourceBuilder noPitSearchSourceBuilder = new SearchSourceBuilder()
                    .query(QueryBuilders.matchAllQuery())
                    .size(200)
                    .trackTotalHits(true);

            SearchRequest noPitSearchRequest = new SearchRequest(indexName).source(noPitSearchSourceBuilder);
            SearchResponse searchResponse3 = client.search(noPitSearchRequest, RequestOptions.DEFAULT);
            long thirdCount = searchResponse3.getHits().getTotalHits().value();
            assertEquals("Search without PIT should return all 200 documents", 200, thirdCount);

        } finally {
            // Delete PIT
            org.opensearch.action.search.DeletePitRequest deletePitRequest = new org.opensearch.action.search.DeletePitRequest(pitId);
            client.deletePit(deletePitRequest, RequestOptions.DEFAULT);

            // Clean up
            client.indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
        }
    }

    @Test
    public void testPitPagination() throws Exception {
        String indexName = "test-pit-pagination";

        // Create index
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);
        client.indices().create(createIndexRequest, RequestOptions.DEFAULT);

        // Index documents with specific order
        BulkRequest bulkRequest = new BulkRequest();
        for (int i = 0; i < 500; i++) {
            Map<String, Object> source = new HashMap<>();
            source.put("field1", "value" + i);
            source.put("field2", i);
            source.put("timestamp", System.currentTimeMillis() + i);
            bulkRequest.add(new IndexRequest(indexName).id(String.valueOf(i)).source(source));
        }
        client.bulk(bulkRequest, RequestOptions.DEFAULT);
        client.indices().refresh(new org.opensearch.action.admin.indices.refresh.RefreshRequest(indexName),
                RequestOptions.DEFAULT);

        // Create PIT
        CreatePitRequest createPitRequest = new CreatePitRequest(TimeValue.timeValueMinutes(1), indexName);
        CreatePitResponse createPitResponse = client.createPit(createPitRequest, RequestOptions.DEFAULT);
        String pitId = createPitResponse.getId();

        try {
            List<Integer> allValues = new ArrayList<>();
            Object[] searchAfter = null;
            int pageSize = 50;

            // Paginate through all results
            while (true) {
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                        .query(QueryBuilders.matchAllQuery())
                        .size(pageSize)
                        .sort(SortBuilders.fieldSort("field2").order(SortOrder.ASC))
                        .pointInTimeBuilder(new PointInTimeBuilder(pitId));

                if (searchAfter != null) {
                    searchSourceBuilder.searchAfter(searchAfter);
                }

                SearchRequest searchRequest = new SearchRequest().source(searchSourceBuilder);
                SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

                SearchHit[] hits = searchResponse.getHits().getHits();
                if (hits.length == 0) {
                    break;
                }

                for (SearchHit hit : hits) {
                    Map<String, Object> source = hit.getSourceAsMap();
                    allValues.add((Integer) source.get("field2"));
                }

                searchAfter = hits[hits.length - 1].getSortValues();
            }

            // Verify all documents were fetched
            assertEquals("Should fetch all 500 documents", 500, allValues.size());

            // Verify order is maintained
            for (int i = 0; i < allValues.size(); i++) {
                assertEquals("Documents should be in correct order", Integer.valueOf(i), allValues.get(i));
            }

        } finally {
            // Delete PIT
            org.opensearch.action.search.DeletePitRequest deletePitRequest = new org.opensearch.action.search.DeletePitRequest(pitId);
            client.deletePit(deletePitRequest, RequestOptions.DEFAULT);

            // Clean up
            client.indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
        }
    }

    @Test
    public void testMultipleConcurrentPits() throws Exception {
        String indexName = "test-concurrent-pits";

        // Create index
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);
        client.indices().create(createIndexRequest, RequestOptions.DEFAULT);

        // Index documents
        BulkRequest bulkRequest = new BulkRequest();
        for (int i = 0; i < 100; i++) {
            Map<String, Object> source = new HashMap<>();
            source.put("field1", "value" + i);
            source.put("field2", i);
            bulkRequest.add(new IndexRequest(indexName).id(String.valueOf(i)).source(source));
        }
        client.bulk(bulkRequest, RequestOptions.DEFAULT);
        client.indices().refresh(new org.opensearch.action.admin.indices.refresh.RefreshRequest(indexName),
                RequestOptions.DEFAULT);

        // Create multiple PITs
        CreatePitRequest createPitRequest1 = new CreatePitRequest(TimeValue.timeValueMinutes(1), indexName);
        CreatePitResponse createPitResponse1 = client.createPit(createPitRequest1, RequestOptions.DEFAULT);
        String pitId1 = createPitResponse1.getId();

        CreatePitRequest createPitRequest2 = new CreatePitRequest(TimeValue.timeValueMinutes(1), indexName);
        CreatePitResponse createPitResponse2 = client.createPit(createPitRequest2, RequestOptions.DEFAULT);
        String pitId2 = createPitResponse2.getId();

        try {
            // Search with first PIT
            SearchSourceBuilder searchSourceBuilder1 = new SearchSourceBuilder()
                    .query(QueryBuilders.matchAllQuery())
                    .size(100)
                    .pointInTimeBuilder(new PointInTimeBuilder(pitId1));

            SearchRequest searchRequest1 = new SearchRequest().source(searchSourceBuilder1);
            SearchResponse searchResponse1 = client.search(searchRequest1, RequestOptions.DEFAULT);

            // Search with second PIT
            SearchSourceBuilder searchSourceBuilder2 = new SearchSourceBuilder()
                    .query(QueryBuilders.matchAllQuery())
                    .size(100)
                    .pointInTimeBuilder(new PointInTimeBuilder(pitId2));

            SearchRequest searchRequest2 = new SearchRequest().source(searchSourceBuilder2);
            SearchResponse searchResponse2 = client.search(searchRequest2, RequestOptions.DEFAULT);

            // Both should return same results
            assertEquals("Both PITs should return same count",
                    searchResponse1.getHits().getTotalHits().value(),
                    searchResponse2.getHits().getTotalHits().value());

        } finally {
            // Delete both PITs
            org.opensearch.action.search.DeletePitRequest deletePitRequest1 = new org.opensearch.action.search.DeletePitRequest(pitId1);
            client.deletePit(deletePitRequest1, RequestOptions.DEFAULT);

            org.opensearch.action.search.DeletePitRequest deletePitRequest2 = new org.opensearch.action.search.DeletePitRequest(pitId2);
            client.deletePit(deletePitRequest2, RequestOptions.DEFAULT);

            // Clean up
            client.indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
        }
    }
}
