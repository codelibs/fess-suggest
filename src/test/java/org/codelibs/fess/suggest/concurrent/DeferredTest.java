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
            assertEquals("org.codelibs.fess.suggest.exception.SuggesterException: test", e.getMessage());
        }
    }

}
