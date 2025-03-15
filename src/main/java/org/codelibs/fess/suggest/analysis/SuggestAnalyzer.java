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

import org.opensearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;

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
}
