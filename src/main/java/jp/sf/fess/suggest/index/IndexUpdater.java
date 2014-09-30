package jp.sf.fess.suggest.index;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import jp.sf.fess.suggest.SuggestConstants;

import jp.sf.fess.suggest.entity.SuggestItem;
import jp.sf.fess.suggest.enums.RequestType;
import jp.sf.fess.suggest.server.SuggestSolrServer;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexUpdater extends Thread {
    private static final Logger logger = LoggerFactory
        .getLogger(IndexUpdater.class);

    // TODO BlockingQueue?
    protected Queue<Request> suggestRequestQueue = new ConcurrentLinkedQueue<Request>();

    protected final SuggestSolrServer suggestSolrServer;

    protected AtomicBoolean running = new AtomicBoolean(false);

    protected long updateInterval = 10 * 1000;

    protected int maxUpdateNum = 10000;

    protected int maxIDsBufferCapacity = 100000;

    public IndexUpdater(final SuggestSolrServer suggestSolrServer) {
        super("SuggestIndexUpdater");
        this.suggestSolrServer = suggestSolrServer;
    }

    public void setUpdateInterval(final long updateInterval) {
        this.updateInterval = updateInterval;
    }

    public int getQueuingItemNum() {
        return suggestRequestQueue.size();
    }

    public void addSuggestItem(final SuggestItem item) {
        final Request request = new Request(RequestType.ADD, item);
        suggestRequestQueue.add(request);
        synchronized (suggestSolrServer) {
            suggestSolrServer.notify();
        }
    }

    public void commit() {
        final Request request = new Request(RequestType.COMMIT, null);
        suggestRequestQueue.add(request);
        synchronized (suggestSolrServer) {
            suggestSolrServer.notify();
        }
    }

    public void deleteByQuery(final String query) {
        final Request request = new Request(RequestType.DELETE_BY_QUERY, query);
        suggestRequestQueue.add(request);
        synchronized (suggestSolrServer) {
            suggestSolrServer.notify();
        }
    }

    public void close() {
        running.set(false);
        synchronized (suggestSolrServer) {
            suggestSolrServer.notify();
        }
        suggestSolrServer.shutdown();
    }

    @Override
    public void run() {
        if (logger.isInfoEnabled()) {
            logger.info("Start indexUpdater");
        }

        StringBuilder ids = new StringBuilder(maxIDsBufferCapacity);
        final Request[] requestArray = new Request[maxUpdateNum];
        final SuggestItem[] suggestItemArray = new SuggestItem[maxUpdateNum];
        running.set(true);
        while (running.get()) {
            int requestNum = 0;
            for (int i = 0; i < maxUpdateNum; i++) {
                final Request request = suggestRequestQueue.peek();
                if (request == null) {
                    break;
                } else if (request.type == RequestType.ADD) {
                    suggestRequestQueue.poll();

                    //merge duplicate items
                    boolean exist = false;
                    final SuggestItem item2 = (SuggestItem) request.obj;
                    for (int j = 0; j < requestNum; j++) {
                        final SuggestItem item1 = (SuggestItem) requestArray[j].obj;
                        if (item1.equals(item2)) {
                            mergeSuggestItem(item1, item2);
                            exist = true;
                            break;
                        }
                    }
                    if (!exist) {
                        requestArray[requestNum] = request;
                        requestNum++;
                    }
                } else if (requestNum == 0) {
                    requestArray[requestNum] = suggestRequestQueue.poll();
                    requestNum++;
                    break;
                } else {
                    break;
                }
            }

            if (requestNum == 0) {
                if (ids.capacity() > maxIDsBufferCapacity) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Clear IDs buffer string.");
                    }
                    ids = new StringBuilder(maxIDsBufferCapacity);
                }
                try {
                    //wait next item...
                    synchronized (suggestSolrServer) {
                        suggestSolrServer.wait(updateInterval);
                    }
                } catch (final InterruptedException e) {
                    break;
                } catch (final Exception e) {
                    //ignore
                }
                continue;
            }

            switch (requestArray[0].type) {
                case ADD:
                    final long start = System.currentTimeMillis();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Add " + requestNum + " documents");
                    }

                    int itemSize = 0;

                    ids.setLength(0);
                    for (int i = 0; i < requestNum; i++) {
                        final Request request = requestArray[i];
                        final SuggestItem item = (SuggestItem) request.obj;
                        suggestItemArray[itemSize] = item;
                        if (ids.length() > 0) {
                            ids.append(',');
                        }
                        ids.append(item.getDocumentId());
                        itemSize++;
                    }

                    mergeSolrIndex(suggestItemArray, itemSize, ids.toString());

                    final List<SolrInputDocument> solrInputDocumentList = new ArrayList<SolrInputDocument>(
                        itemSize);
                    for (int i = 0; i < itemSize; i++) {
                        final SuggestItem item = suggestItemArray[i];
                        solrInputDocumentList.add(item.toSolrInputDocument());
                    }
                    try {
                        suggestSolrServer.add(solrInputDocumentList);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Done add " + itemSize + " terms. took: "
                                + (System.currentTimeMillis() - start));
                        }
                    } catch (final Exception e) {
                        logger.warn("Failed to add document.", e);
                    }
                    break;
                case COMMIT:
                    if (logger.isDebugEnabled()) {
                        logger.debug("Commit.");
                    }
                    try {
                        suggestSolrServer.commit();
                    } catch (final Exception e) {
                        logger.warn("Failed to commit.", e);
                    }
                    break;
                case DELETE_BY_QUERY:
                    if (logger.isInfoEnabled()) {
                        logger.info("DeleteByQuery. query="
                            + requestArray[0].obj.toString());
                    }
                    try {
                        suggestSolrServer
                            .deleteByQuery((String) requestArray[0].obj);
                        suggestSolrServer.commit();
                    } catch (final Exception e) {
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

    protected void mergeSolrIndex(final SuggestItem[] suggestItemArray,
                                  final int itemSize, final String ids) {
        final long startTime = System.currentTimeMillis();
        if (itemSize > 0) {
            SolrDocumentList documentList = null;
            try {
                documentList = suggestSolrServer.get(ids);
                if (logger.isDebugEnabled()) {
                    logger.debug("search end. " + "getNum="
                        + documentList.size() + "  took:"
                        + (System.currentTimeMillis() - startTime));
                }
            } catch (final Exception e) {
                logger.warn("Failed merge solr index.", e);
            }
            if (documentList != null) {
                int itemCount = 0;
                for (final SolrDocument doc : documentList) {
                    final Object idObj = doc
                        .getFieldValue(SuggestConstants.SuggestFieldNames.ID);
                    if (idObj == null) {
                        continue;
                    }
                    final String id = idObj.toString();

                    for (; itemCount < itemSize; itemCount++) {
                        final SuggestItem item = suggestItemArray[itemCount];

                        if (item.getDocumentId().equals(id)) {
                            final Object count = doc
                                .getFieldValue(SuggestConstants.SuggestFieldNames.COUNT);
                            if (count != null) {
                                item.setCount(item.getCount()
                                    + Long.parseLong(count.toString()));
                            }
                            final Collection<Object> labels = doc
                                .getFieldValues(SuggestConstants.SuggestFieldNames.LABELS);
                            if (labels != null) {
                                final List<String> itemLabelList = item
                                    .getLabels();
                                for (final Object label : labels) {
                                    if (!itemLabelList.contains(label
                                        .toString())) {
                                        itemLabelList.add(label.toString());
                                    }
                                }
                            }
                            final Collection<Object> roles = doc
                                .getFieldValues(SuggestConstants.SuggestFieldNames.ROLES);
                            if (roles != null) {
                                final List<String> itemRoleList = item
                                    .getRoles();
                                for (final Object role : roles) {
                                    if (!itemRoleList.contains(role.toString())) {
                                        itemRoleList.add(role.toString());
                                    }
                                }
                            }
                            final Collection<Object> fields = doc
                                .getFieldValues(SuggestConstants.SuggestFieldNames.FIELD_NAME);
                            if (fields != null) {
                                final List<String> fieldNameList = item
                                    .getFieldNameList();
                                for (final Object field : fields) {
                                    if (!fieldNameList.contains(field
                                        .toString())) {
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

    protected void mergeSuggestItem(final SuggestItem item1,
                                    final SuggestItem item2) {
        item1.setCount(item1.getCount() + 1);
        item1.setExpires(item2.getExpires());
        item1.setSegment(item2.getSegment());
        final List<String> fieldNameList = item1.getFieldNameList();
        for (final String fieldName : item2.getFieldNameList()) {
            if (!fieldNameList.contains(fieldName)) {
                fieldNameList.add(fieldName);
            }
        }
        final List<String> labelList = item1.getLabels();
        for (final String label : item2.getLabels()) {
            if (!labelList.contains(label)) {
                labelList.add(label);
            }
        }
        final List<String> roleList = item1.getRoles();
        for (final String role : item2.getRoles()) {
            if (!roleList.contains(role)) {
                roleList.add(role);
            }
        }
    }

    protected static class Request {
        public RequestType type;

        public Object obj;

        public Request(final RequestType type, final Object o) {
            this.type = type;
            obj = o;
        }
    }
}
