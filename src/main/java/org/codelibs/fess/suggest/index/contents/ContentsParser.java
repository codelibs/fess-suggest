/*
 * Copyright 2009-2021 the CodeLibs Project and the Others.
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

public interface ContentsParser {
    SuggestItem parseSearchWords(String[] words, String[][] readings, String[] fields, String[] tags, String roles[],
            long score, ReadingConverter readingConverter, Normalizer normalizer, SuggestAnalyzer analyzer,
            String[] langs);

    List<SuggestItem> parseQueryLog(QueryLog queryLog, String[] fields, String[] tagFieldNames, String roleFieldName,
            ReadingConverter readingConverter, Normalizer normalizer);

    List<SuggestItem> parseDocument(Map<String, Object> document, String[] fields, String[] tagFieldNames,
            String roleFieldName, String langFieldName, ReadingConverter readingConverter,
            ReadingConverter contentsReadingConverter, Normalizer normalizer, SuggestAnalyzer analyzer);
}
