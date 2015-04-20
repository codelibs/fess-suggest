package org.codelibs.fess.suggest.settings;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.Suggester;
import org.elasticsearch.indices.IndexMissingException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.junit.Assert.*;

public class NgWordSettingsTest {
    String id = "ngwordSettingsTest";

    static SuggestSettings settings;

    static ElasticsearchClusterRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new ElasticsearchClusterRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("index.number_of_replicas", 0);
            settingsBuilder.put("script.disable_dynamic", false);
            settingsBuilder.put("script.groovy.sandbox.enabled", true);
        }).build(newConfigs().ramIndexStore().numOfNode(1));
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
        } catch (IndexMissingException ignore) {

        }
        runner.refresh();
        settings = Suggester.builder().build(runner.client(), id).settings();
    }

    @Test
    public void test_ngWordIndexName() {
        assertEquals(".suggest-ngword", settings.ngword().arraySettings.arraySettingsIndexName);
    }

    @Test
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

    @Test
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

    @Test
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
