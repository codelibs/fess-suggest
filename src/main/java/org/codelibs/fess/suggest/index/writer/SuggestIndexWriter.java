package org.codelibs.fess.suggest.index.writer;

import java.util.HashSet;
import java.util.Set;

import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggestIndexException;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.ScriptService;

public class SuggestIndexWriter implements SuggestWriter {
    @Override
    public SuggestWriterResult write(final Client client, final SuggestSettings settings, final String index, final String type,
            final SuggestItem[] items) {
        final Set<String> upsertedIdSet = new HashSet<>();
        final BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
        for (final SuggestItem item : items) {
            final UpdateRequestBuilder updateRequestBuilder = new UpdateRequestBuilder(client, index, type, item.getId());
            updateRequestBuilder.setScript(item.getScript(), ScriptService.ScriptType.INLINE);
            updateRequestBuilder.setScriptParams(item.getScriptParams());
            final String itemId = item.getId();
            if (!upsertedIdSet.contains(itemId)) {
                updateRequestBuilder.setScriptedUpsert(true);
                updateRequestBuilder.setUpsert(item.toEmptyMap());
                upsertedIdSet.add(itemId);
            }
            bulkRequestBuilder.add(updateRequestBuilder);
        }

        final BulkResponse response = bulkRequestBuilder.execute().actionGet(SuggestConstants.ACTION_TIMEOUT);
        final SuggestWriterResult result = new SuggestWriterResult();
        if (response.hasFailures()) {
            for (final BulkItemResponse bulkItemResponses : response.getItems()) {
                if (bulkItemResponses.isFailed()) {
                    result.addFailure(new SuggestIndexException("Bulk failure. " + bulkItemResponses.getFailureMessage()));
                }
            }
        }

        return result;
    }

    @Override
    public SuggestWriterResult delete(final Client client, final SuggestSettings settings, final String index, final String type,
            final String id) {
        final SuggestWriterResult result = new SuggestWriterResult();
        try {
            client.prepareDelete().setIndex(index).setType(type).setId(id).execute().actionGet(SuggestConstants.ACTION_TIMEOUT);
        } catch (final Exception e) {
            result.addFailure(e);
        }
        return result;
    }

    @Override
    public SuggestWriterResult deleteByQuery(final Client client, final SuggestSettings settings, final String index, final String type,
            final String queryString) {
        final SuggestWriterResult result = new SuggestWriterResult();
        try {
            client.prepareDeleteByQuery().setIndices(index).setTypes(type).setQuery(QueryBuilders.queryStringQuery(queryString)).execute()
                    .actionGet(SuggestConstants.ACTION_TIMEOUT);
        } catch (final Exception e) {
            result.addFailure(e);
        }
        return result;
    }
}
