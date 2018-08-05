package org.codelibs.fess.suggest;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.entity.ElevateWord;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.SuggestIndexResponse;
import org.codelibs.fess.suggest.index.contents.document.ESSourceReader;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLog;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLogReader;
import org.codelibs.fess.suggest.request.popularwords.PopularWordsResponse;
import org.codelibs.fess.suggest.request.suggest.SuggestResponse;
import org.codelibs.fess.suggest.settings.AnalyzerSettings;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexAction;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.Client;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SuggesterTest {
    static Suggester suggester;

    static ElasticsearchClusterRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new ElasticsearchClusterRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.putList("discovery.zen.ping.unicast.hosts", "localhost:9301-9399");
        }).build(
                newConfigs().clusterName("ArraySettingsTest").numOfNode(1)
                        .pluginTypes("org.codelibs.elasticsearch.kuromoji.neologd.KuromojiNeologdPlugin"));
        runner.ensureYellow();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        runner.close();
        runner.clean();
    }

    @Before
    public void before() throws Exception {
        runner.admin().indices().prepareDelete("_all").execute().actionGet();
        runner.refresh();
        suggester = Suggester.builder().build(runner.client(), "SuggesterTest");
        suggester.createIndexIfNothing();
    }

    @Test
    public void test_indexAndSuggest() throws Exception {
        SuggestItem[] items = getItemSet1();
        suggester.indexer().index(items);
        suggester.refresh();

        SuggestResponse response = suggester.suggest().setQuery("kensaku").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response.getNum());
        assertEquals("検索 エンジン", response.getWords().get(0));

        response = suggester.suggest().setQuery("kensaku　 enj").execute().getResponse();
        assertEquals(1, response.getNum());
        assertEquals("検索 エンジン", response.getWords().get(0));

        response = suggester.suggest().setQuery("zenbun").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response.getNum());
        assertEquals("全文 検索", response.getWords().get(0));

        response = suggester.suggest().addKind("query").execute().getResponse();
        assertEquals(1, response.getNum());

        SuggestResponse response2 = suggester.suggest().setSuggestDetail(true).execute().getResponse();
        assertEquals(2, response2.getNum());
    }

    @Test
    public void test_update() throws Exception {
        SuggestItem[] items = getItemSet1();
        suggester.indexer().index(items);
        suggester.refresh();

        SuggestItem[] items2 = getItemSet1();
        SuggestIndexResponse response = suggester.indexer().index(items2);

        assertFalse(response.hasError());

        suggester.refresh();

        SuggestResponse response2 = suggester.suggest().setSuggestDetail(true).execute().getResponse();
        assertEquals(2, response2.getNum());
    }

    @Test
    public void test_delete() throws Exception {
        SuggestItem[] items = getItemSet1();
        suggester.indexer().index(items);
        suggester.refresh();

        SuggestResponse response = suggester.suggest().setQuery("kensaku").addRole("role1").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response.getNum());
        assertEquals("検索 エンジン", response.getWords().get(0));

        suggester.indexer().delete(items[0].getId());
        suggester.refresh();
        SuggestResponse response2 = suggester.suggest().setQuery("kensaku").setSuggestDetail(true).execute().getResponse();
        assertEquals(0, response2.getNum());
    }

    @Test
    public void test_indexFromQueryString() throws Exception {
        SuggestSettings settings = suggester.settings();
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        SuggestIndexResponse indexResponse = suggester.indexer().indexFromQueryLog(new QueryLog(field + ":検索", null));
        assertEquals(1, indexResponse.getNumberOfInputDocs());
        assertEquals(1, indexResponse.getNumberOfSuggestDocs());
        assertFalse(indexResponse.hasError());

        suggester.refresh();

        SuggestResponse responseKanji = suggester.suggest().setQuery("検索").addRole("role1").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, responseKanji.getNum());
        assertEquals(1, responseKanji.getTotal());
        assertEquals("検索", responseKanji.getWords().get(0));

        SuggestResponse responseKana = suggester.suggest().setQuery("けん").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, responseKana.getNum());
        assertEquals(1, responseKana.getTotal());
        assertEquals("検索", responseKana.getWords().get(0));

        SuggestResponse responseAlphabet = suggester.suggest().setQuery("kennsa").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, responseAlphabet.getNum());
        assertEquals(1, responseAlphabet.getTotal());
        assertEquals("検索", responseAlphabet.getWords().get(0));

        suggester.indexer().indexFromQueryLog(new QueryLog(field + ":検索 AND " + field + ":ワード", null));
        suggester.refresh();

        SuggestResponse responseMulti = suggester.suggest().setQuery("けんさく わーど").setSuggestDetail(true).execute().getResponse();

        assertEquals(1, responseMulti.getNum());
        assertEquals(1, responseMulti.getTotal());
        assertEquals("検索 ワード", responseMulti.getWords().get(0));
    }

    @Test
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

        suggester.indexer().indexFromQueryLog(reader, 10, 100).getResponse();
        suggester.refresh();

        SuggestResponse response1 = suggester.suggest().setQuery("けん").setSuggestDetail(true).execute().getResponse();
        assertEquals(2, response1.getNum());
        assertEquals(2, response1.getTotal());

        SuggestResponse response2 = suggester.suggest().setQuery("fes").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response2.getNum());
        assertEquals(1, response2.getTotal());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_indexFromDocument() throws Exception {
        SuggestSettings settings = suggester.settings();
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        Map<String, Object> document = new HashMap<>();
        document.put(field, "この柿は美味しい。");
        suggester.indexer().indexFromDocument(new Map[] { document });
        suggester.refresh();

        SuggestResponse response1 = suggester.suggest().setQuery("かき").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response1.getNum());
        assertEquals(1, response1.getTotal());
        assertEquals("柿", response1.getWords().get(0));

        SuggestResponse response2 = suggester.suggest().setQuery("美味しい").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response2.getNum());
        assertEquals(1, response2.getTotal());
        assertEquals("美味しい", response2.getWords().get(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_indexFromDocumentEn() throws Exception {
        SuggestSettings settings = suggester.settings();
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        Map<String, Object> document = new HashMap<>();
        document.put(field, "The persimmon is delicious");
        document.put("lang", "en");
        suggester.indexer().indexFromDocument(new Map[] { document });
        suggester.refresh();

        SuggestResponse response1 = suggester.suggest().setQuery("The").addLang("en").setSuggestDetail(true).execute().getResponse();
        assertEquals(0, response1.getNum());
        assertEquals(0, response1.getTotal());

        SuggestResponse response2 = suggester.suggest().setQuery("persimmon").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response2.getNum());
        assertEquals(1, response2.getTotal());
        assertEquals("persimmon", response2.getWords().get(0));

        SuggestResponse response3 = suggester.suggest().setQuery("is").setSuggestDetail(true).execute().getResponse();
        assertEquals(0, response3.getNum());
        assertEquals(0, response3.getTotal());

        SuggestResponse response4 = suggester.suggest().setQuery("delicious").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response4.getNum());
        assertEquals(1, response4.getTotal());
        assertEquals("delicious", response4.getWords().get(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_indexFromDocumentDe() throws Exception {
        SuggestSettings settings = suggester.settings();
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        Map<String, Object> document = new HashMap<>();
        document.put(field, "Schöner Klang der Gitarre");
        document.put("lang", "de");
        suggester.indexer().indexFromDocument(new Map[] { document });
        suggester.refresh();

        SuggestResponse response1 = suggester.suggest().setQuery("schöner").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response1.getNum());
        assertEquals(1, response1.getTotal());
        assertEquals("schöner", response1.getWords().get(0));

        SuggestResponse response2 = suggester.suggest().setQuery("schoner").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response2.getNum());
        assertEquals(1, response2.getTotal());
        assertEquals("schöner", response2.getWords().get(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_indexFromDocumentLengthLimit() throws Exception {
        SuggestSettings settings = suggester.settings();
        String field = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        Map<String, Object> document = new HashMap<>();
        document.put(field, "Sing the Supercalifragilisticexpialidocious for Honorificabilitudinitatibus");
        suggester.indexer().indexFromDocument(new Map[] { document });
        suggester.refresh();

        SuggestResponse response0 = suggester.suggest().setQuery("sin").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response0.getNum());
        assertEquals(1, response0.getTotal());
        assertEquals("sing", response0.getWords().get(0));

        SuggestResponse response1 = suggester.suggest().setQuery("super").setSuggestDetail(true).execute().getResponse();
        assertEquals(0, response1.getNum());
        assertEquals(0, response1.getTotal());

        SuggestResponse response2 = suggester.suggest().setQuery("honor").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response2.getNum());
        assertEquals(1, response2.getTotal());
        assertEquals("honorificabilitudinitatibus", response2.getWords().get(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_indexFromDocumentWithFieldAnalyzer() throws Exception {
        String field = "testField";

        Map<String, String> analyzerMapping = new HashMap<>();
        analyzerMapping.put(FieldNames.ANALYZER_SETTINGS_TYPE, AnalyzerSettings.settingsFieldAnalyzerMappingType);
        analyzerMapping.put(FieldNames.ANALYZER_SETTINGS_FIELD_NAME, field);
        analyzerMapping.put(FieldNames.ANALYZER_SETTINGS_CONTENTS_ANALYZER, "title_contents_analyzer");
        analyzerMapping.put(FieldNames.ANALYZER_SETTINGS_CONTENTS_READING_ANALYZER, "");
        runner.client().prepareIndex().setIndex(suggester.settings().analyzer().getAnalyzerSettingsIndexName())
                .setType(suggester.settings().analyzer().DOC_TYPE_NAME).setSource(analyzerMapping)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL).execute().actionGet();
        suggester.settings().analyzer().init();

        SuggestSettings settings = suggester.settings();
        settings.array().add(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS, field);

        Map<String, Object> document = new HashMap<>();
        document.put(field, "商品１__and__商品２");
        suggester.indexer().indexFromDocument(new Map[] { document });
        suggester.refresh();

        SuggestResponse response1 = suggester.suggest().setQuery("しょうひん１").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response1.getNum());
        assertEquals(1, response1.getTotal());
        assertEquals("商品1", response1.getWords().get(0));

        SuggestResponse response2 = suggester.suggest().setQuery("商品２").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response2.getNum());
        assertEquals(1, response2.getTotal());
        assertEquals("商品2", response2.getWords().get(0));
    }

    @Test
    public void test_indexFromDocumentReader() throws Exception {
        Client client = runner.client();
        int num = 10000;
        String indexName = "test";
        String typeName = "test";

        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
        for (int i = 0; i < num; i++) {
            Map<String, Object> source = Collections.singletonMap("content", "test");
            IndexRequestBuilder indexRequestBuilder = new IndexRequestBuilder(client, IndexAction.INSTANCE);
            indexRequestBuilder.setIndex(indexName).setType(typeName).setId(String.valueOf(i)).setCreate(true).setSource(source);
            bulkRequestBuilder.add(indexRequestBuilder);
        }
        bulkRequestBuilder.execute().actionGet();
        runner.refresh();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger numObInputDoc = new AtomicInteger(0);
        ESSourceReader reader = new ESSourceReader(client, suggester.settings(), indexName, typeName);
        reader.setScrollSize(1000);

        suggester.indexer().indexFromDocument(() -> reader, 1000, 100).then(response -> {
            numObInputDoc.set(response.getNumberOfInputDocs());
            latch.countDown();
        }).error(t -> {
            t.printStackTrace();
            latch.countDown();
            fail();
        });
        latch.await();
        assertEquals(num, numObInputDoc.get());

        SuggestResponse response = suggester.suggest().setQuery("test").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response.getNum());
    }

    @Test
    public void test_indexFromSearchWord() throws Exception {
        SuggestIndexResponse indexResponse = suggester.indexer().indexFromSearchWord("検索　 エンジン", null, null, null, 1, null);
        indexResponse.getErrors();
        suggester.refresh();

        final SuggestResponse response = suggester.suggest().setQuery("検索").setSuggestDetail(true).execute().getResponse();
        final List<SuggestItem> items = response.getItems();
        assertEquals(1, response.getNum());
        assertEquals("検索 エンジン", items.get(0).getText());
    }

    @Test
    public void test_indexFromSearchWordExclude() throws Exception {
        SuggestIndexResponse indexResponse = suggester.indexer().indexFromSearchWord("。」", null, null, null, 1, null);
        indexResponse.getErrors();
        assertEquals(0, indexResponse.getNumberOfSuggestDocs());
        suggester.refresh();

        final SuggestResponse response = suggester.suggest().setQuery("。").setSuggestDetail(true).execute().getResponse();
        assertEquals(0, response.getNum());
    }

    @Test
    public void test_indexElevateWord() throws Exception {
        ElevateWord elevateWord =
                new ElevateWord("Test", 2.0f, Collections.singletonList("Test"), Collections.singletonList("content"), null, null);
        suggester.indexer().addElevateWord(elevateWord, true);
        suggester.refresh();
        SuggestResponse response1 = suggester.suggest().setQuery("tes").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response1.getNum());
        assertEquals(1, response1.getTotal());
        assertEquals(2.0f, response1.getItems().get(0).getUserBoost(), 0);

        ElevateWord[] elevateWords = suggester.settings().elevateWord().get();
        assertEquals(1, elevateWords.length);
        assertEquals("test", elevateWords[0].getElevateWord());

        suggester.indexer().deleteElevateWord(elevateWord.getElevateWord(), true);
        suggester.refresh();
        elevateWords = suggester.settings().elevateWord().get();
        assertEquals(0, elevateWords.length);
    }

    @Test
    public void test_restoreElevateWord() throws Exception {
        ElevateWord elevateWord1 =
                new ElevateWord("test", 2.0f, Collections.singletonList("test"), Collections.singletonList("content"), null, null);
        ElevateWord elevateWord2 =
                new ElevateWord("hoge", 2.0f, Collections.singletonList("hoge"), Collections.singletonList("content"), null, null);
        ElevateWord elevateWord3 =
                new ElevateWord("fuga", 2.0f, Collections.singletonList("fuga"), Collections.singletonList("content"), null, null);

        suggester.settings().elevateWord().add(elevateWord1);
        suggester.settings().elevateWord().add(elevateWord2);
        suggester.settings().elevateWord().add(elevateWord3);

        suggester.indexer().restoreElevateWord();
        suggester.refresh();

        SuggestResponse response1 = suggester.suggest().setQuery("tes").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response1.getNum());
        SuggestResponse response2 = suggester.suggest().setQuery("hoge").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response2.getNum());
        SuggestResponse response3 = suggester.suggest().setQuery("fuga").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response3.getNum());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_deleteOldWords() throws Exception {
        String field = suggester.settings().array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];
        ElevateWord elevateWord =
                new ElevateWord("test", 2.0f, Collections.singletonList("test"), Collections.singletonList("content"), null, null);

        suggester.indexer().indexFromDocument(new Map[] { Collections.singletonMap(field, (Object) "この柿は美味しい。") });
        suggester.indexer().addElevateWord(elevateWord, true);
        suggester.refresh();

        Thread.sleep(1000);
        ZonedDateTime threshold = ZonedDateTime.now();
        Thread.sleep(1000);

        suggester.indexer().indexFromDocument(new Map[] { Collections.singletonMap(field, (Object) "検索エンジン") });
        suggester.refresh();

        SuggestResponse response1 = suggester.suggest().setQuery("柿").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response1.getNum());
        SuggestResponse response2 = suggester.suggest().setQuery("test").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response2.getNum());
        SuggestResponse response3 = suggester.suggest().setQuery("検索").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response3.getNum());

        suggester.indexer().deleteOldWords(threshold);
        suggester.refresh();

        SuggestResponse response4 = suggester.suggest().setQuery("柿").setSuggestDetail(true).execute().getResponse();
        assertEquals(0, response4.getNum());
        SuggestResponse response5 = suggester.suggest().setQuery("test").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response5.getNum());
        SuggestResponse response6 = suggester.suggest().setQuery("検索").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response6.getNum());

    }

    @Test
    public void test_addNgWord() throws Exception {
        SuggestItem[] items = getItemSet1();
        suggester.indexer().index(items);
        suggester.refresh();

        SuggestResponse response = suggester.suggest().setQuery("kensaku").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response.getNum());
        assertEquals("検索 エンジン", response.getWords().get(0));

        response = suggester.suggest().setQuery("zenbun").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response.getNum());
        assertEquals("全文 検索", response.getWords().get(0));

        suggester.indexer().addBadWord("[", true);
        suggester.refresh();
        SuggestResponse response2 = suggester.suggest().setQuery("kensaku").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response2.getNum());

        suggester.indexer().addBadWord("ｴﾝｼﾞﾝ", true);
        suggester.refresh();
        SuggestResponse response3 = suggester.suggest().setQuery("kensaku").setSuggestDetail(true).execute().getResponse();
        assertEquals(0, response3.getNum());
        response3 = suggester.suggest().setQuery("zenbun").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response3.getNum());

        assertEquals(2, suggester.settings().badword().get(false).length);
        suggester.indexer().deleteBadWord("[");
        suggester.indexer().deleteBadWord("ｴﾝｼﾞﾝ");
        assertEquals(0, suggester.settings().badword().get(false).length);
    }

    @Test
    public void test_popularWords() throws Exception {
        SuggestItem[] items = getPopularWordsItemSet2();
        suggester.indexer().index(items);
        suggester.refresh();

        PopularWordsResponse response = suggester.popularWords().setSize(2).execute().getResponse();

        assertEquals(5, response.getTotal());

        for (int i = 0; i < 5; i++) {
            assertEquals(2, response.getNum());
            boolean find = false;
            final String checkStr = "クエリー" + i;
            for (int j = 0; j < 1000; j++) {
                if (response.getWords().contains(checkStr)) {
                    find = true;
                    break;
                }
                response = suggester.popularWords().setSize(2).execute().getResponse();
            }
            assertTrue(find);
        }
    }

    @Test
    public void test_popularWordsExcludeNotQueryWord() throws Exception {
        SuggestItem[] items = getPopularWordsItemSet2();
        suggester.indexer().index(items);
        suggester.refresh();

        PopularWordsResponse response = suggester.popularWords().setSize(100).execute().getResponse();

        assertEquals(5, response.getTotal());

        for (int i = 0; i < 5; i++) {
            assertEquals(5, response.getNum());
            boolean find = false;
            final String checkStr = "クエリー" + i;
            for (int j = 0; j < 1000; j++) {
                if (response.getWords().contains(checkStr)) {
                    find = true;
                    break;
                }
                response = suggester.popularWords().setSize(100).execute().getResponse();
            }
            assertTrue(find);
        }
    }

    @Test
    public void test_popularWordsWithExcludeWord() throws Exception {
        SuggestItem[] items = getPopularWordsItemSet2();
        suggester.indexer().index(items);
        suggester.refresh();

        final String excludeWord = "クエリー1";
        PopularWordsResponse response = suggester.popularWords().setSize(2).addExcludeWord(excludeWord).execute().getResponse();

        assertEquals(4, response.getTotal());

        for (int i = 0; i < 5; i++) {
            assertEquals(2, response.getNum());
            boolean find = false;
            final String checkStr = "クエリー" + i;
            for (int j = 0; j < 1000; j++) {
                if (response.getWords().contains(checkStr)) {
                    find = true;
                    break;
                }
                response = suggester.popularWords().setSize(2).addExcludeWord(excludeWord).execute().getResponse();
            }
            if (checkStr.equals(excludeWord)) {
                assertFalse(find);
            } else {
                assertTrue(find);
            }
        }
    }

    @Test
    public void test_escapeQuery() throws Exception {
        SuggestItem[] items = getItemSet2();
        suggester.indexer().index(items);
        suggester.refresh();

        SuggestResponse response = suggester.suggest().setSuggestDetail(true).execute().getResponse();
        assertEquals(3, response.getNum());

        SuggestResponse response2 = suggester.suggest().setQuery("-a").setSuggestDetail(true).execute().getResponse();
        assertEquals(2, response2.getNum());

        SuggestResponse response3 = suggester.suggest().setQuery("-aa-").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response3.getNum());
    }

    @Test
    public void test_getNum() throws Exception {
        SuggestItem[] items = getItemSet1();
        suggester.indexer().index(items);
        suggester.refresh();

        assertEquals(2, suggester.getAllWordsNum());
        assertEquals(2, suggester.getDocumentWordsNum());
        assertEquals(1, suggester.getQueryWordsNum());
    }

    @Test
    public void test_deleteAllWords() throws Exception {
        SuggestItem[] items = getItemSet1();
        suggester.indexer().index(items);
        suggester.refresh();

        assertEquals(2, suggester.getAllWordsNum());
        assertEquals(2, suggester.getDocumentWordsNum());
        assertEquals(1, suggester.getQueryWordsNum());

        suggester.indexer().deleteAll();
        suggester.refresh();
        assertEquals(0, suggester.getAllWordsNum());
        assertEquals(0, suggester.getDocumentWordsNum());
        assertEquals(0, suggester.getQueryWordsNum());
    }

    @Test
    public void test_deleteDocumentWords() throws Exception {
        SuggestItem[] items = getItemSet1();
        suggester.indexer().index(items);
        suggester.refresh();

        assertEquals(2, suggester.getAllWordsNum());
        assertEquals(2, suggester.getDocumentWordsNum());
        assertEquals(1, suggester.getQueryWordsNum());

        suggester.indexer().deleteDocumentWords();
        suggester.refresh();
        assertEquals(1, suggester.getAllWordsNum());
        assertEquals(0, suggester.getDocumentWordsNum());
        assertEquals(1, suggester.getQueryWordsNum());
    }

    @Test
    public void test_deleteQueryWords() throws Exception {
        SuggestItem[] items = getItemSet1();
        suggester.indexer().index(items);
        suggester.refresh();

        assertEquals(2, suggester.getAllWordsNum());
        assertEquals(2, suggester.getDocumentWordsNum());
        assertEquals(1, suggester.getQueryWordsNum());

        suggester.indexer().deleteQueryWords();
        suggester.refresh();
        assertEquals(2, suggester.getAllWordsNum());
        assertEquals(2, suggester.getDocumentWordsNum());
        assertEquals(0, suggester.getQueryWordsNum());
    }

    @Test
    public void test_switchIndex() throws Exception {
        SuggestItem[] items = getItemSet1();
        suggester.indexer().index(items);
        suggester.refresh();

        SuggestResponse response = suggester.suggest().setQuery("kensaku").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response.getNum());
        assertEquals("検索 エンジン", response.getWords().get(0));

        SuggestResponse response2 = suggester.suggest().setSuggestDetail(true).execute().getResponse();
        assertEquals(2, response2.getNum());

        Thread.sleep(1000);
        suggester.createNextIndex();
        SuggestItem[] items2 = getItemSet2();
        suggester.indexer().index(items2);
        suggester.refresh();
        response = suggester.suggest().setQuery("kensaku").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response.getNum());
        assertEquals("検索 エンジン", response.getWords().get(0));

        response2 = suggester.suggest().setSuggestDetail(true).execute().getResponse();
        assertEquals(2, response2.getNum());

        suggester.switchIndex();

        response = suggester.suggest().setQuery("kensaku").setSuggestDetail(true).execute().getResponse();
        assertEquals(0, response.getNum());

        response = suggester.suggest().setSuggestDetail(true).execute().getResponse();
        assertEquals(3, response.getNum());

        response2 = suggester.suggest().setQuery("-a").setSuggestDetail(true).execute().getResponse();
        assertEquals(2, response2.getNum());

        SuggestResponse response3 = suggester.suggest().setQuery("-aa-").setSuggestDetail(true).execute().getResponse();
        assertEquals(1, response3.getNum());

        GetIndexResponse getIndexResponse = runner.client().admin().indices().prepareGetIndex().execute().actionGet();
        int count = 0;
        for (String index : getIndexResponse.getIndices()) {
            if (index.startsWith(suggester.getIndex())) {
                count++;
            }
        }
        assertEquals(2, count);

        suggester.removeDisableIndices();
        response = suggester.suggest().setSuggestDetail(true).execute().getResponse();
        assertEquals(3, response.getNum());

        getIndexResponse = runner.client().admin().indices().prepareGetIndex().execute().actionGet();
        count = 0;
        for (String index : getIndexResponse.getIndices()) {
            if (index.startsWith(suggester.getIndex())) {
                count++;
            }
        }
        assertEquals(1, count);
    }

    private SuggestItem[] getItemSet1() {
        SuggestItem[] queryItems = new SuggestItem[3];

        String[][] readings = new String[2][];
        readings[0] = new String[] { "kensaku", "fuga" };
        readings[1] = new String[] { "enjin", "fuga" };
        String[] tags = new String[] { "tag1", "tag2" };
        String[] roles = new String[] { SuggestConstants.DEFAULT_ROLE, "role1", "role2", "role3" };
        queryItems[0] =
                new SuggestItem(new String[] { "検索", "エンジン" }, readings, new String[] { "content" }, 1, 0, -1, tags, roles, null,
                        SuggestItem.Kind.DOCUMENT);

        String[][] readings2 = new String[2][];
        readings2[0] = new String[] { "zenbun", "fuga" };
        readings2[1] = new String[] { "kensaku", "fuga" };
        String[] tags2 = new String[] { "tag3" };
        String[] roles2 = new String[] { SuggestConstants.DEFAULT_ROLE, "role4" };
        queryItems[1] =
                new SuggestItem(new String[] { "全文", "検索" }, readings2, new String[] { "content" }, 1, 0, -1, tags2, roles2, null,
                        SuggestItem.Kind.DOCUMENT);

        String[][] readings2Query = new String[2][];
        readings2Query[0] = new String[] { "zenbun", "fuga" };
        readings2Query[1] = new String[] { "kensaku", "fuga" };
        String[] tags2Query = new String[] { "tag4" };
        String[] roles2Query = new String[] { SuggestConstants.DEFAULT_ROLE, "role5" };
        queryItems[2] =
                new SuggestItem(new String[] { "全文", "検索" }, readings2Query, new String[] { "content" }, 0, 1, -1, tags2Query, roles2Query,
                        null, SuggestItem.Kind.QUERY);

        return queryItems;
    }

    private SuggestItem[] getPopularWordsItemSet2() {
        List<SuggestItem> items = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            String[][] readings = new String[2][];
            readings[0] = new String[] { "fuga" };
            String[] tags = new String[] { "tag1", "tag2" };
            String[] roles = new String[] { SuggestConstants.DEFAULT_ROLE, "role1", "role2", "role3" };
            items.add(new SuggestItem(new String[] { "ドキュメント" + i }, readings, new String[] { "content" }, 15 + i, 0, -1, tags, roles,
                    null, SuggestItem.Kind.DOCUMENT));
        }

        for (int i = 0; i < 5; i++) {
            String[][] readings = new String[2][];
            readings[0] = new String[] { "fuga" };
            String[] tags = new String[] { "tag1", "tag2" };
            String[] roles = new String[] { SuggestConstants.DEFAULT_ROLE, "role1", "role2", "role3" };
            items.add(new SuggestItem(new String[] { "クエリー" + i }, readings, new String[] { "content" }, 0, 15 + i, -1, tags, roles, null,
                    SuggestItem.Kind.QUERY));
        }

        for (int i = 0; i < 5; i++) {
            String[][] readings = new String[2][];
            readings[0] = new String[] { "fuga" };
            readings[1] = new String[] { "enjin" };
            String[] tags = new String[] { "tag1", "tag2" };
            String[] roles = new String[] { SuggestConstants.DEFAULT_ROLE, "role1", "role2", "role3" };
            items.add(new SuggestItem(new String[] { "マルチワード" + i, "fuga" }, readings, new String[] { "content" }, 0, 15 + i, -1, tags,
                    roles, null, SuggestItem.Kind.QUERY));
        }

        return items.toArray(new SuggestItem[items.size()]);
    }

    private SuggestItem[] getItemSet2() {
        List<SuggestItem> items = new ArrayList<>();

        {
            String[][] readings = new String[2][];
            readings[0] = new String[] { "-aaa" };
            String[] tags = new String[] { "tag1", "tag2" };
            String[] roles = new String[] { SuggestConstants.DEFAULT_ROLE, "role1", "role2", "role3" };
            items.add(new SuggestItem(new String[] { "ドキュメント" + 1 }, readings, new String[] { "content" }, 15, 0, -1, tags, roles, null,
                    SuggestItem.Kind.DOCUMENT));
        }

        {
            String[][] readings = new String[2][];
            readings[0] = new String[] { "-aa-a" };
            String[] tags = new String[] { "tag1", "tag2" };
            String[] roles = new String[] { SuggestConstants.DEFAULT_ROLE, "role1", "role2", "role3" };
            items.add(new SuggestItem(new String[] { "ドキュメント" + 2 }, readings, new String[] { "content" }, 15, 0, -1, tags, roles, null,
                    SuggestItem.Kind.DOCUMENT));
        }
        {
            String[][] readings = new String[2][];
            readings[0] = new String[] { "aa-a" };
            String[] tags = new String[] { "tag1", "tag2" };
            String[] roles = new String[] { SuggestConstants.DEFAULT_ROLE, "role1", "role2", "role3" };
            items.add(new SuggestItem(new String[] { "ドキュメント" + 3 }, readings, new String[] { "content" }, 15, 0, -1, tags, roles, null,
                    SuggestItem.Kind.DOCUMENT));
        }

        return items.toArray(new SuggestItem[items.size()]);
    }
}
