package org.codelibs.fess.suggest.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.request.suggest.SuggestResponse;
import org.junit.Test;

public class SuggestFutureTest {
    @Test
    public void test_doneBeforeResolve() throws Exception {
        final SuggestFuture<SuggestResponse> future = new SuggestRequestFuture<>();

        Thread th = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {}
            future.resolve(new SuggestResponse(0, Collections.emptyList(), 0, null), null);
        });
        th.start();

        final CountDownLatch latch = new CountDownLatch(1);
        future.done(response -> latch.countDown());
        latch.await();
        assertTrue(true);
    }

    @Test
    public void test_doneAfterResolve() throws Exception {
        final SuggestFuture<SuggestResponse> future = new SuggestRequestFuture<>();

        Thread th = new Thread(() -> {
            future.resolve(new SuggestResponse(0, Collections.emptyList(), 0, null), null);
        });
        th.start();

        Thread.sleep(1000);
        final CountDownLatch latch = new CountDownLatch(1);
        future.done(response -> latch.countDown());
        latch.await();
        assertTrue(true);
    }

    @Test
    public void test_getResponseBeforeResolve() throws Exception {
        final SuggestFuture<SuggestResponse> future = new SuggestRequestFuture<>();

        Thread th = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {}
            future.resolve(new SuggestResponse(0, Collections.emptyList(), 0, null), null);
        });
        th.start();

        SuggestResponse response = future.getResponse();
        assertEquals(0, response.getNum());
    }

    @Test
    public void test_getResponseAfterResolve() throws Exception {
        final SuggestFuture<SuggestResponse> future = new SuggestRequestFuture<>();

        Thread th = new Thread(() -> {
            future.resolve(new SuggestResponse(0, Collections.emptyList(), 0, null), null);
        });
        th.start();

        Thread.sleep(1000);
        SuggestResponse response = future.getResponse();
        assertEquals(0, response.getNum());
    }

    @Test
    public void test_getResponseWithException() throws Exception {
        final SuggestFuture<SuggestResponse> future = new SuggestRequestFuture<>();

        Thread th = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {}
            future.resolve(null, new SuggesterException("test"));
        });
        th.start();

        try {
            future.getResponse();
            fail();
        } catch (SuggesterException e) {
            assertEquals("test", e.getMessage());
        }
    }

}
