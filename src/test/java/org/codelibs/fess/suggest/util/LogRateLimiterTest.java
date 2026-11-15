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
package org.codelibs.fess.suggest.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class LogRateLimiterTest {

    @Test
    public void testFirstCallIsLetThroughAndFollowingCallsAreNotWithinInterval() {
        final LogRateLimiter limiter = new LogRateLimiter(60_000L);

        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());
    }

    @Test
    public void testZeroIntervalLetsEveryCallThrough() {
        final LogRateLimiter limiter = new LogRateLimiter(0L);

        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
    }

    @Test
    public void testCallIsLetThroughAgainAfterInterval() throws Exception {
        final LogRateLimiter limiter = new LogRateLimiter(20L);

        assertTrue(limiter.tryAcquire());
        Thread.sleep(50L);
        assertTrue(limiter.tryAcquire());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeIntervalIsRejected() {
        new LogRateLimiter(-1L);
    }

    @Test
    public void testOnlyOneThreadAcquiresWithinInterval() throws Exception {
        final LogRateLimiter limiter = new LogRateLimiter(60_000L);
        final AtomicInteger acquired = new AtomicInteger();
        final int numThreads = 8;
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        try {
            final List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < numThreads; i++) {
                futures.add(executor.submit(() -> {
                    for (int j = 0; j < 1000; j++) {
                        if (limiter.tryAcquire()) {
                            acquired.incrementAndGet();
                        }
                    }
                }));
            }
            for (final Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, acquired.get());
    }
}
