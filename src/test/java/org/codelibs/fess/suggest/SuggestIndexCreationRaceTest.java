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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.codelibs.fesen.client.HttpClient;
import org.codelibs.fesen.opensearch.action.ActionRequest;
import org.codelibs.fesen.opensearch.action.ActionType;
import org.codelibs.fesen.opensearch.action.admin.indices.alias.Alias;
import org.codelibs.fesen.opensearch.action.admin.indices.create.CreateIndexAction;
import org.codelibs.fesen.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.codelibs.fesen.opensearch.cluster.metadata.AliasMetadata;
import org.codelibs.fesen.opensearch.common.settings.Settings;
import org.codelibs.fesen.opensearch.core.action.ActionListener;
import org.codelibs.fesen.opensearch.core.action.ActionResponse;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Several processes that share the suggest index name can boot at the same time against a cluster
 * that has no suggest index yet. Each of them finds no alias and creates its own timestamped index,
 * so exactly one of those creations must win and the others must use the index it created.
 */
public class SuggestIndexCreationRaceTest {
    private static final String ID = "race-test";

    private static final String SEARCH_ALIAS = ID + ".suggest";

    private static final String UPDATE_ALIAS = SEARCH_ALIAS + ".update";

    private static final int THREADS = 4;

    private static final int ROUNDS = 5;

