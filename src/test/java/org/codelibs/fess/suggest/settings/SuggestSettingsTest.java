/*
 * Copyright 2009-2021 the CodeLibs Project and the Others.
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

import static org.codelibs.fesen.runner.FesenRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.codelibs.fesen.index.IndexNotFoundException;
import org.codelibs.fesen.runner.FesenRunner;
import org.codelibs.fess.suggest.Suggester;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SuggestSettingsTest {
    String id = "settings-test";

    static SuggestSettings settings;

    static FesenRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new FesenRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
            // settingsBuilder.putList("discovery.seed_hosts", "127.0.0.1:9301");
            // settingsBuilder.putList("cluster.initial_master_nodes", "127.0.0.1:9301");
        }).build(newConfigs().clusterName("ArraySettingsTest").numOfNode(1).pluginTypes("org.codelibs.fesen.extension.ExtensionPlugin"));
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
