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

import java.util.List;

import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.request.Response;

/**
 * Represents a response for a suggest request.
 * This class holds the details of the response including the index, time taken, words, total count, and suggested items.
 */
public class SuggestResponse implements Response {
    /** The index name. */
    protected final String index;

    /** The time taken in milliseconds. */
    protected final long tookMs;

    /** The list of suggested words. */
    protected final List<String> words;

    /** The number of suggested words. */
    protected final int num;

    /** The total number of hits. */
    protected final long total;

    /** The list of suggested items. */
    protected final List<SuggestItem> items;

    /**
     * Constructor for SuggestResponse.
     * @param index The index name.
     * @param tookMs The time taken in milliseconds.
     * @param words The list of suggested words.
     * @param total The total number of hits.
     * @param items The list of suggested items.
     */
    public SuggestResponse(final String index, final long tookMs, final List<String> words, final long total,
            final List<SuggestItem> items) {
        this.index = index;
        this.tookMs = tookMs;
        this.words = words;
        num = words.size();
        this.total = total;
        this.items = items;
    }

    /**
     * Returns the index name.
     * @return The index name.
     */
    public String getIndex() {
        return index;
    }

    /**
     * Returns the time taken in milliseconds.
     * @return The time taken.
     */
    public long getTookMs() {
        return tookMs;
    }

    /**
     * Returns the list of suggested words.
     * @return The list of words.
     */
    public List<String> getWords() {
        return words;
    }

    /**
     * Returns the number of suggested words.
     * @return The number of words.
     */
    public int getNum() {
        return num;
    }

    /**
     * Returns the total number of hits.
     * @return The total number of hits.
     */
    public long getTotal() {
        return total;
    }

    /**
     * Returns the list of suggested items.
     * @return The list of items.
     */
    public List<SuggestItem> getItems() {
        return items;
    }
}
