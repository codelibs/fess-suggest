package org.codelibs.fess.suggest.request.famouskeys;

import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.request.Response;

import java.util.List;

public class FamousKeysResponse implements Response {
    protected final String index;

    protected final long tookMs;

    protected final List<String> words;

    protected final int num;

    protected final long total;

    protected final SuggestItem[] items;

    public FamousKeysResponse(final String index, final long tookMs, final List<String> words, final long total, final SuggestItem[] items) {
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

    public SuggestItem[] getItems() {
        return items;
    }
}
