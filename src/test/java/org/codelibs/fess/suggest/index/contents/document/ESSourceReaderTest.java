/*
 * Copyright 2012-2022 CodeLibs Project and the Others.
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
package org.codelibs.fess.suggest.index.contents.document;

import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensearch.action.bulk.BulkRequestBuilder;
import org.opensearch.action.index.IndexAction;
import org.opensearch.action.index.IndexRequestBuilder;
import org.opensearch.client.Client;
import org.opensearch.index.IndexNotFoundException;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;

public class ESSourceReaderTest {
    static Suggester suggester;

    static OpenSearchRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
            // settingsBuilder.putList("discovery.seed_hosts", "127.0.0.1:9301");
            // settingsBuilder.putList("cluster.initial_master_nodes", "127.0.0.1:9301");
        }).build(newConfigs().clusterName("ESSourceReaderTest").numOfNode(1)
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
        suggester = Suggester.builder().build(runner.client(), "SuggesterTest");
        suggester.createIndexIfNothing();
    }

    @Test
    public void test_Read() throws Exception {
        String indexName = "test-index";
        Client client = runner.client();
        SuggestSettings settings = suggester.settings();
        int num = 10000;

        addDocument(indexName, client, num);

        ESSourceReader reader = new ESSourceReader(client, settings, indexName);
        reader.setScrollSize(1000);
        int count = 0;
        Set<String> valueSet = Collections.synchronizedSet(new HashSet<>());
        Map<String, Object> source;
        while ((source = reader.read()) != null) {
            assertTrue(source.get("field1").toString().startsWith("test"));
            valueSet.add(source.get("field1").toString());
            count++;
        }
        assertEquals(num, count);
        assertEquals(num, valueSet.size());
    }

    @Test
    public void test_ReadWithLimit() throws Exception {
        String indexName = "test-index";
        Client client = runner.client();
        SuggestSettings settings = suggester.settings();
        int num = 10000;

        addDocument(indexName, client, num);

        ESSourceReader reader = new ESSourceReader(client, settings, indexName);
        reader.setScrollSize(1);
        reader.setLimitDocNumPercentage("1%");
        int count = 0;
        Set<String> valueSet = Collections.synchronizedSet(new HashSet<>());
        Map<String, Object> source;
        while ((source = reader.read()) != null) {
            assertTrue(source.get("field1").toString().startsWith("test"));
            valueSet.add(source.get("field1").toString());
            count++;
        }
        assertTrue(String.valueOf(count), count < 200);
    }

    @Test
    public void test_ReadMultiThread() throws Exception {
        int threadNum = new Random().nextInt(20) + 1;
        System.out.println("Thread num:" + threadNum);

        String indexName = "test-index";
        Client client = runner.client();
        SuggestSettings settings = suggester.settings();
        int num = 9999;

        addDocument(indexName, client, num);

        AtomicInteger count = new AtomicInteger(0);
        Set<String> valueSet = Collections.synchronizedSet(new HashSet<>());
        Thread[] threads = new Thread[threadNum];
        ESSourceReader reader = new ESSourceReader(client, settings, indexName);
        reader.setScrollSize(100);
        for (int i = 0; i < threadNum; i++) {
            threads[i] = new Thread(() -> {
                Map<String, Object> source;
                while ((source = reader.read()) != null) {
                    assertTrue(source.get("field1").toString().startsWith("test"));
                    valueSet.add(source.get("field1").toString());
                    count.getAndIncrement();
                }
            });
        }

        for (Thread th : threads) {
            th.start();
        }
        for (Thread th : threads) {
            th.join();
        }
        assertEquals(num, count.get());
        assertEquals(num, valueSet.size());

        Set<String> valueSet2 = Collections.synchronizedSet(new HashSet<>());
        threads = new Thread[threadNum];
        ESSourceReader reader2 = new ESSourceReader(client, settings, indexName);
        reader2.setScrollSize(100);
        for (int i = 0; i < threadNum; i++) {
            threads[i] = new Thread(() -> {
                Map<String, Object> source;
                while ((source = reader2.read()) != null) {
                    assertTrue(source.get("field1").toString().startsWith("test"));
                    valueSet2.add(source.get("field1").toString());
                    count.getAndIncrement();
                }
            });
        }

        for (Thread th : threads) {
            th.start();
        }
        for (Thread th : threads) {
            th.join();
        }
        assertEquals(num * 2, count.get());
        assertEquals(num, valueSet2.size());
    }

    @Test
    public void test_getLimitDocNum() throws Exception {
        assertEquals(10, ESSourceReader.getLimitDocNum(100, 10, -1));
        assertEquals(25, ESSourceReader.getLimitDocNum(50, 50, -1));
        assertEquals(10, ESSourceReader.getLimitDocNum(50, 50, 10));
    }

    @Test
    public void test_sort() throws Exception {
        String indexName = "test-index";
        Client client = runner.client();
        SuggestSettings settings = suggester.settings();
        int num = 10000;

        addDocument(indexName, client, num);

        ESSourceReader reader = new ESSourceReader(client, settings, indexName);
        reader.setScrollSize(1000);
        reader.addSort(SortBuilders.fieldSort("field2"));
        int count = 0;
        int prev = -1;
        Map<String, Object> source;
        while ((source = reader.read()) != null) {
            int current = Integer.parseInt(source.get("field2").toString());
            assertTrue(prev < current);
            prev = current;
            count++;
        }
        assertEquals(num, count);

        reader = new ESSourceReader(client, settings, indexName);
        reader.setScrollSize(1000);
        reader.addSort(SortBuilders.fieldSort("field2").order(SortOrder.DESC));
        count = 0;
        prev = Integer.MAX_VALUE;
        while ((source = reader.read()) != null) {
            int current = Integer.parseInt(source.get("field2").toString());
            assertTrue(prev > current);
            prev = current;
            count++;
        }
        assertEquals(num, count);
    }

    private void addDocument(String indexName, Client client, int num) {
        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
        for (int i = 0; i < num; i++) {
            final Map<String, Object> source = new HashMap<>();
            source.put("field1", "test" + i);
            source.put("field2", i);
            IndexRequestBuilder indexRequestBuilder = new IndexRequestBuilder(client, IndexAction.INSTANCE);
            indexRequestBuilder.setIndex(indexName).setId(String.valueOf(i)).setCreate(true).setSource(source);
            bulkRequestBuilder.add(indexRequestBuilder);
        }
        bulkRequestBuilder.execute().actionGet();
        runner.refresh();
    }
}
