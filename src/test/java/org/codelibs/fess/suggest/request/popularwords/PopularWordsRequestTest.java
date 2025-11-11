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
package org.codelibs.fess.suggest.request.popularwords;

import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PopularWordsRequestTest {
    static Suggester suggester;
    static OpenSearchRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        }).build(newConfigs().clusterName("PopularWordsRequestTest").numOfNode(1)
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
        suggester = Suggester.builder().build(runner.client(), "PopularWordsRequestTest");
        suggester.createIndexIfNothing();
    }

    @Test
    public void test_basicRequest() throws Exception {
        indexQueryItems();

        PopularWordsResponse response = suggester.popularWords().setSize(10).execute().getResponse();

        assertNotNull(response);
        assertTrue(response.getNum() > 0);
        assertTrue(response.getTotal() > 0);
        assertNotNull(response.getWords());
        assertNotNull(response.getIndex());
    }

    @Test
    public void test_setters() throws Exception {
        PopularWordsRequest request = new PopularWordsRequest();
        request.setIndex("test-index");
        request.setSize(20);
        request.setSeed("test-seed");
        request.setWindowSize(30);
        request.setDetail(false);
        request.setQueryFreqThreshold(5);

        assertNotNull(request);
    }

    @Test
    public void test_addTag() throws Exception {
        indexQueryItems();

        PopularWordsResponse response = suggester.popularWords().setSize(10).addTag("tag1").execute().getResponse();

        assertNotNull(response);
        assertTrue(response.getTotal() > 0);
    }

    @Test
    public void test_addRole() throws Exception {
        indexQueryItems();

        PopularWordsResponse response = suggester.popularWords().setSize(10).addRole(SuggestConstants.DEFAULT_ROLE).execute()
                .getResponse();

        assertNotNull(response);
        assertTrue(response.getTotal() > 0);
    }

    @Test
    public void test_addField() throws Exception {
        indexQueryItems();

        PopularWordsResponse response = suggester.popularWords().setSize(10).addField("content").execute().getResponse();

        assertNotNull(response);
        assertTrue(response.getTotal() > 0);
    }

    @Test
    public void test_addLanguage() throws Exception {
        SuggestItem[] items = new SuggestItem[1];
        String[][] readings = new String[1][];
        readings[0] = new String[] { "test" };
        items[0] = new SuggestItem(new String[] { "test" }, readings, new String[] { "content" }, 0, 15, -1, new String[] { "tag1" },
                new String[] { SuggestConstants.DEFAULT_ROLE }, new String[] { "en" }, SuggestItem.Kind.QUERY);

        suggester.indexer().index(items);
        suggester.refresh();

        PopularWordsResponse response = suggester.popularWords().setSize(10).addLanguage("en").execute().getResponse();

        assertNotNull(response);
        assertTrue(response.getTotal() > 0);
    }

    @Test
    public void test_addExcludeWord() throws Exception {
        indexQueryItems();

        PopularWordsResponse response = suggester.popularWords().setSize(10).addExcludeWord("クエリ0").execute().getResponse();

        assertNotNull(response);
        for (String word : response.getWords()) {
            assertTrue(!word.equals("クエリ0"));
        }
    }

    @Test
    public void test_setQueryFreqThreshold() throws Exception {
        indexQueryItems();

        PopularWordsResponse response = suggester.popularWords().setSize(10).setQueryFreqThreshold(20).execute().getResponse();

        assertNotNull(response);
    }

    @Test
    public void test_withDetail() throws Exception {
        indexQueryItems();

        PopularWordsResponse response = suggester.popularWords().setSize(10).setDetail(true).execute().getResponse();

        assertNotNull(response);
        assertNotNull(response.getItems());
        if (response.getNum() > 0) {
            assertTrue(response.getItems().size() > 0);
        }
    }

    @Test
    public void test_withoutDetail() throws Exception {
        indexQueryItems();

        PopularWordsResponse response = suggester.popularWords().setSize(10).setDetail(false).execute().getResponse();

        assertNotNull(response);
        assertEquals(0, response.getItems().size());
    }

    @Test
    public void test_buildQuery() throws Exception {
        PopularWordsRequest request = new PopularWordsRequest();
        request.setIndex("test-index");
        request.addTag("tag1");
        request.addRole("role1");
        request.addField("field1");
        request.addLanguage("ja");
        request.addExcludeWord("exclude");
        request.setQueryFreqThreshold(5);

        assertNotNull(request.buildQuery());
    }

    @Test
    public void test_buildRescore() throws Exception {
        PopularWordsRequest request = new PopularWordsRequest();
        request.setSeed("test-seed");

        assertNotNull(request.buildRescore());
    }

    @Test
    public void test_constructor() throws Exception {
        PopularWordsRequest request = new PopularWordsRequest();
        assertNotNull(request);
    }

    private void indexQueryItems() throws Exception {
        SuggestItem[] items = new SuggestItem[5];
        for (int i = 0; i < 5; i++) {
            String[][] readings = new String[1][];
            readings[0] = new String[] { "query" + i };
            items[i] = new SuggestItem(new String[] { "クエリ" + i }, readings, new String[] { "content" }, 0, 15 + i, -1,
                    new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.QUERY);
        }
        suggester.indexer().index(items);
        suggester.refresh();
    }
}
