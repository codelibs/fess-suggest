package org.codelibs.fess.suggest;

import org.apache.lucene.analysis.Analyzer;
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
    protected final ReadingConverter readingConverter;
    protected final Normalizer normalizer;
    protected final Analyzer analyzer;

    protected final String index;
    protected final String type;

    protected final String[] supportedFields;
    protected final String tagFieldName;
    protected final String roleFieldName;
    protected final String[] ngWords;

    public Suggester(final Client client, final SuggestSettings settings, final ReadingConverter readingConverter,
            final Normalizer normalizer, final Analyzer analyzer) {
        this.client = client;
        this.settings = settings;
        this.readingConverter = readingConverter;
        this.normalizer = normalizer;
        this.analyzer = analyzer;
        this.index = settings.getAsString(SuggestSettings.DefaultKeys.INDEX, "");
        this.type = settings.getAsString(SuggestSettings.DefaultKeys.TYPE, "");

        this.supportedFields = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS);
        this.ngWords = settings.ngword().get();
        tagFieldName = settings.getAsString(SuggestSettings.DefaultKeys.TAG_FIELD_NAME, "");
        roleFieldName = settings.getAsString(SuggestSettings.DefaultKeys.ROLE_FIELD_NAME, "");
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
        return new SuggestIndexer(client, index, type, supportedFields, ngWords, tagFieldName, roleFieldName, readingConverter, normalizer,
                analyzer, settings);
    }

}
