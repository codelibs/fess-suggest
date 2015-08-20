package org.codelibs.fess.suggest.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.request.Response;

public class SuggestRequestFuture<T extends Response> implements SuggestFuture<T> {

    protected volatile T response = null;
    protected volatile Throwable failure = null;
    protected CountDownLatch latch;

    public SuggestRequestFuture() {
        latch = new CountDownLatch(1);
    }

    @Override
    public void resolve(final T response, final Throwable failure) {
        this.response = response;
        this.failure = failure;
        latch.countDown();
    }

    @Override
    public T getResponse() {
        try {
            latch.await();
            if (failure != null) {
                throw failure;
            }
            return response;
        } catch (final Throwable t) {
            if (t instanceof SuggesterException) {
                throw (SuggesterException) t;
            } else {
                throw new SuggesterException("Failed to process a request.", t);
            }
        }
    }

    @Override
    public SuggestFuture<T> done(final Consumer<T> consumer) {
        try {
            latch.await();
            if (response != null) {
                consumer.accept(response);
            }
        } catch (final InterruptedException e) {
            failure = e;
        }

        return this;
    }

    @Override
    public SuggestFuture<T> error(final Consumer<Throwable> consumer) {
        try {
            latch.await();
            if (failure != null) {
                consumer.accept(failure);
            }
        } catch (final InterruptedException e) {
            consumer.accept(e);
        }
        return this;
    }

}
