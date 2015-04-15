package org.codelibs.fess.suggest.index.contents.querylog;

import org.elasticsearch.common.Nullable;

public class QueryLog {
    private final String q;
    private final String fq;

    public QueryLog(final String queryString, @Nullable final String filterQueryString) {
        this.q = queryString;
        this.fq = filterQueryString;
    }

    public String getQueryString() {
        return q;
    }

    public String getFilterQueryString() {
        return fq;
    }
}
