package jp.sf.fess.suggest.service;

import jp.sf.fess.suggest.SpellChecker;
import jp.sf.fess.suggest.SuggestConstants;
import jp.sf.fess.suggest.Suggester;
import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.entity.SpellCheckResponse;
import jp.sf.fess.suggest.entity.SuggestItem;
import jp.sf.fess.suggest.entity.SuggestResponse;
import jp.sf.fess.suggest.exception.FessSuggestException;
import jp.sf.fess.suggest.index.IndexUpdater;
import jp.sf.fess.suggest.server.SuggestSolrServer;
import jp.sf.fess.suggest.util.SuggestUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.util.DateUtil;
import org.codelibs.core.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class SuggestService {
    private static final Logger logger = LoggerFactory.getLogger(SuggestService.class);

    protected Suggester suggester;

    protected SpellChecker spellChecker;

    protected IndexUpdater indexUpdater;

    protected SuggestSolrServer suggestSolrServer;

    public String[] supportedFields = {"content"};

    public String labelFieldName = "label";

    public String roleFieldName = "role";

    public int defaultSearchLogExpires = 30;

    public long updateInterval = 1 * 1000;

    public SuggestService(Suggester suggester, SpellChecker spellChecker, SuggestSolrServer suggestSolrServer) {
        this.suggester = suggester;
        this.spellChecker = spellChecker;
        this.suggestSolrServer = suggestSolrServer;
        this.indexUpdater = new IndexUpdater(suggestSolrServer);
        indexUpdater.setUpdateInterval(updateInterval);
        indexUpdater.start();
    }

    public void shutdown() {
        indexUpdater.close();
        try {
            indexUpdater.join();
        } catch (InterruptedException e) {
            logger.info("Interrupted suggestService.");
        }
    }

    public SuggestResponse getSuggestResponse(final String q,
                                              final List<String> fieldNames,
                                              final List<String> labels,
                                              final List<String> roleList,
                                              final int rows) {

        final String suggestQuery = suggester.buildSuggestQuery(q, fieldNames,
            labels, roleList);

        final long startTime = System.currentTimeMillis();

        QueryResponse queryResponse = null;
        final SolrQuery solrQuery = new SolrQuery();
        if (StringUtil.isNotBlank(suggestQuery)) {
            // query
            solrQuery.setQuery(suggestQuery);
            // size
            solrQuery.setRows(rows);
            //sort
            solrQuery.addSort(SuggestConstants.SuggestFieldNames.BOOST,
                SolrQuery.ORDER.desc);
            solrQuery.addSort(SuggestConstants.SuggestFieldNames.COUNT,
                SolrQuery.ORDER.desc);

            try {
                queryResponse = suggestSolrServer.query(solrQuery,
                    SolrRequest.METHOD.POST);
            } catch (SolrServerException e) {
                logger.warn("Failed to request.", e);
            }
        }
        final long execTime = System.currentTimeMillis() - startTime;
        final SuggestResponse suggestResponse = new SuggestResponse(
            queryResponse, rows, q);
        suggestResponse.setExecTime(execTime);
        return suggestResponse;
    }

    public SpellCheckResponse getSpellCheckResponse(final String q,
                                                    final List<String> fieldNames,
                                                    final List<String> labels,
                                                    final List<String> roleList,
                                                    final int rows) {
        final String spellCheckQuery = spellChecker.buildSpellCheckQuery(q,
            fieldNames, labels, roleList);

        final long startTime = System.currentTimeMillis();

        QueryResponse queryResponse = null;
        final SolrQuery solrQuery = new SolrQuery();
        if (StringUtil.isNotBlank(spellCheckQuery)) {
            // query
            solrQuery.setQuery(spellCheckQuery);
            // size
            solrQuery.setRows(rows);
            //sort
            solrQuery.setSort(SuggestConstants.SuggestFieldNames.COUNT,
                SolrQuery.ORDER.desc);

            try {
                queryResponse = suggestSolrServer.query(solrQuery,
                    SolrRequest.METHOD.POST);
            } catch (SolrServerException e) {
                logger.warn("Failed to request.", e);
            }
        }
        final long execTime = System.currentTimeMillis() - startTime;
        final SpellCheckResponse spellCheckResponse = new SpellCheckResponse(
            queryResponse, rows, q);
        spellCheckResponse.setExecTime(execTime);
        return spellCheckResponse;
    }

    public void addElevateWord(String word, String reading, List<String> labels,
                               List<String> roles, long boost) {
        SuggestItem item = new SuggestItem();
        item.setText(word);

        final String readingTarget;
        if (StringUtils.isNotBlank(reading)) {
            readingTarget = reading;
        } else {
            readingTarget = word;
        }

        SuggestReadingConverter converter = suggester.getConverter();
        List<String> readings = converter.convert(readingTarget);
        for (String r : readings) {
            item.addReading(r);
        }

        for (String field : supportedFields) {
            item.addFieldName(field);
        }

        item.setLabels(labels);
        item.setRoles(roles);

        item.setSegment(SuggestConstants.SEGMENT_ELEVATE);
        item.setSegmentField(SuggestConstants.SuggestFieldNames.SEGMENT);

        item.setCount(0);
        item.setUserBoost(boost);
        indexUpdater.addSuggestItem(item);
    }

    public void deleteAllElevateWords() {
        try {
            suggestSolrServer.deleteByQuery(SuggestConstants.SuggestFieldNames.SEGMENT + ":"
                + SuggestConstants.SEGMENT_ELEVATE);
        } catch (Exception e) {
            logger.warn("Failed to delete elevate words.", e);
        }
    }

    public void deleteBadWord(String word) {
        try {
            suggestSolrServer.deleteByQuery(SuggestConstants.SuggestFieldNames.TEXT + ":"
                + ClientUtils.escapeQueryChars(word));
        } catch (Exception e) {
            logger.warn("Failed to delete BadWord", e);
        }
    }


    public void addSolrParams(String solrParams) {
        addSolrParams(solrParams, defaultSearchLogExpires);
    }


    public void addSolrParams(String solrParams, int dayForCleanup) {
        Map<String, String> paramMap = SuggestUtil.parseSolrParams(solrParams);
        String q = paramMap.get("q");
        String fq = paramMap.get("fq");

        if (StringUtils.isBlank(q)) {
            return;
        }

        List<String> labels = null;
        if (q != null && StringUtils.isNotBlank(labelFieldName)) {
            labels = SuggestUtil.parseQuery(q, labelFieldName);
        }

        List<String> roles = null;
        if (fq != null && StringUtils.isNotBlank(roleFieldName)) {
            roles = SuggestUtil.parseQuery(fq, roleFieldName);
        }

        StringBuilder sb = new StringBuilder(30);
        SuggestReadingConverter converter = suggester.getConverter();
        for (String field : supportedFields) {
            List<String> words = SuggestUtil.parseQuery(q, field);
            if (words.isEmpty()) {
                continue;
            }

            sb.setLength(0);
            SuggestItem item = new SuggestItem();
            item.addFieldName(field);
            for (String word : words) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(word);
                List<String> readings = converter.convert(word);
                for (String reading : readings) {
                    item.addReading(reading);
                }
            }
            item.setText(sb.toString());
            if (labels != null) {
                item.setLabels(labels);
            }
            if (roles != null) {
                item.setRoles(roles);
            }

            if (dayForCleanup >= 0) {
                String dayForCleanupStr = DateUtil.getThreadLocalDateFormat().format(new Date(getExpiredTimestamp(dayForCleanup)));
                item.setExpires(dayForCleanupStr);
                item.setExpiresField(SuggestConstants.SuggestFieldNames.EXPIRES);
            }
            item.setSegment(SuggestConstants.SEGMENT_QUERY);
            item.setSegmentField(SuggestConstants.SuggestFieldNames.SEGMENT);

            indexUpdater.addSuggestItem(item);
        }
    }

    public void commit() {
        indexUpdater.commit();
    }

    public long getDocumentNum() {
        return getDocumentNum("*:*");
    }

    public long getContentDocumentNum() {
        return getDocumentNum("*:* NOT " + SuggestConstants.SuggestFieldNames.SEGMENT + ":" + SuggestConstants.SEGMENT_QUERY
        + " NOT " + SuggestConstants.SuggestFieldNames.SEGMENT + ":" + SuggestConstants.SEGMENT_ELEVATE);
    }

    public long getSearchLogDocumentNum() {
        return getDocumentNum(SuggestConstants.SuggestFieldNames.SEGMENT + ":" + SuggestConstants.SEGMENT_QUERY);
    }

    public long getElevateDocumentNum() {
        return getDocumentNum(SuggestConstants.SuggestFieldNames.SEGMENT + ":" + SuggestConstants.SEGMENT_ELEVATE);
    }

    protected long getDocumentNum(String query) {
        QueryResponse queryResponse = null;
        try {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);
            queryResponse = suggestSolrServer.query(solrQuery, SolrRequest.METHOD.POST);
        } catch (SolrServerException e) {
            return -1;
        }

        return queryResponse.getResults().getNumFound();
    }

    public String getLabelFieldName() {
        return labelFieldName;
    }

    public void setLabelFieldName(String labelFieldName) {
        this.labelFieldName = labelFieldName;
    }

    public String getRoleFieldName() {
        return roleFieldName;
    }

    public void setRoleFieldName(String roleFieldName) {
        this.roleFieldName = roleFieldName;
    }

    protected long getExpiredTimestamp(final int days) {
        return DateUtils.addDays(new Date(), days).getTime();
    }

    public SuggestSolrServer getSuggestSolrServer() {
        return suggestSolrServer;
    }
}
