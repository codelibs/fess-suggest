package org.codelibs.fess.suggest.request;

import org.codelibs.fess.suggest.exception.SuggestorException;
import org.elasticsearch.client.Client;

public abstract class RequestBuilder<Req extends Request<Res>, Res extends Response> {
    protected Client client;
    protected Req request;

    public RequestBuilder(Client client, Req request) {
        this.client = client;
        this.request = request;
    }

    public Res execute() throws SuggestorException {
        return request.execute(client);
    }
}
