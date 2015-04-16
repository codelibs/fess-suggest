package org.codelibs.fess.suggest.index.writer;

import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggestIndexException;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.client.Client;

public interface SuggestWriter {
    void write(Client client, SuggestSettings settings, String index, String type, SuggestItem[] items) throws SuggestIndexException;

    void delete(Client client, SuggestSettings settings, String index, String type, String id) throws SuggestIndexException;

    void deleteByQuery(Client client, SuggestSettings settings, String index, String type, String queryString) throws SuggestIndexException;
}
