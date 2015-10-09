package org.codelibs.fess.suggest.request.suggest;

import java.util.List;

import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.request.Response;

public class SuggestResponse implements Response {
    protected final String index;

    protected final long tookMs;

    protected final List<String> words;

    protected final int num;

    protected final long total;

    protected final List<SuggestItem> items;

    public SuggestResponse(final String index, final long tookMs, final List<String> words, final long total, final List<SuggestItem> items) {
        this.index = index;
        this.tookMs = tookMs;
        this.words = words;
        this.num = words.size();
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
