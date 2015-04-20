package org.codelibs.fess.suggest.index.writer;

import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.client.Client;

public interface SuggestWriter {
    SuggestWriterResult write(Client client, SuggestSettings settings, String index, String type, SuggestItem[] items);

    SuggestWriterResult delete(Client client, SuggestSettings settings, String index, String type, String id);

    SuggestWriterResult deleteByQuery(Client client, SuggestSettings settings, String index, String type, String queryString);
}
