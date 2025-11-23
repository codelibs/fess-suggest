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
import static org.junit.Assert.assertNotNull;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PopularWordsRequestBuilderTest {
    static Suggester suggester;
    static OpenSearchRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        }).build(newConfigs().clusterName("PopularWordsRequestBuilderTest").numOfNode(1)
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
        // Delete test indices and settings indices for complete cleanup
        try {
            runner.admin().indices().prepareDelete("PopularWordsRequestBuilderTest*", "fess_suggest*").execute().actionGet();
        } catch (Exception e) {
            // Index might not exist, ignore
        }
        suggester = Suggester.builder().build(runner.client(), "PopularWordsRequestBuilderTest");
        suggester.createIndexIfNothing();
    }

    @Test
    public void test_basicBuilder() throws Exception {
        indexItems();

        PopularWordsResponse response = suggester.popularWords().execute().getResponse();

        assertNotNull(response);
    }

    @Test
    public void test_setSize() throws Exception {
        indexItems();

        PopularWordsResponse response = suggester.popularWords().setSize(5).execute().getResponse();

        assertNotNull(response);
    }

    @Test
    public void test_addTag() throws Exception {
        indexItems();

        PopularWordsResponse response = suggester.popularWords().addTag("tag1").execute().getResponse();

        assertNotNull(response);
    }

    @Test
    public void test_addRole() throws Exception {
        indexItems();

        PopularWordsResponse response = suggester.popularWords().addRole(SuggestConstants.DEFAULT_ROLE).execute().getResponse();

        assertNotNull(response);
    }

    @Test
    public void test_addField() throws Exception {
        indexItems();

        PopularWordsResponse response = suggester.popularWords().addField("content").execute().getResponse();

        assertNotNull(response);
    }

    @Test
    public void test_addExcludeWord() throws Exception {
        indexItems();

        PopularWordsResponse response = suggester.popularWords().addExcludeWord("test").execute().getResponse();

        assertNotNull(response);
    }

    @Test
    public void test_setWindowSize() throws Exception {
        indexItems();

        PopularWordsResponse response = suggester.popularWords().setWindowSize(50).execute().getResponse();

        assertNotNull(response);
    }

    @Test
    public void test_setQueryFreqThreshold() throws Exception {
        indexItems();

        PopularWordsResponse response = suggester.popularWords().setQueryFreqThreshold(5).execute().getResponse();

        assertNotNull(response);
    }

    @Test
    public void test_chainedBuilder() throws Exception {
        indexItems();

        PopularWordsResponse response = suggester.popularWords().setSize(10).addTag("tag1").addRole(SuggestConstants.DEFAULT_ROLE)
                .addField("content").setWindowSize(30).setQueryFreqThreshold(5).execute().getResponse();

        assertNotNull(response);
    }

    private void indexItems() throws Exception {
        SuggestItem[] items = new SuggestItem[3];
        for (int i = 0; i < 3; i++) {
            String[][] readings = new String[1][];
            readings[0] = new String[] { "query" + i };
            items[i] = new SuggestItem(new String[] { "クエリ" + i }, readings, new String[] { "content" }, 0, 15 + i, -1,
                    new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.QUERY);
        }
        suggester.indexer().index(items);
        suggester.refresh();
    }
}
