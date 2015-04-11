package org.codelibs.fess.suggest.index.writer;

import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.script.ScriptService;

import java.util.HashSet;
import java.util.Set;

public class SuggestIndexWriter implements SuggestWriter {
    @Override
    public void write(Client client, SuggestSettings settings, String index, String type, SuggestItem[] items) {
        Set<String> upsertedIdSet = new HashSet<>();
        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
        for (SuggestItem item : items) {
            final UpdateRequestBuilder updateRequestBuilder = new UpdateRequestBuilder(client, index, type, item.getId());
            updateRequestBuilder.setScript(item.getScript(), ScriptService.ScriptType.INLINE);
            updateRequestBuilder.setScriptParams(item.getScriptParams());
            String itemId = item.getId();
            if (!upsertedIdSet.contains(itemId)) {
                updateRequestBuilder.setScriptedUpsert(true);
                updateRequestBuilder.setUpsert(item.toEmptyMap());
                upsertedIdSet.add(itemId);
            }
            bulkRequestBuilder.add(updateRequestBuilder);
        }

        //TODO return WriteResult?
        bulkRequestBuilder.execute().actionGet();
    }
}
