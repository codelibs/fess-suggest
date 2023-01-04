/*
 * Copyright 2012-2023 CodeLibs Project and the Others.
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

import org.junit.Test;

public class SuggestRequestTest {
    @Test
    public void test_isHiraganaQuery() throws Exception {
        SuggestRequest request = new SuggestRequest();
        assertTrue(request.isHiraganaQuery("あ"));
        assertTrue(request.isHiraganaQuery("あおぞら"));
        assertTrue(request.isHiraganaQuery("けんさく"));
        assertTrue(request.isHiraganaQuery("あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよをわん"));
        assertTrue(request.isHiraganaQuery("がぎぐげござじずぜぞだぢづでどばびぶべぼ"));
        assertTrue(request.isHiraganaQuery("ぁぃぅぇぉっゃゅょ"));

        assertFalse(request.isHiraganaQuery("こ犬"));
        assertFalse(request.isHiraganaQuery("abc"));
        assertFalse(request.isHiraganaQuery("カキク"));
        assertFalse(request.isHiraganaQuery("あカ"));
        assertFalse(request.isHiraganaQuery("アか"));
        assertFalse(request.isHiraganaQuery("abcあ"));
    }
}
