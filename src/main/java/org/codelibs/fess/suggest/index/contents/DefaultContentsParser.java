package org.codelibs.fess.suggest.index.contents;

import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.util.SuggestUtil;

import java.util.ArrayList;
import java.util.List;

public class DefaultContentsParser implements ContentsParser {
    @Override
    public SuggestItem parseSearchWords(String... words) {
        return null;
    }

    @Override
    public List<SuggestItem> parseQueryString(final String queryString, final String[] fields, final ReadingConverter readingConverter,
            final Normalizer normalizer) {
        final List<SuggestItem> items = new ArrayList<>(fields.length);

        for (String field : fields) {
            final String[] words = SuggestUtil.parseQuery(queryString, field);
            if (words.length == 0) {
                continue;
            }

            String[][] readings = new String[words.length][];
            for (int j = 0; j < words.length; j++) {
                words[j] = normalizer.normalize(words[j]);
                List<String> l = readingConverter.convert(words[j]);
                readings[j] = l.toArray(new String[l.size()]);
            }

            items.add(new SuggestItem(words, readings, 1L, null, //TODO label
                    null, //TODO role
                    SuggestItem.Kind.QUERY));
        }

        return items;
    }

    @Override
    public List<SuggestItem> parseDocument(final String fieldName, final String document) {
        return null;
    }
}
