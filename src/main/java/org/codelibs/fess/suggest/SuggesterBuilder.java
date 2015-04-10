package org.codelibs.fess.suggest;

import org.codelibs.fess.suggest.converter.KatakanaConverter;
import org.codelibs.fess.suggest.converter.KatakanaToAlphabetConverter;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.converter.ReadingConverterChain;
import org.codelibs.fess.suggest.index.SuggestIndexer;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.normalizer.NormalizerChain;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fess.suggest.settings.SuggestSettingsBuilder;
import org.elasticsearch.client.Client;

public class SuggesterBuilder {

    protected SuggestSettings settings;
    protected SuggestSettingsBuilder settingsBuilder;
    protected SuggestIndexer indexer;
    protected ReadingConverter readingConverter;
    protected Normalizer normalizer;

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

    public SuggesterBuilder indexer(SuggestIndexer indexer) {
        this.indexer = indexer;
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

        String index = settings.getAsString(SuggestSettings.DefaultKeys.INDEX, "");
        String type = settings.getAsString(SuggestSettings.DefaultKeys.TYPE, "");
        String[] supportedFields = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS);
        String tagFieldName = settings.getAsString(SuggestSettings.DefaultKeys.TAG_FIELD_NAME, "");
        String roleFieldName = settings.getAsString(SuggestSettings.DefaultKeys.ROLE_FIELD_NAME, "");

        if (indexer == null) {
            indexer =
                    createDefaultIndexer(client, index, type, supportedFields, tagFieldName, roleFieldName, settings, readingConverter,
                            normalizer);
        }

        return new Suggester(client, settings, readingConverter, normalizer, indexer);
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

    protected SuggestIndexer createDefaultIndexer(final Client client, final String index, final String type,
            final String[] supportedFields, final String tagFieldName, final String roleFieldName, final SuggestSettings settings,
            final ReadingConverter readingConverter, final Normalizer normalizer) {
        return new SuggestIndexer(client, index, type, supportedFields, tagFieldName, roleFieldName, readingConverter, normalizer, settings);

    }

}
