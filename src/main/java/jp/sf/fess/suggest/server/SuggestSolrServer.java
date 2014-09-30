package jp.sf.fess.suggest.server;

/*
 * Copyright 2009-2014 the CodeLibs Project and the Others.
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

import jp.sf.fess.suggest.SuggestConstants;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class SuggestSolrServer {
    private static final String IDS = "ids";

    private static final String GET_PATH = "/get";

    private static final String MATCH_ALL_QUERY = "*:*";

    private static final Logger logger = LoggerFactory
        .getLogger(SuggestSolrServer.class);

    private SolrServer solrServer;

    // For Testing
    protected SuggestSolrServer(final String url) { // NOSONAR
        try {
            final HttpSolrServer server = new HttpSolrServer(url);
            server.setConnectionTimeout(10 * 1000);
            server.setMaxRetries(3);
            solrServer = server;
        } catch (final Exception e) {
            logger.warn("Failed to create SuggestSolrServer object.", e);
        }
    }

    public SuggestSolrServer(final SolrServer server) {
        solrServer = server;
    }

    public void add(final SolrInputDocument doc) throws IOException,
        SolrServerException {
        solrServer.add(doc);
    }

    public void add(final List<SolrInputDocument> documents)
        throws IOException, SolrServerException {
        solrServer.add(documents);
    }

    public void commit() throws IOException, SolrServerException {
        solrServer.commit();
    }

    public void deleteAll() throws IOException, SolrServerException {
        solrServer.deleteByQuery(MATCH_ALL_QUERY);
    }

    public void deleteByQuery(final String query) throws IOException,
        SolrServerException {
        solrServer.deleteByQuery(query);
    }

    public SolrDocumentList select(final String query) throws IOException,
        SolrServerException {

        final SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setFields(new String[] {
            SuggestConstants.SuggestFieldNames.ID,
            SuggestConstants.SuggestFieldNames.COUNT,
            SuggestConstants.SuggestFieldNames.LABELS,
            SuggestConstants.SuggestFieldNames.ROLES,
            SuggestConstants.SuggestFieldNames.FIELD_NAME });
        final QueryResponse queryResponse = solrServer.query(solrQuery,
            SolrRequest.METHOD.POST);
        return queryResponse.getResults();
    }

    public SolrDocumentList get(final String ids) throws IOException,
        SolrServerException {
        final SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRequestHandler(GET_PATH);
        solrQuery.set(IDS, ids);
        final QueryResponse response = solrServer.query(solrQuery,
            SolrRequest.METHOD.POST);
        return response.getResults();
    }

    public QueryResponse query(SolrQuery solrQuery, SolrRequest.METHOD method) throws SolrServerException {
        return solrServer.query(solrQuery, method);
    }

    public void shutdown() {
        solrServer.shutdown();
    }
}
