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
package org.codelibs.fess.suggest.index.contents.querylog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class QueryLogTest {

    @Test
    public void test_constructor() throws Exception {
        QueryLog queryLog = new QueryLog("test query", "test filter");

        assertNotNull(queryLog);
        assertEquals("test query", queryLog.getQueryString());
        assertEquals("test filter", queryLog.getFilterQueryString());
    }

    @Test
    public void test_constructorWithNullFilter() throws Exception {
        QueryLog queryLog = new QueryLog("test query", null);

        assertNotNull(queryLog);
        assertEquals("test query", queryLog.getQueryString());
        assertNull(queryLog.getFilterQueryString());
    }

    @Test
    public void test_getQueryString() throws Exception {
        QueryLog queryLog = new QueryLog("search term", null);

        assertEquals("search term", queryLog.getQueryString());
    }

    @Test
    public void test_getFilterQueryString() throws Exception {
        QueryLog queryLog = new QueryLog("query", "field:value");

        assertEquals("field:value", queryLog.getFilterQueryString());
    }

    @Test
    public void test_emptyStrings() throws Exception {
        QueryLog queryLog = new QueryLog("", "");

        assertEquals("", queryLog.getQueryString());
        assertEquals("", queryLog.getFilterQueryString());
    }

    @Test
    public void test_complexQuery() throws Exception {
        String query = "field1:value1 AND field2:value2";
        String filterQuery = "category:electronics";
        QueryLog queryLog = new QueryLog(query, filterQuery);

        assertEquals(query, queryLog.getQueryString());
        assertEquals(filterQuery, queryLog.getFilterQueryString());
    }

    @Test
    public void test_japaneseQuery() throws Exception {
        QueryLog queryLog = new QueryLog("検索 エンジン", "カテゴリ:技術");

        assertEquals("検索 エンジン", queryLog.getQueryString());
        assertEquals("カテゴリ:技術", queryLog.getFilterQueryString());
    }

    @Test
    public void test_specialCharacters() throws Exception {
        String query = "test-query_with.special@chars!";
        QueryLog queryLog = new QueryLog(query, null);

        assertEquals(query, queryLog.getQueryString());
    }

    @Test
    public void test_longQuery() throws Exception {
        StringBuilder longQuery = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longQuery.append("word").append(i).append(" ");
        }
        String query = longQuery.toString().trim();

        QueryLog queryLog = new QueryLog(query, null);

        assertEquals(query, queryLog.getQueryString());
    }
}
