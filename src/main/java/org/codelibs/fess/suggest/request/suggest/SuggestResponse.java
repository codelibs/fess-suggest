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
    protected final String index;

    protected final long tookMs;

    protected final List<String> words;

    protected final int num;

    protected final long total;

    protected final List<SuggestItem> items;

    public SuggestResponse(final String index, final long tookMs, final List<String> words, final long total,
            final List<SuggestItem> items) {
        this.index = index;
        this.tookMs = tookMs;
        this.words = words;
        num = words.size();
        this.total = total;
        this.items = items;
    }

    public String getIndex() {
        return index;
    }

    public long getTookMs() {
        return tookMs;
    }

    public List<String> getWords() {
        return words;
    }

    public int getNum() {
        return num;
    }

    public long getTotal() {
        return total;
    }

    public List<SuggestItem> getItems() {
        return items;
    }
}