    static OpenSearchRunner runner;
    static SuggestTestServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = SuggestTestServer.start("SuggestIndexCreationRaceTest");
        runner = server.runner();
        runner.ensureYellow();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.close();
    }

    @Before
    public void before() throws Exception {
        runner.admin().indices().prepareDelete("_all").execute().actionGet();
        runner.refresh();
    }

    @Test
    public void test_concurrentCreateIndexIfNothingOnFreshCluster() throws Exception {
        for (int round = 1; round <= ROUNDS; round++) {
            runner.admin().indices().prepareDelete("_all").execute().actionGet();
            runner.refresh();

            final List<Suggester> suggesters = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                suggesters.add(Suggester.builder().build(server.client(), ID));
            }
            final CyclicBarrier barrier = new CyclicBarrier(THREADS);
            final ExecutorService executor = Executors.newFixedThreadPool(THREADS);
            int created = 0;
            try {
                final List<Future<Boolean>> futures = new ArrayList<>();
                for (final Suggester suggester : suggesters) {
                    futures.add(executor.submit(() -> {
                        barrier.await(1, TimeUnit.MINUTES);
                        return suggester.createIndexIfNothing();
                    }));
                }
                for (final Future<Boolean> future : futures) {
                    try {
                        if (future.get(2, TimeUnit.MINUTES)) {
                            created++;
                        }
                    } catch (final ExecutionException e) {
                        throw new AssertionError("createIndexIfNothing() failed in round " + round, e.getCause());
                    }
                }
            } finally {
                executor.shutdownNow();
            }

            assertEquals("processes that created the suggest index in round " + round, 1, created);
            assertSingleSuggestIndex();
        }
    }

    @Test
    public void test_writeAliasTakenByAnotherProcess() throws Exception {
        // another process created its own index a moment earlier under a different timestamp
        try (final PreemptingClient client = new PreemptingClient(server.httpPort(), true)) {
            final Suggester suggester = Suggester.builder().build(client, ID);
            assertFalse(suggester.createIndexIfNothing());
            assertTrue("the create request for the suggest index was not intercepted", client.preempted.get());
        }
        assertEquals(PreemptingClient.OTHER_INDEX, assertSingleSuggestIndex());
    }

    @Test
    public void test_sameIndexNameCreatedByAnotherProcess() throws Exception {
        // another process generated the same timestamped index name
        try (final PreemptingClient client = new PreemptingClient(server.httpPort(), false)) {
            final Suggester suggester = Suggester.builder().build(client, ID);
            assertFalse(suggester.createIndexIfNothing());
            assertTrue("the create request for the suggest index was not intercepted", client.preempted.get());
        }
        assertSingleSuggestIndex();
    }

    @Test
    public void test_createIndexIfNothingKeepsIndexRotationWorking() throws Exception {
        final Suggester suggester = Suggester.builder().build(server.client(), ID);
        assertTrue(suggester.createIndexIfNothing());
        final String first = assertSingleSuggestIndex();

        suggester.indexer()
                .index(new SuggestItem(new String[] { "test" }, new String[][] { new String[] { "test" } }, new String[] { "content" }, 1,
                        0, -1, new String[] { "tag" }, new String[] { "role" }, null, SuggestItem.Kind.DOCUMENT));
        suggester.refresh();
        assertEquals(1, suggester.getAllWordsNum());

        suggester.createNextIndex();
        suggester.switchIndex();
        final List<String> updateIndices = getIndices(UPDATE_ALIAS);
        assertEquals(1, updateIndices.size());
        assertFalse(first.equals(updateIndices.get(0)));
        assertEquals(updateIndices, getIndices(SEARCH_ALIAS));

        suggester.indexer()
                .index(new SuggestItem(new String[] { "next" }, new String[][] { new String[] { "next" } }, new String[] { "content" }, 1,
                        0, -1, new String[] { "tag" }, new String[] { "role" }, null, SuggestItem.Kind.DOCUMENT));
        suggester.refresh();
        assertEquals(1, suggester.getAllWordsNum());
    }

    /**
     * Asserts that both aliases point to the same single index and that the update alias is its write index.
     *
     * @return the suggest index
     */
    private static String assertSingleSuggestIndex() {
        final List<String> searchIndices = getIndices(SEARCH_ALIAS);
        final List<String> updateIndices = getIndices(UPDATE_ALIAS);
        assertEquals("indices behind " + SEARCH_ALIAS, 1, searchIndices.size());
        assertEquals("indices behind " + UPDATE_ALIAS, searchIndices, updateIndices);
        final Map<String, List<AliasMetadata>> aliases =
                server.client().admin().indices().prepareGetAliases(UPDATE_ALIAS).execute().actionGet().getAliases();
        final AliasMetadata updateAlias = aliases.get(updateIndices.get(0)).get(0);
        assertEquals(Boolean.TRUE, updateAlias.writeIndex());
        return updateIndices.get(0);
    }

    private static List<String> getIndices(final String alias) {
        final List<String> indices = new ArrayList<>();
        server.client().admin().indices().prepareGetAliases(alias).execute().actionGet().getAliases().forEach((index, metadata) -> {
            if (metadata.stream().anyMatch(m -> alias.equals(m.alias()))) {
                indices.add(index);
            }
        });
        return indices;
    }

    /**
     * Stands in for another process that boots at the same moment: the first time the suggest index
     * is created, it creates one of its own with the same aliases first.
     */
    static class PreemptingClient extends HttpClient {
        static final String OTHER_INDEX = SEARCH_ALIAS + ".20000101000000000";

        private final boolean otherName;

        final AtomicBoolean preempted = new AtomicBoolean();

        PreemptingClient(final int httpPort, final boolean otherName) {
            super(Settings.builder().putList("http.hosts", "localhost:" + httpPort).put("http.socket_timeout", 60000).build(), null);
            this.otherName = otherName;
        }

        @Override
        protected <Request extends ActionRequest, Response extends ActionResponse> void doExecute(final ActionType<Response> action,
                final Request request, final ActionListener<Response> listener) {
            if (action == CreateIndexAction.INSTANCE && ((CreateIndexRequest) request).index().startsWith(SEARCH_ALIAS + ".")
                    && preempted.compareAndSet(false, true)) {
                server.client()
                        .admin()
                        .indices()
                        .prepareCreate(otherName ? OTHER_INDEX : ((CreateIndexRequest) request).index())
                        .addAlias(new Alias(SEARCH_ALIAS))
                        .addAlias(new Alias(UPDATE_ALIAS).writeIndex(true))
                        .execute()
                        .actionGet();
            }
            super.doExecute(action, request, listener);
        }
    }
}
