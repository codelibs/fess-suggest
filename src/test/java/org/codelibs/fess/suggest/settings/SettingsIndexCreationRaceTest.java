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
package org.codelibs.fess.suggest.settings;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.codelibs.fesen.client.HttpClient;
import org.codelibs.fesen.opensearch.OpenSearchStatusException;
import org.codelibs.fesen.opensearch.action.ActionRequest;
import org.codelibs.fesen.opensearch.action.ActionType;
import org.codelibs.fesen.opensearch.action.admin.indices.create.CreateIndexAction;
import org.codelibs.fesen.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.codelibs.fesen.opensearch.action.get.GetAction;
import org.codelibs.fesen.opensearch.action.get.GetRequest;
import org.codelibs.fesen.opensearch.action.support.ActiveShardCount;
import org.codelibs.fesen.opensearch.action.support.PlainActionFuture;
import org.codelibs.fesen.opensearch.common.settings.Settings;
import org.codelibs.fesen.opensearch.core.action.ActionListener;
import org.codelibs.fesen.opensearch.core.action.ActionResponse;
import org.codelibs.fesen.opensearch.core.rest.RestStatus;
import org.codelibs.fesen.opensearch.transport.client.Client;
import org.codelibs.fess.suggest.SuggestTestServer;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Several processes that share the settings index names can boot at the same time against a
 * cluster that has none of these indices yet. Each of them sees an index missing and creates it,
 * so all but one get resource_already_exists_exception back; they then write the same default
 * array settings document at once, and a process that arrives a moment later finds an index whose
 * primary shard has not started yet.
 */
public class SettingsIndexCreationRaceTest {
    private static final String ID = "race-test";

    private static final String SETTINGS_INDEX = "fess_suggest";

    private static final String ARRAY_SETTINGS_INDEX = SETTINGS_INDEX + "_array." + ID;

    private static final String ANALYZER_SETTINGS_INDEX = SETTINGS_INDEX + "_analyzer";

    private static final String ALLOCATION_FILTER = "index.routing.allocation.require._name";

    private static final int THREADS = 4;

    private static final int ROUNDS = 5;

    static OpenSearchRunner runner;
    static SuggestTestServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = SuggestTestServer.start("SettingsIndexCreationRaceTest");
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
    public void test_concurrentInitOnFreshCluster() throws Exception {
        for (int round = 1; round <= ROUNDS; round++) {
            runner.admin().indices().prepareDelete("_all").execute().actionGet();
            runner.refresh();

            final CyclicBarrier barrier = new CyclicBarrier(THREADS);
            final ExecutorService executor = Executors.newFixedThreadPool(THREADS);
            try {
                final List<Future<Void>> futures = new ArrayList<>();
                for (int i = 0; i < THREADS; i++) {
                    futures.add(executor.submit(() -> {
                        barrier.await(1, TimeUnit.MINUTES);
                        SuggestSettings.builder().build(server.client(), ID).init();
                        return null;
                    }));
                }
                for (final Future<Void> future : futures) {
                    try {
                        future.get(2, TimeUnit.MINUTES);
                    } catch (final ExecutionException e) {
                        throw new AssertionError("init() failed in round " + round, e.getCause());
                    }
                }
            } finally {
                executor.shutdownNow();
            }

            assertSettingsUsable();
        }
    }

    @Test
    public void test_settingsIndexCreatedByAnotherProcess() throws Exception {
        assertInitSurvivesPreemptedCreate(SETTINGS_INDEX);
    }

    @Test
    public void test_arraySettingsIndexCreatedByAnotherProcess() throws Exception {
        assertInitSurvivesPreemptedCreate(ARRAY_SETTINGS_INDEX);
    }

    @Test
    public void test_analyzerSettingsIndexCreatedByAnotherProcess() throws Exception {
        assertInitSurvivesPreemptedCreate(ANALYZER_SETTINGS_INDEX);
    }

