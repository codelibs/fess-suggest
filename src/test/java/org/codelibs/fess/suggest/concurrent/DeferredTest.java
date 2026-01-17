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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.request.suggest.SuggestResponse;
import org.junit.Test;

public class DeferredTest {
    @Test
    public void test_doneBeforeResolve() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();

        Thread th = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {}
            deferred.resolve(new SuggestResponse("", 0, Collections.emptyList(), 0, null));
        });
        th.start();

        final CountDownLatch latch = new CountDownLatch(1);
        deferred.promise().then(response -> latch.countDown());
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void test_doneAfterResolve() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();

        Thread th = new Thread(() -> {
            deferred.resolve(new SuggestResponse("", 0, Collections.emptyList(), 0, null));
        });
        th.start();

        Thread.sleep(1000);
        final CountDownLatch latch = new CountDownLatch(1);
        deferred.promise().then(response -> latch.countDown());
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void test_doneBeforeReject() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();

        Thread th = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {}
            deferred.reject(new Exception());
        });
        th.start();

        final CountDownLatch latch = new CountDownLatch(1);
        deferred.promise().error(error -> latch.countDown());
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void test_doneAfterReject() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();

        Thread th = new Thread(() -> {
            deferred.reject(new Exception());
        });
        th.start();

        Thread.sleep(1000);
        final CountDownLatch latch = new CountDownLatch(1);
        deferred.promise().error(error -> latch.countDown());
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void test_getResponseBeforeResolve() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();

        Thread th = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {}
            deferred.resolve(new SuggestResponse("", 0, Collections.emptyList(), 0, null));
        });
        th.start();

        SuggestResponse response = deferred.promise().getResponse(10, TimeUnit.SECONDS);
        assertEquals(0, response.getNum());
    }

    @Test
    public void test_getResponseAfterResolve() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();

        Thread th = new Thread(() -> {
            deferred.resolve(new SuggestResponse("", 0, Collections.emptyList(), 0, null));
        });
        th.start();

        Thread.sleep(1000);
        SuggestResponse response = deferred.promise().getResponse(10, TimeUnit.SECONDS);
        assertEquals(0, response.getNum());
    }

    @Test
    public void test_getResponseWithException() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();

        Thread th = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {}
            deferred.reject(new SuggesterException("test"));
        });
        th.start();

        try {
            deferred.promise().getResponse(10, TimeUnit.SECONDS);
            fail();
        } catch (SuggesterException e) {
            assertEquals("An error occurred during the deferred computation.", e.getMessage());
        }
    }

    // ============================================================
    // Edge case tests for timeout handling
    // ============================================================

    @Test
    public void test_getResponseTimeout() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();
        // Don't resolve - let it timeout

        try {
            deferred.promise().getResponse(100, TimeUnit.MILLISECONDS);
            fail("Should throw timeout exception");
        } catch (SuggesterException e) {
            assertTrue("Should mention timeout", e.getMessage().contains("timeout"));
        }
    }

    @Test
    public void test_getResponseDefaultTimeout() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();

        // Resolve quickly to avoid default 1 minute timeout
        Thread th = new Thread(() -> {
            deferred.resolve(new SuggestResponse("", 0, Collections.emptyList(), 0, null));
        });
        th.start();

        // Use default timeout (should work since we resolve quickly)
        SuggestResponse response = deferred.promise().getResponse();
        assertNotNull(response);
    }

    // ============================================================
    // Edge case tests for multiple callbacks
    // ============================================================

    @Test
    public void test_multipleCallbacks_allInvoked() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();
        final AtomicInteger callbackCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(3);

        deferred.promise().then(response -> {
            callbackCount.incrementAndGet();
            latch.countDown();
        }).then(response -> {
            callbackCount.incrementAndGet();
            latch.countDown();
        }).then(response -> {
            callbackCount.incrementAndGet();
            latch.countDown();
        });

        deferred.resolve(new SuggestResponse("", 0, Collections.emptyList(), 0, null));

        assertTrue("All callbacks should complete", latch.await(10, TimeUnit.SECONDS));
        assertEquals("All 3 callbacks should be invoked", 3, callbackCount.get());
    }

    @Test
    public void test_multipleErrorCallbacks_allInvoked() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();
        final AtomicInteger callbackCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(3);

        deferred.promise().error(error -> {
            callbackCount.incrementAndGet();
            latch.countDown();
        }).error(error -> {
            callbackCount.incrementAndGet();
            latch.countDown();
        }).error(error -> {
            callbackCount.incrementAndGet();
            latch.countDown();
        });

        deferred.reject(new RuntimeException("test"));

        assertTrue("All error callbacks should complete", latch.await(10, TimeUnit.SECONDS));
        assertEquals("All 3 error callbacks should be invoked", 3, callbackCount.get());
    }

    @Test
    public void test_mixedCallbacks_onlyThenInvokedOnResolve() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();
        final AtomicInteger thenCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(1);

        deferred.promise().then(response -> {
            thenCount.incrementAndGet();
            latch.countDown();
        }).error(error -> {
            errorCount.incrementAndGet();
        });

        deferred.resolve(new SuggestResponse("", 0, Collections.emptyList(), 0, null));

        assertTrue("Then callback should complete", latch.await(10, TimeUnit.SECONDS));
        Thread.sleep(100); // Wait a bit to ensure error callback isn't called
        assertEquals("Then callback should be invoked", 1, thenCount.get());
        assertEquals("Error callback should not be invoked", 0, errorCount.get());
    }

    @Test
    public void test_mixedCallbacks_onlyErrorInvokedOnReject() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();
        final AtomicInteger thenCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(1);

        deferred.promise().then(response -> {
            thenCount.incrementAndGet();
        }).error(error -> {
            errorCount.incrementAndGet();
            latch.countDown();
        });

        deferred.reject(new RuntimeException("test"));

        assertTrue("Error callback should complete", latch.await(10, TimeUnit.SECONDS));
        Thread.sleep(100); // Wait a bit to ensure then callback isn't called
        assertEquals("Then callback should not be invoked", 0, thenCount.get());
        assertEquals("Error callback should be invoked", 1, errorCount.get());
    }

    // ============================================================
    // Edge case tests for multiple resolve/reject calls
    // ============================================================

    @Test
    public void test_multipleResolve_onlyFirstCounts() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();
        final AtomicInteger callbackCount = new AtomicInteger(0);
        final AtomicReference<String> receivedIndex = new AtomicReference<>();

        deferred.promise().then(response -> {
            callbackCount.incrementAndGet();
            receivedIndex.set(response.getIndex());
        });

        // First resolve
        deferred.resolve(new SuggestResponse("first", 0, Collections.emptyList(), 0, null));
        // Second resolve (should be ignored)
        deferred.resolve(new SuggestResponse("second", 0, Collections.emptyList(), 0, null));
        // Third resolve (should be ignored)
        deferred.resolve(new SuggestResponse("third", 0, Collections.emptyList(), 0, null));

        Thread.sleep(500);

        assertEquals("Callback should only be invoked once", 1, callbackCount.get());
        assertEquals("Should receive first response", "first", receivedIndex.get());
    }

    @Test
    public void test_multipleReject_onlyFirstCounts() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();
        final AtomicInteger callbackCount = new AtomicInteger(0);
        final AtomicReference<String> receivedMessage = new AtomicReference<>();

        deferred.promise().error(error -> {
            callbackCount.incrementAndGet();
            receivedMessage.set(error.getMessage());
        });

        // First reject
        deferred.reject(new RuntimeException("first"));
        // Second reject (should be ignored)
        deferred.reject(new RuntimeException("second"));
        // Third reject (should be ignored)
        deferred.reject(new RuntimeException("third"));

        Thread.sleep(500);

        assertEquals("Error callback should only be invoked once", 1, callbackCount.get());
        assertEquals("Should receive first error", "first", receivedMessage.get());
    }

    @Test
    public void test_resolveAfterReject_ignored() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();
        final AtomicInteger thenCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);

        deferred.promise().then(response -> thenCount.incrementAndGet()).error(error -> errorCount.incrementAndGet());

        deferred.reject(new RuntimeException("error"));
        deferred.resolve(new SuggestResponse("", 0, Collections.emptyList(), 0, null));

        Thread.sleep(500);

        assertEquals("Then callback should not be invoked", 0, thenCount.get());
        assertEquals("Error callback should be invoked once", 1, errorCount.get());
    }

    @Test
    public void test_rejectAfterResolve_ignored() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();
        final AtomicInteger thenCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);

        deferred.promise().then(response -> thenCount.incrementAndGet()).error(error -> errorCount.incrementAndGet());

        deferred.resolve(new SuggestResponse("", 0, Collections.emptyList(), 0, null));
        deferred.reject(new RuntimeException("error"));

        Thread.sleep(500);

        assertEquals("Then callback should be invoked once", 1, thenCount.get());
        assertEquals("Error callback should not be invoked", 0, errorCount.get());
    }

    // ============================================================
    // Edge case tests for callback exception handling
    // ============================================================

    @Test
    public void test_callbackException_doesNotBreakDeferred() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();
        final AtomicInteger callbackCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(1);

        deferred.promise().then(response -> {
            callbackCount.incrementAndGet();
            latch.countDown();
            throw new RuntimeException("Callback exception");
        });

        deferred.resolve(new SuggestResponse("", 0, Collections.emptyList(), 0, null));

        // Callback should still be invoked even if it throws
        assertTrue("Callback should be invoked", latch.await(10, TimeUnit.SECONDS));
        assertEquals("Callback should be called", 1, callbackCount.get());
    }

    @Test
    public void test_errorCallbackException_doesNotBreakDeferred() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();
        final AtomicInteger callbackCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(1);

        deferred.promise().error(error -> {
            callbackCount.incrementAndGet();
            latch.countDown();
            throw new RuntimeException("Error callback exception");
        });

        deferred.reject(new RuntimeException("original error"));

        assertTrue("Error callback should be invoked", latch.await(10, TimeUnit.SECONDS));
        assertEquals("Error callback should be called", 1, callbackCount.get());
    }

    // ============================================================
    // Edge case tests for promise chaining
    // ============================================================

    @Test
    public void test_promiseChaining_returnsSamePromise() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();

        Deferred<SuggestResponse>.Promise promise1 = deferred.promise();
        Deferred<SuggestResponse>.Promise promise2 = promise1.then(response -> {});
        Deferred<SuggestResponse>.Promise promise3 = promise2.error(error -> {});

        // All should be the same promise instance
        assertEquals("Promise chain should return same instance", promise1, promise2);
        assertEquals("Promise chain should return same instance", promise2, promise3);
    }

    @Test
    public void test_promiseFromDeferredThen() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();
        final CountDownLatch latch = new CountDownLatch(1);

        // Using deferred.then() directly (instead of promise().then())
        deferred.then(response -> latch.countDown());

        deferred.resolve(new SuggestResponse("", 0, Collections.emptyList(), 0, null));

        assertTrue("Callback via deferred.then() should work", latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void test_promiseFromDeferredError() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();
        final CountDownLatch latch = new CountDownLatch(1);

        // Using deferred.error() directly (instead of promise().error())
        deferred.error(error -> latch.countDown());

        deferred.reject(new RuntimeException("test"));

        assertTrue("Callback via deferred.error() should work", latch.await(10, TimeUnit.SECONDS));
    }

    // ============================================================
    // Edge case tests for concurrent access
    // ============================================================

    @Test
    public void test_concurrentResolve_onlyOneWins() throws Exception {
        final int threadCount = 10;
        final Deferred<SuggestResponse> deferred = new Deferred<>();
        final AtomicInteger callbackCount = new AtomicInteger(0);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);

        deferred.promise().then(response -> callbackCount.incrementAndGet());

        // Start multiple threads that all try to resolve
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    deferred.resolve(new SuggestResponse("thread-" + index, 0, Collections.emptyList(), 0, null));
                } catch (InterruptedException ignore) {} finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown(); // Start all threads
        assertTrue("All threads should complete", doneLatch.await(10, TimeUnit.SECONDS));

        Thread.sleep(500); // Wait for callback to execute

        assertEquals("Only one resolve should succeed", 1, callbackCount.get());
    }

    @Test
    public void test_registerCallbackWhileResolving() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();
        final AtomicInteger callbackCount = new AtomicInteger(0);
        final CountDownLatch resolveLatch = new CountDownLatch(1);
        final CountDownLatch callbackLatch = new CountDownLatch(2);

        // Register first callback
        deferred.promise().then(response -> {
            callbackCount.incrementAndGet();
            callbackLatch.countDown();
        });

        // Start resolver thread
        Thread resolver = new Thread(() -> {
            try {
                resolveLatch.await();
            } catch (InterruptedException ignore) {}
            deferred.resolve(new SuggestResponse("", 0, Collections.emptyList(), 0, null));
        });
        resolver.start();

        // Register another callback and trigger resolve at about the same time
        Thread.sleep(50);
        resolveLatch.countDown();
        deferred.promise().then(response -> {
            callbackCount.incrementAndGet();
            callbackLatch.countDown();
        });

        assertTrue("Both callbacks should complete", callbackLatch.await(10, TimeUnit.SECONDS));
        assertEquals("Both callbacks should be invoked", 2, callbackCount.get());
    }

    // ============================================================
    // Edge case tests for null handling
    // ============================================================

    @Test
    public void test_resolveWithNullResponse() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();
        final AtomicReference<SuggestResponse> received = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        deferred.promise().then(response -> {
            received.set(response);
            latch.countDown();
        });

        // Resolve with null
        deferred.resolve(null);

        assertTrue("Callback should be invoked", latch.await(10, TimeUnit.SECONDS));
        assertNull("Received response should be null", received.get());
    }

    @Test
    public void test_rejectWithNullError() throws Exception {
        final Deferred<SuggestResponse> deferred = new Deferred<>();
        final AtomicReference<Throwable> received = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        deferred.promise().error(error -> {
            received.set(error);
            latch.countDown();
        });

        // Reject with null
        deferred.reject(null);

        assertTrue("Error callback should be invoked", latch.await(10, TimeUnit.SECONDS));
        assertNull("Received error should be null", received.get());
    }
}
