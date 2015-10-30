package org.codelibs.fess.suggest.request.famouskeys;

import org.codelibs.fess.suggest.request.RequestBuilder;
import org.elasticsearch.client.Client;

public class FamousKeysRequestBuilder extends RequestBuilder<FamousKeysRequest, FamousKeysResponse> {
    public FamousKeysRequestBuilder(final Client client) {
        super(client, new FamousKeysRequest());
    }

    public FamousKeysRequestBuilder setIndex(final String index) {
        request.setIndex(index);
        return this;
    }

    public FamousKeysRequestBuilder setType(final String type) {
        request.setType(type);
        return this;
    }

    public FamousKeysRequestBuilder setSize(final int size) {
        request.setSize(size);
        return this;
    }

    public FamousKeysRequestBuilder addTag(final String tag) {
        request.addTag(tag);
        return this;
    }

    public FamousKeysRequestBuilder addRole(final String role) {
        request.addRole(role);
        return this;
    }

    public FamousKeysRequestBuilder addField(final String field) {
        request.addField(field);
        return this;
    }
}
