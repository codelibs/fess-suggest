package org.codelibs.fess.suggest.request;

import org.codelibs.fess.suggest.concurrent.SuggestFuture;
import org.elasticsearch.client.Client;

public abstract class RequestBuilder<Req extends Request<Res>, Res extends Response> {
    protected Client client;
    protected Req request;

    public RequestBuilder(final Client client, final Req request) {
        this.client = client;
        this.request = request;
    }

    public SuggestFuture<Res> execute() {
        return request.execute(client);
    }
}
