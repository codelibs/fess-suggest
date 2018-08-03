package org.codelibs.fess.suggest.normalizer;

import java.util.List;

import org.codelibs.fess.suggest.settings.AnalyzerSettings;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.client.Client;

public class AnalyzerNormalizer implements Normalizer {
    protected final Client client;
    protected final AnalyzerSettings analyzerSettings;
    private SuggestSettings settings;

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
            final AnalyzeResponse termResponse =
                    client.admin().indices().prepareAnalyze(analyzerSettings.getAnalyzerSettingsIndexName(), text)
                            .setAnalyzer(analyzerSettings.getNormalizeAnalyzerName(field, lang)).execute()
                            .actionGet(settings.getIndicesTimeout());

            final List<AnalyzeResponse.AnalyzeToken> termTokenList = termResponse.getTokens();
            if (termTokenList.isEmpty()) {
                return text;
            }

            return termTokenList.get(0).getTerm();
        }
    }
}
