package org.codelibs.fess.suggest;

import junit.framework.TestCase;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.common.lang3.StringUtils;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;

public class SuggesterBuilderTest extends TestCase {
    ElasticsearchClusterRunner runner;

    @Override
    public void setUp() throws Exception {
        runner = new ElasticsearchClusterRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("index.number_of_replicas", 0);
            settingsBuilder.put("script.disable_dynamic", false);
            settingsBuilder.put("script.groovy.sandbox.enabled", true);
        }).build(newConfigs().ramIndexStore().numOfNode(1));
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
        assertTrue(StringUtils.isNotBlank(suggester.index));
        assertTrue(StringUtils.isNotBlank(suggester.type));
    }

    public void test_buildWithParameters() throws Exception {
        final String settingsIndexName = "test-settings-index";
        final String settingsTypeName = "test-settings-type";
        final String id = "BuildTest";

        final ReadingConverter converter = (text) -> null;

        final Normalizer normalizer = (text) -> null;

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
