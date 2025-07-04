/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLog;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.opensearch.OpenSearchStatusException;
import org.opensearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;

/**
 * DefaultContentsParser is an implementation of the ContentsParser interface.
 * It provides methods to parse search words, query logs, and documents into SuggestItem objects.
 *
 * <p>This class uses various utilities such as ReadingConverter, Normalizer, and SuggestAnalyzer
 * to process and analyze the input data.</p>
 *
 * <p>It also handles the exclusion of search words based on certain criteria and manages the
 * maximum length of analyzed content.</p>
 *
 * <p>Methods in this class may throw SuggesterException in case of failures during the parsing process.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * DefaultContentsParser parser = new DefaultContentsParser();
 * SuggestItem item = parser.parseSearchWords(words, readings, fields, tags, roles, score, readingConverter, normalizer, analyzer, langs);
 * }
 * </pre>
 *
 * @see ContentsParser
 * @see SuggestItem
 * @see ReadingConverter
 * @see Normalizer
 * @see SuggestAnalyzer
 */
public class DefaultContentsParser implements ContentsParser {

    private static final Logger logger = LogManager.getLogger(DefaultContentsParser.class);

    private final int maxAnalyzedContentLength;

    /**
     * Constructor.
     */
    public DefaultContentsParser() {
        maxAnalyzedContentLength = Integer.getInteger("fess.suggest.max.analyzed.content.length", 1000);
    }

