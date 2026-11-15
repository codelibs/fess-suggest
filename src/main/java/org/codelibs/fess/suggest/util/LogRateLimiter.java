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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Lets an operation through at most once per interval. It is used to keep a warning that can occur for every chunk of text
 * from flooding the log. The first call is always let through. This class is thread safe.
 */
public final class LogRateLimiter {

    private final long intervalMillis;

    private final AtomicLong lastAcquiredMillis = new AtomicLong();

    /**
     * Constructs a new LogRateLimiter.
     *
     * @param intervalMillis the minimum interval between two calls that are let through, 0 or more
     * @throws IllegalArgumentException if intervalMillis is negative
     */
    public LogRateLimiter(final long intervalMillis) {
        if (intervalMillis < 0) {
            throw new IllegalArgumentException("intervalMillis must not be negative: " + intervalMillis);
        }
        this.intervalMillis = intervalMillis;
    }

    /**
     * Tries to get the permission to log now.
     *
     * @return true if the interval has elapsed since the last permission (or this is the first call), false otherwise
     */
    public boolean tryAcquire() {
        final long now = System.currentTimeMillis();
        final long last = lastAcquiredMillis.get();
        final boolean intervalElapsed = last == 0 || now - last >= intervalMillis;
        return intervalElapsed && lastAcquiredMillis.compareAndSet(last, now);
    }
}
