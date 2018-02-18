package org.codelibs.fess.suggest.index.contents.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilder;

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
    protected long limitOfDocumentSize = 50000;
    protected QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
    protected int limitPercentage = 100;
    protected long limitNumber = -1;
    protected List<SortBuilder<?>> sortList = new ArrayList<>();;

    protected String scrollId = null;

    protected final AtomicLong docCount = new AtomicLong(0);
    protected final long totalDocNum;

    public ESSourceReader(final Client client, final SuggestSettings settings, final String indexName, final String typeName) {
        this.client = client;
        this.settings = settings;
        this.indexName = indexName;
        this.typeName = typeName;
        this.supportedFields = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS);
        this.totalDocNum = getTotal();
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

    public void setQuery(final QueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
    }

    public void addSort(final SortBuilder<?> sortBuilder) {
        this.sortList.add(sortBuilder);
    }

    public void setLimitDocNumPercentage(final String limitPercentage) {
        if (limitPercentage.endsWith("%")) {
            this.limitPercentage = Integer.parseInt(limitPercentage.substring(0, limitPercentage.length() - 1));
        } else {
            this.limitPercentage = Integer.parseInt(limitPercentage);
        }

        if (this.limitPercentage > 100) {
            this.limitPercentage = 100;
        } else if (this.limitPercentage < 1) {
            this.limitPercentage = 1;
        }
    }

    public void setLimitNumber(final long limitNumber) {
        this.limitNumber = limitNumber;
    }

    protected void addDocumentToQueue() {
        if (docCount.get() > getLimitDocNum(totalDocNum, limitPercentage, limitNumber)) {
            isFinished.set(true);
            return;
        }

        RuntimeException exception = null;

        for (int i = 0; i < maxRetryCount; i++) {
            try {
                final SearchResponse response;
                if (scrollId == null) {
                    final SearchRequestBuilder builder =
                            client.prepareSearch().setIndices(indexName).setTypes(typeName).setScroll(settings.getScrollTimeout())
                                    .setQuery(queryBuilder).setSize(scrollSize);
                    for (final SortBuilder<?> sortBuilder : sortList) {
                        builder.addSort(sortBuilder);
                    }
                    response = builder.execute().actionGet(settings.getSearchTimeout());
                    scrollId = response.getScrollId();
                } else {
                    response =
                            client.prepareSearchScroll(scrollId).setScroll(settings.getScrollTimeout()).execute()
                                    .actionGet(settings.getSearchTimeout());
                    scrollId = response.getScrollId();
                }
                final SearchHit[] hits = response.getHits().getHits();
                if (scrollId == null || hits.length == 0) {
                    isFinished.set(true);
                }

                for (final SearchHit hit : hits) {
                    final Map<String, Object> source = hit.getSourceAsMap();
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

        docCount.getAndAdd(queue.size());

        if (exception != null) {
            throw exception;
        }
    }

    protected static long getLimitDocNum(final long total, final long limitPercentage, final long limitNumber) {
        final long percentNum = (long) (total * (limitPercentage / 100f));
        if (limitNumber < 0) {
            return percentNum;
        }
        return percentNum < limitNumber ? percentNum : limitNumber;
    }

    protected long getTotal() {
        final SearchResponse response =
                client.prepareSearch().setIndices(indexName).setTypes(typeName).setQuery(queryBuilder).setSize(0).execute()
                        .actionGet(settings.getSearchTimeout());
        return response.getHits().getTotalHits();
    }

}
