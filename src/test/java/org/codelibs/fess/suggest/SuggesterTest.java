package org.codelibs.fess.suggest;

import junit.framework.TestCase;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.SuggestIndexer;
import org.codelibs.fess.suggest.index.querylog.QueryLogReader;
import org.codelibs.fess.suggest.request.suggest.SuggestResponse;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.action.bulk.BulkResponse;

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
        BulkResponse responses = suggester.indexer().index(items);
        System.out.println(responses);

        suggester.refresh();

        SuggestResponse response = suggester.suggest().setQuery("kensaku").setSuggestDetail(true).execute();
        assertEquals(1, response.getNum());
        assertEquals("検索 エンジン", response.getWords().get(0));
        assertEquals(1, response.getItems().get(0).getScore());

        response = suggester.suggest().setQuery("kensaku　 enj").execute();
        assertEquals(1, response.getNum());
        assertEquals("検索 エンジン", response.getWords().get(0));

        response = suggester.suggest().setQuery("zenbun").setSuggestDetail(true).execute();
        assertEquals(1, response.getNum());
        assertEquals("全文 検索", response.getWords().get(0));
        assertEquals(2, response.getItems().get(0).getScore());
    }

    public void test_indexFromQueryString() throws Exception {
        SuggestSettings settings = suggester.getSettings();
        String field = settings.getAsArray(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        suggester.indexer().indexFromQueryString(field + ":検索");
        suggester.refresh();

        SuggestResponse responseKanji = suggester.suggest().setQuery("検索").setSuggestDetail(true).execute();
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

        suggester.indexer().indexFromQueryString(field + ":検索 AND " + field + ":ワード");
        suggester.refresh();

        SuggestResponse responseMulti = suggester.suggest().setQuery("けんさく わーど").setSuggestDetail(true).execute();

        assertEquals(1, responseMulti.getNum());
        assertEquals(1, responseMulti.getTotal());
        assertEquals("検索 ワード", responseMulti.getWords().get(0));
    }

    public void test_indexFromQueryLog() throws Exception {
        SuggestSettings settings = suggester.getSettings();
        String field = settings.getAsArray(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0];

        final QueryLogReader reader = new QueryLogReader() {
            AtomicInteger count = new AtomicInteger();
            String[] queryLogs = new String[] { field + ":検索", field + ":fess", field + ":検索エンジン" };

            @Override
            public String read() {
                if (count.get() >= queryLogs.length) {
                    return null;
                }
                return queryLogs[count.getAndIncrement()];
            }
        };

        SuggestIndexer.IndexingStatus status = suggester.indexer().indexFromQueryLog(reader);
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

    private SuggestItem[] getItemSet1() {
        SuggestItem[] queryItems = new SuggestItem[3];

        String[][] readings = new String[2][];
        readings[0] = new String[] { "kensaku", "fuga" };
        readings[1] = new String[] { "enjin", "fuga" };
        String[] tags = new String[] { "tag1", "tag2" };
        String[] roles = new String[] { "role1", "role2", "role3" };
        queryItems[0] = new SuggestItem(new String[] { "検索", "エンジン" }, readings, 1, tags, roles, SuggestItem.Kind.DOCUMENT);

        String[][] readings2 = new String[2][];
        readings2[0] = new String[] { "zenbun", "fuga" };
        readings2[1] = new String[] { "kensaku", "fuga" };
        String[] tags2 = new String[] { "tag3" };
        String[] roles2 = new String[] { "role1", "role2", "role3", "role4" };
        queryItems[1] = new SuggestItem(new String[] { "全文", "検索" }, readings2, 1, tags2, roles2, SuggestItem.Kind.DOCUMENT);

        String[][] readings2Query = new String[2][];
        readings2Query[0] = new String[] { "zenbun", "fuga" };
        readings2Query[1] = new String[] { "kensaku", "fuga" };
        String[] tags2Query = new String[] { "tag3" };
        String[] roles2Query = new String[] { "role1", "role2", "role3", "role4" };
        queryItems[2] = new SuggestItem(new String[] { "全文", "検索" }, readings2Query, 1, tags2Query, roles2Query, SuggestItem.Kind.QUERY);

        return queryItems;
    }
}
