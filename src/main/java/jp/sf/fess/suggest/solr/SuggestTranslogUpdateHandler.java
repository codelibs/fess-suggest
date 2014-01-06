package jp.sf.fess.suggest.solr;


import jp.sf.fess.solr.plugin.update.FessDirectUpdateHandler;
import jp.sf.fess.suggest.entity.SuggestFieldInfo;
import jp.sf.fess.suggest.util.SuggestUtil;
import jp.sf.fess.suggest.util.TransactionLogUtil;
import org.apache.solr.core.SolrCore;
import org.apache.solr.update.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class SuggestTranslogUpdateHandler extends FessDirectUpdateHandler {
    private final static Logger logger = LoggerFactory.getLogger(SuggestTranslogUpdateHandler.class);

    private SuggestUpdateController suggestUpdateController;

    public SuggestTranslogUpdateHandler(SolrCore core) {
        super(core);
        startup();
    }

    public SuggestTranslogUpdateHandler(SolrCore core, UpdateHandler updateHandler) {
        super(core, updateHandler);
        startup();
    }

    protected void startup() {
        //TODO replay?
        TransactionLogUtil.clearSuggestTransactionLog(ulog.getLogDir());

        try {
            SuggestUpdateConfig config = SuggestUtil.getUpdateHandlerConfig(core.getSolrConfig());
            List<SuggestFieldInfo> suggestFieldInfoList = SuggestUtil.getSuggestFieldInfoList(config);
            suggestUpdateController = new SuggestUpdateController(config, suggestFieldInfoList);
            if (config.getLabelFields() != null) {
                for (String label : config.getLabelFields()) {
                    suggestUpdateController.addLabelFieldName(label);
                }
            }
            suggestUpdateController.setLimitDocumentQueuingNum(2);
            suggestUpdateController.start();
        } catch (Exception e) {
            logger.warn("Failed to startup handler.", e);
        }
    }

    @Override
    public void commit(CommitUpdateCommand cmd) throws IOException {
        File logDir = new File(ulog.getLogDir());
        long lastLogId = ulog.getLastLogId();
        String lastLogName = String.format(Locale.ROOT, ulog.LOG_FILENAME_PATTERN, ulog.TLOG_NAME, lastLogId);

        super.commit(cmd);

        File logFile = new File(logDir, lastLogName);
        if(logger.isInfoEnabled()) {
            logger.info("Loading... " + logFile.getAbsolutePath());
        }
        try {
            TransactionLog transactionLog = TransactionLogUtil.createSuggestTransactionLog(logFile, null, true);
            suggestUpdateController.addTransactionLog(transactionLog);
        } catch (Exception e) {
            logger.warn("Failed to add transactionLog", e);
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        suggestUpdateController.close();
    }
}
