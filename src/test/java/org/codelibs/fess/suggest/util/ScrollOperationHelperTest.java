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
package org.codelibs.fess.suggest.util;

import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.transport.client.Client;

/**
 * Integration tests for ScrollOperationHelper.
 */
public class ScrollOperationHelperTest {

    private static OpenSearchRunner runner;
    private static Client client;
    private static final String INDEX_NAME = "scroll-test-index";
    private Suggester suggester;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        })
                .build(newConfigs().clusterName("ScrollOperationHelperTest")
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
        suggester = Suggester.builder().build(client, "ScrollTest");
        suggester.createIndexIfNothing();
    }

    @Test
    public void testScrollSearch_emptyResults() {
        // Create index without any documents
        createTestIndex();

        List<String> results = ScrollOperationHelper.scrollSearch(client, suggester.settings(), INDEX_NAME, QueryBuilders.matchAllQuery(),
                10, (hit, accumulator) -> accumulator.add(hit.getId()));

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testScrollSearch_singlePage() {
        createTestIndex();
        indexDocuments(5);
        runner.refresh();

        List<String> results =
                ScrollOperationHelper.scrollSearch(client, suggester.settings(), INDEX_NAME, QueryBuilders.matchAllQuery(), 10, // page size larger than document count
                        (hit, accumulator) -> accumulator.add(hit.getId()));

        assertNotNull(results);
        assertEquals(5, results.size());
    }

    @Test
    public void testScrollSearch_multiplePages() {
        createTestIndex();
        indexDocuments(25);
        runner.refresh();

        List<String> results =
                ScrollOperationHelper.scrollSearch(client, suggester.settings(), INDEX_NAME, QueryBuilders.matchAllQuery(), 10, // page size smaller than document count
                        (hit, accumulator) -> accumulator.add(hit.getId()));

        assertNotNull(results);
        assertEquals(25, results.size());
    }

    @Test
    public void testScrollSearch_withQuery() {
        createTestIndex();

        // Index documents with different numeric values for easy filtering
        for (int i = 0; i < 10; i++) {
            Map<String, Object> source = new HashMap<>();
            source.put("category", i < 5 ? 1 : 2); // Use numeric field for reliable filtering
            source.put("value", i);
            client.prepareIndex(INDEX_NAME).setSource(source, XContentType.JSON).execute().actionGet();
        }
        runner.refresh();

        // Use range query for reliable filtering on numeric field
        List<String> results = ScrollOperationHelper.scrollSearch(client, suggester.settings(), INDEX_NAME,
                QueryBuilders.rangeQuery("value").lt(5), 10, (hit, accumulator) -> accumulator.add(hit.getId()));

        assertNotNull(results);
        assertEquals(5, results.size());
    }

    @Test
    public void testScrollSearch_extractsSourceData() {
        createTestIndex();

        // Index documents with specific data
        for (int i = 0; i < 3; i++) {
            Map<String, Object> source = new HashMap<>();
            source.put("name", "item" + i);
            source.put("value", i * 10);
            client.prepareIndex(INDEX_NAME).setSource(source, XContentType.JSON).execute().actionGet();
        }
        runner.refresh();

        List<Map<String, Object>> results = ScrollOperationHelper.scrollSearch(client, suggester.settings(), INDEX_NAME,
                QueryBuilders.matchAllQuery(), 10, (hit, accumulator) -> accumulator.add(hit.getSourceAsMap()));

        assertNotNull(results);
        assertEquals(3, results.size());

        // Verify we can access the source data
        for (Map<String, Object> result : results) {
            assertTrue(result.containsKey("name"));
            assertTrue(result.containsKey("value"));
        }
    }

    @Test
    public void testScrollSearch_customAccumulator() {
        createTestIndex();
        indexDocuments(5);
        runner.refresh();

        // Use custom accumulator to sum values
        List<Integer> values = new ArrayList<>();
        List<Integer> results = ScrollOperationHelper.scrollSearch(client, suggester.settings(), INDEX_NAME, QueryBuilders.matchAllQuery(),
                10, (hit, accumulator) -> {
                    Map<String, Object> source = hit.getSourceAsMap();
                    if (source.containsKey("value")) {
                        accumulator.add(((Number) source.get("value")).intValue());
                    }
                });

        assertNotNull(results);
        assertEquals(5, results.size());
    }

    private void createTestIndex() {
        if (!runner.indexExists(INDEX_NAME)) {
            runner.admin().indices().prepareCreate(INDEX_NAME).execute().actionGet();
        }
    }

    private void indexDocuments(int count) {
        for (int i = 0; i < count; i++) {
            Map<String, Object> source = new HashMap<>();
            source.put("name", "document" + i);
            source.put("value", i);
            IndexResponse response = client.prepareIndex(INDEX_NAME).setSource(source, XContentType.JSON).execute().actionGet();
        }
    }
}
