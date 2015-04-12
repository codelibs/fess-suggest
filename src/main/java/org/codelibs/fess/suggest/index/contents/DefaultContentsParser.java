package org.codelibs.fess.suggest.index.contents;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.util.SuggestUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultContentsParser implements ContentsParser {
    @Override
    public SuggestItem parseSearchWords(final String[] words, final String[] fields, final ReadingConverter readingConverter,
            final Normalizer normalizer) {
        String[][] readings = new String[words.length][];
        for (int j = 0; j < words.length; j++) {
            words[j] = normalizer.normalize(words[j]);
            List<String> l = readingConverter.convert(words[j]);
            readings[j] = l.toArray(new String[l.size()]);
        }

        return new SuggestItem(words, readings, 1L, -1, null, //TODO label
                null, //TODO role
                SuggestItem.Kind.QUERY);
    }

    @Override
    public List<SuggestItem> parseQueryString(final String queryString, final String[] fields, final ReadingConverter readingConverter,
            final Normalizer normalizer) throws SuggesterException {
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

            items.add(new SuggestItem(words, readings, 1L, -1, null, //TODO label
                    null, //TODO role
                    SuggestItem.Kind.QUERY));
        }

        return items;
    }

    @Override
    public List<SuggestItem> parseDocument(final Map<String, Object> document, final String[] fields,
            final ReadingConverter readingConverter, final Normalizer normalizer, final Analyzer analyzer) throws Exception {
        List<SuggestItem> items = null;

        for (String field : fields) {
            Object textObj = document.get(field);
            if (textObj == null) {
                continue;
            }
            String text = textObj.toString();

            try (TokenStream stream = analyzer.tokenStream(field, text)) {
                stream.reset();
                while (stream.incrementToken()) {
                    CharTermAttribute charTermAttribute = stream.getAttribute(CharTermAttribute.class);
                    String[] words = new String[] { charTermAttribute.toString() };

                    String[][] readings = new String[words.length][];
                    for (int j = 0; j < words.length; j++) {
                        words[j] = normalizer.normalize(words[j]);
                        List<String> l = readingConverter.convert(words[j]);
                        readings[j] = l.toArray(new String[l.size()]);
                    }

                    if (items == null) {
                        items = new ArrayList<>(text.length() * fields.length / field.length());
                    }

                    items.add(new SuggestItem(words, readings, 1L, -1, null, //TODO label
                            null, //TODO role
                            SuggestItem.Kind.DOCUMENT));
                }
            }
        }

        return items == null ? new ArrayList<>() : items;
    }
}
