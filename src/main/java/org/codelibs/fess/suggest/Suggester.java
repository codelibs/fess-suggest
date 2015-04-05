package org.codelibs.fess.suggest;

import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.converter.ReadingConverterChain;
import org.codelibs.fess.suggest.index.SuggestIndexer;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.normalizer.NormalizerChain;
import org.codelibs.fess.suggest.request.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.client.Client;

public class Suggester {
    protected final Client client;
    protected final SuggestSettings settings;
    protected final SuggestIndexer indexer;
    protected final ReadingConverter readingConverter;
    protected final Normalizer normalizer;

    public Suggester(Client client) {
        this(client, SuggestSettings.defaultSettings());
    }

    public Suggester(Client client, SuggestSettings settings) {
        this(client, settings, createReadingConverter(settings), createNormalizer(settings));
    }

    public Suggester(final Client client, final SuggestSettings settings, final ReadingConverter readingConverter,
            final Normalizer normalizer) {
        this.client = client;
        this.settings = settings;
        this.readingConverter = readingConverter;
        this.normalizer = normalizer;

        this.indexer =
                new SuggestIndexer(client, settings.index, settings.type, settings.supportedFields, settings.tagFieldName,
                        settings.roleFieldName, this.readingConverter, this.normalizer);
    }

    public SuggestRequestBuilder suggest() {
        return new SuggestRequestBuilder(client).setIndex(settings.index).setType(settings.type);
    }

    public RefreshResponse refresh() {
        return indexer.refresh();
    }

    public SuggestIndexer indexer() {
        return indexer;
    }

    protected static ReadingConverter createReadingConverter(SuggestSettings settings) {
        //TODO
        return new ReadingConverterChain();
    }

    protected static Normalizer createNormalizer(SuggestSettings settings) {
        //TODO
        return new NormalizerChain();
    }

    //getter
    public SuggestSettings getSettings() {
        return settings;
    }

    public SuggestIndexer getIndexer() {
        return indexer;
    }

    public ReadingConverter getReadingConverter() {
        return readingConverter;
    }

    public Normalizer getNormalizer() {
        return normalizer;
    }
}
