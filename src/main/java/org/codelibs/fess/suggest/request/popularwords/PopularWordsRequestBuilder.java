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
package org.codelibs.fess.suggest.request.popularwords;

import org.codelibs.fesen.client.Client;
import org.codelibs.fess.suggest.request.RequestBuilder;

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

    public PopularWordsRequestBuilder addLanguage(final String lang) {
        request.addLanguage(lang);
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
