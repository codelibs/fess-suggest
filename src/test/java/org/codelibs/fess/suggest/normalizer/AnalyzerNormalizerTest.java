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
package org.codelibs.fess.suggest.normalizer;

import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AnalyzerNormalizerTest {
    static Suggester suggester;
    static OpenSearchRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        }).build(newConfigs().clusterName("AnalyzerNormalizerTest").numOfNode(1)
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
        runner.admin().indices().prepareDelete("_all").execute().actionGet();
        runner.refresh();
        suggester = Suggester.builder().build(runner.client(), "AnalyzerNormalizerTest");
        suggester.createIndexIfNothing();
    }

    @Test
    public void test_constructor() throws Exception {
        AnalyzerNormalizer normalizer = new AnalyzerNormalizer(runner.client(), suggester.settings());
        assertNotNull(normalizer);
    }

    @Test
    public void test_normalizeWithDefaultLanguage() throws Exception {
        AnalyzerNormalizer normalizer = new AnalyzerNormalizer(runner.client(), suggester.settings());

        String result = normalizer.normalize("test", "content");

        assertNotNull(result);
    }

    @Test
    public void test_normalizeWithLanguage() throws Exception {
        AnalyzerNormalizer normalizer = new AnalyzerNormalizer(runner.client(), suggester.settings());

        String result = normalizer.normalize("test", "content", "en");

        assertNotNull(result);
    }

    @Test
    public void test_normalizeWithMultipleLanguages() throws Exception {
        AnalyzerNormalizer normalizer = new AnalyzerNormalizer(runner.client(), suggester.settings());

        String result = normalizer.normalize("test", "content", "en", "ja");

        assertNotNull(result);
    }

    @Test
    public void test_normalizeEmptyString() throws Exception {
        AnalyzerNormalizer normalizer = new AnalyzerNormalizer(runner.client(), suggester.settings());

        String result = normalizer.normalize("", "content");

        assertNotNull(result);
    }

    @Test
    public void test_normalizeJapaneseText() throws Exception {
        AnalyzerNormalizer normalizer = new AnalyzerNormalizer(runner.client(), suggester.settings());

        String result = normalizer.normalize("検索", "content", "ja");

        assertNotNull(result);
    }

    @Test
    public void test_normalizeEnglishText() throws Exception {
        AnalyzerNormalizer normalizer = new AnalyzerNormalizer(runner.client(), suggester.settings());

        String result = normalizer.normalize("search", "content", "en");

        assertNotNull(result);
        assertEquals("search", result);
    }
}
