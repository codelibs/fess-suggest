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
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.opensearch.core.common.Strings;
import org.opensearch.transport.client.Client;

/**
 * Abstract class representing a request that can be executed to produce a response.
 *
 * @param <T> the type of response produced by this request
 */
public abstract class Request<T extends Response> {
    /**
     * Constructs a new request.
     */
    public Request() {
        // nothing
    }

    /**
     * Executes the request.
     * @param client The OpenSearch client.
     * @return A Promise that will be resolved with the response or rejected with an error.
     */
    public Deferred<T>.Promise execute(final Client client) {
        final String error = getValidationError();
        if (!Strings.isNullOrEmpty(error)) {
            throw new IllegalArgumentException(error);
        }

        final Deferred<T> deferred = new Deferred<>();
        try {
            processRequest(client, deferred);
        } catch (final Exception e) {
            throw new SuggesterException(e);
        }
        return deferred.promise();
    }

    /**
     * Processes the request.
     * @param client The OpenSearch client.
     * @param deferred The Deferred object to resolve or reject the response.
     */
    protected abstract void processRequest(Client client, Deferred<T> deferred);

    /**
     * Returns a validation error message, or null if there are no errors.
     * @return A validation error message, or null.
     */
    protected abstract String getValidationError();
}
