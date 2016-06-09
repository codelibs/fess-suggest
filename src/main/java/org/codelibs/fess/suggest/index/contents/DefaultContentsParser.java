package org.codelibs.fess.suggest.index.contents;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLog;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;

public class DefaultContentsParser implements ContentsParser {
    @Override
    public SuggestItem parseSearchWords(final String[] words, final String[][] readings, final String[] fields, final String[] tags,
            final String[] roles, final long score, final ReadingConverter readingConverter, final Normalizer normalizer) {
        try {
            final String[][] readingArray = new String[words.length][];
            for (int i = 0; i < words.length; i++) {
                words[i] = normalizer.normalize(words[i]);
                final List<String> l = readingConverter.convert(words[i]);
                if (readings != null && readings.length > i && readings[i].length > 0) {
                    for (final String reading : readings[i]) {
                        if (!l.contains(reading)) {
                            l.add(reading);
                        }
                    }
                }

                readingArray[i] = l.toArray(new String[l.size()]);
            }
            return new SuggestItem(words, readingArray, fields, score, -1, tags, roles, SuggestItem.Kind.QUERY);
        } catch (final IOException e) {
            throw new SuggesterException("Failed to SuggestItem from search words.", e);
        }
    }

    @Override
    public List<SuggestItem> parseQueryLog(final QueryLog queryLog, final String[] fields, final String tagFieldName,
            final String roleFieldName, final ReadingConverter readingConverter, final Normalizer normalizer) {
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
            for (final String field : fields) {
                final String[] words = SuggestUtil.parseQuery(queryString, field);
                if (words.length == 0) {
                    continue;
                }

                final String[][] readings = new String[words.length][];
                for (int j = 0; j < words.length; j++) {
                    words[j] = normalizer.normalize(words[j]);
                    final List<String> l = readingConverter.convert(words[j]);
                    readings[j] = l.toArray(new String[l.size()]);
                }

                items.add(new SuggestItem(words, readings, new String[] { field }, 1L, -1, tags, roles, SuggestItem.Kind.QUERY));
            }
        } catch (final IOException e) {
            throw new SuggesterException("Failed to create SuggestItem from queryLog.", e);
        }

        return items;
    }

    @Override
    public List<SuggestItem> parseDocument(final Map<String, Object> document, final String[] fields, final String tagFieldName,
            final String roleFieldName, final ReadingConverter readingConverter, final Normalizer normalizer, final SuggestAnalyzer analyzer) {
        List<SuggestItem> items = null;
        final String[] tags = getRoleFromDoc(document, tagFieldName);
        final String[] roles = getRoleFromDoc(document, roleFieldName);

        for (final String field : fields) {
            final Object textObj = document.get(field);
            if (textObj == null) {
                continue;
            }
            final String text = textObj.toString();
            final List<AnalyzeResponse.AnalyzeToken> tokens = analyzer.analyze(text);
            try {
                for (final AnalyzeResponse.AnalyzeToken token : tokens) {
                    final String word = token.getTerm();
                    if (StringUtil.isBlank(word)) {
                        continue;
                    }
                    final String[] words = new String[] { word };
                    final String[][] readings = new String[words.length][];
                    for (int j = 0; j < words.length; j++) {
                        words[j] = normalizer.normalize(words[j]);
                        final List<String> l = readingConverter.convert(words[j]);
                        readings[j] = l.toArray(new String[l.size()]);
                    }

                    if (items == null) {
                        items = new ArrayList<>(text.length() * fields.length / field.length());
                    }

                    items.add(new SuggestItem(words, readings, new String[] { field }, 1L, -1, tags, roles, SuggestItem.Kind.DOCUMENT));
                }
            } catch (final IOException e) {
                throw new SuggesterException("Failed to create SuggestItem from document.", e);
            }
        }

        return items == null ? new ArrayList<>() : items;
    }

    private String[] getRoleFromDoc(Map<String, Object> document, String roleFieldName) {
        final Object value = document.get(roleFieldName);
        if (value instanceof String) {
            return new String[] { value.toString() };
        } else if (value instanceof String[]) {
            return (String[]) value;
        } else if (value instanceof List) {
            return ((List<?>) value).stream().map(v -> v.toString()).toArray(n -> new String[n]);
        } else if (value != null) {
            return new String[] { value.toString() };
        }

        return null;
    }
}
