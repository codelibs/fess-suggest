package org.codelibs.fess.suggest.settings;

import junit.framework.TestCase;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;

public class SuggestSettingsTest extends TestCase {
    String id = "settings-test";

    SuggestSettings settings;

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

        settings = SuggestSettings.builder().build(runner.client(), id);
        settings.init();
    }

    @Override
    protected void tearDown() throws Exception {
        runner.close();
        runner.clean();
    }

    public void test_defaultSettings() throws Exception {
        assertEquals("settings-test-suggest", settings.getAsString(SuggestSettings.DefaultKeys.INDEX, ""));
        assertEquals("doc", settings.getAsString(SuggestSettings.DefaultKeys.TYPE, ""));
        assertEquals("content", settings.getAsArray(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS)[0]);
        assertEquals("label", settings.getAsString(SuggestSettings.DefaultKeys.TAG_FIELD_NAME, ""));
        assertEquals("role", settings.getAsString(SuggestSettings.DefaultKeys.ROLE_FIELD_NAME, ""));
    }

    public void test_useExistenceSettings() {
        String indexName = "test";
        settings.set(SuggestSettings.DefaultKeys.INDEX, indexName);

        SuggestSettings newSettingsInstance = SuggestSettings.builder().build(runner.client(), id);
        assertEquals(indexName, newSettingsInstance.getAsString(SuggestSettings.DefaultKeys.INDEX, ""));
    }

    public void test_setAndGetAsInt() {
        String key = "key";
        int value = 1;
        settings.set(key, value);
        assertEquals(value, settings.getAsInt(key, -1));
    }

    public void test_setAndGetAsLong() {
        String key = "key";
        long value = Long.MAX_VALUE;
        settings.set(key, value);
        assertEquals(value, settings.getAsLong(key, -1));
    }

    public void test_setAndGetAsFloat() {
        String key = "key";
        float value = 0.01F;
        settings.set(key, value);
        assertEquals(value, settings.getAsFloat(key, -1));
    }

    public void test_setAndGetAsBoolean() {
        String key = "key";
        settings.set(key, true);
        assertEquals(true, settings.getAsBoolean(key, false));
    }

    public void test_setAndGetAsArray() {
        String key = "key";
        String value1 = "a";
        String value2 = "b";
        String value3 = "c";
        String[] value = new String[]{value1, value2, value3};
        settings.set(key, value);
        assertEquals(value[0], settings.getAsArray(key)[0]);
        assertEquals(value[1], settings.getAsArray(key)[1]);
        assertEquals(value[2], settings.getAsArray(key)[2]);
    }
}
