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
package org.codelibs.fess.suggest.index.contents.querylog;

import org.opensearch.common.Nullable;

/**
 * The QueryLog class represents a log entry containing a query string and an optional filter query string.
 */
public class QueryLog {
    private final String q;
    private final String fq;

    /**
     * Constructor for QueryLog.
     * @param queryString The query string.
     * @param filterQueryString The filter query string (can be null).
     */
    public QueryLog(final String queryString, @Nullable final String filterQueryString) {
        q = queryString;
        fq = filterQueryString;
    }

    /**
     * Returns the query string.
     * @return The query string.
     */
    public String getQueryString() {
        return q;
    }

    /**
     * Returns the filter query string.
     * @return The filter query string.
     */
    public String getFilterQueryString() {
        return fq;
    }
}
