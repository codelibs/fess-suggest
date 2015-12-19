package org.codelibs.fess.suggest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.constants.SuggestConstants;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.index.SuggestIndexer;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.request.popularwords.PopularWordsRequestBuilder;
import org.codelibs.fess.suggest.request.suggest.SuggestRequestBuilder;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexAlreadyExistsException;

public class Suggester {
    protected final Client client;
    protected final SuggestSettings suggestSettings;
    protected final ReadingConverter readingConverter;
    protected final Normalizer normalizer;
    protected final SuggestAnalyzer analyzer;

    protected final String index;
    protected final String type;

    protected final ExecutorService threadPool;

    public Suggester(final Client client, final SuggestSettings settings, final ReadingConverter readingConverter,
            final Normalizer normalizer, final SuggestAnalyzer analyzer, final ExecutorService threadPool) {
        this.client = client;
        this.suggestSettings = settings;
        this.readingConverter = readingConverter;
        this.normalizer = normalizer;
        this.analyzer = analyzer;
        this.index = settings.getAsString(SuggestSettings.DefaultKeys.INDEX, StringUtil.EMPTY);
        this.type = settings.getAsString(SuggestSettings.DefaultKeys.TYPE, StringUtil.EMPTY);
        this.threadPool = threadPool;
    }

    public SuggestRequestBuilder suggest() {
        return new SuggestRequestBuilder(client, readingConverter, normalizer).setIndex(index).setType(type);
    }

    public PopularWordsRequestBuilder popularWords() {
        return new PopularWordsRequestBuilder(client).setIndex(index).setType(type);
    }

    public RefreshResponse refresh() {
        return client.admin().indices().prepareRefresh(index).execute().actionGet(SuggestConstants.ACTION_TIMEOUT);
    }

    public void shutdown() {
        threadPool.shutdownNow();
    }

    public boolean createIndexIfNothing() {
        try {
            boolean created = false;
            final IndicesExistsResponse response =
                    client.admin().indices().prepareExists(index).execute().actionGet(SuggestConstants.ACTION_TIMEOUT);
            if (!response.isExists()) {
                BufferedReader br = null;
                final StringBuilder sb = new StringBuilder();
                try {
                    br =
                            new BufferedReader(new InputStreamReader(this.getClass().getClassLoader()
                                    .getResourceAsStream("suggest-mappings.json")));
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                } finally {
                    if (br != null) {
                        br.close();
                    }
                }
                try {
                    final String mappingSource = sb.toString();
                    client.admin().indices().prepareCreate(index).addMapping("_default_", mappingSource).addMapping(type, mappingSource)
                            .execute().actionGet(SuggestConstants.ACTION_TIMEOUT);

                    client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute()
                            .actionGet(SuggestConstants.ACTION_TIMEOUT * 10);
                } catch (IndexAlreadyExistsException ignore) {}
                created = true;
            }

            return created;
        } catch (final Exception e) {
            throw new SuggesterException("Failed to create index.", e);
        }
    }

    public SuggestIndexer indexer() {
        return createDefaultIndexer();
    }

    public static SuggesterBuilder builder() {
        return new SuggesterBuilder();
    }

    //getter
    public SuggestSettings settings() {
        return suggestSettings;
    }

    public ReadingConverter getReadingConverter() {
        return readingConverter;
    }

    public Normalizer getNormalizer() {
        return normalizer;
    }

    protected SuggestIndexer createDefaultIndexer() {
        return new SuggestIndexer(client, index, type, readingConverter, normalizer, analyzer, suggestSettings, threadPool);
    }

    public String getIndex() {
        return index;
    }

    public String getType() {
        return type;
    }
}
