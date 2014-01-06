package jp.sf.fess.suggest.index;

import jp.sf.fess.suggest.SuggestConstants;
import jp.sf.fess.suggest.entity.SuggestItem;
import jp.sf.fess.suggest.enums.RequestType;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class IndexUpdater extends Thread {
    private Logger logger = LoggerFactory.getLogger(IndexUpdater.class);

    protected Queue<Request> suggestRequestQueue = new ConcurrentLinkedQueue<Request>();

    protected SuggestSolrServer suggestSolrServer;

    protected AtomicBoolean running = new AtomicBoolean(false);

    protected AtomicLong updateInterval = new AtomicLong(10 * 1000);

    protected AtomicInteger maxUpdateNum = new AtomicInteger(10000);

    public IndexUpdater(SuggestSolrServer suggestSolrServer) {
        this.suggestSolrServer = suggestSolrServer;
    }

    public void setUpdateInterval(long updateInterval) {
        this.updateInterval.set(updateInterval);
    }

    public int getQueuingItemNum() {
        return suggestRequestQueue.size();
    }

    public synchronized void addSuggestItem(SuggestItem item) {
        boolean exist = false;
        Iterator<Request> it = suggestRequestQueue.iterator();
        while (it.hasNext()) {
            Request request = it.next();
            if (request.type == RequestType.ADD) {
                SuggestItem existItem = (SuggestItem) request.obj;
                if (existItem.equals(item)) {
                    exist = true;
                    existItem.setCount(existItem.getCount() + 1);
                    existItem.setExpires(item.getExpires());
                    existItem.setSegment(item.getSegment());
                    List<String> fieldNameList = existItem.getFieldNameList();
                    for (String fieldName : item.getFieldNameList()) {
                        if (!fieldNameList.contains(fieldName)) {
                            fieldNameList.add(fieldName);
                        }
                    }
                    List<String> labelFieldNameList = existItem.getLabels();
                    for (String label : item.getLabels()) {
                        if (!labelFieldNameList.contains(label)) {
                            labelFieldNameList.add(label);
                        }
                    }
                    break;
                }
            }
        }

        if (!exist) {
            Request request = new Request(RequestType.ADD, item);
            suggestRequestQueue.add(request);
            notify();
        }
    }

    public void commit() {
        Request request = new Request(RequestType.COMMIT, null);
        suggestRequestQueue.add(request);
        synchronized (this) {
            notify();
        }
    }

    public void deleteByQuery(String query) {
        Request request = new Request(RequestType.DELETE_BY_QUERY, query);
        suggestRequestQueue.add(request);
        synchronized (this) {
            notify();
        }
    }

    public void close() {
        running.set(false);
    }


    @Override
    public void run() {
        logger.info("Start indexUpdater");
        running.set(true);
        boolean doCommit = false;
        while (running.get()) {
            int maxUpdateNum = this.maxUpdateNum.get();
            Request[] requestArray = new Request[maxUpdateNum];
            int requestNum = 0;
            synchronized (this) {
                for (int i = 0; i < maxUpdateNum; i++) {
                    Request request = suggestRequestQueue.peek();
                    if (request == null) {
                        break;
                    }
                    if (request.type == RequestType.ADD) {
                        requestArray[requestNum] = suggestRequestQueue.poll();
                        requestNum++;
                    } else if (requestNum == 0) {
                        requestArray[requestNum] = suggestRequestQueue.poll();
                        requestNum++;
                        break;
                    } else {
                        break;
                    }
                }
            }

            if (requestNum == 0) {
                try {
                    if (doCommit) {
                        suggestSolrServer.commit();
                        doCommit = false;
                    }
                    try {
                        synchronized (this) {
                            this.wait(updateInterval.get());
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                } catch (Exception e) {
                }
                continue;
            }
            doCommit = true;

            switch (requestArray[0].type) {
                case ADD:
                    long start = System.currentTimeMillis();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Add " + requestNum + "documents");
                    }

                    SuggestItem[] suggestItemArray = new SuggestItem[requestNum];
                    int itemSize = 0;
                    StringBuilder ids = new StringBuilder(10000);
                    for (int i = 0; i < requestNum; i++) {
                        Request request = requestArray[i];
                        SuggestItem item = (((SuggestItem) request.obj));
                        suggestItemArray[itemSize] = item;
                        if (ids.length() > 0) {
                            ids.append(',');
                        }
                        ids.append(item.getDocumentId());
                        itemSize++;
                    }
                    mergeSolrIndex(suggestItemArray, itemSize, ids.toString());

                    List<SolrInputDocument> solrInputDocumentList = new ArrayList<SolrInputDocument>(itemSize);
                    for (int i = 0; i < itemSize; i++) {
                        SuggestItem item = suggestItemArray[i];
                        solrInputDocumentList.add(item.toSolrInputDocument());
                    }
                    try {
                        suggestSolrServer.add(solrInputDocumentList);
                        if (logger.isInfoEnabled()) {
                            logger.info("Done add " + itemSize + " terms. took: " + (System.currentTimeMillis() - start));
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to add document.", e);
                    }
                    break;
                case COMMIT:
                    if (logger.isDebugEnabled()) {
                        logger.debug("Commit.");
                    }
                    try {
                        suggestSolrServer.commit();
                        doCommit = false;
                    } catch (Exception e) {
                        logger.warn("Failed to commit.", e);
                    }
                    break;
                case DELETE_BY_QUERY:
                    if (logger.isInfoEnabled()) {
                        logger.info("DeleteByQuery. query=" + requestArray[0].obj.toString());
                    }
                    try {
                        suggestSolrServer.deleteByQuery((String) requestArray[0].obj);
                        suggestSolrServer.commit();
                    } catch (Exception e) {
                        logger.warn("Failed to deleteByQuery.", e);
                    }
                    break;
                default:
                    break;
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info("Stop IndexUpdater");
            suggestRequestQueue.clear();
        }
    }

    protected void mergeSolrIndex(final SuggestItem[] suggestItemArray, final int itemSize, final String ids) {
        long startTime = System.currentTimeMillis();
        if (itemSize > 0) {
            SolrDocumentList documentList = null;
            try {
                documentList = suggestSolrServer.get(ids);
                if(logger.isDebugEnabled()) {
                    logger.debug("search end. " + "getNum=" + documentList.size() +
                            "  took:" + (System.currentTimeMillis() - startTime));
                }
            } catch (Exception e) {
                logger.warn("Failed merge solr index.", e);
            }
            if (documentList != null) {
                int itemCount = 0;
                for (SolrDocument doc : documentList) {
                    Object idObj = doc.getFieldValue("id");
                    if (idObj == null) {
                        continue;
                    }
                    String id = idObj.toString();

                    for (; itemCount < itemSize; itemCount++) {
                        SuggestItem item = suggestItemArray[itemCount];

                        if (item.getDocumentId().equals(id)) {
                            Object count = doc.getFieldValue(SuggestConstants.SuggestFieldNames.COUNT);
                            if (count != null) {
                                item.setCount(item.getCount() + Long.parseLong(count.toString()));
                            }
                            Collection<Object> labels = doc.getFieldValues(SuggestConstants.SuggestFieldNames.LABELS);
                            if (labels != null) {
                                List<String> itemLabelList = item.getLabels();
                                for (Object label : labels) {
                                    if (!itemLabelList.contains(label.toString())) {
                                        itemLabelList.add(label.toString());
                                    }
                                }
                            }
                            Collection<Object> fields = doc.getFieldValues(SuggestConstants.SuggestFieldNames.FIELD_NAME);
                            if (fields != null) {
                                List<String> fieldNameList = item.getFieldNameList();
                                for (Object field : fields) {
                                    if (!fieldNameList.contains(field.toString())) {
                                        fieldNameList.add(field.toString());
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    protected static class Request {
        public RequestType type;

        public Object obj;

        public Request(RequestType type, Object o) {
            this.type = type;
            this.obj = o;
        }
    }
}
