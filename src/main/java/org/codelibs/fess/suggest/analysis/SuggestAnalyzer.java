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

import org.codelibs.fesen.opensearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;

/**
 * Interface for analyzing and processing suggestion tokens.
 */
public interface SuggestAnalyzer {

    /**
     * Analyzes the given text and returns a list of tokens.
     *
     * @param text the text to analyze
     * @param field the field associated with the text
     * @param lang the language of the text
     * @return a list of analyzed tokens
     */
    List<AnalyzeToken> analyze(String text, String field, String lang);

    /**
     * Analyzes the given text and returns a list of tokens along with their readings.
     *
     * @param text the text to analyze
     * @param field the field associated with the text
     * @param lang the language of the text
     * @return a list of analyzed tokens with their readings
     */
    List<AnalyzeToken> analyzeAndReading(String text, String field, String lang);

    /**
     * Analyzes the given text and returns the output of the tokenizer stage, i.e. the tokens
     * before any token filter runs, together with the text after the char filters have been applied.
     * The tokens carry the attributes {@code partOfSpeech} and {@code reading} when the tokenizer provides them.
     *
     * <p>
     * If the analysis succeeds but the tokenizer provides no part of speech (e.g. a non-Japanese analyzer),
     * the returned result has no tokens. {@code null} means that the tokenizer stage could not be obtained.
     * The default implementation does not support this operation and returns {@code null}.
     * </p>
     *
     * @param text the text to analyze
     * @param field the field associated with the text
     * @param lang the language of the text
     * @return the tokenizer stage result (without tokens if the tokenizer provides no part of speech),
     *         or {@code null} if it could not be obtained (not supported by this analyzer, the text is too long,
     *         the analysis failed, or the response lacks the tokenizer stage or the char-filtered text)
     */
    default TokenizerStageResult analyzeTokenizerStage(final String text, final String field, final String lang) {
        return null;
    }
}
