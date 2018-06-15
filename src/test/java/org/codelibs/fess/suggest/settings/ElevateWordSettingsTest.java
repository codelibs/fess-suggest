package org.codelibs.fess.suggest.settings;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.entity.ElevateWord;
import org.elasticsearch.index.IndexNotFoundException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ElevateWordSettingsTest {
    String id = "elevateWordSettingsTest";

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
    public void test_elevateWordIndexName() {
        assertEquals(".suggest_elevate", settings.elevateWord().arraySettings.arraySettingsIndexName);
    }

    @Test
    public void test_setAndGet() {
        ElevateWord elevateWord1 =
                new ElevateWord("a", 1.0f, Collections.singletonList("a"), Collections.singletonList("content"), null, null);
        ElevateWord elevateWord2 =
                new ElevateWord("b", 0.0f, Collections.singletonList("b"), Collections.singletonList("content"), null, null);
        ElevateWord elevateWord3 =
                new ElevateWord("c", 100.0f, Collections.singletonList("c"), Collections.singletonList("content"),
                        Collections.singletonList("tag1"), Collections.singletonList("role1"));

        settings.elevateWord().add(elevateWord1);
        settings.elevateWord().add(elevateWord2);
        settings.elevateWord().add(elevateWord3);

        assertEquals(3, settings.elevateWord().get().length);
        assertEquals(elevateWord1.getElevateWord(), settings.elevateWord().get()[0].getElevateWord());
        assertEquals(elevateWord1.getBoost(), settings.elevateWord().get()[0].getBoost(), 0);
        assertEquals(elevateWord1.getReadings().get(0), settings.elevateWord().get()[0].getReadings().get(0));

        assertEquals(elevateWord2.getElevateWord(), settings.elevateWord().get()[1].getElevateWord());
        assertEquals(elevateWord2.getBoost(), settings.elevateWord().get()[1].getBoost(), 0);
        assertEquals(elevateWord2.getReadings().get(0), settings.elevateWord().get()[1].getReadings().get(0));

        assertEquals(elevateWord3.getElevateWord(), settings.elevateWord().get()[2].getElevateWord());
        assertEquals(elevateWord3.getBoost(), settings.elevateWord().get()[2].getBoost(), 0);
        assertEquals(elevateWord3.getReadings().get(0), settings.elevateWord().get()[2].getReadings().get(0));
        assertEquals(elevateWord3.getTags().get(0), settings.elevateWord().get()[2].getTags().get(0));
        assertEquals(elevateWord3.getRoles().get(0), settings.elevateWord().get()[2].getRoles().get(0));
    }

    @Test
    public void test_delete() {
        ElevateWord elevateWord1 =
                new ElevateWord("a", 1.0f, Collections.singletonList("a"), Collections.singletonList("content"), null, null);
        ElevateWord elevateWord2 =
                new ElevateWord("b", 0.0f, Collections.singletonList("b"), Collections.singletonList("content"), null, null);
        ElevateWord elevateWord3 =
                new ElevateWord("c", 100.0f, Collections.singletonList("c"), Collections.singletonList("content"), null, null);

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
