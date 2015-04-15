package org.codelibs.fess.suggest;

import org.apache.lucene.analysis.Analyzer;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.index.SuggestIndexer;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.request.suggest.SuggestRequestBuilder;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.client.Client;

import java.util.concurrent.Executor;

public class Suggester {
    protected final Client client;
    protected final SuggestSettings settings;
    protected final ReadingConverter readingConverter;
    protected final Normalizer normalizer;
    protected final Analyzer analyzer;

    protected final String index;
    protected final String type;

    protected final Executor threadPool;

    public Suggester(final Client client, final SuggestSettings settings, final ReadingConverter readingConverter,
            final Normalizer normalizer, final Analyzer analyzer, final Executor threadPool) {
        this.client = client;
        this.settings = settings;
        this.readingConverter = readingConverter;
        this.normalizer = normalizer;
        this.analyzer = analyzer;
        this.index = settings.getAsString(SuggestSettings.DefaultKeys.INDEX, "");
        this.type = settings.getAsString(SuggestSettings.DefaultKeys.TYPE, "");
        this.threadPool = threadPool;

    }

    public SuggestRequestBuilder suggest() {
        return new SuggestRequestBuilder(client, readingConverter, normalizer).setIndex(index).setType(type);
    }

    public RefreshResponse refresh() {
        return client.admin().indices().prepareRefresh(index).execute().actionGet();
    }

    public SuggestIndexer indexer() {
        return createDefaultIndexer();
    }

    public static SuggesterBuilder builder() {
        return new SuggesterBuilder();
    }

    //getter
    public SuggestSettings settings() {
        return settings;
    }

    public ReadingConverter getReadingConverter() {
        return readingConverter;
    }

    public Normalizer getNormalizer() {
        return normalizer;
    }

    protected SuggestIndexer createDefaultIndexer() {
        return new SuggestIndexer(client, index, type, readingConverter, normalizer, analyzer, settings, threadPool);
    }

}
