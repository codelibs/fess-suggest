/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
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

import org.codelibs.fess.suggest.request.RequestBuilder;
import org.opensearch.transport.client.Client;

/**
 * Builder for creating a {@link PopularWordsRequest} to fetch popular words.
 * This builder provides methods to set various parameters for the request.
 */
public class PopularWordsRequestBuilder extends RequestBuilder<PopularWordsRequest, PopularWordsResponse> {
    /**
     * Constructor for PopularWordsRequestBuilder.
     * @param client The OpenSearch client.
     */
    public PopularWordsRequestBuilder(final Client client) {
        super(client, new PopularWordsRequest());
    }

    /**
     * Sets the index for the request.
     * @param index The index name.
     * @return This builder instance.
     */
    public PopularWordsRequestBuilder setIndex(final String index) {
        request.setIndex(index);
        return this;
    }

    /**
     * Sets the size of results for the request.
     * @param size The size.
     * @return This builder instance.
     */
    public PopularWordsRequestBuilder setSize(final int size) {
        request.setSize(size);
        return this;
    }

    /**
     * Adds a tag to filter by.
     * @param tag The tag.
     * @return This builder instance.
     */
    public PopularWordsRequestBuilder addTag(final String tag) {
        request.addTag(tag);
        return this;
    }

    /**
     * Adds a role to filter by.
     * @param role The role.
     * @return This builder instance.
     */
    public PopularWordsRequestBuilder addRole(final String role) {
        request.addRole(role);
        return this;
    }

    /**
     * Adds a field to filter by.
     * @param field The field name.
     * @return This builder instance.
     */
    public PopularWordsRequestBuilder addField(final String field) {
        request.addField(field);
        return this;
    }

    /**
     * Adds a language to filter by.
     * @param lang The language.
     * @return This builder instance.
     */
    public PopularWordsRequestBuilder addLanguage(final String lang) {
        request.addLanguage(lang);
        return this;
    }

    /**
     * Sets the seed for random function.
     * @param seed The seed.
     * @return This builder instance.
     */
    public PopularWordsRequestBuilder setSeed(final String seed) {
        request.setSeed(seed);
        return this;
    }

    /**
     * Sets the window size for rescoring.
     * @param windowSize The window size.
     * @return This builder instance.
     */
    public PopularWordsRequestBuilder setWindowSize(final int windowSize) {
        request.setWindowSize(windowSize);
        return this;
    }

    /**
     * Adds an exclude word.
     * @param excludeWord The word to exclude.
     * @return This builder instance.
     */
    public PopularWordsRequestBuilder addExcludeWord(final String excludeWord) {
        request.addExcludeWord(excludeWord);
        return this;
    }

    /**
     * Sets the query frequency threshold.
     * @param queryFreqThreshold The query frequency threshold.
     * @return This builder instance.
     */
    public PopularWordsRequestBuilder setQueryFreqThreshold(final int queryFreqThreshold) {
        request.setQueryFreqThreshold(queryFreqThreshold);
        return this;
    }
}
