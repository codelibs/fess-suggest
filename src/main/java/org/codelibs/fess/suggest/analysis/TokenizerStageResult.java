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

import java.util.List;
import java.util.Objects;

import org.codelibs.fesen.opensearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;

/**
 * The output of the tokenizer stage of an analyzer, i.e. the tokens before any token filter runs,
 * together with the text after the char filters have been applied.
 *
 * <p>
 * The offsets of the tokens are positions in the original text, not in {@link #getCharFilteredText()}.
 * The two coordinate systems differ when a char filter changes the length of the text.
 * </p>
 */
public final class TokenizerStageResult {

    private final List<AnalyzeToken> tokens;

    private final String charFilteredText;

    /**
     * Constructs a new TokenizerStageResult.
     *
     * @param tokens the tokens produced by the tokenizer
     * @param charFilteredText the text after the char filters have been applied
     */
    public TokenizerStageResult(final List<AnalyzeToken> tokens, final String charFilteredText) {
        this.tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens"));
        this.charFilteredText = Objects.requireNonNull(charFilteredText, "charFilteredText");
    }

    /**
     * Returns the tokens produced by the tokenizer.
     * Each token has its offsets in the original text, and carries the attributes
     * {@code partOfSpeech} and {@code reading} when the tokenizer provides them.
     *
     * @return an unmodifiable list of tokens, which is empty if the tokenizer provides no part of speech
     */
    public List<AnalyzeToken> getTokens() {
        return tokens;
    }

    /**
     * Returns the text after the char filters have been applied.
     *
     * @return the char-filtered text
     */
    public String getCharFilteredText() {
        return charFilteredText;
    }
}
