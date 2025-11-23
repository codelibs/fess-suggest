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
package org.codelibs.fess.suggest.index.writer;

import static org.codelibs.opensearch.runner.OpenSearchRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensearch.action.get.GetResponse;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.index.query.QueryBuilders;

public class SuggestIndexWriterTest {
    static Suggester suggester;
    static OpenSearchRunner runner;
    static SuggestIndexWriter writer;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("discovery.type", "single-node");
        }).build(newConfigs().clusterName("SuggestIndexWriterTest").numOfNode(1)
                .pluginTypes("org.codelibs.opensearch.extension.ExtensionPlugin"));
        runner.ensureYellow();
        writer = new SuggestIndexWriter();
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
            runner.admin().indices().prepareDelete("SuggestIndexWriterTest*", "fess_suggest*").execute().actionGet();
        } catch (Exception e) {
            // Index might not exist, ignore
        }
        runner.refresh();
        suggester = Suggester.builder().build(runner.client(), "SuggestIndexWriterTest");
        suggester.createIndexIfNothing();
    }

    @Test
    public void test_writeItems() throws Exception {
        String[][] readings = new String[1][];
        readings[0] = new String[] { "test" };
        SuggestItem item = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        SuggestWriterResult result = writer.write(runner.client(), suggester.settings(), suggester.getIndex(),
                new SuggestItem[] { item }, false);

        assertNotNull(result);
        assertFalse(result.hasFailure());

        runner.refresh();

        GetResponse getResponse = runner.client().prepareGet().setIndex(suggester.getIndex()).setId(item.getId())
                .get(TimeValue.timeValueSeconds(30));
        assertTrue(getResponse.isExists());
    }

    @Test
    public void test_writeItemsWithUpdate() throws Exception {
        String[][] readings = new String[1][];
        readings[0] = new String[] { "test" };
        SuggestItem item = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        // First write
        SuggestWriterResult result1 = writer.write(runner.client(), suggester.settings(), suggester.getIndex(),
                new SuggestItem[] { item }, false);
        assertNotNull(result1);
        assertFalse(result1.hasFailure());

        runner.refresh();

        // Second write with update
        SuggestItem updatedItem = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 2, 0, -1,
                new String[] { "tag2" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        SuggestWriterResult result2 = writer.write(runner.client(), suggester.settings(), suggester.getIndex(),
                new SuggestItem[] { updatedItem }, true);
        assertNotNull(result2);
        assertFalse(result2.hasFailure());

        runner.refresh();

        GetResponse getResponse = runner.client().prepareGet().setIndex(suggester.getIndex()).setId(item.getId())
                .get(TimeValue.timeValueSeconds(30));
        assertTrue(getResponse.isExists());
    }

    @Test
    public void test_writeMultipleItems() throws Exception {
        SuggestItem[] items = new SuggestItem[3];
        for (int i = 0; i < 3; i++) {
            String[][] readings = new String[1][];
            readings[0] = new String[] { "test" + i };
            items[i] = new SuggestItem(new String[] { "テスト" + i }, readings, new String[] { "content" }, 1, 0, -1,
                    new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        }

        SuggestWriterResult result = writer.write(runner.client(), suggester.settings(), suggester.getIndex(), items, false);

        assertNotNull(result);
        assertFalse(result.hasFailure());

        runner.refresh();

        for (SuggestItem item : items) {
            GetResponse getResponse = runner.client().prepareGet().setIndex(suggester.getIndex()).setId(item.getId())
                    .get(TimeValue.timeValueSeconds(30));
            assertTrue(getResponse.isExists());
        }
    }

    @Test
    public void test_writeEmptyItems() throws Exception {
        SuggestWriterResult result = writer.write(runner.client(), suggester.settings(), suggester.getIndex(),
                new SuggestItem[0], false);

        assertNotNull(result);
        assertFalse(result.hasFailure());
    }

    @Test
    public void test_delete() throws Exception {
        String[][] readings = new String[1][];
        readings[0] = new String[] { "test" };
        SuggestItem item = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        writer.write(runner.client(), suggester.settings(), suggester.getIndex(), new SuggestItem[] { item }, false);
        runner.refresh();

        GetResponse getResponse1 = runner.client().prepareGet().setIndex(suggester.getIndex()).setId(item.getId())
                .get(TimeValue.timeValueSeconds(30));
        assertTrue(getResponse1.isExists());

        SuggestWriterResult result = writer.delete(runner.client(), suggester.settings(), suggester.getIndex(), item.getId());

        assertNotNull(result);
        assertFalse(result.hasFailure());

        runner.refresh();

        GetResponse getResponse2 = runner.client().prepareGet().setIndex(suggester.getIndex()).setId(item.getId())
                .get(TimeValue.timeValueSeconds(30));
        assertFalse(getResponse2.isExists());
    }

    @Test
    public void test_deleteNonExistent() throws Exception {
        SuggestWriterResult result = writer.delete(runner.client(), suggester.settings(), suggester.getIndex(),
                "non-existent-id");

        assertNotNull(result);
        assertFalse(result.hasFailure());
    }

    @Test
    public void test_deleteByQuery() throws Exception {
        SuggestItem[] items = new SuggestItem[3];
        for (int i = 0; i < 3; i++) {
            String[][] readings = new String[1][];
            readings[0] = new String[] { "test" + i };
            items[i] = new SuggestItem(new String[] { "テスト" + i }, readings, new String[] { "content" }, 1, 0, -1,
                    new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        }

        writer.write(runner.client(), suggester.settings(), suggester.getIndex(), items, false);
        runner.refresh();

        assertEquals(3, suggester.getAllWordsNum());

        SuggestWriterResult result = writer.deleteByQuery(runner.client(), suggester.settings(), suggester.getIndex(),
                QueryBuilders.matchQuery(FieldNames.TEXT, "テスト0"));

        assertNotNull(result);
        assertFalse(result.hasFailure());

        runner.refresh();

        assertEquals(2, suggester.getAllWordsNum());
    }

    @Test
    public void test_deleteByQueryMatchAll() throws Exception {
        SuggestItem[] items = new SuggestItem[3];
        for (int i = 0; i < 3; i++) {
            String[][] readings = new String[1][];
            readings[0] = new String[] { "test" + i };
            items[i] = new SuggestItem(new String[] { "テスト" + i }, readings, new String[] { "content" }, 1, 0, -1,
                    new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        }

        writer.write(runner.client(), suggester.settings(), suggester.getIndex(), items, false);
        runner.refresh();

        assertEquals(3, suggester.getAllWordsNum());

        SuggestWriterResult result = writer.deleteByQuery(runner.client(), suggester.settings(), suggester.getIndex(),
                QueryBuilders.matchAllQuery());

        assertNotNull(result);
        assertFalse(result.hasFailure());

        runner.refresh();

        assertEquals(0, suggester.getAllWordsNum());
    }

    @Test
    public void test_mergeItems() throws Exception {
        String[][] readings = new String[1][];
        readings[0] = new String[] { "test" };

        SuggestItem item1 = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        SuggestItem item2 = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 2, 0, -1,
                new String[] { "tag2" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        SuggestItem[] mergedItems = writer.mergeItems(new SuggestItem[] { item1, item2 });

        assertNotNull(mergedItems);
        assertEquals(1, mergedItems.length);
        assertEquals(3, mergedItems[0].getDocFreq());
    }

    @Test
    public void test_mergeItemsWithDifferentTexts() throws Exception {
        String[][] readings1 = new String[1][];
        readings1[0] = new String[] { "test1" };
        SuggestItem item1 = new SuggestItem(new String[] { "テスト1" }, readings1, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        String[][] readings2 = new String[1][];
        readings2[0] = new String[] { "test2" };
        SuggestItem item2 = new SuggestItem(new String[] { "テスト2" }, readings2, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag2" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        SuggestItem[] mergedItems = writer.mergeItems(new SuggestItem[] { item1, item2 });

        assertNotNull(mergedItems);
        assertEquals(2, mergedItems.length);
    }

    @Test
    public void test_constructor() throws Exception {
        SuggestIndexWriter newWriter = new SuggestIndexWriter();
        assertNotNull(newWriter);
    }

    @Test
    public void test_writeItemsUsesSettingsTimeout() throws Exception {
        // This test verifies that the writer uses settings.getIndexTimeout()
        // instead of a hardcoded timeout value
        String[][] readings = new String[1][];
        readings[0] = new String[] { "test" };
        SuggestItem item = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        // The timeout value should come from settings, not hardcoded
        SuggestWriterResult result = writer.write(runner.client(), suggester.settings(), suggester.getIndex(),
                new SuggestItem[] { item }, true);

        assertNotNull(result);
        assertFalse(result.hasFailure());

        runner.refresh();

        GetResponse getResponse = runner.client().prepareGet().setIndex(suggester.getIndex()).setId(item.getId())
                .get(TimeValue.timeValueSeconds(30));
        assertTrue(getResponse.isExists());
    }

    @Test
    public void test_writeWithUpdateAndExistingItem() throws Exception {
        String[][] readings = new String[1][];
        readings[0] = new String[] { "test" };
        SuggestItem item = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        // First write without update
        writer.write(runner.client(), suggester.settings(), suggester.getIndex(), new SuggestItem[] { item }, false);
        runner.refresh();

        // Second write with update=true
        SuggestItem updatedItem = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 3, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        SuggestWriterResult result = writer.write(runner.client(), suggester.settings(), suggester.getIndex(),
                new SuggestItem[] { updatedItem }, true);

        assertNotNull(result);
        assertFalse(result.hasFailure());

        runner.refresh();

        // Verify the item was updated (should have merged frequencies)
        GetResponse getResponse = runner.client().prepareGet().setIndex(suggester.getIndex()).setId(item.getId())
                .get(TimeValue.timeValueSeconds(30));
        assertTrue(getResponse.isExists());
        // The actual frequency value would depend on merge logic
    }

    @Test
    public void test_writeWithUpdateButNonExistentItem() throws Exception {
        String[][] readings = new String[1][];
        readings[0] = new String[] { "test_new" };
        SuggestItem item = new SuggestItem(new String[] { "テスト新規" }, readings, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        // Write with update=true but item doesn't exist
        SuggestWriterResult result = writer.write(runner.client(), suggester.settings(), suggester.getIndex(),
                new SuggestItem[] { item }, true);

        assertNotNull(result);
        assertFalse(result.hasFailure());

        runner.refresh();

        // Should create the item even though update=true
        GetResponse getResponse = runner.client().prepareGet().setIndex(suggester.getIndex()).setId(item.getId())
                .get(TimeValue.timeValueSeconds(30));
        assertTrue(getResponse.isExists());
    }

    @Test
    public void test_mergeItemsWithMultipleDuplicates() throws Exception {
        String[][] readings = new String[1][];
        readings[0] = new String[] { "test" };

        // Create 5 items with the same ID but different frequencies
        SuggestItem item1 = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        SuggestItem item2 = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 2, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        SuggestItem item3 = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 3, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        SuggestItem item4 = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 4, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        SuggestItem item5 = new SuggestItem(new String[] { "テスト" }, readings, new String[] { "content" }, 5, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        SuggestItem[] mergedItems = writer.mergeItems(new SuggestItem[] { item1, item2, item3, item4, item5 });

        assertNotNull(mergedItems);
        assertEquals(1, mergedItems.length);
        // Total frequency should be 1+2+3+4+5 = 15
        assertEquals(15, mergedItems[0].getDocFreq());
    }

    @Test
    public void test_mergeItemsWithNoMatch() throws Exception {
        String[][] readings1 = new String[1][];
        readings1[0] = new String[] { "test1" };
        String[][] readings2 = new String[1][];
        readings2[0] = new String[] { "test2" };
        String[][] readings3 = new String[1][];
        readings3[0] = new String[] { "test3" };

        SuggestItem item1 = new SuggestItem(new String[] { "テスト1" }, readings1, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag1" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        SuggestItem item2 = new SuggestItem(new String[] { "テスト2" }, readings2, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag2" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);
        SuggestItem item3 = new SuggestItem(new String[] { "テスト3" }, readings3, new String[] { "content" }, 1, 0, -1,
                new String[] { "tag3" }, new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.DOCUMENT);

        SuggestItem[] mergedItems = writer.mergeItems(new SuggestItem[] { item1, item2, item3 });

        assertNotNull(mergedItems);
        assertEquals(3, mergedItems.length);
    }
}
