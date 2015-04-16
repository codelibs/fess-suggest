package org.codelibs.fess.suggest;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.util.IOUtils;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.converter.KatakanaConverter;
import org.codelibs.fess.suggest.converter.KatakanaToAlphabetConverter;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.converter.ReadingConverterChain;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.normalizer.NormalizerChain;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fess.suggest.settings.SuggestSettingsBuilder;
import org.elasticsearch.client.Client;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SuggesterBuilder {

    protected SuggestSettings settings;
    protected SuggestSettingsBuilder settingsBuilder;
    protected ReadingConverter readingConverter;
    protected Normalizer normalizer;
    protected Analyzer analyzer;
    protected ExecutorService threadPool;

    protected int threadPoolSize = Runtime.getRuntime().availableProcessors();

    public SuggesterBuilder settings(SuggestSettings settings) {
        this.settings = settings;
        this.settingsBuilder = null;
        return this;
    }

    public SuggesterBuilder settings(SuggestSettingsBuilder settingsBuilder) {
        this.settingsBuilder = settingsBuilder;
        this.settings = null;
        return this;
    }

    public SuggesterBuilder readingConverter(ReadingConverter readingConverter) {
        this.readingConverter = readingConverter;
        return this;
    }

    public SuggesterBuilder normalizer(Normalizer normalizer) {
        this.normalizer = normalizer;
        return this;
    }

    public SuggesterBuilder analyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
        return this;
    }

    public SuggesterBuilder threadPool(ExecutorService threadPool) {
        this.threadPool = threadPool;
        return this;
    }

    public SuggesterBuilder threadPoolSize(int threadPoolSize) {
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
            readingConverter = createDefaultReadingConverter();
        }

        if (normalizer == null) {
            normalizer = createDefaultNormalizer();
        }

        if (analyzer == null) {
            analyzer = createDefaultAnalyzer();
        }

        if (threadPool == null) {
            threadPool = Executors.newFixedThreadPool(threadPoolSize);
        }

        return new Suggester(client, settings, readingConverter, normalizer, analyzer, threadPool);
    }

    protected ReadingConverter createDefaultReadingConverter() {
        ReadingConverterChain chain = new ReadingConverterChain();
        chain.addConverter(new KatakanaConverter());
        chain.addConverter(new KatakanaToAlphabetConverter());
        return chain;
    }

    protected Normalizer createDefaultNormalizer() {
        //TODO
        return new NormalizerChain();
    }

    protected Analyzer createDefaultAnalyzer() throws SuggesterException {
        try {
            final UserDictionary userDictionary;
            final String userDictionaryPath = System.getProperty(SuggestConstants.USER_DICT_PATH);
            if (StringUtils.isBlank(userDictionaryPath) || !new File(userDictionaryPath).exists()) {
                userDictionary = null;
            } else {
                InputStream stream = new FileInputStream(new File(userDictionaryPath));
                String encoding = System.getProperty(SuggestConstants.USER_DICT_ENCODING);
                if (encoding == null) {
                    encoding = IOUtils.UTF_8;
                }

                CharsetDecoder decoder =
                        Charset.forName(encoding).newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                                .onUnmappableCharacter(CodingErrorAction.REPORT);
                InputStreamReader reader = new InputStreamReader(stream, decoder);
                userDictionary = new UserDictionary(reader);
            }

            Set<String> stopTags = new HashSet<>();

            return new JapaneseAnalyzer(userDictionary, JapaneseTokenizer.Mode.NORMAL, null, stopTags);
        } catch (IOException e) {
            throw new SuggesterException("Failed to create default analyzer.", e);
        }
    }

}
