package org.codelibs.fess.suggest.request.suggest;

import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.request.RequestBuilder;
import org.elasticsearch.client.Client;

public class SuggestRequestBuilder extends RequestBuilder<SuggestRequest, SuggestResponse> {
    public SuggestRequestBuilder(final Client client, final ReadingConverter readingConverter, final Normalizer normalizer) {
        super(client, new SuggestRequest());
        request.setReadingConverter(readingConverter);
        request.setNormalizer(normalizer);
    }

    public SuggestRequestBuilder setIndex(final String index) {
        request.setIndex(index);
        return this;
    }

    public SuggestRequestBuilder setType(final String type) {
        request.setType(type);
        return this;
    }

    public SuggestRequestBuilder setSize(final int size) {
        request.setSize(size);
        return this;
    }

    public SuggestRequestBuilder setQuery(final String query) {
        request.setQuery(query);
        return this;
    }

    public SuggestRequestBuilder addTag(final String tag) {
        request.addTag(tag);
        return this;
    }

    public SuggestRequestBuilder addRole(final String role) {
        request.addRole(role);
        return this;
    }

    public SuggestRequestBuilder addField(final String field) {
        request.addField(field);
        return this;
    }

    public SuggestRequestBuilder addKind(final String kind) {
        request.addKind(kind);
        return this;
    }

    public SuggestRequestBuilder setSuggestDetail(final boolean suggestDetail) {
        request.setSuggestDetail(suggestDetail);
        return this;
    }

    public SuggestRequestBuilder setPrefixMatchWeight(final float prefixMatchWeight) {
        request.setPrefixMatchWeight(prefixMatchWeight);
        return this;
    }

    public SuggestRequestBuilder setMatchWordFirst(final boolean matchWordFirst) {
        request.setMatchWordFirst(matchWordFirst);
        return this;
    }

    public SuggestRequestBuilder setSkipDuplicateWords(final boolean skipDuplicateWords) {
        request.setSkipDuplicateWords(skipDuplicateWords);
        return this;
    }

    public SuggestRequestBuilder addLang(final String lang) {
        request.addLang(lang);
        return this;
    }
}
