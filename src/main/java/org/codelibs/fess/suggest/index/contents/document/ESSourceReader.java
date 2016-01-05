package org.codelibs.fess.suggest.index.contents.document;

import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;

public class ESSourceReader implements DocumentReader {
    protected final Queue<Map<String, Object>> queue = new ConcurrentLinkedQueue<>();
    protected final AtomicBoolean isFinished = new AtomicBoolean(false);
    protected final Random random = new Random();

    protected final Client client;
    protected final SuggestSettings settings;
    protected final String indexName;
    protected final String typeName;
    protected final String[] supportedFields;

    protected int scrollSize = 1;
    protected int maxRetryCount = 5;

    protected long limitOfDocumentSize = 100000;

    protected String scrollId = null;

    public ESSourceReader(final Client client, final SuggestSettings settings, final String indexName, final String typeName) {
        this.client = client;
        this.settings = settings;
        this.indexName = indexName;
        this.typeName = typeName;
        this.supportedFields = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS);
    }

    @Override
    public synchronized Map<String, Object> read() {
        while (!isFinished.get() && queue.isEmpty()) {
            addDocumentToQueue();
        }

        return queue.poll();
    }

    @Override
    public void close() {
        isFinished.set(true);
        queue.clear();
    }

    public void setScrollSize(final int scrollSize) {
        this.scrollSize = scrollSize;
    }

    public void setLimitOfDocumentSize(final long limitOfDocumentSize) {
        this.limitOfDocumentSize = limitOfDocumentSize;
    }

    protected void addDocumentToQueue() {
        RuntimeException exception = null;

        for (int i = 0; i < maxRetryCount; i++) {
            try {
                final SearchResponse response;
                if (scrollId == null) {
                    response =
                            client.prepareSearch().setIndices(indexName).setTypes(typeName)
                                    .setScroll(new Scroll(TimeValue.timeValueMinutes(1))).setQuery(QueryBuilders.matchAllQuery())
                                    .setSize(scrollSize).execute().actionGet(SuggestConstants.ACTION_TIMEOUT);
                    scrollId = response.getScrollId();
                } else {
                    response =
                            client.prepareSearchScroll(scrollId).setScroll(TimeValue.timeValueMinutes(1)).execute()
                                    .actionGet(SuggestConstants.ACTION_TIMEOUT);
                    scrollId = response.getScrollId();
                }
                final SearchHit[] hits = response.getHits().getHits();
                if (scrollId == null || hits.length == 0) {
                    isFinished.set(true);
                }

                for (final SearchHit hit : hits) {
                    final Map<String, Object> source = hit.sourceAsMap();
                    if (limitOfDocumentSize > 0) {
                        long size = 0;
                        for (final String field : supportedFields) {
                            final Object value = source.get(field);
                            if (value != null) {
                                size += value.toString().length();
                            }
                        }

                        if (size <= limitOfDocumentSize) {
                            queue.add(source);
                        }
                    } else {
                        queue.add(source);
                    }
                }
                exception = null;
                break;
            } catch (final Exception e) {
                exception = new RuntimeException(e);
                scrollId = null;
            }
        }

        if (exception != null) {
            throw exception;
        }
    }

}
