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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.junit.Test;

public class PopularWordsResponseTest {

    @Test
    public void test_constructor() throws Exception {
        List<String> words = new ArrayList<>();
        words.add("word1");
        words.add("word2");

        List<SuggestItem> items = new ArrayList<>();
        String[][] readings = new String[1][];
        readings[0] = new String[] { "word1" };
        items.add(new SuggestItem(new String[] { "word1" }, readings, new String[] { "content" }, 0, 10, -1, new String[] { "tag1" },
                new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.QUERY));

        PopularWordsResponse response = new PopularWordsResponse("test-index", 100, words, 10, items);

        assertNotNull(response);
        assertEquals("test-index", response.getIndex());
        assertEquals(100, response.getTookMs());
        assertEquals(2, response.getNum());
        assertEquals(10, response.getTotal());
        assertEquals(2, response.getWords().size());
        assertEquals(1, response.getItems().size());
    }

    @Test
    public void test_getIndex() throws Exception {
        List<String> words = new ArrayList<>();
        PopularWordsResponse response = new PopularWordsResponse("my-index", 50, words, 5, new ArrayList<>());

        assertEquals("my-index", response.getIndex());
    }

    @Test
    public void test_getTookMs() throws Exception {
        List<String> words = new ArrayList<>();
        PopularWordsResponse response = new PopularWordsResponse("test-index", 123, words, 5, new ArrayList<>());

        assertEquals(123, response.getTookMs());
    }

    @Test
    public void test_getWords() throws Exception {
        List<String> words = new ArrayList<>();
        words.add("apple");
        words.add("banana");
        words.add("cherry");

        PopularWordsResponse response = new PopularWordsResponse("test-index", 100, words, 10, new ArrayList<>());

        assertNotNull(response.getWords());
        assertEquals(3, response.getWords().size());
        assertTrue(response.getWords().contains("apple"));
        assertTrue(response.getWords().contains("banana"));
        assertTrue(response.getWords().contains("cherry"));
    }

    @Test
    public void test_getNum() throws Exception {
        List<String> words = new ArrayList<>();
        words.add("word1");
        words.add("word2");
        words.add("word3");
        words.add("word4");

        PopularWordsResponse response = new PopularWordsResponse("test-index", 100, words, 20, new ArrayList<>());

        assertEquals(4, response.getNum());
    }

    @Test
    public void test_getTotal() throws Exception {
        List<String> words = new ArrayList<>();
        words.add("word1");

        PopularWordsResponse response = new PopularWordsResponse("test-index", 100, words, 100, new ArrayList<>());

        assertEquals(100, response.getTotal());
    }

    @Test
    public void test_getItems() throws Exception {
        List<String> words = new ArrayList<>();
        words.add("word1");

        List<SuggestItem> items = new ArrayList<>();
        String[][] readings = new String[1][];
        readings[0] = new String[] { "word1" };
        items.add(new SuggestItem(new String[] { "word1" }, readings, new String[] { "content" }, 0, 10, -1, new String[] { "tag1" },
                new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.QUERY));
        items.add(new SuggestItem(new String[] { "word2" }, readings, new String[] { "content" }, 0, 5, -1, new String[] { "tag2" },
                new String[] { SuggestConstants.DEFAULT_ROLE }, null, SuggestItem.Kind.QUERY));

        PopularWordsResponse response = new PopularWordsResponse("test-index", 100, words, 10, items);

        assertNotNull(response.getItems());
        assertEquals(2, response.getItems().size());
    }

    @Test
    public void test_emptyResponse() throws Exception {
        List<String> words = new ArrayList<>();
        List<SuggestItem> items = new ArrayList<>();

        PopularWordsResponse response = new PopularWordsResponse("test-index", 10, words, 0, items);

        assertNotNull(response);
        assertEquals(0, response.getNum());
        assertEquals(0, response.getTotal());
        assertEquals(0, response.getWords().size());
        assertEquals(0, response.getItems().size());
    }

    @Test
    public void test_numMatchesWordsSize() throws Exception {
        List<String> words = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            words.add("word" + i);
        }

        PopularWordsResponse response = new PopularWordsResponse("test-index", 100, words, 100, new ArrayList<>());

        assertEquals(words.size(), response.getNum());
    }
}
