package org.codelibs.fess.suggest.index.writer;

import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.client.Client;

import java.time.LocalDateTime;

public interface SuggestWriter {
    void write(Client client, SuggestSettings settings, String index, String type, SuggestItem[] items);

    void delete(Client client, SuggestSettings settings, String index, String type, String id);

    void deleteByQuery(Client client, SuggestSettings settings, String index, String type, String queryString);

    void deleteOldWords(Client client, SuggestSettings settings, String index, String type, LocalDateTime threshold);
}
