package jp.sf.fess.suggest.solr;


import jp.sf.fess.suggest.entity.SuggestFieldInfo;
import jp.sf.fess.suggest.util.SuggestUtil;
import org.apache.solr.core.SolrCore;
import org.apache.solr.update.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class SuggestUpdateHandler extends DirectUpdateHandler2 {
    private final static Logger logger = LoggerFactory.getLogger(SuggestUpdateHandler.class);

    private SuggestUpdateController suggestUpdateController;

    public SuggestUpdateHandler(SolrCore core) {
        super(core);
        startup();
    }

    public SuggestUpdateHandler(SolrCore core, UpdateHandler updateHandler) {
        super(core, updateHandler);
        startup();
    }

    protected void startup() {
        try {
            SuggestUpdateConfig config = SuggestUtil.getUpdateHandlerConfig(core.getSolrConfig());
            List<SuggestFieldInfo> suggestFieldInfoList = SuggestUtil.getSuggestFieldInfoList(config);
            suggestUpdateController = new SuggestUpdateController(config, suggestFieldInfoList);
            if (config.getLabelFields() != null) {
                for (String label : config.getLabelFields()) {
                    suggestUpdateController.addLabelFieldName(label);
                }
            }
            suggestUpdateController.start();
        } catch (Exception e) {

        }
    }


    @Override
    public int addDoc(AddUpdateCommand cmd) throws IOException {
        int ret = super.addDoc(cmd);
        suggestUpdateController.add(cmd.getSolrInputDocument());
        return ret;
    }

    @Override
    public void commit(CommitUpdateCommand cmd) throws IOException {
        super.commit(cmd);
        suggestUpdateController.commit();
    }

    @Override
    public void deleteByQuery(DeleteUpdateCommand cmd) throws IOException {
        super.deleteByQuery(cmd);
        suggestUpdateController.deleteByQuery(cmd.getQuery());
    }

    @Override
    public void close() throws IOException {
        super.close();
        suggestUpdateController.close();
    }
}
