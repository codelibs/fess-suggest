package org.codelibs.fess.suggest.index.contents.document;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.junit.Assert.*;

public class ESSourceReaderTest {
    static Suggester suggester;

    static ElasticsearchClusterRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new ElasticsearchClusterRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("index.number_of_replicas", 0);
            settingsBuilder.put("script.disable_dynamic", false);
            settingsBuilder.put("script.groovy.sandbox.enabled", true);
        }).build(newConfigs().ramIndexStore().numOfNode(1));
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
        } catch (IndexMissingException ignore) {

        }
        runner.refresh();
        suggester = Suggester.builder().build(runner.client(), "SuggesterTest");
        suggester.createIndexIfNothing();
    }

    @Test
    public void test_Read() throws Exception {
        String indexName = "test-index";
        String typeName = "test-type";
        Client client = runner.client();
        SuggestSettings settings = suggester.settings();
        int num = 10000;

        addDocument(indexName, typeName, client, num);

        ESSourceReader reader = new ESSourceReader(client, settings, indexName, typeName);
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
    public void test_ReadMultiThread() throws Exception {
        int threadNum = new Random().nextInt(20) + 1;
        System.out.println("Thread num:" + threadNum);

        String indexName = "test-index";
        String typeName = "test-type";
        Client client = runner.client();
        SuggestSettings settings = suggester.settings();
        int num = 20000;

        addDocument(indexName, typeName, client, num);

        AtomicInteger count = new AtomicInteger(0);
        Set<String> valueSet = Collections.synchronizedSet(new HashSet<>());
        Thread[] threads = new Thread[threadNum];
        ESSourceReader reader = new ESSourceReader(client, settings, indexName, typeName);
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
        ESSourceReader reader2 = new ESSourceReader(client, settings, indexName, typeName);
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
    public void test_ReadMultiThreadOtherInstance() throws Exception {
        int threadNum = new Random().nextInt(20) + 1;
        System.out.println("Thread num:" + threadNum);

        String indexName = "test-index";
        String typeName = "test-type";
        Client client = runner.client();
        SuggestSettings settings = suggester.settings();
        int num = 20000;

        addDocument(indexName, typeName, client, num);

        AtomicInteger count = new AtomicInteger(0);
        Set<String> valueSet = Collections.synchronizedSet(new HashSet<>());
        Thread[] threads = new Thread[threadNum];
        for (int i = 0; i < threadNum; i++) {
            threads[i] = new Thread(() -> {
                ESSourceReader reader = new ESSourceReader(client, settings, indexName, typeName);
                reader.setScrollSize(100);
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
        for (int i = 0; i < threadNum; i++) {
            threads[i] = new Thread(() -> {
                ESSourceReader reader2 = new ESSourceReader(client, settings, indexName, typeName);
                reader2.setScrollSize(100);
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

    private void addDocument(String indexName, String typeName, Client client, int num) {
        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
        for (int i = 0; i < num; i++) {
            Map<String, Object> source = Collections.singletonMap("field1", "test" + i);
            IndexRequestBuilder indexRequestBuilder = new IndexRequestBuilder(client);
            indexRequestBuilder.setIndex(indexName).setType(typeName).setId(String.valueOf(i)).setCreate(true).setSource(source);
            bulkRequestBuilder.add(indexRequestBuilder);
        }
        bulkRequestBuilder.execute().actionGet();
        runner.refresh();
    }
}
