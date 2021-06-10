/*
 * Copyright 2012-2021 CodeLibs Project and the Others.
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

import org.codelibs.fesen.action.admin.indices.analyze.AnalyzeAction;
import org.codelibs.fesen.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;
import org.codelibs.fesen.client.Client;
import org.codelibs.fess.suggest.settings.AnalyzerSettings;
import org.codelibs.fess.suggest.settings.SuggestSettings;

public class AnalyzerNormalizer implements Normalizer {
    protected final Client client;
    protected final AnalyzerSettings analyzerSettings;
    private final SuggestSettings settings;

    public AnalyzerNormalizer(final Client client, final SuggestSettings settings) {
        this.client = client;
        this.settings = settings;
        this.analyzerSettings = settings.analyzer();
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

    protected class LangAnalyzerNormalizer implements Normalizer {
        protected final String lang;

        protected LangAnalyzerNormalizer(final String lang) {
            this.lang = lang;
        }

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
