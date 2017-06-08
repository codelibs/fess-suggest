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
            final String[] roles, final long score, final ReadingConverter readingConverter, final Normalizer normalizer,
            final SuggestAnalyzer analyzer, final String[] langs) {
        try {
            final List<String> wordsList = new ArrayList<>(words.length);
            final List<String[]> readingList = new ArrayList<>(words.length);

            for (int i = 0; i < words.length; i++) {
                if (isExcludeSearchword(words[i], langs, analyzer)) {
                    continue;
                }

                final String word = normalizer.normalize(words[i], langs);
                final List<String> l = readingConverter.convert(word, langs);
                if (readings != null && readings.length > i && readings[i].length > 0) {
                    for (final String reading : readings[i]) {
                        if (!l.contains(reading)) {
                            l.add(reading);
                        }
                    }
                }

                wordsList.add(word);
                readingList.add(l.toArray(new String[l.size()]));
            }
            if (wordsList.isEmpty()) {
                return null;
            }
            return new SuggestItem(wordsList.toArray(new String[wordsList.size()]), readingList.toArray(new String[readingList.size()][]),
                    fields, 0, score, -1, tags, roles, langs, SuggestItem.Kind.QUERY);
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

                items.add(new SuggestItem(words, readings, new String[] { field }, 0, 1, -1, tags, roles, null, SuggestItem.Kind.QUERY));
            }
        } catch (final IOException e) {
            throw new SuggesterException("Failed to create SuggestItem from queryLog.", e);
        }

        return items;
    }

    @Override
    public List<SuggestItem> parseDocument(final Map<String, Object> document, final String[] fields, final String tagFieldName,
            final String roleFieldName, final String langFieldName, final ReadingConverter readingConverter, final Normalizer normalizer,
            final SuggestAnalyzer analyzer) {
        List<SuggestItem> items = null;
        final String[] tags = getRoleFromDoc(document, tagFieldName);
        final String[] roles = getRoleFromDoc(document, roleFieldName);

        for (final String field : fields) {
            final Object textObj = document.get(field);
            if (textObj == null) {
                continue;
            }
            final String text = textObj.toString();
            final String lang = document.get(langFieldName) == null ? null : document.get(langFieldName).toString();

            final List<AnalyzeResponse.AnalyzeToken> tokens = analyzer.analyze(text, lang);
            final List<AnalyzeResponse.AnalyzeToken> readingTokens = analyzer.analyzeAndReading(text, lang);
            if (tokens.size() != readingTokens.size()) {
                throw new SuggesterException("Failed to get reading. token_size=" + tokens.size() + " reading_size=" + readingTokens.size());
            }

            try {
                for (int i = 0; i < tokens.size(); i++) {
                    final AnalyzeResponse.AnalyzeToken token = tokens.get(i);
                    final AnalyzeResponse.AnalyzeToken readingToken = readingTokens.get(i);
                    final String word = token.getTerm();
                    final String reading = readingToken.getTerm();
                    if (StringUtil.isBlank(word)) {
                        continue;
                    }
                    final String[] words = new String[] { word };
                    final String[][] readings = new String[words.length][];
                    final List<String> l = readingConverter.convert(reading, lang);
                    l.add(word);
                    readings[0] = l.toArray(new String[l.size()]);

                    if (items == null) {
                        items = new ArrayList<>(text.length() * fields.length / field.length());
                    }

                    final String[] langs = lang == null ? new String[] {} : new String[] { lang };
                    items.add(new SuggestItem(words, readings, new String[] { field }, 1L, 0, -1, tags, roles, langs,
                            SuggestItem.Kind.DOCUMENT));
                }
            } catch (final IOException e) {
                throw new SuggesterException("Failed to create SuggestItem from document.", e);
            }
        }

        return items == null ? new ArrayList<>() : items;
    }

    protected String[] getRoleFromDoc(final Map<String, Object> document, final String roleFieldName) {
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

    protected boolean isExcludeSearchword(final String searchWord, final String[] langs, final SuggestAnalyzer analyzer) {
        if (langs == null || langs.length == 0) {
            final List<AnalyzeResponse.AnalyzeToken> tokens = analyzer.analyze(searchWord, null);
            return tokens == null || tokens.size() == 0;
        } else {
            for (final String lang : langs) {
                final List<AnalyzeResponse.AnalyzeToken> tokens = analyzer.analyze(searchWord, lang);
                if (tokens != null && tokens.size() > 0) {
                    return false;
                }
            }
            return true;
        }

    }
}
