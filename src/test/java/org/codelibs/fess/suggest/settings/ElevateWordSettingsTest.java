package org.codelibs.fess.suggest.settings;

import junit.framework.TestCase;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.entity.ElevateWord;

import java.util.Collections;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;

public class ElevateWordSettingsTest extends TestCase {
    String id = "elevateWordSettingsTest";

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

    public void test_elevateWordIndexName() {
        assertEquals(".suggest-elevate", settings.elevateWord().arraySettings.arraySettingsIndexName);
    }

    public void test_setAndGet() {
        ElevateWord elevateWord1 = new ElevateWord("a", 1.0f, Collections.singletonList("a"));
        ElevateWord elevateWord2 = new ElevateWord("b", 0.0f, Collections.singletonList("b"));
        ElevateWord elevateWord3 = new ElevateWord("c", 100.0f, Collections.singletonList("c"));

        settings.elevateWord().add(elevateWord1);
        settings.elevateWord().add(elevateWord2);
        settings.elevateWord().add(elevateWord3);

        assertEquals(3, settings.elevateWord().get().length);
        assertEquals(elevateWord1.getElevateWord(), settings.elevateWord().get()[0].getElevateWord());
        assertEquals(elevateWord1.getBoost(), settings.elevateWord().get()[0].getBoost());
        assertEquals(elevateWord1.getReadings().get(0), settings.elevateWord().get()[0].getReadings().get(0));

        assertEquals(elevateWord2.getElevateWord(), settings.elevateWord().get()[1].getElevateWord());
        assertEquals(elevateWord2.getBoost(), settings.elevateWord().get()[1].getBoost());
        assertEquals(elevateWord2.getReadings().get(0), settings.elevateWord().get()[1].getReadings().get(0));

        assertEquals(elevateWord3.getElevateWord(), settings.elevateWord().get()[2].getElevateWord());
        assertEquals(elevateWord3.getBoost(), settings.elevateWord().get()[2].getBoost());
        assertEquals(elevateWord3.getReadings().get(0), settings.elevateWord().get()[2].getReadings().get(0));
    }

    public void test_delete() {
        ElevateWord elevateWord1 = new ElevateWord("a", 1.0f, Collections.singletonList("a"));
        ElevateWord elevateWord2 = new ElevateWord("b", 0.0f, Collections.singletonList("b"));
        ElevateWord elevateWord3 = new ElevateWord("c", 100.0f, Collections.singletonList("c"));

        settings.elevateWord().add(elevateWord1);
        settings.elevateWord().add(elevateWord2);
        settings.elevateWord().add(elevateWord3);
        assertEquals(3, settings.elevateWord().get().length);

        settings.elevateWord().delete(elevateWord2.getElevateWord());
        assertEquals(2, settings.elevateWord().get().length);
        assertEquals(elevateWord1.getElevateWord(), settings.elevateWord().get()[0].getElevateWord());
        assertEquals(elevateWord3.getElevateWord(), settings.elevateWord().get()[1].getElevateWord());

        settings.elevateWord().deleteAll();
        assertEquals(0, settings.elevateWord().get().length);
    }
}
