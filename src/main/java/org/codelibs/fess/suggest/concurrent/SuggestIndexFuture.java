package org.codelibs.fess.suggest.concurrent;

import org.codelibs.fess.suggest.index.SuggestIndexResponse;

import java.util.concurrent.Future;

public class SuggestIndexFuture extends SuggestFuture<SuggestIndexResponse> {
    public volatile Future future = null;

    @Override
    public boolean cancel() {
        return future != null && future.cancel(true);
    }
}
