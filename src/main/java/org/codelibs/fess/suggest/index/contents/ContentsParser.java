/*
 * Copyright 2012-2024 CodeLibs Project and the Others.
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
package org.codelibs.fess.suggest.index.contents;

import java.util.List;
import java.util.Map;

import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLog;
import org.codelibs.fess.suggest.normalizer.Normalizer;

/**
 * Interface for parsing content and creating suggest items.
 */
public interface ContentsParser {
    /**
     * Parses the given search words and creates a SuggestItem.
     *
     * @param words the array of search words
     * @param readings the array of readings corresponding to the search words
     * @param fields the array of fields associated with the search words
     * @param tags the array of tags associated with the search words
     * @param roles the array of roles associated with the search words
     * @param score the score associated with the search words
     * @param readingConverter the converter to use for converting readings
     * @param normalizer the normalizer to use for normalizing the search words
     * @param analyzer the analyzer to use for analyzing the search words
     * @param langs the array of languages associated with the search words
     * @return a SuggestItem created from the given search words and associated data
     */
    SuggestItem parseSearchWords(String[] words, String[][] readings, String[] fields, String[] tags, String[] roles, long score,
            ReadingConverter readingConverter, Normalizer normalizer, SuggestAnalyzer analyzer, String[] langs);

    /**
     * Parses the given query log and returns a list of suggest items.
     *
     * @param queryLog the query log to parse
     * @param fields the fields to extract from the query log
     * @param tagFieldNames the names of the fields to use as tags
     * @param roleFieldName the name of the field to use for roles
     * @param readingConverter the converter to use for reading values
     * @param normalizer the normalizer to use for normalizing values
     * @return a list of suggest items parsed from the query log
     */
    List<SuggestItem> parseQueryLog(QueryLog queryLog, String[] fields, String[] tagFieldNames, String roleFieldName,
            ReadingConverter readingConverter, Normalizer normalizer);

    /**
     * Parses a document and extracts suggest items based on the provided fields and converters.
     *
     * @param document The document to parse, represented as a map of field names to values.
     * @param fields The fields to extract from the document.
     * @param tagFieldNames The names of the fields that contain tags.
     * @param roleFieldName The name of the field that contains role information.
     * @param langFieldName The name of the field that contains language information.
     * @param readingConverter The converter to use for reading fields.
     * @param contentsReadingConverter The converter to use for reading content fields.
     * @param normalizer The normalizer to use for normalizing field values.
     * @param analyzer The analyzer to use for analyzing field values.
     * @return A list of suggest items extracted from the document.
     */
    List<SuggestItem> parseDocument(Map<String, Object> document, String[] fields, String[] tagFieldNames, String roleFieldName,
            String langFieldName, ReadingConverter readingConverter, ReadingConverter contentsReadingConverter, Normalizer normalizer,
            SuggestAnalyzer analyzer);
}
