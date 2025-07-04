/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.suggest.index.writer;

import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggestIndexException;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkRequestBuilder;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexAction;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexRequestBuilder;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.transport.client.Client;

/**
 * SuggestIndexWriter is an implementation of the SuggestWriter interface that provides methods to write, delete,
 * and delete by query suggest items in an OpenSearch index.
 */
public class SuggestIndexWriter implements SuggestWriter {
    /**
     * Constructs a new {@link SuggestIndexWriter}.
     */
    public SuggestIndexWriter() {
        // nothing
    }

    @Override
    public SuggestWriterResult write(final Client client, final SuggestSettings settings, final String index, final SuggestItem[] items,
            final boolean update) {
        final BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();

        final SuggestItem[] mergedItems = mergeItems(items);
        if (mergedItems.length == 0) {
            return new SuggestWriterResult();
        }

        for (final SuggestItem item : mergedItems) {
            final GetResponse getResponse = client.prepareGet().setIndex(index).setId(item.getId()).get(TimeValue.timeValueSeconds(30));
            if (update && getResponse.isExists()) {
                final IndexRequestBuilder indexRequestBuilder = new IndexRequestBuilder(client, IndexAction.INSTANCE, index);
                indexRequestBuilder.setId(item.getId()).setOpType(IndexRequest.OpType.INDEX)
                        .setSource(item.getUpdatedSource(getResponse.getSourceAsMap()));
                bulkRequestBuilder.add(indexRequestBuilder);
            } else {
                final IndexRequestBuilder indexRequestBuilder = new IndexRequestBuilder(client, IndexAction.INSTANCE, index);
                indexRequestBuilder.setId(item.getId()).setOpType(IndexRequest.OpType.INDEX).setSource(item.getSource());
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
    public SuggestWriterResult delete(final Client client, final SuggestSettings settings, final String index, final String id) {
        final SuggestWriterResult result = new SuggestWriterResult();
        try {
            client.prepareDelete().setIndex(index).setId(id).execute().actionGet(settings.getIndexTimeout());
        } catch (final Exception e) {
            result.addFailure(e);
        }
        return result;
    }

    @Override
    public SuggestWriterResult deleteByQuery(final Client client, final SuggestSettings settings, final String index,
            final QueryBuilder queryBuilder) {
        final SuggestWriterResult result = new SuggestWriterResult();
        try {
            SuggestUtil.deleteByQuery(client, settings, index, queryBuilder);
        } catch (final Exception e) {
            result.addFailure(e);
        }
        return result;
    }

}
