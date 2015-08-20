package org.codelibs.fess.suggest;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.lucene.analysis.Analyzer;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fess.suggest.settings.SuggestSettingsBuilder;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.elasticsearch.client.Client;

public class SuggesterBuilder {

    protected SuggestSettings settings;
    protected SuggestSettingsBuilder settingsBuilder;
    protected ReadingConverter readingConverter;
    protected Normalizer normalizer;
    protected Analyzer analyzer;
    protected ExecutorService threadPool;

    protected int threadPoolSize = Runtime.getRuntime().availableProcessors();

    public SuggesterBuilder settings(final SuggestSettings settings) {
        this.settings = settings;
        this.settingsBuilder = null;
        return this;
    }

    public SuggesterBuilder settings(final SuggestSettingsBuilder settingsBuilder) {
        this.settingsBuilder = settingsBuilder;
        this.settings = null;
        return this;
    }

    public SuggesterBuilder readingConverter(final ReadingConverter readingConverter) {
        this.readingConverter = readingConverter;
        return this;
    }

    public SuggesterBuilder normalizer(final Normalizer normalizer) {
        this.normalizer = normalizer;
        return this;
    }

    public SuggesterBuilder analyzer(final Analyzer analyzer) {
        this.analyzer = analyzer;
        return this;
    }

    public SuggesterBuilder threadPool(final ExecutorService threadPool) {
        this.threadPool = threadPool;
        return this;
    }

    public SuggesterBuilder threadPoolSize(final int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
        return this;
    }

    public Suggester build(final Client client, final String id) {
        if (settings == null) {
            if (settingsBuilder == null) {
                settingsBuilder = SuggestSettings.builder();
            }
            settings = settingsBuilder.build(client, id);
        }
        settings.init();

        if (readingConverter == null) {
            readingConverter = SuggestUtil.createDefaultReadingConverter();
        }
        try {
            readingConverter.init();
        } catch (final IOException e) {
            throw new SuggesterException(e);
        }

        if (normalizer == null) {
            normalizer = SuggestUtil.createDefaultNormalizer();
        }

        if (analyzer == null) {
            analyzer = SuggestUtil.createDefaultAnalyzer();
        }

        if (threadPool == null) {
            threadPool = Executors.newFixedThreadPool(threadPoolSize);
        }

        return new Suggester(client, settings, readingConverter, normalizer, analyzer, threadPool);
    }
}
