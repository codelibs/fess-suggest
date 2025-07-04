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
package org.codelibs.fess.suggest.concurrent;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.codelibs.core.exception.InterruptedRuntimeException;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.request.Response;

/**
 * <p>
 *   Deferred is a class that represents a deferred computation.
 *   It is similar to a Promise in JavaScript.
 *   It allows you to register callbacks that will be executed when the computation is complete,
 *   either successfully (resolve) or with an error (reject).
 * </p>
 *
 * <p>
 *   The Deferred class has a Promise inner class that allows you to register callbacks
 *   to be executed when the computation is complete.
 *   The Promise class has then and error methods that allow you to register callbacks
 *   for successful and unsuccessful computations, respectively.
 * </p>
 *
 * <p>
 *   The Deferred class uses a CountDownLatch to allow you to wait for the computation to complete.
 *   The resolve and reject methods decrement the CountDownLatch, allowing the getResponse method
 *   to return the result of the computation.
 * </p>
 *
 * @param <RESPONSE> The type of the response.
 */
public class Deferred<RESPONSE extends Response> {
    /**
     * Constructs a new Deferred object.
     */
    public Deferred() {
        // nothing
    }

    private RESPONSE response = null;

    private Throwable error = null;

    private final Promise promise = new Promise();

    private final Queue<Consumer<RESPONSE>> doneCallbacks = new LinkedBlockingQueue<>();

    private final Queue<Consumer<Throwable>> errorCallbacks = new LinkedBlockingQueue<>();

    private final CountDownLatch latch = new CountDownLatch(1);

    /**
     * Resolves the deferred computation with the given response.
     * @param r The response.
     */
    public void resolve(final RESPONSE r) {
        final ArrayList<Consumer<RESPONSE>> executeCallbacks;
        synchronized (Deferred.this) {
            if (response != null || error != null) {
                return;
            }
            response = r;

            executeCallbacks = new ArrayList<>(doneCallbacks.size());
            Consumer<RESPONSE> callback;
            while ((callback = doneCallbacks.poll()) != null) {
                executeCallbacks.add(callback);
            }
        }
        if (executeCallbacks.size() > 0) {
            try {
                executeCallbacks.stream().forEach(callback -> callback.accept(response));
            } catch (final Exception ignore) {}
        }
        latch.countDown();
    }

    /**
     * Rejects the deferred computation with the given throwable.
     * @param t The throwable.
     */
    public void reject(final Throwable t) {
        final ArrayList<Consumer<Throwable>> executeCallbacks;
        synchronized (Deferred.this) {
            if (response != null || error != null) {
                return;
            }
            error = t;

            executeCallbacks = new ArrayList<>(errorCallbacks.size());
            Consumer<Throwable> callback;
            while ((callback = errorCallbacks.poll()) != null) {
                executeCallbacks.add(callback);
            }
        }
        if (executeCallbacks.size() > 0) {
            try {
                executeCallbacks.stream().forEach(callback -> callback.accept(error));
            } catch (final Exception ignore) {}
        }
        latch.countDown();
    }

    /**
     * Registers a callback to be executed when the computation is complete.
     * @param consumer The callback.
     * @return The promise.
     */
    public Promise then(final Consumer<RESPONSE> consumer) {
        return promise.then(consumer);
    }

    /**
     * Registers a callback to be executed when the computation fails.
     * @param consumer The callback.
     * @return The promise.
     */
    public Promise error(final Consumer<Throwable> consumer) {
        return promise.error(consumer);
    }

    /**
     * Returns the promise.
     * @return The promise.
     */
    public Promise promise() {
        return promise;
    }

    /**
     * The promise.
     */
    public class Promise {
        /**
         * Constructs a new Promise object.
         */
        public Promise() {
            // nothing
        }

        /**
         * Registers a callback to be executed when the computation is complete.
         * @param consumer The callback.
         * @return The promise.
         */
        public Promise then(final Consumer<RESPONSE> consumer) {
            final ArrayList<Consumer<RESPONSE>> executeCallbacks;
            synchronized (Deferred.this) {
                doneCallbacks.add(consumer);
                executeCallbacks = new ArrayList<>(doneCallbacks.size());
                if (response != null) {
                    Consumer<RESPONSE> callback;
                    while ((callback = doneCallbacks.poll()) != null) {
                        executeCallbacks.add(callback);
                    }
                }
            }
            if (executeCallbacks.size() > 0) {
                executeCallbacks.stream().forEach(callback -> callback.accept(response));
            }
            return this;
        }

        /**
         * Registers a callback to be executed when the computation fails.
         * @param consumer The callback.
         * @return The promise.
         */
        public Promise error(final Consumer<Throwable> consumer) {
            final ArrayList<Consumer<Throwable>> executeCallbacks;
            synchronized (Deferred.this) {
                errorCallbacks.add(consumer);
                executeCallbacks = new ArrayList<>(errorCallbacks.size());
                if (error != null) {
                    Consumer<Throwable> callback;
                    while ((callback = errorCallbacks.poll()) != null) {
                        executeCallbacks.add(callback);
                    }
                }
            }
            if (executeCallbacks.size() > 0) {
                executeCallbacks.stream().forEach(callback -> callback.accept(error));
            }
            return this;
        }

        /**
         * Returns the response.
         * @return The response.
         */
        public RESPONSE getResponse() {
            return getResponse(1, TimeUnit.MINUTES);
        }

        /**
         * Returns the response.
         * @param time The time to wait.
         * @param unit The time unit.
         * @return The response.
         */
        public RESPONSE getResponse(final long time, final TimeUnit unit) {
            try {
                final boolean isTimeout = !latch.await(time, unit);
                if (isTimeout) {
                    throw new SuggesterException("Request timeout. time:" + time + " unit:" + unit.name());
                }
                if (error != null) {
                    throw new SuggesterException("An error occurred during the deferred computation.", error);
                }
                return response;
            } catch (final InterruptedException e) {
                throw new InterruptedRuntimeException(e);
            }
        }
    }
}
