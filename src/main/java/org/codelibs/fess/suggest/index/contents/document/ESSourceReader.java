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
package org.codelibs.fess.suggest.index.contents.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.opensearch.action.search.SearchRequestBuilder;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.sort.SortBuilder;
import org.opensearch.transport.client.Client;

/**
 * <p>
 * {@link ESSourceReader} reads documents from Elasticsearch using the scroll API.
 * It implements the {@link DocumentReader} interface to provide a way to iterate over documents
 * in a large index without loading all of them into memory at once.
 * </p>
 *
 * <p>
 * The reader supports limiting the number of documents read based on a percentage of the total documents
 * or a fixed number. It also allows filtering documents based on their size, using the {@code limitOfDocumentSize}
 * parameter.
 * </p>
 *
 * <p>
 * The reader uses a queue to buffer documents read from Elasticsearch, and it retries failed requests
 * up to a maximum number of times.
 * </p>
 *
 * <p>
 * <b>Usage:</b>
 * </p>
 * <pre>
 * {@code
 * Client client = // Obtain Elasticsearch client
 * SuggestSettings settings = // Obtain SuggestSettings
 * String indexName = "your_index_name";
 *
 * ESSourceReader reader = new ESSourceReader(client, settings, indexName);
 * reader.setScrollSize(1000); // Set the scroll size
 * reader.setLimitOfDocumentSize(1024 * 1024); // Limit document size to 1MB
 * reader.setQuery(QueryBuilders.termQuery("field", "value")); // Set a query
 *
 * Map<String, Object> document;
 * while ((document = reader.read()) != null) {
 *     // Process the document
 *     System.out.println(document);
 * }
 *
 * reader.close(); // Close the reader to release resources
 * }
 * </pre>
 */
public class ESSourceReader implements DocumentReader {
    private static final Logger logger = LogManager.getLogger(ESSourceReader.class);

    protected final Queue<Map<String, Object>> queue = new ConcurrentLinkedQueue<>();
    protected final AtomicBoolean isFinished = new AtomicBoolean(false);
    protected final Random random = new Random();

    protected final Client client;
    protected final SuggestSettings settings;
    protected final String indexName;
    protected final String[] supportedFields;

    protected int scrollSize = 1;
    protected int maxRetryCount = 5;
    protected long limitOfDocumentSize = 50000;
    protected QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
    protected int limitPercentage = 100;
    protected long limitNumber = -1;
    protected List<SortBuilder<?>> sortList = new ArrayList<>();

    protected String scrollId = null;

    protected final AtomicLong docCount = new AtomicLong(0);
    protected final long totalDocNum;

    public ESSourceReader(final Client client, final SuggestSettings settings, final String indexName) {
        this.client = client;
        this.settings = settings;
        this.indexName = indexName;
        supportedFields = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS);
        totalDocNum = getTotal();
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
        if (logger.isInfoEnabled()) {
            logger.info("Set document limit: {}", limitOfDocumentSize);
        }
        this.limitOfDocumentSize = limitOfDocumentSize;
    }

    public void setQuery(final QueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
    }

    public void addSort(final SortBuilder<?> sortBuilder) {
        sortList.add(sortBuilder);
    }

    public void setLimitDocNumPercentage(final String limitPercentage) {
        if (logger.isInfoEnabled()) {
            logger.info("Set document limitPercentage: {}", limitPercentage);
        }

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
                    final SearchRequestBuilder builder = client.prepareSearch().setIndices(indexName).setScroll(settings.getScrollTimeout())
                            .setQuery(queryBuilder).setSize(scrollSize);
                    for (final SortBuilder<?> sortBuilder : sortList) {
                        builder.addSort(sortBuilder);
                    }
                    response = builder.execute().actionGet(settings.getSearchTimeout());
                } else {
                    response = client.prepareSearchScroll(scrollId).setScroll(settings.getScrollTimeout()).execute()
                            .actionGet(settings.getSearchTimeout());
                    if (!scrollId.equals(response.getScrollId())) {
                        SuggestUtil.deleteScrollContext(client, scrollId);
                    }
                }
                scrollId = response.getScrollId();
                final SearchHit[] hits = response.getHits().getHits();
                if (scrollId == null || hits.length == 0) {
                    SuggestUtil.deleteScrollContext(client, scrollId);
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
                exception = new SuggesterException(e);
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
        final SearchResponse response = client.prepareSearch().setIndices(indexName).setQuery(queryBuilder).setSize(0)
                .setTrackTotalHits(true).execute().actionGet(settings.getSearchTimeout());
        return response.getHits().getTotalHits().value();
    }

}
