package org.codelibs.fess.suggest.request.suggest;

import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.request.RequestBuilder;
import org.elasticsearch.client.Client;

public class SuggestRequestBuilder extends RequestBuilder<SuggestRequest, SuggestResponse> {
    public SuggestRequestBuilder(Client client, ReadingConverter readingConverter, Normalizer normalizer) {
        super(client, new SuggestRequest());
        request.setReadingConverter(readingConverter);
        request.setNormalizer(normalizer);
    }

    public SuggestRequestBuilder setIndex(String index) {
        request.setIndex(index);
        return this;
    }

    public SuggestRequestBuilder setType(String type) {
        request.setType(type);
        return this;
    }

    public SuggestRequestBuilder setSize(int size) {
        request.setSize(size);
        return this;
    }

    public SuggestRequestBuilder setQuery(String query) {
        request.setQuery(query);
        return this;
    }

    public SuggestRequestBuilder addTag(String tag) {
        request.addTag(tag);
        return this;
    }

    public SuggestRequestBuilder addRole(String role) {
        request.addRole(role);
        return this;
    }

    public SuggestRequestBuilder setSuggestDetail(boolean suggestDetail) {
        request.setSuggestDetail(suggestDetail);
        return this;
    }

}
