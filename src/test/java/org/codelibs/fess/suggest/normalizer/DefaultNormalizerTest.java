/*
 * Copyright 2009-2019 the CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
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
            settingsBuilder.putList("discovery.seed_hosts", "127.0.0.1:9301");
            settingsBuilder.putList("cluster.initial_master_nodes", "127.0.0.1:9301");
        }).build(newConfigs().clusterName("DefaultNormalizerTest").numOfNode(1)
                .pluginTypes("org.codelibs.elasticsearch.kuromoji.ipadic.neologd.KuromojiNeologdPlugin"));
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
