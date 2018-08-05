package org.codelibs.fess.suggest.converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codelibs.fess.suggest.settings.AnalyzerSettings;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;

import com.ibm.icu.text.Transliterator;

public class AnalyzerConverter implements ReadingConverter {
    protected final Client client;
    private SuggestSettings settings;
    protected final AnalyzerSettings analyzerSettings;

    protected final Transliterator transliterator = Transliterator.getInstance("Hiragana-Katakana");

    public AnalyzerConverter(final Client client, final SuggestSettings settings) {
        this.client = client;
        this.settings = settings;
        this.analyzerSettings = settings.analyzer();
    }

    @Override
    public void init() throws IOException {

    }

    @Override
    public List<String> convert(final String text, final String field, final String... langs) throws IOException {
        final ReadingConverter converter;
        if (langs == null || langs.length == 0) {
            converter = new LangAnayzerConverter(null);
        } else {
            final ReadingConverterChain chain = new ReadingConverterChain();
            for (final String lang : langs) {
                chain.addConverter(new LangAnayzerConverter(lang));
            }
            converter = chain;
        }
        return converter.convert(text, field);
    }

    protected class LangAnayzerConverter implements ReadingConverter {
        protected final String lang;

        protected LangAnayzerConverter(final String lang) {
            this.lang = lang;
        }

        @Override
        public void init() throws IOException {

        }

        @Override
        public List<String> convert(final String text, final String field, final String... dummy) throws IOException {
            final AnalyzeResponse readingResponse =
                    client.admin().indices().prepareAnalyze(analyzerSettings.getAnalyzerSettingsIndexName(), text)
                            .setAnalyzer(analyzerSettings.getReadingAnalyzerName(field, lang)).execute()
                            .actionGet(settings.getIndicesTimeout());

            final AnalyzeResponse termResponse =
                    client.admin().indices().prepareAnalyze(analyzerSettings.getAnalyzerSettingsIndexName(), text)
                            .setAnalyzer(analyzerSettings.getReadingTermAnalyzerName(field, lang)).execute()
                            .actionGet(settings.getIndicesTimeout());

            final List<AnalyzeResponse.AnalyzeToken> readingTokenList = readingResponse.getTokens();
            final List<AnalyzeResponse.AnalyzeToken> termTokenList = termResponse.getTokens();

            final StringBuilder readingBuf = new StringBuilder(text.length());
            int offset = 0;
            for (int i = 0; i < readingTokenList.size(); i++) {
                final String term = termTokenList.get(i).getTerm();
                String reading = readingTokenList.get(i).getTerm();
                if (Strings.isNullOrEmpty(reading)) {
                    reading = term;
                }
                reading = transliterator.transliterate(reading);

                final int pos = text.substring(offset).indexOf(term);
                if (pos > 0) {
                    final String tmp = text.substring(offset, offset + pos);
                    readingBuf.append(transliterator.transliterate(tmp));
                    offset += pos;
                } else if (pos == -1) {
                    continue;
                }

                readingBuf.append(reading);
                offset += term.length();
            }

            final List<String> list = new ArrayList<>(1);
            if (readingBuf.length() > 0) {
                list.add(readingBuf.toString());
            }
            return list;
        }
    }

}
