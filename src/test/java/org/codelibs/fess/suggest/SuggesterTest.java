package org.codelibs.fess.suggest;

import junit.framework.TestCase;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.SuggestIndexer;
import org.codelibs.fess.suggest.request.suggest.SuggestResponse;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;

public class SuggesterTest extends TestCase {
    Suggester suggester;

    @Override
    public void setUp() throws Exception {
        ElasticsearchClusterRunner runner = new ElasticsearchClusterRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("index.number_of_replicas", 0);
            settingsBuilder.put("script.disable_dynamic", false);
            settingsBuilder.put("script.groovy.sandbox.enabled", true);
        }).build(newConfigs().ramIndexStore().numOfNode(1));
        runner.ensureYellow();

        suggester = new Suggester(runner.client());
    }

    public void test_indexAndSuggest() throws Exception {
        SuggestIndexer indexor = suggester.indexer();

        SuggestItem[] items = getItemSet1();
        indexor.index(items);
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
