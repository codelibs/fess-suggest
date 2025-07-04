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
import org.opensearch.transport.client.Client;

/**
 * Builder class for creating instances of {@link Suggester}.
 * This class allows for the configuration of various components used by the {@link Suggester}.
 */
public class SuggesterBuilder {
    /**
     * Constructs a new {@link SuggesterBuilder}.
     */
    public SuggesterBuilder() {
        // nothing
    }

    /** The suggest settings. */
    protected SuggestSettings settings;
    /** The suggest settings builder. */
    protected SuggestSettingsBuilder settingsBuilder;
    /** The reading converter. */
    protected ReadingConverter readingConverter;
    /** The contents reading converter. */
    protected ReadingConverter contentsReadingConverter;
    /** The normalizer. */
    protected Normalizer normalizer;
    /** The analyzer. */
    protected SuggestAnalyzer analyzer;
    /** The thread pool. */
    protected ExecutorService threadPool;

    /** The thread pool size. */
    protected int threadPoolSize = Runtime.getRuntime().availableProcessors();

    /**
     * Sets the suggest settings.
     * @param settings The suggest settings.
     * @return This builder instance.
     */
    public SuggesterBuilder settings(final SuggestSettings settings) {
        this.settings = settings;
        settingsBuilder = null;
        return this;
    }

    /**
     * Sets the suggest settings builder.
     * @param settingsBuilder The suggest settings builder.
     * @return This builder instance.
     */
    public SuggesterBuilder settings(final SuggestSettingsBuilder settingsBuilder) {
        this.settingsBuilder = settingsBuilder;
        settings = null;
        return this;
    }

    /**
     * Sets the reading converter.
     * @param readingConverter The reading converter.
     * @return This builder instance.
     */
    public SuggesterBuilder readingConverter(final ReadingConverter readingConverter) {
        this.readingConverter = readingConverter;
        return this;
    }

    /**
     * Sets the contents reading converter.
     * @param contentsReadingConverter The contents reading converter.
     * @return This builder instance.
     */
    public SuggesterBuilder contentsReadigConverter(final ReadingConverter contentsReadingConverter) {
        this.contentsReadingConverter = contentsReadingConverter;
        return this;
    }

    /**
     * Sets the normalizer.
     * @param normalizer The normalizer.
     * @return This builder instance.
     */
    public SuggesterBuilder normalizer(final Normalizer normalizer) {
        this.normalizer = normalizer;
        return this;
    }

    /**
     * Sets the analyzer.
     * @param analyzer The analyzer.
     * @return This builder instance.
     */
    public SuggesterBuilder analyzer(final SuggestAnalyzer analyzer) {
        this.analyzer = analyzer;
        return this;
    }

    /**
     * Sets the thread pool.
     * @param threadPool The thread pool.
     * @return This builder instance.
     */
    public SuggesterBuilder threadPool(final ExecutorService threadPool) {
        this.threadPool = threadPool;
        return this;
    }

    /**
     * Sets the thread pool size.
     * @param threadPoolSize The thread pool size.
     * @return This builder instance.
     */
    public SuggesterBuilder threadPoolSize(final int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
        return this;
    }

    /**
     * Builds a Suggester instance.
     * @param client The OpenSearch client.
     * @param id The ID.
     * @return A Suggester instance.
     */
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
