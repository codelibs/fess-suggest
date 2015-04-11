package org.codelibs.fess.suggest.index.contents;

import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.normalizer.Normalizer;

import java.util.List;

public interface ContentsParser {
    SuggestItem parseSearchWords(String... words);

    List<SuggestItem> parseQueryString(String queryString, String[] fields, ReadingConverter readingConverter, final Normalizer normalizer);

    List<SuggestItem> parseDocument(String fieldName, String document);
}
