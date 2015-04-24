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
    protected final Random random = new Random();

    protected final Client client;
    protected final SuggestSettings settings;
    protected final String indexName;
    protected final String typeName;

    protected int scrollSize = 1000;
    protected int maxRetryCount = 5;

    public final String scrollIdKey;
    public final String lock1Key;
    public final String lock2Key;
    public final String execFlgKey;

    public static final String KEY_SCROLL_ID_PREFIX = "ESSourceReader.scrollID.";
    public static final String KEY_LOCK1_PREFIX = "ESSourceReader.lock1.";
    public static final String KEY_LOCK2_PREFIX = "ESSourceReader.locl2.";
    public static final String KEY_EXEC_FLG_PREFIX = "ESSourceReader.exec.";
    public static final String VALUE_EXEC = "executing";
    public static final String VALUE_IDLE = "idle";

    public static final long WAIT_TIMEOUT = 60 * 1000;

    public ESSourceReader(final Client client, final SuggestSettings settings, final String indexName, final String typeName) {
        this.client = client;
        this.settings = settings;
        this.indexName = indexName;
        this.typeName = typeName;

        this.scrollIdKey = KEY_SCROLL_ID_PREFIX + indexName + '_' + typeName;
        this.lock1Key = KEY_LOCK1_PREFIX + indexName + '_' + typeName;
        this.lock2Key = KEY_LOCK2_PREFIX + indexName + '_' + typeName;
        this.execFlgKey = KEY_EXEC_FLG_PREFIX + indexName + '_' + typeName;

        settings.set(execFlgKey, VALUE_EXEC);
    }

    @Override
    public Map<String, Object> read() {
        if (!isFinished.get() && queue.isEmpty()) {
            try {
                waiLock();

                final String execFlg = settings.getAsString(execFlgKey, VALUE_IDLE);
                if (VALUE_IDLE.equals(execFlg)) {
                    isFinished.set(true);
                    return null;
                }

                String scrollId = settings.getAsString(scrollIdKey, "");
                if (StringUtils.isBlank(scrollId)) {
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
                    settings.set(execFlgKey, VALUE_IDLE);
                    settings.set(scrollIdKey, "");
                    isFinished.set(true);
                }
            } catch (InterruptedException ignore) {
                isFinished.set(true);
                queue.clear();
            } finally {
                clearLock();
            }
        }

        return queue.poll();
    }

    @Override
    public void close() {
        isFinished.set(true);
        queue.clear();
        clearLock();
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

    protected void waiLock() throws InterruptedException {
        final String id = String.valueOf(random.nextInt());

        // wait for other process...
        long time = System.currentTimeMillis();
        int idleCount = 0;
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            final String execFlg = settings.getAsString(execFlgKey, VALUE_IDLE);
            if (VALUE_IDLE.equals(execFlg)) {
                break;
            }

            final String lock1 = settings.getAsString(lock1Key, "");
            if (StringUtils.isBlank(lock1) || id.equals(lock1)) {
                settings.set(lock1Key, id);
                idleCount++;
                if (idleCount > 3) {
                    final String lock2 = settings.getAsString(lock2Key, "");
                    if (StringUtils.isBlank(lock2) || id.equals(lock2)) {
                        settings.set(lock2Key, id);
                        final String lock1_2 = settings.getAsString(lock1Key, "");
                        final String lock2_2 = settings.getAsString(lock2Key, "");
                        if (id.equals(lock1_2) && id.equals(lock2_2)) {
                            break;
                        }
                    }
                    idleCount = 0;
                }
            } else {
                idleCount = 0;
            }

            if (System.currentTimeMillis() - time > WAIT_TIMEOUT) {
                clearLock();
                time = System.currentTimeMillis();
            }

            Thread.sleep(10 + random.nextInt(100));
        }

    }

    protected void clearLock() {
        settings.set(lock1Key, "");
        settings.set(lock2Key, "");
    }
}
