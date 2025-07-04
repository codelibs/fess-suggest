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
package org.codelibs.fess.suggest.request;

import org.codelibs.fess.suggest.concurrent.Deferred;
import org.opensearch.transport.client.Client;

/**
 * An abstract class that serves as a builder for creating and executing requests.
 *
 * @param <Req> the type of the request
 * @param <Res> the type of the response
 */
public abstract class RequestBuilder<Req extends Request<Res>, Res extends Response> {
    /** The OpenSearch client. */
    protected Client client;
    /** The request being built. */
    protected Req request;

    /**
     * Constructor for RequestBuilder.
     * @param client The OpenSearch client.
     * @param request The request instance.
     */
    public RequestBuilder(final Client client, final Req request) {
        this.client = client;
        this.request = request;
    }

    /**
     * Executes the request.
     * @return A Promise that will be resolved with the response or rejected with an error.
     */
    public Deferred<Res>.Promise execute() {
        return request.execute(client);
    }
}
