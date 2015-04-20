package org.codelibs.fess.suggest.index.writer;

import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggestIndexException;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.client.Client;

public class SuggestBulkFileWriter implements SuggestWriter {
    @Override
    public SuggestWriterResult write(Client client, SuggestSettings settings, String index, String type, SuggestItem[] items) {
        throw new UnsupportedOperationException("not yet.");
    }

    @Override
    public SuggestWriterResult delete(Client client, SuggestSettings settings, String index, String type, String id) {
        throw new UnsupportedOperationException("not yet.");
    }

    @Override
    public SuggestWriterResult deleteByQuery(Client client, SuggestSettings settings, String index, String type, String queryString) {
        throw new UnsupportedOperationException("deleteByQuery is unsupported.");
    }
}
