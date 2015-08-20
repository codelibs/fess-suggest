package org.codelibs.fess.suggest.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.codelibs.core.misc.Pair;
import org.codelibs.fess.suggest.exception.SuggestIndexException;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.index.SuggestIndexResponse;

public class SuggestIndexFuture implements SuggestFuture<SuggestIndexResponse> {
    protected final Future<Pair<SuggestIndexResponse, Throwable>> future;
    protected SuggestIndexResponse response = null;
    protected Throwable failure = null;

    public SuggestIndexFuture(final Future<Pair<SuggestIndexResponse, Throwable>> future) {
        this.future = future;
    }

    protected void processFuture() {
        if (response == null && failure == null) {
            try {
                final Pair<SuggestIndexResponse, Throwable> result = future.get();
                resolve(result.getFirst(), result.getSecond());
            } catch (InterruptedException | ExecutionException e) {
                throw new SuggestIndexException("Failed to execute indexing process.", e);
            }
        }
    }

    @Override
    public SuggestIndexResponse getResponse() {
        processFuture();
        if (failure != null) {
            if (failure instanceof SuggesterException) {
                throw (SuggesterException) failure;
            } else {
                throw new SuggesterException("Failed to process the index.", failure);
            }
        }
        return response;
    }

    @Override
    public SuggestFuture<SuggestIndexResponse> done(final Consumer<SuggestIndexResponse> consumer) {
        processFuture();
        if (response != null) {
            try {
                consumer.accept(response);
            } catch (Throwable t) {
                failure = t;
            }
        }
        return this;
    }

    @Override
    public SuggestFuture<SuggestIndexResponse> error(final Consumer<Throwable> consumer) {
        processFuture();
        if (failure != null) {
            consumer.accept(failure);
        }
        return this;
    }

    @Override
    public void resolve(final SuggestIndexResponse response, final Throwable failure) {
        this.response = response;
        this.failure = failure;
    }

}
