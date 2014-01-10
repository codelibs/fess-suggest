package jp.sf.fess.suggest.solr;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import jp.sf.fess.solr.plugin.update.FessUpdateHandler;
import jp.sf.fess.solr.plugin.update.UpdateHandlerFilter;
import jp.sf.fess.solr.plugin.update.UpdateHandlerFilterChain;
import jp.sf.fess.suggest.entity.SuggestFieldInfo;
import jp.sf.fess.suggest.util.SuggestUtil;
import jp.sf.fess.suggest.util.TransactionLogUtil;

import org.apache.solr.core.SolrCore;
import org.apache.solr.update.CommitUpdateCommand;
import org.apache.solr.update.TransactionLog;
import org.apache.solr.update.UpdateLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuggestTranslogUpdateHandlerFilter extends UpdateHandlerFilter {
    private final static Logger logger = LoggerFactory
            .getLogger(SuggestTranslogUpdateHandlerFilter.class);

    private SuggestUpdateController suggestUpdateController;

    @Override
    public void setFessUpdateHandler(final FessUpdateHandler updateHandler) {
        super.setFessUpdateHandler(updateHandler);
        startup();
    }

    protected void startup() {
        final SolrCore core = updateHandler.getSolrCore();
        final UpdateLog ulog = updateHandler.getUpdateLog();

        //TODO replay?
        TransactionLogUtil.clearSuggestTransactionLog(ulog.getLogDir());

        try {
            final SuggestUpdateConfig config = SuggestUtil
                    .getUpdateHandlerConfig(core.getSolrConfig());
            final List<SuggestFieldInfo> suggestFieldInfoList = SuggestUtil
                    .getSuggestFieldInfoList(config);
            suggestUpdateController = new SuggestUpdateController(config,
                    suggestFieldInfoList);
            if (config.getLabelFields() != null) {
                for (final String label : config.getLabelFields()) {
                    suggestUpdateController.addLabelFieldName(label);
                }
            }
            suggestUpdateController.setLimitDocumentQueuingNum(2);
            suggestUpdateController.start();
        } catch (final Exception e) {
            logger.warn("Failed to startup handler.", e);
        }
    }

    @Override
    public void commit(final CommitUpdateCommand cmd,
            final UpdateHandlerFilterChain chain) throws IOException {
        final UpdateLog ulog = updateHandler.getUpdateLog();
        final File logDir = new File(ulog.getLogDir());
        final long lastLogId = ulog.getLastLogId();
        final String lastLogName = String.format(Locale.ROOT,
                UpdateLog.LOG_FILENAME_PATTERN, UpdateLog.TLOG_NAME, lastLogId);

        chain.commit(cmd);

        final File logFile = new File(logDir, lastLogName);
        if (logger.isInfoEnabled()) {
            logger.info("Loading... " + logFile.getAbsolutePath());
        }
        try {
            final TransactionLog transactionLog = TransactionLogUtil
                    .createSuggestTransactionLog(logFile, null, true);
            suggestUpdateController.addTransactionLog(transactionLog);
        } catch (final Exception e) {
            logger.warn("Failed to add transactionLog", e);
        }
    }

    @Override
    public void close(final UpdateHandlerFilterChain chain) throws IOException {
        chain.close();
        suggestUpdateController.close();
    }

}
