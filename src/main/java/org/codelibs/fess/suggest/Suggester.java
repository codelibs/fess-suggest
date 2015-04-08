package org.codelibs.fess.suggest;

import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.index.SuggestIndexer;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.request.suggest.SuggestRequestBuilder;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.client.Client;

public class Suggester {
    protected final Client client;
    protected final SuggestSettings settings;
    protected final SuggestIndexer indexer;
    protected final ReadingConverter readingConverter;
    protected final Normalizer normalizer;

    protected final String index;
    protected final String type;

    public Suggester(final Client client, final SuggestSettings settings, final ReadingConverter readingConverter,
                     final Normalizer normalizer, final SuggestIndexer indexer) {
        this.client = client;
        this.settings = settings;
        this.readingConverter = readingConverter;
        this.normalizer = normalizer;
        this.index = settings.getAsString(SuggestSettings.DefaultKeys.INDEX, "");
        this.type = settings.getAsString(SuggestSettings.DefaultKeys.TYPE, "");
        this.indexer = indexer;
    }

    public SuggestRequestBuilder suggest() {
        return new SuggestRequestBuilder(client, readingConverter, normalizer).setIndex(index).setType(type);
    }

    public RefreshResponse refresh() {
        return indexer.refresh();
    }

    public SuggestIndexer indexer() {
        return indexer;
    }

    public static SuggesterBuilder builder() {
        return new SuggesterBuilder();
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
