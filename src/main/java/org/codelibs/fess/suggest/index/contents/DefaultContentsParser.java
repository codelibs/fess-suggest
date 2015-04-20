package org.codelibs.fess.suggest.index.contents;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLog;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.util.SuggestUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultContentsParser implements ContentsParser {
    @Override
    public SuggestItem parseSearchWords(final String[] words, final String[] fields, final String[] tags, final String[] roles,
            final ReadingConverter readingConverter, final Normalizer normalizer) throws SuggesterException {
        try {
            String[][] readings = new String[words.length][];
            for (int j = 0; j < words.length; j++) {
                words[j] = normalizer.normalize(words[j]);
                List<String> l = readingConverter.convert(words[j]);
                readings[j] = l.toArray(new String[l.size()]);
            }

            return new SuggestItem(words, readings, fields, 1L, -1, tags, roles, SuggestItem.Kind.QUERY);
        } catch (IOException e) {
            throw new SuggesterException("Failed to SuggestItem from search words.", e);
        }

    }

    @Override
    public List<SuggestItem> parseQueryLog(final QueryLog queryLog, final String[] fields, final String tagFieldName,
            final String roleFieldName, final ReadingConverter readingConverter, final Normalizer normalizer) throws SuggesterException {
        final String queryString = queryLog.getQueryString();
        final String filterQueryString = queryLog.getFilterQueryString();

        final String[] tags1 = SuggestUtil.parseQuery(queryString, tagFieldName);
        final String[] roles1 = SuggestUtil.parseQuery(queryString, roleFieldName);
        final String[] tags2 = filterQueryString == null ? new String[0] : SuggestUtil.parseQuery(filterQueryString, tagFieldName);
        final String[] roles2 = filterQueryString == null ? new String[0] : SuggestUtil.parseQuery(filterQueryString, roleFieldName);
        final String[] tags = new String[tags1.length + tags2.length];
        final String[] roles = new String[roles1.length + roles2.length];

        if (tags1.length > 0) {
            System.arraycopy(tags1, 0, tags, 0, tags1.length);
        }
        if (tags2.length > 0) {
            System.arraycopy(tags2, 0, tags, tags1.length, tags2.length);
        }
        if (roles1.length > 0) {
            System.arraycopy(roles1, 0, roles, 0, roles1.length);
        }
        if (roles2.length > 0) {
            System.arraycopy(roles2, 0, roles, roles1.length, roles2.length);
        }

        final List<SuggestItem> items = new ArrayList<>(fields.length);
        try {
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

                items.add(new SuggestItem(words, readings, new String[] { field }, 1L, -1, tags, roles, SuggestItem.Kind.QUERY));
            }
        } catch (IOException e) {
            throw new SuggesterException("Failed to create SuggestItem from queryLog.", e);
        }

        return items;
    }

    @Override
    public List<SuggestItem> parseDocument(final Map<String, Object> document, final String[] fields,
            final ReadingConverter readingConverter, final Normalizer normalizer, final Analyzer analyzer) throws SuggesterException {
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

                    items.add(new SuggestItem(words, readings, new String[] { field }, 1L, -1, null, //TODO label
                            null, //TODO role
                            SuggestItem.Kind.DOCUMENT));
                }
            } catch (IOException e) {
                throw new SuggesterException("Failed to create SuggestItem from document.", e);
            }
        }

        return items == null ? new ArrayList<>() : items;
    }
}
