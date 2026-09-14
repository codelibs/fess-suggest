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
package org.codelibs.fess.suggest;

import java.io.IOException;
import java.util.List;

import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.codelibs.fesen.opensearch.core.common.Strings;

import junit.framework.TestCase;

public class SuggesterBuilderTest extends TestCase {
    OpenSearchRunner runner;
    SuggestTestServer server;

    @Override
    public void setUp() throws Exception {
        server = SuggestTestServer.start("ArraySettingsTest");
        runner = server.runner();
        runner.ensureYellow();
    }

    @Override
    protected void tearDown() throws Exception {
        server.close();
    }

    public void test_buildWithDefault() throws Exception {
        final String id = "BuildTest";
        final Suggester suggester = Suggester.builder().build(server.client(), id);

        assertNotNull(suggester);
        assertNotNull(suggester.client);
        assertNotNull(suggester.indexer());
        assertNotNull(suggester.getNormalizer());
        assertNotNull(suggester.getReadingConverter());
        assertNotNull(suggester.settings());
        assertTrue(!Strings.isNullOrEmpty(suggester.index));
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
                .settings(SuggestSettings.builder().setSettingsIndexName(settingsIndexName))
                .readingConverter(converter)
                .normalizer(normalizer)
                .build(server.client(), id);

        assertEquals(server.client(), suggester.client);

        SuggestSettings settings = suggester.settings();
        assertEquals(settingsIndexName, settings.getSettingsIndexName());
        assertEquals(id, settings.getSettingsId());

        assertEquals(converter, suggester.getReadingConverter());
        assertEquals(normalizer, suggester.getNormalizer());

    }
}
