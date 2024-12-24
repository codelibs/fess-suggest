/*
 * Copyright 2012-2024 CodeLibs Project and the Others.
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

import java.util.List;

import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.request.Response;

/**
 * Represents the response for popular words request.
 */
public class PopularWordsResponse implements Response {

    /**
     * The index associated with the response.
     */
    protected final String index;

    /**
     * The time taken to generate the response in milliseconds.
     */
    protected final long tookMs;

    /**
     * The list of popular words.
     */
    protected final List<String> words;

    /**
     * The number of popular words.
     */
    protected final int num;

    /**
     * The total number of words.
     */
    protected final long total;

    /**
     * The list of suggested items.
     */
    protected final List<SuggestItem> items;

    /**
     * Constructs a new PopularWordsResponse.
     *
     * @param index the index associated with the response
     * @param tookMs the time taken to generate the response in milliseconds
     * @param words the list of popular words
     * @param total the total number of words
     * @param items the list of suggested items
     */
    public PopularWordsResponse(final String index, final long tookMs, final List<String> words, final long total,
            final List<SuggestItem> items) {
        this.index = index;
        this.tookMs = tookMs;
        this.words = words;
        num = words.size();
        this.total = total;
        this.items = items;
    }

    /**
     * Returns the index associated with the response.
     *
     * @return the index
     */
    public String getIndex() {
        return index;
    }

    /**
     * Returns the time taken to generate the response in milliseconds.
     *
     * @return the time taken in milliseconds
     */
    public long getTookMs() {
        return tookMs;
    }

    /**
     * Returns the list of popular words.
     *
     * @return the list of popular words
     */
    public List<String> getWords() {
        return words;
    }

    /**
     * Returns the number of popular words.
     *
     * @return the number of popular words
     */
    public int getNum() {
        return num;
    }

    /**
     * Returns the total number of words.
     *
     * @return the total number of words
     */
    public long getTotal() {
        return total;
    }

    /**
     * Returns the list of suggested items.
     *
     * @return the list of suggested items
     */
    public List<SuggestItem> getItems() {
        return items;
    }
}
