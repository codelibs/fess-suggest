package org.codelibs.fess.suggest.request.popularwords;

import org.codelibs.fess.suggest.request.RequestBuilder;
import org.elasticsearch.client.Client;

public class PopularWordsRequestBuilder extends RequestBuilder<PopularWordsRequest, PopularWordsResponse> {
    public PopularWordsRequestBuilder(final Client client) {
        super(client, new PopularWordsRequest());
    }

    public PopularWordsRequestBuilder setIndex(final String index) {
        request.setIndex(index);
        return this;
    }

    public PopularWordsRequestBuilder setType(final String type) {
        request.setType(type);
        return this;
    }

    public PopularWordsRequestBuilder setSize(final int size) {
        request.setSize(size);
        return this;
    }

    public PopularWordsRequestBuilder addTag(final String tag) {
        request.addTag(tag);
        return this;
    }

    public PopularWordsRequestBuilder addRole(final String role) {
        request.addRole(role);
        return this;
    }

    public PopularWordsRequestBuilder addField(final String field) {
        request.addField(field);
        return this;
    }

    public PopularWordsRequestBuilder setSeed(final String seed) {
        request.setSeed(seed);
        return this;
    }

    public PopularWordsRequestBuilder setWindowSize(final int windowSize) {
        request.setWindowSize(windowSize);
        return this;
    }

    public PopularWordsRequestBuilder addExcludeWord(final String excludeWord) {
        request.addExcludeWord(excludeWord);
        return this;
    }

    public PopularWordsRequestBuilder setQueryFreqThreshold(final int queryFreqThreshold) {
        request.setQueryFreqThreshold(queryFreqThreshold);
        return this;
    }
}
