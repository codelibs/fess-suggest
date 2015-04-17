package org.codelibs.fess.suggest.index.contents.document;

import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.lang3.StringUtils;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;

import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ESSourceReader implements DocumentReader {
    protected final Queue<Map<String, Object>> queue = new ConcurrentLinkedQueue<>();
    protected final AtomicBoolean isFinished = new AtomicBoolean(false);

    protected final Client client;
    protected final SuggestSettings settings;
    protected final String indexName;
    protected final String typeName;

    protected int scrollSize = 1000;
    protected int maxRetryCount = 5;

    public final String scrollIdKey;
    public final String execStatusKey;

    public static final String KEY_SCROLL_ID_PREFIX = "ESSourceReader.scrollID.";
    public static final String KEY_EXEC_STATUS_PREFIX = "ESSourceReader.status.";
    public static final String VALUE_EXEC = "executing";
    public static final String VALUE_IDLE = "idle";
    public static final String VALUE_FINISH = "finish";

    public ESSourceReader(final Client client, final SuggestSettings settings, final String indexName, final String typeName) {
        this.client = client;
        this.settings = settings;
        this.indexName = indexName;
        this.typeName = typeName;

        this.scrollIdKey = KEY_SCROLL_ID_PREFIX + indexName + '_' + typeName;
        this.execStatusKey = KEY_EXEC_STATUS_PREFIX + indexName + '_' + typeName;

        String status = settings.getAsString(execStatusKey, VALUE_IDLE);
        if (VALUE_FINISH.equals(status)) {
            settings.set(execStatusKey, VALUE_IDLE);
        }

    }

    @Override
    public Map<String, Object> read() {
        if (!isFinished.get() && queue.isEmpty()) {
            boolean execOthers = false;

            // wait for other process...
            int idleCount = 0;
            while (true) {
                String status = settings.getAsString(execStatusKey, VALUE_IDLE);
                if (VALUE_IDLE.equals(status)) {
                    if (++idleCount > 3) {
                        break;
                    }
                } else if (VALUE_EXEC.equals(status)) {
                    execOthers = true;
                    idleCount = 0;
                } else {
                    return null;
                }

                try {
                    Random random = new Random();
                    Thread.sleep(100 + random.nextInt(1000));
                } catch (InterruptedException e) {
                    return null;
                }
            }
            settings.set(execStatusKey, VALUE_EXEC);

            String scrollId = settings.getAsString(scrollIdKey, "");
            if (StringUtils.isBlank(scrollId)) {
                if (execOthers) {
                    return null;
                }
                scrollId = createNewScroll();
            }

            for (int i = 0; i < maxRetryCount; i++) {
                try {
                    SearchResponse response =
                            client.prepareSearchScroll(scrollId).setScroll(TimeValue.timeValueMinutes(1)).execute().actionGet();
                    scrollId = response.getScrollId();
                    if (scrollId == null) {
                        isFinished.set(true);
                    }
                    settings.set(scrollIdKey, scrollId);
                    settings.set(execStatusKey, VALUE_IDLE);
                    SearchHit[] hits = response.getHits().getHits();
                    for (SearchHit hit : hits) {
                        queue.add(hit.sourceAsMap());
                    }
                    break;
                } catch (Exception e) {
                    scrollId = createNewScroll();
                }
            }
            if (queue.isEmpty()) {
                settings.set(execStatusKey, VALUE_FINISH);
                settings.set(scrollIdKey, "");
                isFinished.set(true);
            }
        }

        return queue.poll();
    }

    @Override
    public void close() {

    }

    public void setScrollSize(final int scrollSize) {
        this.scrollSize = scrollSize;
    }

    protected String createNewScroll() {
        SearchResponse response =
                client.prepareSearch().setIndices(indexName).setTypes(typeName).setScroll(new Scroll(TimeValue.timeValueMinutes(1)))
                        .setSearchType(SearchType.SCAN).setQuery(QueryBuilders.matchAllQuery()).setSize(scrollSize).execute().actionGet();
        return response.getScrollId();
    }
}
