package org.codelibs.fess.suggest.settings;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.Suggester;
import org.elasticsearch.index.IndexNotFoundException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SuggestSettingsTest {
    String id = "settings-test";

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
    public void test_defaultSettings() throws Exception {
        assertEquals("settings-test.suggest", settings.getAsString(SuggestSettings.DefaultKeys.INDEX, ""));
        assertEquals("doc", settings.getAsString(SuggestSettings.DefaultKeys.TYPE, ""));
        assertEquals("content", settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0]);
        assertEquals("label,virtual_host", settings.getAsString(SuggestSettings.DefaultKeys.TAG_FIELD_NAME, ""));
        assertEquals("role", settings.getAsString(SuggestSettings.DefaultKeys.ROLE_FIELD_NAME, ""));
    }

    @Test
    public void test_useExistenceSettings() {
        String indexName = "test";
        settings.set(SuggestSettings.DefaultKeys.INDEX, indexName);

        SuggestSettings newSettingsInstance = SuggestSettings.builder().build(runner.client(), id);
        newSettingsInstance.init();
        assertEquals(indexName, newSettingsInstance.getAsString(SuggestSettings.DefaultKeys.INDEX, ""));
    }

    @Test
    public void test_useOwnSettings() {
        String indexName = "test";
        settings.set(SuggestSettings.DefaultKeys.INDEX, indexName);

        SuggestSettings anotherSettingsInstance = SuggestSettings.builder().build(runner.client(), id + "-2");
        anotherSettingsInstance.init();
        assertNotSame(indexName, anotherSettingsInstance.getAsString(SuggestSettings.DefaultKeys.INDEX, ""));
        assertEquals("settings-test-2.suggest", anotherSettingsInstance.getAsString(SuggestSettings.DefaultKeys.INDEX, ""));
    }

    @Test
    public void test_setAndGetAsInt() {
        String key = "key";
        int value = 1;
        settings.set(key, value);
        assertEquals(value, settings.getAsInt(key, -1));
    }

    @Test
    public void test_setAndGetAsLong() {
        String key = "key";
        long value = Long.MAX_VALUE;
        settings.set(key, value);
        assertEquals(value, settings.getAsLong(key, -1));
    }

    @Test
    public void test_setAndGetAsFloat() {
        String key = "key";
        float value = 0.01F;
        settings.set(key, value);
        assertEquals(value, settings.getAsFloat(key, -1), 0);
    }

    @Test
    public void test_setAndGetAsBoolean() {
        String key = "key";
        settings.set(key, true);
        assertEquals(true, settings.getAsBoolean(key, false));
    }

}
