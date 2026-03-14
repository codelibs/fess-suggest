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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

public class SuggestRequestTest {
    @Test
    public void test_isHiraganaQuery() throws Exception {
        SuggestQueryBuilder queryBuilder = new SuggestQueryBuilder(null, null, new ArrayList<>(), 2.0f);
        assertTrue(queryBuilder.isHiraganaQuery("あ"));
        assertTrue(queryBuilder.isHiraganaQuery("あおぞら"));
        assertTrue(queryBuilder.isHiraganaQuery("けんさく"));
        assertTrue(queryBuilder.isHiraganaQuery("あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよをわん"));
        assertTrue(queryBuilder.isHiraganaQuery("がぎぐげござじずぜぞだぢづでどばびぶべぼ"));
        assertTrue(queryBuilder.isHiraganaQuery("ぁぃぅぇぉっゃゅょ"));

        assertFalse(queryBuilder.isHiraganaQuery("こ犬"));
        assertFalse(queryBuilder.isHiraganaQuery("abc"));
        assertFalse(queryBuilder.isHiraganaQuery("カキク"));
        assertFalse(queryBuilder.isHiraganaQuery("あカ"));
        assertFalse(queryBuilder.isHiraganaQuery("アか"));
        assertFalse(queryBuilder.isHiraganaQuery("abcあ"));
    }
}
