package jp.sf.fess.suggest.index;


import jp.sf.fess.suggest.SuggestConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.codelibs.solr.lib.server.SolrLibHttpSolrServer;
import org.codelibs.solr.lib.server.interceptor.PreemptiveAuthInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class SuggestSolrServer {
    private static final Logger logger = LoggerFactory.getLogger(SuggestSolrServer.class);

    private SolrLibHttpSolrServer server;

    public SuggestSolrServer(String url) {
        try {
            server = new SolrLibHttpSolrServer(url);
            server.setConnectionTimeout(10 * 1000);
            server.setMaxRetries(3);
        } catch (Exception e) {
            logger.warn("Failed to create SuggestSolrServer object.", e);
        }
    }

    public SuggestSolrServer(String url, String user, String password) {
        try {
            server = new SolrLibHttpSolrServer(url);
            server.setConnectionTimeout(10 * 1000);
            server.setMaxRetries(3);

            if (StringUtils.isNotBlank(user)) {
                URL u = new URL(url);
                AuthScope authScope = new AuthScope(u.getHost(), u.getPort());
                Credentials credentials = new UsernamePasswordCredentials(user, password);
                server.setCredentials(authScope, credentials);
                server.addRequestInterceptor(new PreemptiveAuthInterceptor());
            }
        } catch (Exception e) {
            logger.warn("Failed to create SuggestSolrServer object.", e);
        }

        //TODO その他設定
    }

    public void add(SolrInputDocument doc) throws IOException, SolrServerException {
        server.add(doc);
    }

    public void add(List<SolrInputDocument> documents) throws IOException, SolrServerException {
        server.add(documents);
    }

    public void commit() throws IOException, SolrServerException {
        server.commit();
    }

    public void deleteAll() throws IOException, SolrServerException {
        server.deleteByQuery("*:*");
    }

    public void deleteByQuery(String query) throws IOException, SolrServerException {
        server.deleteByQuery(query);
    }

    public SolrDocumentList select(String query) throws IOException, SolrServerException {

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setFields(new String[]{"id", SuggestConstants.SuggestFieldNames.COUNT,
                SuggestConstants.SuggestFieldNames.LABELS, SuggestConstants.SuggestFieldNames.FIELD_NAME});
        QueryResponse queryResponse = server.query(solrQuery,
                SolrRequest.METHOD.POST);
        SolrDocumentList responseList = queryResponse.getResults();
        return responseList;
    }

    public SolrDocumentList get(String ids) throws IOException, SolrServerException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setRequestHandler("/get");
        solrQuery.set("ids", ids);
        QueryResponse response = server.query(solrQuery, SolrRequest.METHOD.POST);
        SolrDocumentList responseList = response.getResults();
        return responseList;
    }
}
