package org.codelibs.fess.suggest;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
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
    protected ReadingConverter contentsReadingConverter;
    protected Normalizer normalizer;
    protected SuggestAnalyzer analyzer;
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

    public SuggesterBuilder contentsReadigConverter(final ReadingConverter contentsReadigConverter) {
        this.contentsReadingConverter = contentsReadigConverter;
        return this;
    }

    public SuggesterBuilder normalizer(final Normalizer normalizer) {
        this.normalizer = normalizer;
        return this;
    }

    public SuggesterBuilder analyzer(final SuggestAnalyzer analyzer) {
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
            readingConverter = SuggestUtil.createDefaultReadingConverter(client, settings);
        }
        try {
            readingConverter.init();
        } catch (final IOException e) {
            throw new SuggesterException(e);
        }

        if (contentsReadingConverter == null) {
            contentsReadingConverter = SuggestUtil.createDefaultContentsReadingConverter(client, settings);
        }
        try {
            contentsReadingConverter.init();
        } catch (final IOException e) {
            throw new SuggesterException(e);
        }

        if (normalizer == null) {
            normalizer = SuggestUtil.createDefaultNormalizer(client, settings);
        }

        if (analyzer == null) {
            analyzer = SuggestUtil.createDefaultAnalyzer(client, settings);
        }

        if (threadPool == null) {
            threadPool = Executors.newFixedThreadPool(threadPoolSize);
        }

        return new Suggester(client, settings, readingConverter, contentsReadingConverter, normalizer, analyzer, threadPool);
    }
}
