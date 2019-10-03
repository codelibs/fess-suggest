/*
 * Copyright 2009-2019 the CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.suggest.settings;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.Suggester;
import org.elasticsearch.index.IndexNotFoundException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class BadWordSettingsTest {
    String id = "badwordSettingsTest";

    static SuggestSettings settings;

    static ElasticsearchClusterRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new ElasticsearchClusterRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
            // settingsBuilder.putList("discovery.seed_hosts", "127.0.0.1:9301");
            // settingsBuilder.putList("cluster.initial_master_nodes", "127.0.0.1:9301");
        }).build(newConfigs().clusterName("ArraySettingsTest").numOfNode(1)
                .pluginTypes("org.codelibs.elasticsearch.kuromoji.ipadic.neologd.KuromojiNeologdPlugin"));
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
    public void test_badWordIndexName() {
        assertEquals(".suggest_badword", settings.badword().arraySettings.arraySettingsIndexName);
    }

    @Test
    public void test_validation() {
        try {
            settings.badword().add("aaaa");
            assertTrue(true);
        } catch (IllegalArgumentException e) {
            fail();
        }

        try {
            settings.badword().add("");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            settings.badword().add("aaaa bbb");
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
        settings.badword().add(value1);
        settings.badword().add(value2);
        settings.badword().add(value3);
        assertEquals(3, settings.badword().get(false).length);
        assertEquals(value1, settings.badword().get(false)[0]);
        assertEquals(value2, settings.badword().get(false)[1]);
        assertEquals(value3, settings.badword().get(false)[2]);
    }

    @Test
    public void test_delete() {
        String value1 = "a";
        String value2 = "b";
        String value3 = "c";
        settings.badword().add(value1);
        settings.badword().add(value2);
        settings.badword().add(value3);
        assertEquals(3, settings.badword().get(false).length);
        assertEquals(value1, settings.badword().get(false)[0]);
        assertEquals(value2, settings.badword().get(false)[1]);
        assertEquals(value3, settings.badword().get(false)[2]);

        settings.badword().delete(value2);
        assertEquals(2, settings.badword().get(false).length);
        assertEquals(value1, settings.badword().get(false)[0]);
        assertEquals(value3, settings.badword().get(false)[1]);

        settings.badword().deleteAll();
        assertEquals(0, settings.badword().get(false).length);
    }

}
