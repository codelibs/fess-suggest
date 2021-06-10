/*
 * Copyright 2012-2021 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.suggest.request.suggest;

import org.codelibs.fesen.client.Client;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.request.RequestBuilder;

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
