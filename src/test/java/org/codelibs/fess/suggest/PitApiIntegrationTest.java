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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
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
import org.opensearch.action.search.CreatePitAction;
import org.opensearch.action.search.CreatePitRequest;
import org.opensearch.action.search.CreatePitResponse;
import org.opensearch.action.search.DeletePitAction;
import org.opensearch.action.search.DeletePitRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexResponse;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.PointInTimeBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.FieldSortBuilder;
import org.opensearch.search.sort.SortOrder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration test for PIT API using TestContainers with OpenSearch 3.3.2
 */
public class PitApiIntegrationTest {

    @ClassRule
    public static GenericContainer<?> opensearchContainer = new GenericContainer<>(
            DockerImageName.parse("opensearchproject/opensearch:3.3.2"))
                    .withEnv("discovery.type", "single-node")
                    .withEnv("OPENSEARCH_INITIAL_ADMIN_PASSWORD", "Admin123!")
                    .withEnv("DISABLE_SECURITY_PLUGIN", "true")
                    .withExposedPorts(9200)
                    .waitingFor(Wait.forHttp("/").forStatusCode(200));

    private RestHighLevelClient client;
    private static final String TEST_INDEX = "test_pit_index";

    @Before
    public void setUp() throws IOException {
        String host = opensearchContainer.getHost();
        int port = opensearchContainer.getMappedPort(9200);

        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, "http"));

        client = new RestHighLevelClient(builder);

        // Create test index
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(TEST_INDEX);
        createIndexRequest.settings(Settings.builder()
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0));

        CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
        assertTrue("Index creation failed", createIndexResponse.isAcknowledged());

        // Index test documents
        BulkRequest bulkRequest = new BulkRequest();
        for (int i = 0; i < 100; i++) {
            Map<String, Object> doc = new HashMap<>();
            doc.put("id", i);
            doc.put("name", "document_" + i);
            doc.put("value", i * 10);
            bulkRequest.add(new IndexRequest(TEST_INDEX).id(String.valueOf(i)).source(doc, XContentType.JSON));
        }
        client.bulk(bulkRequest, RequestOptions.DEFAULT);

        // Refresh index to make documents searchable
        client.indices().refresh(new org.opensearch.client.indices.RefreshRequest(TEST_INDEX), RequestOptions.DEFAULT);
    }

    @After
    public void tearDown() throws IOException {
        if (client != null) {
            try {
                client.indices().delete(new DeleteIndexRequest(TEST_INDEX), RequestOptions.DEFAULT);
            } catch (Exception e) {
                // Ignore
            }
            client.close();
        }
    }

    @Test
    public void testPitCreationAndDeletion() throws IOException {
        // Create PIT
        CreatePitRequest createPitRequest = new CreatePitRequest(TimeValue.timeValueMinutes(1), TEST_INDEX);
        CreatePitResponse createPitResponse = client.execute(CreatePitAction.INSTANCE, createPitRequest, RequestOptions.DEFAULT);

        assertNotNull("PIT ID should not be null", createPitResponse.getId());
        String pitId = createPitResponse.getId();

        // Delete PIT
        DeletePitRequest deletePitRequest = new DeletePitRequest(pitId);
        org.opensearch.action.search.DeletePitResponse deletePitResponse = client.execute(DeletePitAction.INSTANCE, deletePitRequest, RequestOptions.DEFAULT);

        assertTrue("PIT deletion should succeed", deletePitResponse.isSucceeded());
    }

    @Test
    public void testPitWithSearchAfterPagination() throws IOException {
        // Create PIT
        CreatePitRequest createPitRequest = new CreatePitRequest(TimeValue.timeValueMinutes(1), TEST_INDEX);
        CreatePitResponse createPitResponse = client.execute(CreatePitAction.INSTANCE, createPitRequest, RequestOptions.DEFAULT);
        String pitId = createPitResponse.getId();

        try {
            int pageSize = 10;
            int totalDocuments = 0;
            Object[] searchAfter = null;

            // Paginate through all documents using PIT and search_after
            while (true) {
                PointInTimeBuilder pointInTimeBuilder = new PointInTimeBuilder(pitId)
                        .setKeepAlive(TimeValue.timeValueMinutes(1));

                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                        .pointInTimeBuilder(pointInTimeBuilder)
                        .query(QueryBuilders.matchAllQuery())
                        .size(pageSize)
                        .sort(new FieldSortBuilder("_shard_doc").order(SortOrder.ASC));

                if (searchAfter != null) {
                    searchSourceBuilder.searchAfter(searchAfter);
                }

                org.opensearch.action.search.SearchRequest searchRequest = new org.opensearch.action.search.SearchRequest();
                searchRequest.source(searchSourceBuilder);

                SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
                SearchHit[] hits = response.getHits().getHits();

                if (hits.length == 0) {
                    break;
                }

                totalDocuments += hits.length;
                searchAfter = hits[hits.length - 1].getSortValues();
            }

            assertEquals("Should retrieve all 100 documents", 100, totalDocuments);

        } finally {
            // Clean up PIT
            DeletePitRequest deletePitRequest = new DeletePitRequest(pitId);
            client.execute(DeletePitAction.INSTANCE, deletePitRequest, RequestOptions.DEFAULT);
        }
    }

    @Test
    public void testPitWithQueryFilter() throws IOException {
        // Create PIT
        CreatePitRequest createPitRequest = new CreatePitRequest(TimeValue.timeValueMinutes(1), TEST_INDEX);
        CreatePitResponse createPitResponse = client.execute(CreatePitAction.INSTANCE, createPitRequest, RequestOptions.DEFAULT);
        String pitId = createPitResponse.getId();

        try {
            int count = 0;
            Object[] searchAfter = null;

            // Search for documents with value >= 500 (should be 50 documents)
            while (true) {
                PointInTimeBuilder pointInTimeBuilder = new PointInTimeBuilder(pitId)
                        .setKeepAlive(TimeValue.timeValueMinutes(1));

                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                        .pointInTimeBuilder(pointInTimeBuilder)
                        .query(QueryBuilders.rangeQuery("value").gte(500))
                        .size(10)
                        .sort(new FieldSortBuilder("_shard_doc").order(SortOrder.ASC));

                if (searchAfter != null) {
                    searchSourceBuilder.searchAfter(searchAfter);
                }

                org.opensearch.action.search.SearchRequest searchRequest = new org.opensearch.action.search.SearchRequest();
                searchRequest.source(searchSourceBuilder);

                SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
                SearchHit[] hits = response.getHits().getHits();

                if (hits.length == 0) {
                    break;
                }

                count += hits.length;
                searchAfter = hits[hits.length - 1].getSortValues();
            }

            assertEquals("Should retrieve 50 documents with value >= 500", 50, count);

        } finally {
            // Clean up PIT
            DeletePitRequest deletePitRequest = new DeletePitRequest(pitId);
            client.execute(DeletePitAction.INSTANCE, deletePitRequest, RequestOptions.DEFAULT);
        }
    }

    @Test
    public void testPitConsistency() throws IOException {
        // Create PIT
        CreatePitRequest createPitRequest = new CreatePitRequest(TimeValue.timeValueMinutes(1), TEST_INDEX);
        CreatePitResponse createPitResponse = client.execute(CreatePitAction.INSTANCE, createPitRequest, RequestOptions.DEFAULT);
        String pitId = createPitResponse.getId();

        try {
            PointInTimeBuilder pointInTimeBuilder = new PointInTimeBuilder(pitId)
                    .setKeepAlive(TimeValue.timeValueMinutes(1));

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                    .pointInTimeBuilder(pointInTimeBuilder)
                    .query(QueryBuilders.matchAllQuery())
                    .size(10)
                    .sort(new FieldSortBuilder("_shard_doc").order(SortOrder.ASC));

            org.opensearch.action.search.SearchRequest searchRequest = new org.opensearch.action.search.SearchRequest();
            searchRequest.source(searchSourceBuilder);

            // First search
            SearchResponse response1 = client.search(searchRequest, RequestOptions.DEFAULT);
            SearchHit[] hits1 = response1.getHits().getHits();

            // Index new documents (should not affect PIT results)
            BulkRequest bulkRequest = new BulkRequest();
            for (int i = 100; i < 110; i++) {
                Map<String, Object> doc = new HashMap<>();
                doc.put("id", i);
                doc.put("name", "new_document_" + i);
                doc.put("value", i * 10);
                bulkRequest.add(new IndexRequest(TEST_INDEX).id(String.valueOf(i)).source(doc, XContentType.JSON));
            }
            client.bulk(bulkRequest, RequestOptions.DEFAULT);
            client.indices().refresh(new org.opensearch.client.indices.RefreshRequest(TEST_INDEX), RequestOptions.DEFAULT);

            // Second search with same PIT (should return same results)
            SearchResponse response2 = client.search(searchRequest, RequestOptions.DEFAULT);
            SearchHit[] hits2 = response2.getHits().getHits();

            assertEquals("PIT should return consistent results", hits1.length, hits2.length);

            // Verify first document ID is the same
            assertEquals("First document should be the same",
                    hits1[0].getId(), hits2[0].getId());

        } finally {
            // Clean up PIT
            DeletePitRequest deletePitRequest = new DeletePitRequest(pitId);
            client.execute(DeletePitAction.INSTANCE, deletePitRequest, RequestOptions.DEFAULT);
        }
    }
}
