package org.codelibs.fess.suggest.request;

import com.google.common.base.Strings;
import org.codelibs.fess.suggest.concurrent.Deferred;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.elasticsearch.client.Client;

public abstract class Request<T extends Response> {
    public Deferred<T>.Promise execute(final Client client) {
        final String error = getValidationError();
        if (!Strings.isNullOrEmpty(error)) {
            throw new IllegalArgumentException(error);
        }

        final Deferred<T> deferred = new Deferred<>();
        try {
            processRequest(client, deferred);
        } catch (final Exception e) {
            throw new SuggesterException(e);
        }
        return deferred.promise();
    }

    protected abstract void processRequest(Client client, Deferred<T> deferred);

    protected abstract String getValidationError();
}
