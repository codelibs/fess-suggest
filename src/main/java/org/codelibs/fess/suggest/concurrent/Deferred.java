package org.codelibs.fess.suggest.concurrent;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.request.Response;

public class Deferred<RESPONSE extends Response> {
    private RESPONSE response = null;

    private Throwable error = null;

    private final Promise promise = new Promise();

    private final Queue<Consumer<RESPONSE>> doneCallbacks = new LinkedBlockingQueue<>();

    private final Queue<Consumer<Throwable>> errorCallbacks = new LinkedBlockingQueue<>();

    private final CountDownLatch latch = new CountDownLatch(1);

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

    public Promise then(final Consumer<RESPONSE> consumer) {
        return promise.then(consumer);
    }

    public Promise error(final Consumer<Throwable> consumer) {
        return promise.error(consumer);
    }

    public Promise promise() {
        return promise;
    }

    public class Promise {
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

        public RESPONSE getResponse() {
            return getResponse(1, TimeUnit.MINUTES);
        }

        public RESPONSE getResponse(final long time, final TimeUnit unit) {
            try {
                final boolean isTimeout = !latch.await(time, unit);
                if (isTimeout) {
                    throw new SuggesterException("Request timeout. time:" + time + " unit:" + unit.name());
                }
                if (error != null) {
                    throw new SuggesterException(error);
                }
                return response;
            } catch (final InterruptedException e) {
                throw new SuggesterException(e);
            }
        }
    }
}
