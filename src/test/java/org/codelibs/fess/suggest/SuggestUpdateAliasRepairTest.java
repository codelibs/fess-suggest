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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.codelibs.fess.suggest.index.SuggestIndexResponse;
import org.codelibs.fesen.opensearch.action.admin.indices.alias.Alias;
import org.codelibs.fesen.opensearch.cluster.metadata.AliasMetadata;
import org.codelibs.fesen.opensearch.common.xcontent.XContentType;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Fess 15.8 and earlier created the suggest index through fesen-httpclient, which sent
 * {@code is_write_index: false} for every alias of a create index request. An update alias that
 * points at a single index with an explicit {@code false} has no write index, so every write
 * through it fails until the next index rotation. An existing index must become writable again
 * as soon as the suggester starts.
 */
public class SuggestUpdateAliasRepairTest {
    private static final String ID = "repair-test";

    private static final String SEARCH_ALIAS = ID + ".suggest";

    private static final String UPDATE_ALIAS = SEARCH_ALIAS + ".update";

    private static final String LEGACY_INDEX = SEARCH_ALIAS + ".20250101000000000";

    static OpenSearchRunner runner;
    static SuggestTestServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = SuggestTestServer.start("SuggestUpdateAliasRepairTest");
        runner = server.runner();
        runner.ensureYellow();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.close();
    }

    @Before
    public void before() throws Exception {
        runner.admin().indices().prepareDelete("_all").execute().actionGet();
        runner.refresh();
    }

    @Test
    public void test_updateAliasWithoutWriteIndexIsRepaired() throws Exception {
        createLegacyIndex(Boolean.FALSE);
        assertEquals(Boolean.FALSE, getAlias(UPDATE_ALIAS).writeIndex());

        final Suggester suggester = Suggester.builder().build(server.client(), ID);
        assertFalse(suggester.createIndexIfNothing());

        assertEquals(Boolean.TRUE, getAlias(UPDATE_ALIAS).writeIndex());
        assertEquals("the search alias is left as it is", Boolean.FALSE, getAlias(SEARCH_ALIAS).writeIndex());

        final SuggestIndexResponse response = suggester.indexer().indexFromSearchWord("fess search", null, null, null, 1, null);
        assertFalse("errors: " + response.getErrors(), response.hasError());
        suggester.refresh();
        assertEquals(1, suggester.getAllWordsNum());
    }

    @Test
    public void test_updateAliasWithImplicitWriteIndexIsLeftAlone() throws Exception {
        // an index switched by the suggest indexer carries plain aliases, which are writable already
        createLegacyIndex(null);

        final Suggester suggester = Suggester.builder().build(server.client(), ID);
        assertFalse(suggester.createIndexIfNothing());

        assertNull(getAlias(UPDATE_ALIAS).writeIndex());
        final SuggestIndexResponse response = suggester.indexer().indexFromSearchWord("fess search", null, null, null, 1, null);
        assertFalse("errors: " + response.getErrors(), response.hasError());
        suggester.refresh();
        assertEquals(1, suggester.getAllWordsNum());
    }

    @Test
    public void test_writeFailureIsReported() throws Exception {
        // without the repair the write fails; the response must carry the cause instead of dropping it
        createLegacyIndex(Boolean.FALSE);

        final Suggester suggester = Suggester.builder().build(server.client(), ID);
        final SuggestIndexResponse response = suggester.indexer().indexFromSearchWord("fess search", null, null, null, 1, null);
        assertTrue(response.hasError());
        assertTrue(String.valueOf(response.getErrors()), response.getErrors().get(0).getMessage().contains("no write index"));
    }

    /**
     * Creates the suggest index the way Fess 15.8 did.
     *
     * @param writeIndex the {@code is_write_index} value of both aliases, or null to leave it out
     */
    private static void createLegacyIndex(final Boolean writeIndex) throws IOException {
        server.client()
                .admin()
                .indices()
                .prepareCreate(LEGACY_INDEX)
                .setSettings(readResource("suggest_indices/suggest.json"), XContentType.JSON)
                .setMapping(readResource("suggest_indices/suggest/mappings-default.json"))
                .addAlias(new Alias(SEARCH_ALIAS).writeIndex(writeIndex))
                .addAlias(new Alias(UPDATE_ALIAS).writeIndex(writeIndex))
                .execute()
                .actionGet();
        runner.ensureYellow(LEGACY_INDEX);
    }

    private static AliasMetadata getAlias(final String alias) {
        final Map<String, List<AliasMetadata>> aliases =
                server.client().admin().indices().prepareGetAliases(alias).execute().actionGet().getAliases();
        assertEquals("indices behind " + alias, 1, aliases.size());
        final List<AliasMetadata> list = aliases.get(LEGACY_INDEX);
        return list.stream().filter(m -> alias.equals(m.alias())).findFirst().orElseThrow();
    }

    private static String readResource(final String path) throws IOException {
        try (InputStream is = SuggestUpdateAliasRepairTest.class.getClassLoader().getResourceAsStream(path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
