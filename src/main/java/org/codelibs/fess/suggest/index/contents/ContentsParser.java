package org.codelibs.fess.suggest.index.contents;

import org.apache.lucene.analysis.Analyzer;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLog;
import org.codelibs.fess.suggest.normalizer.Normalizer;

import java.util.List;
import java.util.Map;

public interface ContentsParser {
    SuggestItem parseSearchWords(String[] words, String[] fields, String[] tags, String roles[], ReadingConverter readingConverter,
            Normalizer normalizer) throws SuggesterException;

    List<SuggestItem> parseQueryLog(QueryLog queryLog, String[] fields, String tagFieldName, String roleFieldName,
            ReadingConverter readingConverter, Normalizer normalizer) throws SuggesterException;

    List<SuggestItem> parseDocument(Map<String, Object> document, String[] fields, ReadingConverter readingConverter,
            Normalizer normalizer, Analyzer analyzer) throws SuggesterException;
}
