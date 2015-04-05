package org.codelibs.fess.suggest.index;

import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.document.DocumentReader;
import org.codelibs.fess.suggest.index.querylog.QueryLogReader;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.script.ScriptService;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SuggestIndexer {
    protected final Client client;
    protected final String index;
    protected final String type;

    protected final String[] supportedFields;
    protected final String tagFieldName;
    protected final String roleFieldName;

    protected final ReadingConverter readingConverter;
    protected final Normalizer normalizer;

    public SuggestIndexer(final Client client, final String index, final String type, final String[] supportedField,
                          final String tagFieldName, final String roleFieldName, final ReadingConverter readingConverter, final Normalizer normalizer) {
        this.client = client;
        this.index = index;
        this.type = type;
        this.supportedFields = supportedField;
        this.tagFieldName = tagFieldName;
        this.roleFieldName = roleFieldName;
        this.readingConverter = readingConverter;
        this.normalizer = normalizer;
    }

    public BulkResponse index(final SuggestItem item) {
        return index(new SuggestItem[]{item});
    }

    public BulkResponse index(final SuggestItem[] items) {
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

        return bulkRequestBuilder.execute().actionGet();
    }

    public RefreshResponse refresh() {
        return client.admin().indices().prepareRefresh(index).execute().actionGet();
    }

    public BulkResponse indexByQueryString(final String queryString) {
        return indexByQueryString(new String[]{queryString});
    }

    public BulkResponse indexByQueryString(final String[] queryStrings) {
        List<SuggestItem> items = new ArrayList<>();
        for (final String queryString : queryStrings) {
            items.addAll(queryStringToSuggestItem(queryString));
        }
        return index(items.toArray(new SuggestItem[items.size()]));
    }

    protected List<SuggestItem> queryStringToSuggestItem(final String queryString) {
        final List<SuggestItem> items = new ArrayList<>();

        //TODO support query dsl.

        for (String field : supportedFields) {
            final String[] words = SuggestUtil.parseQuery(queryString, field);
            if (words == null || words.length == 0) {
                continue;
            }

            String[][] readings = new String[words.length][];
            for (int i = 0; i < words.length; i++) {
                words[i] = normalizer.normalize(words[i]);
                readings[i] = readingConverter.convert(words[i]);
            }

            items.add(new SuggestItem(words, readings, 1L, null, //TODO label
                null, //TODO role
                SuggestItem.Kind.QUERY));
        }

        return items;
    }

    public IndexingStatus indexByDocument(final DocumentReader documentReader) {
        final IndexingStatus indexingStatus = new IndexingStatus();
        indexingStatus.running.set(true);
        indexingStatus.done.set(false);

        //TODO thread pool
        Thread th = new Thread(() -> {
            String doc;
            while ((doc = documentReader.next()) != null) {

            }

            indexingStatus.running.set(false);
            indexingStatus.done.set(true);
        });

        th.start();
        return indexingStatus;
    }

    public IndexingStatus indexByQueryLog(final QueryLogReader queryLogReader) {
        final IndexingStatus indexingStatus = new IndexingStatus();
        indexingStatus.running.set(true);
        indexingStatus.done.set(false);

        //TODO thread pool
        Thread th = new Thread(() -> {
            int maxNum = 1000; //TODO

            List<String> queryStrings = new ArrayList<>(maxNum);
            String queryString = queryLogReader.next();
            while (queryString != null) {
                queryStrings.add(queryString);
                queryString = queryLogReader.next();
                if (queryString == null || queryStrings.size() >= maxNum) {
                    indexByQueryString(queryStrings.toArray(new String[queryStrings.size()]));
                    queryStrings.clear();
                }
            }

            indexingStatus.running.set(false);
            indexingStatus.done.set(true);
        });

        th.start();
        return indexingStatus;
    }

    public class IndexingStatus {
        final protected AtomicBoolean running = new AtomicBoolean(false);
        final protected AtomicBoolean done = new AtomicBoolean(false);
        final protected List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        public boolean isStarted() {
            return running.get() || done.get();
        }

        public boolean isDone() {
            return done.get();
        }

        public boolean hasError() {
            return errors.size() > 0;
        }

        public List<Throwable> errors() {
            return errors;
        }
    }
}
