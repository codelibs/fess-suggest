package org.codelibs.fess.suggest.index.writer;

import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggestIndexException;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexAction;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;

public class SuggestIndexWriter implements SuggestWriter {
    @Override
    public SuggestWriterResult write(final Client client, final SuggestSettings settings, final String index, final String type,
            final SuggestItem[] items, final boolean update) {
        final BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();

        final SuggestItem[] mergedItems = mergeItems(items);
        if (mergedItems.length == 0) {
            return new SuggestWriterResult();
        }

        for (final SuggestItem item : mergedItems) {
            final GetResponse getResponse =
                    client.prepareGet().setIndex(index).setType(type).setId(item.getId()).get(TimeValue.timeValueSeconds(30));
            if (update && getResponse.isExists()) {
                final IndexRequestBuilder indexRequestBuilder = new IndexRequestBuilder(client, IndexAction.INSTANCE, index);
                indexRequestBuilder.setType(type).setId(item.getId()).setOpType(IndexRequest.OpType.INDEX)
                        .setSource(item.getUpdatedSource(getResponse.getSourceAsMap()));
                bulkRequestBuilder.add(indexRequestBuilder);
            } else {
                final IndexRequestBuilder indexRequestBuilder = new IndexRequestBuilder(client, IndexAction.INSTANCE, index);
                indexRequestBuilder.setType(type).setId(item.getId()).setOpType(IndexRequest.OpType.INDEX).setSource(item.getSource());
                bulkRequestBuilder.add(indexRequestBuilder);
            }
        }

        final BulkResponse response = bulkRequestBuilder.execute().actionGet(settings.getBulkTimeout());
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
            client.prepareDelete().setIndex(index).setType(type).setId(id).execute().actionGet(settings.getIndexTimeout());
        } catch (final Exception e) {
            result.addFailure(e);
        }
        return result;
    }

    @Override
    public SuggestWriterResult deleteByQuery(final Client client, final SuggestSettings settings, final String index, final String type,
            final QueryBuilder queryBuilder) {
        final SuggestWriterResult result = new SuggestWriterResult();
        try {
            SuggestUtil.deleteByQuery(client, settings, index, type, queryBuilder);
        } catch (final Exception e) {
            result.addFailure(e);
        }
        return result;
    }

}
