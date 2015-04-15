package org.codelibs.fess.suggest.index.writer;

import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.client.Client;

import java.time.LocalDateTime;

//TODO
public class SuggestBulkFileWriter implements SuggestWriter {
    @Override
    public void write(Client client, SuggestSettings settings, String index, String type, SuggestItem[] items) {

    }

    @Override
    public void delete(Client client, SuggestSettings settings, String index, String type, String id) {

    }

    @Override
    public void deleteByQuery(Client client, SuggestSettings settings, String index, String type, String queryString) {
        throw new UnsupportedOperationException("deleteByQuery is unsupported.");
    }

    @Override
    public void deleteOldWords(Client client, SuggestSettings settings, String index, String type, LocalDateTime threshold) {
        throw new UnsupportedOperationException("deleteOldWords is unsupported.");
    }
}
