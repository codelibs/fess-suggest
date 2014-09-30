package jp.sf.fess.suggest.entity;

import jp.sf.fess.suggest.entity.SuggestResponse;

import org.apache.solr.client.solrj.response.QueryResponse;

public class SpellCheckResponse extends SuggestResponse {
    public SpellCheckResponse(final QueryResponse queryResponse, final int num,
                              final String query) {
        super(queryResponse, num, query);
    }
}
