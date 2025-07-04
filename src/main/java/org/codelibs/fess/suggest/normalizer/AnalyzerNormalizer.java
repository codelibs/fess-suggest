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
package org.codelibs.fess.suggest.normalizer;

import java.util.List;

import org.codelibs.fess.suggest.settings.AnalyzerSettings;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.opensearch.action.admin.indices.analyze.AnalyzeAction;
import org.opensearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;
import org.opensearch.transport.client.Client;

/**
 * AnalyzerNormalizer is a class that implements the Normalizer interface.
 * It uses an OpenSearch client and settings to normalize text based on specified languages.
 */
public class AnalyzerNormalizer implements Normalizer {
    /**
     * The OpenSearch client used for analyzing text.
     */
    protected final Client client;

    /**
     * The settings for the analyzer.
     */
    protected final AnalyzerSettings analyzerSettings;

    /**
     * The settings for suggestions.
     */
    private final SuggestSettings settings;

    /**
     * Constructs an AnalyzerNormalizer with the specified client and settings.
     *
     * @param client   the OpenSearch client
     * @param settings the settings for suggestions
     */
    public AnalyzerNormalizer(final Client client, final SuggestSettings settings) {
        this.client = client;
        this.settings = settings;
        analyzerSettings = settings.analyzer();
    }

    @Override
    public String normalize(final String text, final String field, final String... langs) {
        final Normalizer normalizer;
        if (langs == null || langs.length == 0) {
            normalizer = new LangAnalyzerNormalizer(null);
        } else {
            final NormalizerChain chain = new NormalizerChain();
            for (final String lang : langs) {
                chain.add(new LangAnalyzerNormalizer(lang));
            }
            normalizer = chain;
        }
        return normalizer.normalize(text, field);
    }

    /**
     * Language-specific analyzer normalizer.
     */
    protected class LangAnalyzerNormalizer implements Normalizer {
        /**
         * The language used for normalization.
         */
        protected final String lang;

        /**
         * Constructs a LangAnalyzerNormalizer with the specified language.
         *
         * @param lang the language to use for normalization
         */
        protected LangAnalyzerNormalizer(final String lang) {
            this.lang = lang;
        }

        /**
         * Normalizes the given text based on the specified field.
         *
         * @param text  the text to normalize
         * @param field the field to use for normalization
         * @param dummy additional parameters (not used)
         * @return the normalized text
         */
        @Override
        public String normalize(final String text, final String field, final String... dummy) {
            final AnalyzeAction.Response termResponse = client.admin().indices()
                    .prepareAnalyze(analyzerSettings.getAnalyzerSettingsIndexName(), text)
                    .setAnalyzer(analyzerSettings.getNormalizeAnalyzerName(field, lang)).execute().actionGet(settings.getIndicesTimeout());

            final List<AnalyzeToken> termTokenList = termResponse.getTokens();
            if (termTokenList.isEmpty()) {
                return text;
            }

            return termTokenList.get(0).getTerm();
        }
    }
}
