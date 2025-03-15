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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

}
