package org.codelibs.fess.suggest.index.writer;

import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.client.Client;

public class SuggestBulkFileWriter implements SuggestWriter {
    @Override
    public void write(Client client, SuggestSettings settings, String index, String type, SuggestItem[] items) {
        //TODO
    }

    @Override
    public void delete(Client client, SuggestSettings settings, String index, String type, String id) {
        //TODO
    }

    @Override
    public void deleteByQuery(Client client, SuggestSettings settings, String index, String type, String queryString) {
        throw new UnsupportedOperationException("deleteByQuery is unsupported.");
    }
}
