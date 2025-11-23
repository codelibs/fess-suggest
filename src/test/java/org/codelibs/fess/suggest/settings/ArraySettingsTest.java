/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
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

import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;
import static org.junit.Assert.assertEquals;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensearch.index.IndexNotFoundException;

public class ArraySettingsTest {
    String id = "arraySettingsTest";

    static SuggestSettings settings;

    static OpenSearchRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        })
                .build(newConfigs().clusterName("ArraySettingsTest")
                        .numOfNode(1)
                        .pluginTypes("org.codelibs.opensearch.extension.ExtensionPlugin"));
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
            runner.admin().indices().prepareDelete("ArraySettingsTest*", "fess_suggest*").execute().actionGet();
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
