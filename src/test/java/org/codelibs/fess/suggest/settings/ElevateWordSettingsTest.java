package org.codelibs.fess.suggest.settings;

import junit.framework.TestCase;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;

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
        ElevateWordSettings.ElevateWord elevateWord1 = new ElevateWordSettings.ElevateWord("a", 1.0f, Collections.singletonList("a"));
        ElevateWordSettings.ElevateWord elevateWord2 = new ElevateWordSettings.ElevateWord("b", 0.0f, Collections.singletonList("b"));
        ElevateWordSettings.ElevateWord elevateWord3 = new ElevateWordSettings.ElevateWord("c", 100.0f, Collections.singletonList("c"));

        settings.elevateWord().add(elevateWord1.elevateWord, elevateWord1.boost, elevateWord1.readings);
        settings.elevateWord().add(elevateWord2.elevateWord, elevateWord2.boost, elevateWord2.readings);
        settings.elevateWord().add(elevateWord3.elevateWord, elevateWord3.boost, elevateWord3.readings);

        assertEquals(3, settings.elevateWord().get().length);
        assertEquals(elevateWord1.elevateWord, settings.elevateWord().get()[0].getElevateWord());
        assertEquals(elevateWord1.boost, settings.elevateWord().get()[0].getBoost());
        assertEquals(elevateWord1.readings.get(0), settings.elevateWord().get()[0].getReadings().get(0));

        assertEquals(elevateWord2.elevateWord, settings.elevateWord().get()[1].getElevateWord());
        assertEquals(elevateWord2.boost, settings.elevateWord().get()[1].getBoost());
        assertEquals(elevateWord2.readings.get(0), settings.elevateWord().get()[1].getReadings().get(0));

        assertEquals(elevateWord3.elevateWord, settings.elevateWord().get()[2].getElevateWord());
        assertEquals(elevateWord3.boost, settings.elevateWord().get()[2].getBoost());
        assertEquals(elevateWord3.readings.get(0), settings.elevateWord().get()[2].getReadings().get(0));
    }

    public void test_delete() {
        ElevateWordSettings.ElevateWord elevateWord1 = new ElevateWordSettings.ElevateWord("a", 1.0f, Collections.singletonList("a"));
        ElevateWordSettings.ElevateWord elevateWord2 = new ElevateWordSettings.ElevateWord("b", 0.0f, Collections.singletonList("b"));
        ElevateWordSettings.ElevateWord elevateWord3 = new ElevateWordSettings.ElevateWord("c", 100.0f, Collections.singletonList("c"));

        settings.elevateWord().add(elevateWord1.elevateWord, elevateWord1.boost, elevateWord1.readings);
        settings.elevateWord().add(elevateWord2.elevateWord, elevateWord2.boost, elevateWord2.readings);
        settings.elevateWord().add(elevateWord3.elevateWord, elevateWord3.boost, elevateWord3.readings);
        assertEquals(3, settings.elevateWord().get().length);

        settings.elevateWord().delete(elevateWord2.elevateWord);
        assertEquals(2, settings.elevateWord().get().length);
        assertEquals(elevateWord1.elevateWord, settings.elevateWord().get()[0].getElevateWord());
        assertEquals(elevateWord3.elevateWord, settings.elevateWord().get()[1].getElevateWord());

        settings.elevateWord().deleteAll();
        assertEquals(0, settings.elevateWord().get().length);
    }
}
