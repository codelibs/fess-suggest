package jp.sf.fess.suggest.solr;


import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.entity.SuggestFieldInfo;
import jp.sf.fess.suggest.entity.SuggestItem;
import jp.sf.fess.suggest.enums.RequestType;
import jp.sf.fess.suggest.exception.FessSuggestException;
import jp.sf.fess.suggest.index.DocumentReader;
import jp.sf.fess.suggest.index.IndexUpdater;
import jp.sf.fess.suggest.index.SuggestSolrServer;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.update.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class SuggestUpdateController {
    private static Logger logger = LoggerFactory.getLogger(SuggestUpdateController.class);

    protected final UpdateTask updateTask;

    protected final TransactionLogParseTask transactionLogParseTask;

    protected final IndexUpdater indexUpdater;

    protected int limitDocumentQueuingNum = 50;

    protected int limitTermQueuingNum = 100000;

    protected final Queue<Request> requestQueue = new ConcurrentLinkedQueue<Request>();

    protected final List<String> labelFieldNameList = Collections.synchronizedList(new ArrayList<String>());

    protected final List<SuggestFieldInfo> suggestFieldInfoList;

    protected final SuggestUpdateConfig config;

    public SuggestUpdateController(SuggestUpdateConfig config, List<SuggestFieldInfo> fieldInfoList) {
        SuggestSolrServer suggestSolrServer = new SuggestSolrServer(config.getSolrUrl(),
                config.getSolrUser(), config.getSolrPassword());
        IndexUpdater indexUpdater = new IndexUpdater(suggestSolrServer);
        indexUpdater.setUpdateInterval(config.getUpdateInterval());
        suggestFieldInfoList = fieldInfoList;

        this.indexUpdater = indexUpdater;
        this.config = config;

        updateTask = new UpdateTask();

        transactionLogParseTask = new TransactionLogParseTask(new TransactionLogParseListener() {
            @Override
            public void addCBK(SolrInputDocument solrInputDocument) {
                add(solrInputDocument);
            }

            @Override
            public void deleteByQueryCBK(String query) {
                deleteByQuery(query);
            }

            @Override
            public void commitCBK() {
                commit();
            }
        });
    }

    public void start() {
        indexUpdater.start();
        updateTask.start();
        transactionLogParseTask.start();
    }

    public void setLimitTermQueuingNum(int limitTermQueuingNum) {
        this.limitTermQueuingNum = limitTermQueuingNum;
    }

    public void setLimitDocumentQueuingNum(int limitDocumentQueuingNum) {
        this.limitDocumentQueuingNum = limitDocumentQueuingNum;
    }

    public void add(SolrInputDocument doc) {
        request(new Request(RequestType.ADD, doc));
    }

    public void commit() {
        request(new Request(RequestType.COMMIT, null));
    }

    public void deleteByQuery(String query) {
        request(new Request(RequestType.DELETE_BY_QUERY, query));
    }

    public void addTransactionLog(TransactionLog translog) {
        transactionLogParseTask.addTransactionLog(translog);
        synchronized (transactionLogParseTask) {
            transactionLogParseTask.notify();
        }
    }

    public void close() {
        if(logger.isInfoEnabled()) {
            logger.info("closing suggestController");
        }
        transactionLogParseTask.close();
        updateTask.close();
        indexUpdater.close();
        requestQueue.clear();
    }

    protected void request(Request request) {
        while ((requestQueue.size() > limitDocumentQueuingNum ||
                indexUpdater.getQueuingItemNum() > limitTermQueuingNum)
                && updateTask.isRunning()) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("waiting dequeue documents... doc:" + requestQueue.size() +
                            " term:" + indexUpdater.getQueuingItemNum());
                }
                Thread.sleep(1000);
            } catch (Exception e) {
                break;
            }
        }

        requestQueue.add(request);
        synchronized (updateTask) {
            updateTask.notify();
        }
    }

    public void addLabelFieldName(String labelFieldName) {
        labelFieldNameList.add(labelFieldName);
    }

    protected class UpdateTask extends Thread {
        protected AtomicBoolean isRunning = new AtomicBoolean(false);

        @Override
        public void run() {
            isRunning.set(true);
            while (isRunning.get()) {
                Request request = requestQueue.poll();
                if (request == null) {
                    try {
                        synchronized (this) {
                            this.wait(config.getUpdateInterval());
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                    continue;
                }

                switch (request.type) {
                    case ADD:
                        int count = 0;
                        long start = System.currentTimeMillis();
                        for (SuggestFieldInfo fieldInfo : suggestFieldInfoList) {
                            List<String> fieldNameList = fieldInfo.getFieldNameList();
                            TokenizerFactory tokenizerFactory = fieldInfo.getTokenizerFactory();
                            SuggestReadingConverter converter = fieldInfo.getSuggestReadingConverter();
                            SuggestNormalizer normalizer = fieldInfo.getSuggestNormalizer();
                            SolrInputDocument doc = (SolrInputDocument) request.obj;
                            DocumentReader reader = new DocumentReader(tokenizerFactory,
                                    converter, normalizer, doc,
                                    fieldNameList, labelFieldNameList, config.getExpiresField(),
                                    config.getSegmentField());
                            SuggestItem item;
                            try {
                                while ((item = reader.next()) != null) {
                                    count++;
                                    indexUpdater.addSuggestItem(item);
                                }
                            } catch (Exception e) {
                                logger.warn("Failed to tokenize document.", e);
                            }
                        }
                        if(logger.isDebugEnabled()) {
                            logger.info("updateTask finish add. took:" + (System.currentTimeMillis() - start) +
                                    "  count: " + count);
                        }
                        break;
                    case COMMIT:
                        indexUpdater.commit();
                        break;
                    case DELETE_BY_QUERY:
                        indexUpdater.deleteByQuery(request.obj.toString());
                        break;
                    default:
                        break;
                }
            }
        }

        private void close() {
            isRunning.set(false);
            this.interrupt();
        }

        public boolean isRunning() {
            return isRunning.get();
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

    protected static class TransactionLogParseTask extends Thread {
        protected static Logger logger = LoggerFactory.getLogger(TransactionLogParseTask.class);

        protected AtomicBoolean isRunning = new AtomicBoolean(false);

        protected Queue<TransactionLog> transactionLogQueue = new ConcurrentLinkedDeque<TransactionLog>();

        protected final TransactionLogParseListener listener;

        public TransactionLogParseTask(TransactionLogParseListener listener) {
            super();
            this.listener = listener;
        }

        public void close() {
            transactionLogQueue.clear();
            this.isRunning.set(false);
            this.interrupt();
        }

        public void addTransactionLog(TransactionLog translog) {
            transactionLogQueue.add(translog);
        }

        @Override
        public void run() {
            if(logger.isInfoEnabled()) {
                logger.info("Starting TransactionLogParseTask...");
            }

            isRunning.set(true);
            while(isRunning.get()) {
                TransactionLog translog = transactionLogQueue.poll();
                if(translog == null) {
                    try {
                        synchronized (this) {
                            if(transactionLogQueue.isEmpty()) {
                                this.wait(60 * 1000);
                            }
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                    continue;
                }

                if(logger.isInfoEnabled()) {
                    logger.info("");
                }
                TransactionLog.LogReader tlogReader = translog.getReader(0);
                if(tlogReader == null) {
                    logger.warn("Failed to get reader.");
                    continue;
                }

                while(true) {
                    Object o = null;
                    if (!isRunning.get()) break;
                    try {
                        o = tlogReader.next();
                    } catch (Exception e) {
                        logger.warn("Failed to read transaction log. ", e);
                    }
                    if (o == null) break;

                    try {
                        // should currently be a List<Oper,Ver,Doc/Id>
                        List entry = (List)o;

                        int operationAndFlags = (Integer)entry.get(0);
                        int oper = operationAndFlags & UpdateLog.OPERATION_MASK;

                        switch (oper) {
                            case UpdateLog.ADD:
                            {
                                // byte[] idBytes = (byte[]) entry.get(2);
                                SolrInputDocument sdoc = (SolrInputDocument)entry.get(entry.size()-1);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("add " +  sdoc);
                                }
                                listener.addCBK(sdoc);
                                break;
                            }
                            case UpdateLog.DELETE_BY_QUERY:
                            {
                                String query = (String)entry.get(2);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("deleteByQuery " +  query);
                                }
                                listener.deleteByQueryCBK(query);
                                break;
                            }
                            case UpdateLog.COMMIT:
                            {
                                if(logger.isDebugEnabled()) {
                                    logger.debug("commit");
                                }
                                listener.commitCBK();
                                break;
                            }
                            default:
                                throw new FessSuggestException("Unknown Operation! " + oper);
                        }
                    } catch (FessSuggestException e) {
                        logger.warn("Unknown Operation.", e);
                    }
                }
                tlogReader.close();
                translog.decref();
            }
        }
    }

    protected static interface TransactionLogParseListener {
        public void addCBK(SolrInputDocument solrInputDocument);

        public void deleteByQueryCBK(String query);

        public void commitCBK();
    }
}
