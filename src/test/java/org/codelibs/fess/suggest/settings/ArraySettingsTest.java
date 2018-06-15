package org.codelibs.fess.suggest.settings;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.junit.Assert.assertEquals;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.Suggester;
import org.elasticsearch.index.IndexNotFoundException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ArraySettingsTest {
    String id = "arraySettingsTest";

    static SuggestSettings settings;

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
        try {
            runner.admin().indices().prepareDelete("_all").execute().actionGet();
        } catch (IndexNotFoundException ignore) {

        }
        runner.refresh();
        settings = Suggester.builder().build(runner.client(), id).settings();
    }

    @Test
    public void test_setAndGetAsArray() {
        String key = "key";
        String value1 = "a";
        String value2 = "b";
        String value3 = "c";
        settings.array().add(key, value1);
        settings.array().add(key, value2);
        settings.array().add(key, value3);
        assertEquals(3, settings.array().get(key).length);
        assertEquals(value1, settings.array().get(key)[0]);
        assertEquals(value2, settings.array().get(key)[1]);
        assertEquals(value3, settings.array().get(key)[2]);
    }

    @Test
    public void test_delete() {
        String key = "key";
        String value1 = "a";
        String value2 = "b";
        String value3 = "c";
        settings.array().add(key, value1);
        settings.array().add(key, value2);
        settings.array().add(key, value3);
        assertEquals(3, settings.array().get(key).length);
        assertEquals(value1, settings.array().get(key)[0]);
        assertEquals(value2, settings.array().get(key)[1]);
        assertEquals(value3, settings.array().get(key)[2]);

        settings.array().delete(key, value2);
        assertEquals(2, settings.array().get(key).length);
        assertEquals(value1, settings.array().get(key)[0]);
        assertEquals(value3, settings.array().get(key)[1]);

        settings.array().delete(key);
        assertEquals(0, settings.array().get(key).length);
    }
}
