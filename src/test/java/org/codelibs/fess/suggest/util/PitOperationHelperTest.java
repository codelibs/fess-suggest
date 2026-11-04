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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.SuggestTestServer;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.codelibs.fess.suggest.exception.PitUnavailableException;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fesen.opensearch.action.index.IndexResponse;
import org.codelibs.fesen.opensearch.action.support.ActiveShardCount;
import org.codelibs.fesen.opensearch.common.settings.Settings;
import org.codelibs.fesen.opensearch.common.xcontent.XContentType;
import org.codelibs.fesen.opensearch.index.IndexNotFoundException;
import org.codelibs.fesen.opensearch.index.query.QueryBuilders;
import org.codelibs.fesen.opensearch.transport.client.Client;

/**
 * Integration tests for PitOperationHelper.
 */
public class PitOperationHelperTest {

    private static OpenSearchRunner runner;
    private static SuggestTestServer server;
    private static Client client;
    private static final String INDEX_NAME = "pit-test-index";
    private Suggester suggester;

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = SuggestTestServer.start("PitOperationHelperTest");
        runner = server.runner();
        runner.ensureYellow();
        client = server.client();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.close();
    }

    @Before
    public void before() throws Exception {
        runner.admin().indices().prepareDelete("_all").execute().actionGet();
        runner.refresh();
        suggester = Suggester.builder().build(client, "PitTest");
        suggester.createIndexIfNothing();
    }

    @Test
    public void testSearch_emptyResults() {
        // Create index without any documents
        createTestIndex();

        List<String> results = PitOperationHelper.search(client, suggester.settings(), INDEX_NAME, QueryBuilders.matchAllQuery(), 10,
                (hit, accumulator) -> accumulator.add(hit.getId()));

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testSearch_singlePage() {
        createTestIndex();
        indexDocuments(5);
        runner.refresh();

        List<String> results = PitOperationHelper.search(client, suggester.settings(), INDEX_NAME, QueryBuilders.matchAllQuery(), 10, // page size larger than document count
                (hit, accumulator) -> accumulator.add(hit.getId()));

        assertNotNull(results);
        assertEquals(5, results.size());
    }

    @Test
    public void testSearch_multiplePages() {
        createTestIndex();
        indexDocuments(25);
        runner.refresh();

        List<String> results = PitOperationHelper.search(client, suggester.settings(), INDEX_NAME, QueryBuilders.matchAllQuery(), 10, // page size smaller than document count
                (hit, accumulator) -> accumulator.add(hit.getId()));

        assertNotNull(results);
        assertEquals(25, results.size());
    }

    @Test
    public void testSearch_withQuery() {
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
        List<String> results = PitOperationHelper.search(client, suggester.settings(), INDEX_NAME, QueryBuilders.rangeQuery("value").lt(5),
                10, (hit, accumulator) -> accumulator.add(hit.getId()));

        assertNotNull(results);
        assertEquals(5, results.size());
    }

    @Test
    public void testSearch_extractsSourceData() {
        createTestIndex();

        // Index documents with specific data
        for (int i = 0; i < 3; i++) {
            Map<String, Object> source = new HashMap<>();
            source.put("name", "item" + i);
            source.put("value", i * 10);
            client.prepareIndex(INDEX_NAME).setSource(source, XContentType.JSON).execute().actionGet();
        }
        runner.refresh();

        List<Map<String, Object>> results = PitOperationHelper.search(client, suggester.settings(), INDEX_NAME,
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
    public void testSearch_customAccumulator() {
        createTestIndex();
        indexDocuments(5);
        runner.refresh();

        // Use custom accumulator to sum values
        List<Integer> values = new ArrayList<>();
        List<Integer> results = PitOperationHelper.search(client, suggester.settings(), INDEX_NAME, QueryBuilders.matchAllQuery(), 10,
                (hit, accumulator) -> {
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

    @Test
    public void testCreatePit_indexWithUnassignedShards() {
        // no shard of the index can be allocated, as after a restore that leaves the index red
        client.admin()
                .indices()
                .prepareCreate("pit-red-index")
                .setSettings(
                        Settings.builder().put("index.number_of_replicas", 0).put("index.routing.allocation.require._name", "no-such-node"))
                .setWaitForActiveShards(ActiveShardCount.NONE)
                .execute()
                .actionGet();

        try {
            PitOperationHelper.search(client, suggester.settings(), "pit-red-index", QueryBuilders.matchAllQuery(), 10,
                    (hit, accumulator) -> accumulator.add(hit.getId()));
            fail("A PIT search on an index without available shards must fail.");
        } catch (final PitUnavailableException e) {
            // the exact type matters: it is the contract PitOperationHelper#readWithRetry relies on to tell this
            // failure apart from any other SuggesterException
            assertTrue(e.getMessage(), e.getMessage().contains("pit-red-index"));
            assertTrue(e.getMessage(), e.getMessage().contains("unassigned"));
        }
    }

    @Test
    public void testReadWithRetry_succeedsAfterOneRetry() {
        createTestIndex();

        final AtomicInteger calls = new AtomicInteger();
        final String result = PitOperationHelper.readWithRetry(client, suggester.settings(), INDEX_NAME, () -> {
            if (calls.incrementAndGet() == 1) {
                throw new PitUnavailableException("temporarily unavailable");
            }
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(2, calls.get());
    }

    @Test
    public void testReadWithRetry_rethrowsSameExceptionInstanceAfterDeadline() {
        createTestIndex();

        // a short cluster timeout keeps the test fast without depending on how many retries fit in it
        final SuggestSettings shortDeadlineSettings = SuggestSettings.builder().clusterTimeout("300ms").build(client, "pit-retry-test");
        final List<PitUnavailableException> attempts = new ArrayList<>();

        try {
            PitOperationHelper.readWithRetry(client, shortDeadlineSettings, INDEX_NAME, () -> {
                final PitUnavailableException e = new PitUnavailableException("still unavailable");
                attempts.add(e);
                throw e;
            });
            fail("readWithRetry must give up once the cluster timeout has elapsed.");
        } catch (final PitUnavailableException e) {
            assertFalse(attempts.isEmpty());
            // the rethrown exception must be the exact instance from the last attempt, not a new wrapper
            assertSame(attempts.get(attempts.size() - 1), e);
        }
    }

    @Test
    public void testReadWithRetry_propagatesOtherFailuresWithoutRetrying() {
        final AtomicInteger calls = new AtomicInteger();

        try {
            PitOperationHelper.readWithRetry(client, suggester.settings(), INDEX_NAME, () -> {
                calls.incrementAndGet();
                throw new IndexNotFoundException(INDEX_NAME);
            });
            fail("A failure that is not a PitUnavailableException must not be retried.");
        } catch (final IndexNotFoundException e) {
            assertEquals(1, calls.get());
        }
    }

    // A test for the InterruptedException branch of readWithRetry was attempted and dropped: the interrupt can land
    // either on Thread.sleep (handled) or on the yellow-wait health check's actionGet (not handled - fesen-httpclient's
    // FutureUtils.get wraps that InterruptedException into an IllegalStateException("Future got interrupted") instead
    // of propagating it, the same as the pre-existing SuggestSettings#getSettingsDocument retry this mirrors). Which
    // one happens is a race with no way to control it from the test, so the test was flaky between the two outcomes
    // instead of exercising a single well-defined behavior. See 0015-pre-phase-review-fixes.md for the write-up.
}
