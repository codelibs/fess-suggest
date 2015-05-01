package org.codelibs.fess.suggest.concurrent;

import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.request.Response;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SuggestFuture<T extends Response> {

    protected volatile SuggesterException failure = null;
    protected volatile ResponseListener<T> listener = null;
    protected volatile FailureListener failureListener = null;
    protected volatile T response = null;
    protected volatile CountDownLatch latch = null;

    public boolean cancel() {
        throw new UnsupportedOperationException();
    }

    public T getResponse() throws SuggesterException {
        if (response == null && failure == null) {
            if (latch == null) {
                latch = new CountDownLatch(1);
            }
            try {
                latch.await(SuggestConstants.ACTION_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new SuggesterException(e);
            }
        }

        if (failure != null) {
            throw failure;
        }
        return response;
    }

    public SuggestFuture<T> done(final ResponseListener<T> listener) {
        this.listener = listener;
        if (response != null) {
            listener.onResponse(response);
        }
        return this;
    }

    public SuggestFuture<T> error(final FailureListener failureListener) {
        this.failureListener = failureListener;
        if (failure != null) {
            failureListener.onFaulure(failure);
        }
        return this;
    }

    public void resolve(final T response, final SuggesterException failure) {
        this.response = response;
        this.failure = failure;
        if (latch != null) {
            latch.countDown();
        } else {
            if (listener != null && response != null) {
                this.listener.onResponse(response);
            } else if (failureListener != null && failure != null) {
                failureListener.onFaulure(failure);
            }
        }
    }

    public interface ResponseListener<T extends Response> {
        void onResponse(T response);
    }

    public interface FailureListener {
        void onFaulure(SuggesterException t);
    }
}
