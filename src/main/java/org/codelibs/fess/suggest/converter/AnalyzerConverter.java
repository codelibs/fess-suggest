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
package org.codelibs.fess.suggest.converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codelibs.fess.suggest.settings.AnalyzerSettings;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.opensearch.action.admin.indices.analyze.AnalyzeAction;
import org.opensearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;
import org.opensearch.core.common.Strings;
import org.opensearch.transport.client.Client;

import com.ibm.icu.text.Transliterator;

/**
 * AnalyzerConverter is a class that implements the ReadingConverter interface.
 * It is responsible for converting text using specified language analyzers.
 * The class uses a Transliterator to convert between Hiragana and Katakana.
 *
 * <p>Constructor:
 * <ul>
 *   <li>{@link #AnalyzerConverter(Client, SuggestSettings)}: Initializes the converter with the given client and settings.</li>
 * </ul>
 *
 * <p>Methods:
 * <ul>
 *   <li>{@link #init()}: Initializes the converter. Currently does nothing.</li>
 *   <li>{@link #convert(String, String, String...)}: Converts the given text using the specified field and languages.</li>
 * </ul>
 *
 * <p>Inner Class:
 * <ul>
 *   <li>LangAnayzerConverter: A protected inner class that implements the ReadingConverter interface.
 *       It is responsible for converting text using a specific language analyzer.
 *       <ul>
 *         <li>LangAnayzerConverter#init(): Initializes the converter. Currently does nothing.</li>
 *         <li>LangAnayzerConverter#convert(String, String, String...): Converts the given text using the specified field and language.</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p>Fields:
 * <ul>
 *   <li>{@link #client}: The client used to perform analysis.</li>
 *   <li>{@link #settings}: The settings for suggestions.</li>
 *   <li>{@link #analyzerSettings}: The settings for the analyzer.</li>
 *   <li>{@link #transliterator}: The transliterator used to convert between Hiragana and Katakana.</li>
 * </ul>
 */
public class AnalyzerConverter implements ReadingConverter {
    /** OpenSearch client. */
    protected final Client client;
    private final SuggestSettings settings;
    /** Analyzer settings. */
    protected final AnalyzerSettings analyzerSettings;

    /** Transliterator for Hiragana to Katakana. */
    protected final Transliterator transliterator = Transliterator.getInstance("Hiragana-Katakana");

    /**
     * Constructor.
     * @param client OpenSearch client
     * @param settings Suggest settings
     */
    public AnalyzerConverter(final Client client, final SuggestSettings settings) {
        this.client = client;
        this.settings = settings;
        analyzerSettings = settings.analyzer();
    }

    @Override
    public void init() throws IOException {
        // nothing
    }

    @Override
    public List<String> convert(final String text, final String field, final String... langs) throws IOException {
        final ReadingConverter converter;
        if (langs == null || langs.length == 0) {
            converter = new LangAnalyzerConverter(null);
        } else {
            final ReadingConverterChain chain = new ReadingConverterChain();
            for (final String lang : langs) {
                chain.addConverter(new LangAnalyzerConverter(lang));
            }
            converter = chain;
        }
        return converter.convert(text, field);
    }

    /**
     * Language-specific analyzer converter.
     */
    protected class LangAnalyzerConverter implements ReadingConverter {
        /** Language. */
        protected final String lang;

        /**
         * Constructor.
         * @param lang Language
         */
        protected LangAnalyzerConverter(final String lang) {
            this.lang = lang;
        }

        @Override
        public void init() throws IOException {
            // nothing
        }

        @Override
        public List<String> convert(final String text, final String field, final String... dummy) throws IOException {
            final AnalyzeAction.Response readingResponse = client.admin()
                    .indices()
                    .prepareAnalyze(analyzerSettings.getAnalyzerSettingsIndexName(), text)
                    .setAnalyzer(analyzerSettings.getReadingAnalyzerName(field, lang))
                    .execute()
                    .actionGet(settings.getIndicesTimeout());

            final AnalyzeAction.Response termResponse = client.admin()
                    .indices()
                    .prepareAnalyze(analyzerSettings.getAnalyzerSettingsIndexName(), text)
                    .setAnalyzer(analyzerSettings.getReadingTermAnalyzerName(field, lang))
                    .execute()
                    .actionGet(settings.getIndicesTimeout());

            final List<AnalyzeToken> readingTokenList = readingResponse.getTokens();
            final List<AnalyzeToken> termTokenList = termResponse.getTokens();

            final StringBuilder readingBuf = new StringBuilder(text.length());
            if (readingTokenList != null && termTokenList != null) {
                int offset = 0;
                for (int i = 0; i < readingTokenList.size(); i++) {
                    final String term = termTokenList.get(i).getTerm();
                    String reading = readingTokenList.get(i).getTerm();
                    if (Strings.isNullOrEmpty(reading)) {
                        reading = term;
                    }
                    reading = transliterator.transliterate(reading);

                    final int pos = text.indexOf(term, offset);
                    if (pos > 0) {
                        final String tmp = text.substring(offset, pos);
                        readingBuf.append(transliterator.transliterate(tmp));
                        offset = pos;
                    } else if (pos == -1) {
                        continue;
                    }

                    readingBuf.append(reading);
                    offset += term.length();
                }
            }

            final List<String> list = new ArrayList<>(1);
            if (readingBuf.length() > 0) {
                list.add(readingBuf.toString());
            }
            return list;
        }
    }

}
