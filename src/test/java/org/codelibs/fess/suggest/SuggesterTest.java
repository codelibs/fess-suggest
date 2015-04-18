package org.codelibs.fess.suggest;

import junit.framework.TestCase;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.entity.ElevateWord;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.SuggestIndexResponse;
import org.codelibs.fess.suggest.index.SuggestIndexer;
import org.codelibs.fess.suggest.index.contents.document.ESSourceReader;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLog;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLogReader;
import org.codelibs.fess.suggest.request.suggest.SuggestResponse;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;

public class SuggesterTest extends TestCase {
    Suggester suggester;

    ElasticsearchClusterRunner runner;

    @Override
    public void setUp() throws Exception {
        runner = new ElasticsearchClusterRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("index.number_of_replicas", 0);
            settingsBuilder.put("script.disable_dynamic", false);
            settingsBuilder.put("script.groovy.sandbox.enabled", true);
        }).build(newConfigs().ramIndexStore().numOfNode(1));
        runner.ensureYellow();

        suggester = Suggester.builder().build(runner.client(), "SuggesterTest");
    }

    @Override
    protected void tearDown() throws Exception {
        runner.close();
        runner.clean();
    }

    public void test_indexAndSuggest() throws Exception {
        SuggestItem[] items = getItemSet1();
        suggester.indexer().index(items);
        suggester.refresh();

        SuggestResponse response = suggester.suggest().setQuery("kensaku").setSuggestDetail(true).execute();
        assertEquals(1, response.getNum());
        assertEquals("検索 エンジン", response.getWords().get(0));

        response = suggester.suggest().setQuery("kensaku　 enj").execute();
        assertEquals(1, response.getNum());
        assertEquals("検索 エンジン", response.getWords().get(0));

        response = suggester.suggest().setQuery("zenbun").setSuggestDetail(true).execute();
        assertEquals(1, response.getNum());
        assertEquals("全文 検索", response.getWords().get(0));
    }

    public void test_delete() throws Exception {
        SuggestItem[] items = getItemSet1();
        suggester.indexer().index(items);
        suggester.refresh();

        SuggestResponse response = suggester.suggest().setQuery("kensaku").addRole("role1").setSuggestDetail(true).execute();
        assertEquals(1, response.getNum());
        assertEquals("検索 エンジン", response.getWords().get(0));

        suggester.indexer().delete(items[0].getId());
        suggester.refresh();
        SuggestResponse response2 = suggester.suggest().setQuery("kensaku").setSuggestDetail(true).execute();
        assertEquals(0, response2.getNum());
    }

    public void test_indexFromQueryString() throws Exception {
        SuggestSettings settings = suggester.settings();
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        SuggestIndexResponse indexResponse = suggester.indexer().indexFromQueryLog(new QueryLog(field + ":検索", null));
        assertEquals(1, indexResponse.getNumberOfInputDocs());
        assertEquals(1, indexResponse.getNumberOfSuggestDocs());
        assertFalse(indexResponse.hasError());

        suggester.refresh();

        SuggestResponse responseKanji = suggester.suggest().setQuery("検索").addRole("role1").setSuggestDetail(true).execute();
        assertEquals(1, responseKanji.getNum());
        assertEquals(1, responseKanji.getTotal());
        assertEquals("検索", responseKanji.getWords().get(0));

        SuggestResponse responseKana = suggester.suggest().setQuery("けん").setSuggestDetail(true).execute();
        assertEquals(1, responseKana.getNum());
        assertEquals(1, responseKana.getTotal());
        assertEquals("検索", responseKana.getWords().get(0));

        SuggestResponse responseAlphabet = suggester.suggest().setQuery("kensa").setSuggestDetail(true).execute();
        assertEquals(1, responseAlphabet.getNum());
        assertEquals(1, responseAlphabet.getTotal());
        assertEquals("検索", responseAlphabet.getWords().get(0));

        suggester.indexer().indexFromQueryLog(new QueryLog(field + ":検索 AND " + field + ":ワード", null));
        suggester.refresh();

        SuggestResponse responseMulti = suggester.suggest().setQuery("けんさく わーど").setSuggestDetail(true).execute();

        assertEquals(1, responseMulti.getNum());
        assertEquals(1, responseMulti.getTotal());
        assertEquals("検索 ワード", responseMulti.getWords().get(0));
    }

    public void test_indexFromQueryLog() throws Exception {
        SuggestSettings settings = suggester.settings();
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        final QueryLogReader reader = new QueryLogReader() {
            AtomicInteger count = new AtomicInteger();
            String[] queryLogs = new String[] { field + ":検索", field + ":fess", field + ":検索エンジン" };

            @Override
            public QueryLog read() {
                if (count.get() >= queryLogs.length) {
                    return null;
                }
                return new QueryLog(queryLogs[count.getAndIncrement()], null);
            }

            @Override
            public void close() {
                //ignore
            }
        };

        SuggestIndexer.IndexingFuture status = suggester.indexer().indexFromQueryLog(reader, true);
        while (!status.isDone()) {
            Thread.sleep(1000);
        }
        suggester.refresh();

        SuggestResponse response1 = suggester.suggest().setQuery("けん").setSuggestDetail(true).execute();
        assertEquals(2, response1.getNum());
        assertEquals(2, response1.getTotal());

        SuggestResponse response2 = suggester.suggest().setQuery("fes").setSuggestDetail(true).execute();
        assertEquals(1, response2.getNum());
        assertEquals(1, response2.getTotal());
    }

    @SuppressWarnings("unchecked")
    public void test_indexFromDocument() throws Exception {
        SuggestSettings settings = suggester.settings();
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        Map<String, Object> document = new HashMap<>();
        document.put(field, "この柿は美味しい。");
        suggester.indexer().indexFromDocument(new Map[] { document });
        suggester.refresh();

        SuggestResponse response1 = suggester.suggest().setQuery("かき").setSuggestDetail(true).execute();
        assertEquals(1, response1.getNum());
        assertEquals(1, response1.getTotal());
        assertEquals("柿", response1.getWords().get(0));

        SuggestResponse response2 = suggester.suggest().setQuery("美味しい").setSuggestDetail(true).execute();
        assertEquals(1, response2.getNum());
        assertEquals(1, response2.getTotal());
        assertEquals("美味しい", response2.getWords().get(0));
    }

    public void test_indexFromDocumentReader() throws Exception {
        Client client = runner.client();
        int num = 10000;
        String indexName = "test";
        String typeName = "test";

        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
        for (int i = 0; i < num; i++) {
            Map<String, Object> source = Collections.singletonMap("content", "test" + i);
            IndexRequestBuilder indexRequestBuilder = new IndexRequestBuilder(client);
            indexRequestBuilder.setIndex(indexName).setType(typeName).setId(String.valueOf(i)).setCreate(true).setSource(source);
            bulkRequestBuilder.add(indexRequestBuilder);
        }
        bulkRequestBuilder.execute().actionGet();
        runner.refresh();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger numObInputDoc = new AtomicInteger(0);
        ESSourceReader reader = new ESSourceReader(client, suggester.settings(), indexName, typeName);

        SuggestIndexer.IndexingFuture future = suggester.indexer().indexFromDocument(reader, true).done(response -> {
            numObInputDoc.set(response.getNumberOfInputDocs());
            latch.countDown();
        });
        assertFalse(future.isDone());
        latch.await();
        assertTrue(future.isDone());
        assertEquals(num, numObInputDoc.get());

        SuggestResponse response = suggester.suggest().setQuery("test").setSuggestDetail(true).execute();
        assertEquals(1, response.getNum());
    }

    public void test_indexElevateWord() throws Exception {
        ElevateWord elevateWord = new ElevateWord("test", 2.0f, Collections.singletonList("test"));
        suggester.indexer().addElevateWord(elevateWord);
        suggester.refresh();
        SuggestResponse response1 = suggester.suggest().setQuery("tes").setSuggestDetail(true).execute();
        assertEquals(1, response1.getNum());
        assertEquals(1, response1.getTotal());
        assertEquals(2.0f, response1.getItems().get(0).getUserBoost());

        ElevateWord[] elevateWords = suggester.settings().elevateWord().get();
        assertEquals(1, elevateWords.length);
        assertEquals("test", elevateWord.getElevateWord());
    }

    public void test_restoreElevateWord() throws Exception {
        ElevateWord elevateWord1 = new ElevateWord("test", 2.0f, Collections.singletonList("test"));
        ElevateWord elevateWord2 = new ElevateWord("hoge", 2.0f, Collections.singletonList("hoge"));
        ElevateWord elevateWord3 = new ElevateWord("fuga", 2.0f, Collections.singletonList("fuga"));

        suggester.settings().elevateWord().add(elevateWord1);
        suggester.settings().elevateWord().add(elevateWord2);
        suggester.settings().elevateWord().add(elevateWord3);

        suggester.indexer().restoreElevateWord();
        suggester.refresh();

        SuggestResponse response1 = suggester.suggest().setQuery("tes").setSuggestDetail(true).execute();
        assertEquals(1, response1.getNum());
        SuggestResponse response2 = suggester.suggest().setQuery("hoge").setSuggestDetail(true).execute();
        assertEquals(1, response2.getNum());
        SuggestResponse response3 = suggester.suggest().setQuery("fuga").setSuggestDetail(true).execute();
        assertEquals(1, response3.getNum());
    }

    @SuppressWarnings("unchecked")
    public void test_deleteOldWords() throws Exception {
        String field = suggester.settings().array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];
        ElevateWord elevateWord = new ElevateWord("test", 2.0f, Collections.singletonList("test"));

        suggester.indexer().indexFromDocument(new Map[] { Collections.singletonMap(field, (Object) "この柿は美味しい。") });
        suggester.indexer().addElevateWord(elevateWord);
        suggester.refresh();

        Thread.sleep(1000);
        LocalDateTime threshold = LocalDateTime.now();
        Thread.sleep(1000);

        suggester.indexer().indexFromDocument(new Map[] { Collections.singletonMap(field, (Object) "検索エンジン") });
        suggester.refresh();

        SuggestResponse response1 = suggester.suggest().setQuery("柿").setSuggestDetail(true).execute();
        assertEquals(1, response1.getNum());
        SuggestResponse response2 = suggester.suggest().setQuery("test").setSuggestDetail(true).execute();
        assertEquals(1, response2.getNum());
        SuggestResponse response3 = suggester.suggest().setQuery("検索").setSuggestDetail(true).execute();
        assertEquals(1, response3.getNum());

        suggester.indexer().deleteOldWords(threshold);
        suggester.refresh();

        SuggestResponse response4 = suggester.suggest().setQuery("柿").setSuggestDetail(true).execute();
        assertEquals(0, response4.getNum());
        SuggestResponse response5 = suggester.suggest().setQuery("test").setSuggestDetail(true).execute();
        assertEquals(1, response5.getNum());
        SuggestResponse response6 = suggester.suggest().setQuery("検索").setSuggestDetail(true).execute();
        assertEquals(1, response6.getNum());

    }

    public void test_addNgWord() throws Exception {
        SuggestItem[] items = getItemSet1();
        suggester.indexer().index(items);
        suggester.refresh();

        SuggestResponse response = suggester.suggest().setQuery("kensaku").setSuggestDetail(true).execute();
        assertEquals(1, response.getNum());
        assertEquals("検索 エンジン", response.getWords().get(0));

        suggester.indexer().addNgWord("検索");
        suggester.refresh();
        SuggestResponse response2 = suggester.suggest().setQuery("kensaku").setSuggestDetail(true).execute();
        assertEquals(0, response2.getNum());

        assertEquals(1, suggester.settings().ngword().get().length);
    }

    private SuggestItem[] getItemSet1() {
        SuggestItem[] queryItems = new SuggestItem[3];

        String[][] readings = new String[2][];
        readings[0] = new String[] { "kensaku", "fuga" };
        readings[1] = new String[] { "enjin", "fuga" };
        String[] tags = new String[] { "tag1", "tag2" };
        String[] roles = new String[] { SuggestConstants.DEFAULT_ROLE, "role1", "role2", "role3" };
        queryItems[0] = new SuggestItem(new String[] { "検索", "エンジン" }, readings, 1, -1, tags, roles, SuggestItem.Kind.DOCUMENT);

        String[][] readings2 = new String[2][];
        readings2[0] = new String[] { "zenbun", "fuga" };
        readings2[1] = new String[] { "kensaku", "fuga" };
        String[] tags2 = new String[] { "tag3" };
        String[] roles2 = new String[] { SuggestConstants.DEFAULT_ROLE, "role1", "role2", "role3", "role4" };
        queryItems[1] = new SuggestItem(new String[] { "全文", "検索" }, readings2, 1, -1, tags2, roles2, SuggestItem.Kind.DOCUMENT);

        String[][] readings2Query = new String[2][];
        readings2Query[0] = new String[] { "zenbun", "fuga" };
        readings2Query[1] = new String[] { "kensaku", "fuga" };
        String[] tags2Query = new String[] { "tag3" };
        String[] roles2Query = new String[] { SuggestConstants.DEFAULT_ROLE, "role1", "role2", "role3", "role4" };
        queryItems[2] =
                new SuggestItem(new String[] { "全文", "検索" }, readings2Query, 1, -1, tags2Query, roles2Query, SuggestItem.Kind.QUERY);

        return queryItems;
    }
}
