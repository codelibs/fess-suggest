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
package org.codelibs.fess.suggest.request.suggest;

import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.request.RequestBuilder;
import org.opensearch.transport.client.Client;

/**
 * Builder class for constructing {@link SuggestRequest} instances.
 * This builder provides methods to set various parameters for the suggest request.
 */
public class SuggestRequestBuilder extends RequestBuilder<SuggestRequest, SuggestResponse> {
    /**
     * Constructor for SuggestRequestBuilder.
     * @param client The OpenSearch client.
     * @param readingConverter The reading converter.
     * @param normalizer The normalizer.
     */
    public SuggestRequestBuilder(final Client client, final ReadingConverter readingConverter, final Normalizer normalizer) {
        super(client, new SuggestRequest());
        request.setReadingConverter(readingConverter);
        request.setNormalizer(normalizer);
    }

    /**
     * Sets the index for the request.
     * @param index The index name.
     * @return This builder instance.
     */
    public SuggestRequestBuilder setIndex(final String index) {
        request.setIndex(index);
        return this;
    }

    /**
     * Sets the size of results for the request.
     * @param size The size.
     * @return This builder instance.
     */
    public SuggestRequestBuilder setSize(final int size) {
        request.setSize(size);
        return this;
    }

    /**
     * Sets the query string for the request.
     * @param query The query string.
     * @return This builder instance.
     */
    public SuggestRequestBuilder setQuery(final String query) {
        request.setQuery(query);
        return this;
    }

    /**
     * Adds a tag to filter by.
     * @param tag The tag.
     * @return This builder instance.
     */
    public SuggestRequestBuilder addTag(final String tag) {
        request.addTag(tag);
        return this;
    }

    /**
     * Adds a role to filter by.
     * @param role The role.
     * @return This builder instance.
     */
    public SuggestRequestBuilder addRole(final String role) {
        request.addRole(role);
        return this;
    }

    /**
     * Adds a field to filter by.
     * @param field The field name.
     * @return This builder instance.
     */
    public SuggestRequestBuilder addField(final String field) {
        request.addField(field);
        return this;
    }

    /**
     * Adds a kind to filter by.
     * @param kind The kind.
     * @return This builder instance.
     */
    public SuggestRequestBuilder addKind(final String kind) {
        request.addKind(kind);
        return this;
    }

    /**
     * Sets whether to return detailed suggestion information.
     * @param suggestDetail True to return detailed information, false otherwise.
     * @return This builder instance.
     */
    public SuggestRequestBuilder setSuggestDetail(final boolean suggestDetail) {
        request.setSuggestDetail(suggestDetail);
        return this;
    }

    /**
     * Sets the prefix match weight.
     * @param prefixMatchWeight The prefix match weight.
     * @return This builder instance.
     */
    public SuggestRequestBuilder setPrefixMatchWeight(final float prefixMatchWeight) {
        request.setPrefixMatchWeight(prefixMatchWeight);
        return this;
    }

    /**
     * Sets whether to match the first word.
     * @param matchWordFirst True to match the first word, false otherwise.
     * @return This builder instance.
     */
    public SuggestRequestBuilder setMatchWordFirst(final boolean matchWordFirst) {
        request.setMatchWordFirst(matchWordFirst);
        return this;
    }

    /**
     * Sets whether to skip duplicate words.
     * @param skipDuplicateWords True to skip duplicate words, false otherwise.
     * @return This builder instance.
     */
    public SuggestRequestBuilder setSkipDuplicateWords(final boolean skipDuplicateWords) {
        request.setSkipDuplicateWords(skipDuplicateWords);
        return this;
    }

    /**
     * Adds a language to filter by.
     * @param lang The language.
     * @return This builder instance.
     */
    public SuggestRequestBuilder addLang(final String lang) {
        request.addLang(lang);
        return this;
    }
}
