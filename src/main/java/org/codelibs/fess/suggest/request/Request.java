package org.codelibs.fess.suggest.request;

import org.codelibs.fess.suggest.concurrent.SuggestFuture;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.lang3.StringUtils;

public abstract class Request<T extends Response> {
    public SuggestFuture<T> execute(Client client) {
        String error = getValidationError();
        if (StringUtils.isNotBlank(error)) {
            throw new IllegalArgumentException(error);
        }

        SuggestFuture<T> future = new SuggestFuture<>();
        try {
            processRequest(client, future);
        } catch (Throwable e) {
            future.resolve(null, new SuggesterException(e));
        }
        return future;
    }

    protected abstract void processRequest(Client client, SuggestFuture<T> future) throws SuggesterException;

    protected abstract String getValidationError();
}
