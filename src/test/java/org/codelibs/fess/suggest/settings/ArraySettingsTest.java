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

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.SuggestTestServer;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.codelibs.fesen.client.HttpClient;
import org.codelibs.fesen.opensearch.action.ActionRequest;
import org.codelibs.fesen.opensearch.action.ActionType;
import org.codelibs.fesen.opensearch.action.search.CreatePitAction;
import org.codelibs.fesen.opensearch.action.search.CreatePitRequest;
import org.codelibs.fesen.opensearch.action.search.CreatePitResponse;
import org.codelibs.fesen.opensearch.action.search.ShardSearchFailure;
import org.codelibs.fesen.opensearch.common.settings.Settings;
import org.codelibs.fesen.opensearch.core.action.ActionListener;
import org.codelibs.fesen.opensearch.core.action.ActionResponse;
import org.codelibs.fesen.opensearch.index.IndexNotFoundException;

public class ArraySettingsTest {
    String id = "arraySettingsTest";

    static SuggestSettings settings;

    static OpenSearchRunner runner;
    static SuggestTestServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = SuggestTestServer.start("ArraySettingsTest");
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
        settings = Suggester.builder().build(server.client(), id).settings();
    }

    @Test
    public void test_setAndGetAsArray() {
        String key = "key";
        String value1 = "a";
        String value2 = "b";
        String value3 = "c";
        settings.array().add(key, value1);
        settings.array().add(key, value2);
        settings.array().add(key, value3);
        assertEquals(3, settings.array().get(key).length);
        assertEquals(value1, settings.array().get(key)[0]);
        assertEquals(value2, settings.array().get(key)[1]);
        assertEquals(value3, settings.array().get(key)[2]);
    }

    @Test
    public void test_delete() {
        String key = "key";
        String value1 = "a";
        String value2 = "b";
        String value3 = "c";
        settings.array().add(key, value1);
        settings.array().add(key, value2);
        settings.array().add(key, value3);
        assertEquals(3, settings.array().get(key).length);
        assertEquals(value1, settings.array().get(key)[0]);
        assertEquals(value2, settings.array().get(key)[1]);
        assertEquals(value3, settings.array().get(key)[2]);

        settings.array().delete(key, value2);
        assertEquals(2, settings.array().get(key).length);
        assertEquals(value1, settings.array().get(key)[0]);
        assertEquals(value3, settings.array().get(key)[1]);

        settings.array().delete(key);
        assertEquals(0, settings.array().get(key).length);
    }

    /**
     * Reproduces the race ArraySettings#getFromArrayIndex can hit right after another process has created the
     * array settings index: the first PIT creation on it fails because its primary shard has not started yet.
     * get() must retry instead of failing.
     */
    @Test
    public void test_getRetriesWhenPitCreationFailsOnce() throws Exception {
        final String key = "key";
        final String value = "a";
        // build the array settings index for real first, through the plain client this test does not intercept
        settings.array().add(key, value);
        final String actualIndex = settings.array().arraySettingsIndexName + "." + id.toLowerCase(Locale.ENGLISH);

        try (PitFailingOnceClient client = new PitFailingOnceClient(server.httpPort(), actualIndex, 1)) {
            final SuggestSettings interceptedSettings = SuggestSettings.builder().build(client, id);
            assertArrayEquals(new String[] { value }, interceptedSettings.array().get(key));
            assertEquals(0, client.remainingFailures.get());
        }
    }

    /**
     * Fails the first PIT creation on the given index with a response that carries no ID, the way OpenSearch
     * answers a PIT request none of whose shards are available, and lets every later attempt through normally.
     */
    static class PitFailingOnceClient extends HttpClient {
        private final String index;

        final AtomicInteger remainingFailures;

        PitFailingOnceClient(final int httpPort, final String index, final int failures) {
            super(clientSettings(httpPort), null);
            this.index = index;
            remainingFailures = new AtomicInteger(failures);
        }

        @Override
        protected <Request extends ActionRequest, Response extends ActionResponse> void doExecute(final ActionType<Response> action,
                final Request request, final ActionListener<Response> listener) {
            if (action == CreatePitAction.INSTANCE && index.equals(((CreatePitRequest) request).getIndices()[0])
                    && remainingFailures.getAndUpdate(n -> Math.max(n - 1, 0)) > 0) {
                @SuppressWarnings("unchecked")
                final Response noShardResponse =
                        (Response) new CreatePitResponse(null, System.currentTimeMillis(), 1, 0, 1, 0, new ShardSearchFailure[0]);
                listener.onResponse(noShardResponse);
                return;
            }
            super.doExecute(action, request, listener);
        }
    }

    private static Settings clientSettings(final int httpPort) {
        return Settings.builder().putList("http.hosts", "localhost:" + httpPort).put("http.socket_timeout", 60000).build();
    }
}
