package org.codelibs.fess.suggest.settings;

import junit.framework.TestCase;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;

public class NgWordSettingsTest extends TestCase {
    String id = "ngwordSettingsTest";

    ElasticsearchClusterRunner runner;

    SuggestSettings settings;

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

    public void test_ngWordIndexName() {
        assertEquals(".suggest-ngword", settings.ngword().arraySettings.arraySettingsIndexName);
    }

    public void test_validation() {
        try {
            settings.ngword().add("aaaa");
            assertTrue(true);
        } catch (IllegalArgumentException e) {
            fail();
        }

        try {
            settings.ngword().add("");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            settings.ngword().add("aaaa bbb");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    public void test_setAndGetAsArray() {
        String value1 = "a";
        String value2 = "b";
        String value3 = "c";
        settings.ngword().add(value1);
        settings.ngword().add(value2);
        settings.ngword().add(value3);
        assertEquals(3, settings.ngword().get().length);
        assertEquals(value1, settings.ngword().get()[0]);
        assertEquals(value2, settings.ngword().get()[1]);
        assertEquals(value3, settings.ngword().get()[2]);
    }

    public void test_delete() {
        String value1 = "a";
        String value2 = "b";
        String value3 = "c";
        settings.ngword().add(value1);
        settings.ngword().add(value2);
        settings.ngword().add(value3);
        assertEquals(3, settings.ngword().get().length);
        assertEquals(value1, settings.ngword().get()[0]);
        assertEquals(value2, settings.ngword().get()[1]);
        assertEquals(value3, settings.ngword().get()[2]);

        settings.ngword().delete(value2);
        assertEquals(2, settings.ngword().get().length);
        assertEquals(value1, settings.ngword().get()[0]);
        assertEquals(value3, settings.ngword().get()[1]);

        settings.ngword().deleteAll();
        assertEquals(0, settings.ngword().get().length);
    }

}