    @Override
    public SuggestItem parseSearchWords(final String[] words, final String[][] readings, final String[] fields, final String[] tags,
            final String[] roles, final long score, final ReadingConverter readingConverter, final Normalizer normalizer,
            final SuggestAnalyzer analyzer, final String[] langs) {
        try {
            final List<String> wordsList = new ArrayList<>(words.length);
            final List<String[]> readingList = new ArrayList<>(words.length);

            for (int i = 0; i < words.length; i++) {
                if (isExcludeSearchword(words[i], fields != null && fields.length > 0 ? fields[0] : "", langs, analyzer)) {
                    continue;
                }

                final String word = normalizer.normalize(words[i], fields != null && fields.length > 0 ? fields[0] : "", langs);
                final List<String> l = readingConverter.convert(word, fields != null && fields.length > 0 ? fields[0] : "", langs);
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
            throw new SuggesterException("Failed to create SuggestItem from search words.", e);
        }
    }

    @Override
    public List<SuggestItem> parseQueryLog(final QueryLog queryLog, final String[] fields, final String[] tagFieldNames,
            final String roleFieldName, final ReadingConverter readingConverter, final Normalizer normalizer) {
        final String queryString = queryLog.getQueryString();
        final String filterQueryString = queryLog.getFilterQueryString();

        final List<String> tagList = new ArrayList<>();
        for (final String tagFieldName : tagFieldNames) {
            tagList.addAll(Arrays.asList(SuggestUtil.parseQuery(queryString, tagFieldName)));
            if (filterQueryString != null) {
                tagList.addAll(Arrays.asList(SuggestUtil.parseQuery(filterQueryString, tagFieldName)));
            }
        }
        final String[] tags = tagList.toArray(new String[tagList.size()]);
        final String[] roles1 = SuggestUtil.parseQuery(queryString, roleFieldName);
        final String[] roles2 = filterQueryString == null ? new String[0] : SuggestUtil.parseQuery(filterQueryString, roleFieldName);
        final String[] roles = new String[roles1.length + roles2.length];

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
                    words[j] = normalizer.normalize(words[j], field, "");
                    final List<String> l = readingConverter.convert(words[j], field);
                    readings[j] = l.toArray(new String[l.size()]);
                }

                items.add(new SuggestItem(words, readings, new String[] { field }, 0, 1, -1, tags, roles, null, SuggestItem.Kind.QUERY));
            }
        } catch (final IOException e) {
            throw new SuggesterException("Failed to create SuggestItem from query log.", e);
        }

        return items;
    }

    @Override
    public List<SuggestItem> parseDocument(final Map<String, Object> document, final String[] fields, final String[] tagFieldNames,
            final String roleFieldName, final String langFieldName, final ReadingConverter readingConverter,
            final ReadingConverter contentsReadingConverter, final Normalizer normalizer, final SuggestAnalyzer analyzer) {
        List<SuggestItem> items = null;
        final List<String> tagList = new ArrayList<>();
        for (final String tagFieldName : tagFieldNames) {
            tagList.addAll(Arrays.asList(getFieldValues(document, tagFieldName)));
        }
        final String[] tags = tagList.toArray(new String[tagList.size()]);
        final String[] roles = getFieldValues(document, roleFieldName);

        for (final String field : fields) {
            final Object textObj = document.get(field);
            if (textObj == null) {
                continue;
            }
            final String text = textObj.toString();
            final String lang = document.get(langFieldName) == null ? null : document.get(langFieldName).toString();

            final List<AnalyzeToken> tokens = analyzeText(analyzer, field, text, lang);
            if (tokens == null) {
                continue;
            }
            List<AnalyzeToken> readingTokens = analyzeTextByReading(analyzer, field, text, lang);
            if (readingTokens.size() != tokens.size()) {
                readingTokens = null;
            }

            try {
                for (int i = 0; i < tokens.size(); i++) {
                    final AnalyzeToken token = tokens.get(i);
                    final String word = token.getTerm();
                    if (StringUtil.isBlank(word)) {
                        continue;
                    }
                    final String[] words = { word };
                    final String[][] readings = new String[words.length][];
                    final List<String> l;
                    if (readingTokens == null) {
                        l = readingConverter.convert(word, field, lang);
                    } else {
                        final String reading = readingTokens.get(i).getTerm();
                        l = contentsReadingConverter.convert(reading, field, lang);
                    }
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
                throw new SuggesterException("Failed to create SuggestItem from the document.", e);
            }
        }

        return items == null ? new ArrayList<>() : items;
    }

    /**
     * Analyze text.
     * @param analyzer Analyzer
     * @param field Field
     * @param text Text
     * @param lang Language
     * @return List of tokens
     */
    protected List<AnalyzeToken> analyzeText(final SuggestAnalyzer analyzer, final String field, final String text, final String lang) {
        final List<AnalyzeToken> tokens = new ArrayList<>();
        final StringBuilder buf = new StringBuilder(maxAnalyzedContentLength);
        for (final String t : text.split("\\s")) {
            buf.append(t).append(' ');
            if (buf.length() > maxAnalyzedContentLength) {
                try {
                    tokens.addAll(analyzer.analyze(buf.toString().trim(), field, lang));
                } catch (OpenSearchStatusException | IllegalStateException e) {
                    if (logger.isDebugEnabled()) {
                        logger.warn("[{}][{}] Failed to analyze a text(size:{}).", field, lang, buf.length(), e);
                    } else {
                        logger.warn("[{}][{}] Failed to analyze a text(size:{}). {}", field, lang, buf.length(), e.getMessage());
                    }
                }
                buf.setLength(0);
            }
        }
        if (buf.length() > 0) {
            try {
                tokens.addAll(analyzer.analyze(buf.toString().trim(), field, lang));
            } catch (OpenSearchStatusException | IllegalStateException e) {
                if (logger.isDebugEnabled()) {
                    logger.warn("[{}][{}] Failed to analyze a last text(size:{}).", field, lang, buf.length(), e);
                } else {
                    logger.warn("[{}][{}] Failed to analyze a last text(size:{}). {}", field, lang, buf.length(), e.getMessage());
                }
            }
        }
        return tokens;
    }

    /**
     * Analyze text by reading.
     * @param analyzer Analyzer
     * @param field Field
     * @param text Text
     * @param lang Language
     * @return List of tokens
     */
    protected List<AnalyzeToken> analyzeTextByReading(final SuggestAnalyzer analyzer, final String field, final String text,
            final String lang) {
        final List<AnalyzeToken> tokens = new ArrayList<>();
        final StringBuilder buf = new StringBuilder(maxAnalyzedContentLength);
        for (final String t : text.split("\\s")) {
            buf.append(t).append(' ');
            if (buf.length() > maxAnalyzedContentLength) {
                try {
                    final List<AnalyzeToken> readings = analyzer.analyzeAndReading(buf.toString().trim(), field, lang);
                    if (readings != null) {
                        tokens.addAll(readings);
                    }
                } catch (OpenSearchStatusException | IllegalStateException e) {
                    if (logger.isDebugEnabled()) {
                        logger.warn("[{}][{}] Failed to analyze a reading text(size:{}).", field, lang, buf.length(), e);
                    } else {
                        logger.warn("[{}][{}] Failed to analyze a reading text(size:{}). {}", field, lang, buf.length(), e.getMessage());
                    }
                }
                buf.setLength(0);
            }
        }
        if (buf.length() > 0) {
            try {
                final List<AnalyzeToken> readings = analyzer.analyzeAndReading(buf.toString().trim(), field, lang);
                if (readings != null) {
                    tokens.addAll(readings);
                }
            } catch (OpenSearchStatusException | IllegalStateException e) {
                if (logger.isDebugEnabled()) {
                    logger.warn("[{}][{}] Failed to analyze a last reading text(size:{}).", field, lang, buf.length(), e);
                } else {
                    logger.warn("[{}][{}] Failed to analyze a last reading text(size:{}). {}", field, lang, buf.length(), e.getMessage());
                }
            }
        }
        return tokens;
    }

    /**
     * Get field values.
     * @param document Document
     * @param fieldName Field name
     * @return Field values
     */
    protected String[] getFieldValues(final Map<String, Object> document, final String fieldName) {
        final Object value = document.get(fieldName);
        if (value instanceof String) {
            return new String[] { value.toString() };
        }
        if (value instanceof String[]) {
            return (String[]) value;
        }
        if (value instanceof List) {
            return ((List<?>) value).stream().map(Object::toString).toArray(n -> new String[n]);
        }
        if (value != null) {
            return new String[] { value.toString() };
        }

        return new String[0];
    }

    /**
     * Check if the search word is excluded.
     * @param searchWord Search word
     * @param field Field
     * @param langs Languages
     * @param analyzer Analyzer
     * @return True if the search word is excluded
     */
    protected boolean isExcludeSearchword(final String searchWord, final String field, final String[] langs,
            final SuggestAnalyzer analyzer) {
        if (langs == null || langs.length == 0) {
            final List<AnalyzeToken> tokens = analyzer.analyze(searchWord, "", null);
            return tokens == null || tokens.size() == 0;
        }
        for (final String lang : langs) {
            final List<AnalyzeToken> tokens = analyzer.analyze(searchWord, field, lang);
            if (tokens != null && tokens.size() > 0) {
                return false;
            }
        }
        return true;

    }
}
