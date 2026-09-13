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

import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;

import java.io.IOException;
import java.net.ServerSocket;

import org.codelibs.fesen.client.HttpClient;
import org.codelibs.fesen.opensearch.common.settings.Settings;
import org.codelibs.fesen.opensearch.transport.client.Client;
import org.codelibs.opensearch.runner.OpenSearchRunner;

/**
 * Test fixture that boots an embedded OpenSearch node and exposes it as a
 * fesen-httpclient {@link Client}.
 *
 * <p>
 * The production code compiles against the forked OpenSearch classes in
 * {@code org.codelibs.fesen.opensearch}, so the node client returned by
 * {@link OpenSearchRunner#client()} - which is a real
 * {@code org.opensearch.transport.client.Client} - can no longer be handed to
 * it. The node therefore keeps its role as the server and is driven over its
 * HTTP port instead. The node itself, the vendored analysis plugin and the
 * {@code runner.admin()} / {@code runner.refresh()} fixture calls keep using
 * the real {@code org.opensearch} types from the test-scope
 * {@code opensearch-runner} dependency.
 * </p>
 */
public final class SuggestTestServer implements AutoCloseable {

    /** The vendored analysis plugin; {@code suggest_analyzer.json} needs its kuromoji tokenizers. */
    public static final String EXTENSION_PLUGIN = "org.codelibs.opensearch.extension.ExtensionPlugin";

    /**
     * Socket read timeout for the HTTP client, in milliseconds. Without it a
     * request that the node answers with an error before the body is consumed
     * can block forever, which would hang the build instead of failing a test.
     */
    private static final int SOCKET_TIMEOUT_MILLIS = 60000;

    private final OpenSearchRunner runner;

    private final int httpPort;

    private final HttpClient client;

    private SuggestTestServer(final OpenSearchRunner runner, final int httpPort, final HttpClient client) {
        this.runner = runner;
        this.httpPort = httpPort;
        this.client = client;
    }

    /**
     * Boots a single-node cluster and connects an HTTP client to it.
     *
     * @param clusterName the cluster name
     * @return a started server
     */
    public static SuggestTestServer start(final String clusterName) {
        final OpenSearchRunner runner = new OpenSearchRunner();
        final int reservedPort = findFreePort();
        // OpenSearchRunner binds the first node to (baseHttpPort + 1) and selects the port via an
        // unreliable connect-based scan: it probes localhost over IPv4 and reports a port as free
        // even when it is already bound on [::1]. Pin it to the reserved free port and disable the
        // scan to avoid intermittent "Address already in use" failures - a whole test run boots
        // one node per class, which is exactly the shape that hits it.
        runner.setMaxHttpPort(-1);
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        }).build(newConfigs().clusterName(clusterName).numOfNode(1).baseHttpPort(reservedPort - 1).pluginTypes(EXTENSION_PLUGIN));
        runner.ensureYellow();

        final int httpPort = Integer.parseInt(runner.node().settings().get("http.port", String.valueOf(reservedPort)));
        final HttpClient client = new HttpClient(
                Settings.builder().putList("http.hosts", "localhost:" + httpPort).put("http.socket_timeout", SOCKET_TIMEOUT_MILLIS).build(),
                null);
        return new SuggestTestServer(runner, httpPort, client);
    }

    /**
     * Returns the embedded node runner, for node-side fixture calls.
     *
     * @return the runner
     */
    public OpenSearchRunner runner() {
        return runner;
    }

    /**
     * Returns the HTTP client that production code is driven with.
     *
     * @return the client
     */
    public Client client() {
        return client;
    }

    /**
     * Returns the HTTP port the node is listening on.
     *
     * @return the HTTP port
     */
    public int httpPort() {
        return httpPort;
    }

    @Override
    public void close() throws IOException {
        try {
            client.close();
        } finally {
            try {
                runner.close();
            } finally {
                runner.clean();
            }
        }
    }

    /**
     * Returns an ephemeral port assigned by the OS.
     *
     * @return a free TCP port
     */
    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to find a free port.", e);
        }
    }
}
