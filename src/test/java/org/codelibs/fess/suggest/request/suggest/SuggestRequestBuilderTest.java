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
package org.codelibs.fess.suggest.request.suggest;

import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SuggestRequestBuilderTest {
    static Suggester suggester;
    static OpenSearchRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        })
                .build(newConfigs().clusterName("SuggestRequestBuilderTest")
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
        runner.admin().indices().prepareDelete("_all").execute().actionGet();
        runner.refresh();
        suggester = Suggester.builder().build(runner.client(), "SuggestRequestBuilderTest");
        suggester.createIndexIfNothing();
    }

    @Test
    public void test_basicBuilder() throws Exception {
        indexItems();

        SuggestResponse response = suggester.suggest().setQuery("test").execute().getResponse();

        assertNotNull(response);
        assertEquals(1, response.getNum());
    }

    @Test
    public void test_setIndex() throws Exception {
        indexItems();

        SuggestResponse response = suggester.suggest().setIndex(suggester.getIndex()).setQuery("test").execute().getResponse();

        assertNotNull(response);
    }

    @Test
    public void test_setSize() throws Exception {
        indexItems();

        SuggestResponse response = suggester.suggest().setSize(5).execute().getResponse();

        assertNotNull(response);
    }

    @Test
    public void test_setQuery() throws Exception {
        indexItems();

        SuggestResponse response = suggester.suggest().setQuery("test").execute().getResponse();

        assertNotNull(response);
        assertEquals(1, response.getNum());
    }

    @Test
    public void test_addTag() throws Exception {
        indexItems();

        SuggestResponse response = suggester.suggest().setQuery("test").addTag("tag1").execute().getResponse();

        assertNotNull(response);
    }

    @Test
    public void test_addRole() throws Exception {
        indexItems();

        SuggestResponse response = suggester.suggest().setQuery("test").addRole(SuggestConstants.DEFAULT_ROLE).execute().getResponse();

        assertNotNull(response);
        assertEquals(1, response.getNum());
    }

    @Test
    public void test_addField() throws Exception {
        indexItems();

        SuggestResponse response = suggester.suggest().setQuery("test").addField("content").execute().getResponse();

        assertNotNull(response);
    }

    @Test
    public void test_addKind() throws Exception {
        indexItems();

        SuggestResponse response = suggester.suggest().addKind("document").execute().getResponse();

        assertNotNull(response);
    }

    @Test
    public void test_addLang() throws Exception {
        SuggestItem[] items = new SuggestItem[1];
        String[][] readings = new String[1][];
        readings[0] = new String[] { "test" };
        items[0] = new SuggestItem(new String[] { "test" }, readings, new String[] { "content" }, 1, 0, -1, new String[] { "tag1" },
                new String[] { SuggestConstants.DEFAULT_ROLE }, new String[] { "en" }, SuggestItem.Kind.DOCUMENT);

        suggester.indexer().index(items);
        suggester.refresh();

        SuggestResponse response = suggester.suggest().setQuery("test").addLang("en").execute().getResponse();

        assertNotNull(response);
    }

    @Test
    public void test_setSuggestDetail() throws Exception {
        indexItems();

        SuggestResponse response = suggester.suggest().setQuery("test").setSuggestDetail(true).execute().getResponse();

        assertNotNull(response);
        if (response.getNum() > 0) {
            assertNotNull(response.getItems());
        }
    }

    @Test
    public void test_chainedBuilder() throws Exception {
        indexItems();

        SuggestResponse response = suggester.suggest()
                .setQuery("test")
                .setSize(10)
                .addTag("tag1")
                .addRole(SuggestConstants.DEFAULT_ROLE)
                .addField("content")
                .setSuggestDetail(true)
                .execute()
                .getResponse();

        assertNotNull(response);
    }

    private void indexItems() throws Exception {
        SuggestItem[] items = new SuggestItem[1];
        String[][] readings = new String[1][];
        readings[0] = new String[] { "test" };
        items[0] = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 1, 0, -1, new String[] { "tag1" },
                new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        suggester.indexer().index(items);
        suggester.refresh();
    }
}
