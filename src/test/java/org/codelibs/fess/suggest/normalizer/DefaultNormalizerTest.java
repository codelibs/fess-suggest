package org.codelibs.fess.suggest.normalizer;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.junit.Assert.assertEquals;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultNormalizerTest {
    static Suggester suggester;

    static ElasticsearchClusterRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new ElasticsearchClusterRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.putList("discovery.zen.ping.unicast.hosts", "localhost:9301-9399");
        }).build(
                newConfigs().clusterName("DefaultNormalizerTest").numOfNode(1)
                        .pluginTypes("org.codelibs.elasticsearch.kuromoji.neologd.KuromojiNeologdPlugin"));
        runner.ensureYellow();

        suggester = Suggester.builder().build(runner.client(), "SuggesterTest");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        runner.close();
        runner.clean();
    }

    @Test
    public void test_normalize() throws Exception {
        Normalizer normalizer = SuggestUtil.createDefaultNormalizer(runner.client(), suggester.settings());
        assertEquals("12345,.*[]「」abcケンサクabcdけんさくガギグゲゴ", normalizer.normalize("１２３４５,.*[]「」ＡBCｹﾝｻｸabcdけんさくｶﾞｷﾞｸﾞｹﾞｺﾞ", null));
    }
}
