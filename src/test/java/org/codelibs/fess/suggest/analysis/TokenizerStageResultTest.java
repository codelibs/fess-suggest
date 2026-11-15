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
package org.codelibs.fess.suggest.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codelibs.fesen.opensearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;
import org.junit.Test;

public class TokenizerStageResultTest {

    private static AnalyzeToken token(final String term, final int startOffset, final int endOffset) {
        return new AnalyzeToken(term, 0, startOffset, endOffset, 1, "word", Map.of("partOfSpeech", "名詞-一般"));
    }

    @Test
    public void testAccessors() {
        final List<AnalyzeToken> tokens = List.of(token("新型", 0, 2), token("コロナ", 2, 5));

        final TokenizerStageResult result = new TokenizerStageResult(tokens, "新型コロナ");

        assertEquals(tokens, result.getTokens());
        assertEquals("新型コロナ", result.getCharFilteredText());
    }

    @Test
    public void testTokensAreCopied() {
        final List<AnalyzeToken> tokens = new ArrayList<>(List.of(token("新型", 0, 2)));
        final TokenizerStageResult result = new TokenizerStageResult(tokens, "新型");

        tokens.add(token("コロナ", 2, 5));

        assertEquals(1, result.getTokens().size());
        assertThrows(UnsupportedOperationException.class, () -> result.getTokens().add(token("コロナ", 2, 5)));
    }

    @Test
    public void testEmptyTokensAreAccepted() {
        // The result of a tokenizer without part of speech: no tokens, but the char-filtered text is known.
        final TokenizerStageResult result = new TokenizerStageResult(List.of(), "hello world");

        assertEquals(List.of(), result.getTokens());
        assertEquals("hello world", result.getCharFilteredText());
    }

    @Test
    public void testNullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> new TokenizerStageResult(null, "text"));
        assertThrows(NullPointerException.class, () -> new TokenizerStageResult(List.of(), null));
    }
}
