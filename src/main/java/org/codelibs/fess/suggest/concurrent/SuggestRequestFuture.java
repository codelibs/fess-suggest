package org.codelibs.fess.suggest.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.request.Response;

public class SuggestRequestFuture<T extends Response> implements SuggestFuture<T> {

    protected volatile T response = null;
    protected volatile Throwable failure = null;
    protected final AtomicBoolean got = new AtomicBoolean(false);
    protected final CountDownLatch latch;
    protected final CountDownLatch doneLatch;
    protected final CountDownLatch errorLatch;

    protected Consumer<T> doneConsumer;
    protected Consumer<Throwable> errorConsumer;

    public SuggestRequestFuture() {
        latch = new CountDownLatch(1);
        doneLatch = new CountDownLatch(1);
        errorLatch = new CountDownLatch(1);
    }

    @Override
    public void resolve(final T response, final Throwable failure) {
        this.response = response;
        this.failure = failure;
        latch.countDown();

        if (got.get()) {
            return;
        }

        try {
            if (response != null) {
                doneLatch.await(10, TimeUnit.SECONDS);
                if (doneConsumer != null) {
                    doneConsumer.accept(response);
                }
            } else if (failure != null) {
                errorLatch.await(10, TimeUnit.SECONDS);
                if (errorConsumer != null) {
                    errorConsumer.accept(failure);
                }
            }
        } catch (InterruptedException ignore) {}
    }

    @Override
    public T getResponse() {
        got.set(true);
        try {
            latch.await();
            if (failure != null) {
                throw failure;
            }
            return response;
        } catch (final SuggesterException t) {
            throw t;
        } catch (final Throwable t) {
            throw new SuggesterException("Failed to process a request.", t);
        }
    }

    @Override
    public SuggestFuture<T> done(final Consumer<T> consumer) {
        doneConsumer = consumer;
        doneLatch.countDown();
        return this;
    }

    @Override
    public SuggestFuture<T> error(final Consumer<Throwable> consumer) {
        errorConsumer = consumer;
        errorLatch.countDown();
        return this;
    }

}
