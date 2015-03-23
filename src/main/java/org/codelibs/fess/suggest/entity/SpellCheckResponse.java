package org.codelibs.fess.suggest.entity;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.codelibs.fess.suggest.entity.SuggestResponse;

public class SpellCheckResponse extends SuggestResponse {
    public SpellCheckResponse(final QueryResponse queryResponse, final int num, final String query) {
        super(queryResponse, num, query);
    }
}
