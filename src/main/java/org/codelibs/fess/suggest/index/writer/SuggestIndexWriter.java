package org.codelibs.fess.suggest.index.writer;

import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.ScriptService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class SuggestIndexWriter implements SuggestWriter {
    @Override
    public void write(final Client client, final SuggestSettings settings, final String index, final String type, final SuggestItem[] items) {
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

    @Override
    public void delete(final Client client, final SuggestSettings settings, final String index, final String type, final String id) {
        client.prepareDelete().setIndex(index).setType(type).setId(id).execute().actionGet();
    }

    @Override
    public void deleteByQuery(final Client client, final SuggestSettings settings, final String index, final String type,
            final String queryString) {
        client.prepareDeleteByQuery().setIndices(index).setTypes(type).setQuery(QueryBuilders.queryStringQuery(queryString)).execute()
                .actionGet();
    }

    @Override
    public void deleteOldWords(Client client, SuggestSettings settings, String index, String type, LocalDateTime threshold) {
        client.prepareDeleteByQuery()
                .setIndices(index)
                .setTypes(type)
                .setQuery(
                        QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery(FieldNames.TIMESTAMP).lte(threshold))
                                .mustNot(QueryBuilders.queryStringQuery(FieldNames.KINDS + ':' + SuggestItem.Kind.USER.toString())))
                .execute().actionGet();
    }
}
