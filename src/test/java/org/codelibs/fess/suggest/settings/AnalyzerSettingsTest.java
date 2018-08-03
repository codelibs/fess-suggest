package org.codelibs.fess.suggest.settings;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.index.IndexNotFoundException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AnalyzerSettingsTest {
    String id = "analyzerSettingsTest";

    static SuggestSettings settings;

    static ElasticsearchClusterRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new ElasticsearchClusterRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.putList("discovery.zen.ping.unicast.hosts", "localhost:9301-9399");
        }).build(
                newConfigs().clusterName("ArraySettingsTest").numOfNode(1)
                        .pluginTypes("org.codelibs.elasticsearch.kuromoji.neologd.KuromojiNeologdPlugin"));
        runner.ensureYellow();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        runner.close();
        runner.clean();
    }

    @Before
    public void before() throws Exception {
        try {
            runner.admin().indices().prepareDelete("_all").execute().actionGet();
        } catch (IndexNotFoundException ignore) {

        }
        runner.refresh();
        settings = Suggester.builder().build(runner.client(), id).settings();
    }

    @Test
    public void test_defaultAnalyzer() {
        String text = "Fess (フェス) は「5 分で簡単に構築可能な全文検索サーバー」です。 Java 実行環境があればどの OS でも実行可能です。 Fess は Apache ライセンスで提供され、無料 (フリーソフト) でご利用いただけます。";
        SuggestAnalyzer analyzer = SuggestUtil.createDefaultAnalyzer(runner.client(), settings);
        final List<AnalyzeResponse.AnalyzeToken> tokens = analyzer.analyze(text, "", null);
        final List<AnalyzeResponse.AnalyzeToken> readingTokens = analyzer.analyzeAndReading(text, "", null);

        int matchCount = 0;
        for (int i = 0; i < tokens.size(); i++) {
            final String term = tokens.get(i).getTerm();
            final String reading = readingTokens.get(i).getTerm();
            switch (term) {
            case "fess":
                matchCount++;
                assertEquals("フェス", reading);
                break;
            case "全文検索":
                matchCount++;
                assertEquals("ゼンブンケンサク", reading);
                break;
            case "無料":
                matchCount++;
                assertEquals("ムリョウ", reading);
                break;
            default:
                break;
            }
        }
        assertEquals(4, matchCount);
    }

    @Test
    public void test_analyzerNames() throws Exception {
        final Set<String> analyzerNames = settings.analyzer().getAnalyzerNames();
        assert (analyzerNames.size() > 4);
        assertTrue(analyzerNames.contains(settings.analyzer().getContentsAnalyzerName("", "")));
        assertTrue(analyzerNames.contains(settings.analyzer().getContentsReadingAnalyzerName("", "")));
        assertTrue(analyzerNames.contains(settings.analyzer().getReadingAnalyzerName("", "")));
        assertTrue(analyzerNames.contains(settings.analyzer().getReadingTermAnalyzerName("", "")));
        assertTrue(analyzerNames.contains(settings.analyzer().getNormalizeAnalyzerName("", "")));

        for (final String lang : settings.analyzer().SUPPORTED_LANGUAGES) {
            assertTrue(analyzerNames.contains(settings.analyzer().getContentsAnalyzerName("", lang)));
            assertTrue(analyzerNames.contains(settings.analyzer().getContentsReadingAnalyzerName("", lang)));
            assertTrue(analyzerNames.contains(settings.analyzer().getReadingAnalyzerName("", lang)));
            assertTrue(analyzerNames.contains(settings.analyzer().getReadingTermAnalyzerName("", lang)));
            assertTrue(analyzerNames.contains(settings.analyzer().getNormalizeAnalyzerName("", lang)));
        }
    }

}
