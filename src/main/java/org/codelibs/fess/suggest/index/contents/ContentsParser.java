package org.codelibs.fess.suggest.index.contents;

import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLog;
import org.codelibs.fess.suggest.normalizer.Normalizer;

public interface ContentsParser {
    SuggestItem parseSearchWords(String[] words, String[][] readings, String[] fields, String[] tags, String roles[], long score,
            ReadingConverter readingConverter, Normalizer normalizer);

    List<SuggestItem> parseQueryLog(QueryLog queryLog, String[] fields, String tagFieldName, String roleFieldName,
            ReadingConverter readingConverter, Normalizer normalizer);

    List<SuggestItem> parseDocument(Map<String, Object> document, String[] fields, ReadingConverter readingConverter,
            Normalizer normalizer, SuggestAnalyzer analyzer);
}
