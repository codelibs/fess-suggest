package org.codelibs.fess.suggest.normalizer;

import org.codelibs.fess.suggest.settings.AnalyzerSettings;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.client.Client;

import java.util.List;

public class AnalyzerNormalizer implements Normalizer {
    protected final Client client;
    protected final AnalyzerSettings analyzerSettings;

    public AnalyzerNormalizer(final Client client, final SuggestSettings settings) {
        this.client = client;
        this.analyzerSettings = settings.analyzer();
    }

    @Override
    public String normalize(String text) {
        final AnalyzeResponse termResponse =
                client.admin().indices().prepareAnalyze(analyzerSettings.getAnalyzerSettingsIndexName(), text)
                        .setAnalyzer(analyzerSettings.getNormalizeAnalyzerName()).execute().actionGet();

        final List<AnalyzeResponse.AnalyzeToken> termTokenList = termResponse.getTokens();
        if (termTokenList.isEmpty()) {
            return text;
        }

        return termTokenList.get(0).getTerm();
    }
}
