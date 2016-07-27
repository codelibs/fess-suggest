package org.codelibs.fess.suggest;

import com.google.common.base.Strings;
import junit.framework.TestCase;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.settings.SuggestSettings;

import java.io.IOException;
import java.util.List;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;

public class SuggesterBuilderTest extends TestCase {
    ElasticsearchClusterRunner runner;

    @Override
    public void setUp() throws Exception {
        runner = new ElasticsearchClusterRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("index.number_of_replicas", 0);
            settingsBuilder.putArray("discovery.zen.ping.unicast.hosts", "localhost:9301-9399");
            settingsBuilder.put("plugin.types", "org.codelibs.elasticsearch.kuromoji.neologd.KuromojiNeologdPlugin");
        }).build(newConfigs().clusterName("SuggesterBuilderTest").numOfNode(1));
        runner.ensureYellow();
    }

    @Override
    protected void tearDown() throws Exception {
        runner.close();
        runner.clean();
    }

    public void test_buildWithDefault() throws Exception {
        final String id = "BuildTest";
        final Suggester suggester = Suggester.builder().build(runner.client(), id);

        assertNotNull(suggester);
        assertNotNull(suggester.client);
        assertNotNull(suggester.indexer());
        assertNotNull(suggester.getNormalizer());
        assertNotNull(suggester.getReadingConverter());
        assertNotNull(suggester.settings());
        assertTrue(!Strings.isNullOrEmpty(suggester.index));
        assertTrue(!Strings.isNullOrEmpty(suggester.type));
    }

    public void test_buildWithParameters() throws Exception {
        final String settingsIndexName = "test-settings-index";
        final String settingsTypeName = "test-settings-type";
        final String id = "BuildTest";

        final ReadingConverter converter = new ReadingConverter() {
            @Override
            public void init() throws IOException {

            }

            @Override
            public List<String> convert(String text, String lang) throws IOException {
                return null;
            }
        };

        final Normalizer normalizer = (text, lang) -> null;

        final Suggester suggester =
                Suggester.builder()
                        .settings(SuggestSettings.builder().setSettingsIndexName(settingsIndexName).setSettingsTypeName(settingsTypeName))
                        .readingConverter(converter).normalizer(normalizer).build(runner.client(), id);

        assertEquals(runner.client(), suggester.client);

        SuggestSettings settings = suggester.settings();
        assertEquals(settingsIndexName, settings.getSettingsIndexName());
        assertEquals(settingsTypeName, settings.getSettingsTypeName());
        assertEquals(id, settings.getSettingsId());

        assertEquals(converter, suggester.getReadingConverter());
        assertEquals(normalizer, suggester.getNormalizer());

    }
}
