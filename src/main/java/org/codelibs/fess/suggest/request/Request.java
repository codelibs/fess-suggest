package org.codelibs.fess.suggest.request;

import org.codelibs.fess.suggest.concurrent.SuggestFuture;
import org.codelibs.fess.suggest.concurrent.SuggestRequestFuture;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.lang3.StringUtils;

public abstract class Request<T extends Response> {
    public SuggestFuture<T> execute(final Client client) {
        final String error = getValidationError();
        if (StringUtils.isNotBlank(error)) {
            throw new IllegalArgumentException(error);
        }

        final SuggestFuture<T> future = new SuggestRequestFuture<>();
        try {
            processRequest(client, future);
        } catch (final Exception e) {
            throw new SuggesterException(e);
        }
        return future;
    }

    protected abstract void processRequest(Client client, SuggestFuture<T> future);

    protected abstract String getValidationError();
}
