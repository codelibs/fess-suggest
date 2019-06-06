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
package org.codelibs.fess.suggest;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;

import java.io.IOException;
import java.util.List;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.common.Strings;

import junit.framework.TestCase;

public class SuggesterBuilderTest extends TestCase {
    ElasticsearchClusterRunner runner;

    @Override
    public void setUp() throws Exception {
        runner = new ElasticsearchClusterRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.putList("discovery.zen.ping.unicast.hosts", "localhost:9301-9399");
        }).build(newConfigs().clusterName("ArraySettingsTest").numOfNode(1)
                .pluginTypes("org.codelibs.elasticsearch.kuromoji.ipadic.neologd.KuromojiNeologdPlugin"));
        runner.ensureYellow();
    }

    @Override
    protected void tearDown() throws Exception {
        runner.close();
        runner.clean();
    }

    public void test_buildWithDefault() throws Exception {
        final String id = "BuildTest";
        final Suggester suggester = Suggester.builder().build(runner.client(), id);

        assertNotNull(suggester);
        assertNotNull(suggester.client);
        assertNotNull(suggester.indexer());
        assertNotNull(suggester.getNormalizer());
        assertNotNull(suggester.getReadingConverter());
        assertNotNull(suggester.settings());
        assertTrue(!Strings.isNullOrEmpty(suggester.index));
        assertTrue(!Strings.isNullOrEmpty(suggester.type));
    }

    public void test_buildWithParameters() throws Exception {
        final String settingsIndexName = "test-settings-index";
        final String settingsTypeName = "test-settings-type";
        final String id = "BuildTest";

        final ReadingConverter converter = new ReadingConverter() {
            @Override
            public void init() throws IOException {

            }

            @Override
            public List<String> convert(String text, final String field, String... langs) throws IOException {
                return null;
            }
        };

        final Normalizer normalizer = (text, field, lang) -> null;

        final Suggester suggester = Suggester.builder()
                .settings(SuggestSettings.builder().setSettingsIndexName(settingsIndexName).setSettingsTypeName(settingsTypeName))
                .readingConverter(converter).normalizer(normalizer).build(runner.client(), id);

        assertEquals(runner.client(), suggester.client);

        SuggestSettings settings = suggester.settings();
        assertEquals(settingsIndexName, settings.getSettingsIndexName());
        assertEquals(settingsTypeName, settings.getSettingsTypeName());
        assertEquals(id, settings.getSettingsId());

        assertEquals(converter, suggester.getReadingConverter());
        assertEquals(normalizer, suggester.getNormalizer());

    }
}
