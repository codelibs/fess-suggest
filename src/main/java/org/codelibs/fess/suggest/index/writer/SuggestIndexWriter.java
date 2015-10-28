package org.codelibs.fess.suggest.index.writer;

import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggestIndexException;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;

public class SuggestIndexWriter implements SuggestWriter {
    @Override
    public SuggestWriterResult write(final Client client, final SuggestSettings settings, final String index, final String type,
            final SuggestItem[] items) {
        final BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
        for (final SuggestItem item : items) {
            final GetResponse getResponse =
                    client.prepareGet().setIndex(index).setType(type).setId(item.getId()).get(TimeValue.timeValueSeconds(30));
            if (getResponse.isExists()) {
                final IndexRequestBuilder indexRequestBuilder = new IndexRequestBuilder(client, index);
                indexRequestBuilder.setType(type).setId(item.getId()).setCreate(true)
                        .setSource(item.getUpdatedSource(getResponse.getSourceAsMap()));
                bulkRequestBuilder.add(indexRequestBuilder);
            } else {
                final IndexRequestBuilder indexRequestBuilder = new IndexRequestBuilder(client, index);
                indexRequestBuilder.setType(type).setId(item.getId()).setCreate(true).setSource(item.getSource());
                bulkRequestBuilder.add(indexRequestBuilder);
            }
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