    @Test
    public void test_settingsIndexWhosePrimaryHasNotStarted() throws Exception {
        // another process has just created the settings index; keep its primary shard from starting until the first get
        server.client()
                .admin()
                .indices()
                .prepareCreate(SETTINGS_INDEX)
                .setSettings(Settings.builder().put("index.number_of_replicas", 0).put(ALLOCATION_FILTER, "no-such-node"))
                .setWaitForActiveShards(ActiveShardCount.NONE)
                .execute()
                .actionGet();
        try (final ShardStartingClient client = new ShardStartingClient(server.httpPort(), SETTINGS_INDEX)) {
            SuggestSettings.builder().build(client, ID).init();
            assertTrue("the first get on " + SETTINGS_INDEX + " did not find its shard unavailable", client.unavailable.get());
        }
        assertSettingsUsable();
    }

    private static void assertInitSurvivesPreemptedCreate(final String index) throws Exception {
        try (final PreemptingClient client = new PreemptingClient(server.httpPort(), index)) {
            SuggestSettings.builder().build(client, ID).init();
            assertTrue("the create request for " + index + " was not intercepted", client.preempted.get());
        }
        assertSettingsUsable();
    }

    private static void assertSettingsUsable() {
        final Client client = server.client();
        final SuggestSettings settings = SuggestSettings.builder().build(client, ID);
        settings.init();
        assertEquals(ID + ".suggest", settings.getAsString(SuggestSettings.DefaultKeys.INDEX, ""));
        assertArrayEquals(new String[] { "content" }, settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS));
        assertFalse(settings.analyzer().getAnalyzerNames().isEmpty());
        assertTrue(settings.analyzer().getFieldAnalyzerMapping().isEmpty());
    }

    /**
     * Stands in for another process that boots at the same moment: the first time the given index
     * is created, it sends an identical create request of its own first, so the caller's request is
     * answered with resource_already_exists_exception.
     */
    static class PreemptingClient extends HttpClient {
        private final String index;

        final AtomicBoolean preempted = new AtomicBoolean();

        PreemptingClient(final int httpPort, final String index) {
            super(clientSettings(httpPort), null);
            this.index = index;
        }

        @Override
        protected <Request extends ActionRequest, Response extends ActionResponse> void doExecute(final ActionType<Response> action,
                final Request request, final ActionListener<Response> listener) {
            if (action == CreateIndexAction.INSTANCE && index.equals(((CreateIndexRequest) request).index())
                    && preempted.compareAndSet(false, true)) {
                final PlainActionFuture<Response> future = PlainActionFuture.newFuture();
                super.doExecute(action, request, future);
                future.actionGet();
            }
            super.doExecute(action, request, listener);
        }
    }

    /**
     * Sends the first get on the given index while its primary shard cannot be allocated and, once that get has
     * failed, lets the shard start - as it does a moment after another process has created the index.
     */
    static class ShardStartingClient extends HttpClient {
        private final String index;

        private final AtomicBoolean intercepted = new AtomicBoolean();

        final AtomicBoolean unavailable = new AtomicBoolean();

        ShardStartingClient(final int httpPort, final String index) {
            super(clientSettings(httpPort), null);
            this.index = index;
        }

        @Override
        protected <Request extends ActionRequest, Response extends ActionResponse> void doExecute(final ActionType<Response> action,
                final Request request, final ActionListener<Response> listener) {
            if (action == GetAction.INSTANCE && index.equals(((GetRequest) request).index()) && intercepted.compareAndSet(false, true)) {
                final PlainActionFuture<Response> future = PlainActionFuture.newFuture();
                super.doExecute(action, request, future);
                try {
                    future.actionGet();
                } catch (final OpenSearchStatusException e) {
                    if (e.status() == RestStatus.SERVICE_UNAVAILABLE) {
                        unavailable.set(true);
                        server.client()
                                .admin()
                                .indices()
                                .prepareUpdateSettings(index)
                                .setSettings(Settings.builder().putNull(ALLOCATION_FILTER))
                                .execute()
                                .actionGet();
                    }
                    listener.onFailure(e);
                    return;
                }
            }
            super.doExecute(action, request, listener);
        }
    }

    private static Settings clientSettings(final int httpPort) {
        return Settings.builder().putList("http.hosts", "localhost:" + httpPort).put("http.socket_timeout", 60000).build();
    }
}
