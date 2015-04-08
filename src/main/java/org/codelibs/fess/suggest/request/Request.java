package org.codelibs.fess.suggest.request;

import org.codelibs.fess.suggest.exception.SuggesterException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.lang3.StringUtils;

public abstract class Request<T extends Response> {
    public T execute(Client client) throws SuggesterException {
        String error = getValidationError();
        if (StringUtils.isNotBlank(error)) {
            throw new IllegalArgumentException(error);
        }

        return processRequest(client);
    }

    protected abstract T processRequest(Client client) throws SuggesterException;

    protected abstract String getValidationError();
}
