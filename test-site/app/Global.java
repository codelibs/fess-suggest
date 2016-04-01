
import components.*;
import controllers.*;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.Suggester;
import play.Application;
import play.GlobalSettings;
import play.Logger;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;

public class Global extends GlobalSettings {
    @Override
    public void onStart(Application var1) {
        Logger.info("Start Elasticsearch cluster.");
        esRun();
        Logger.info("Setup suggest... wait 1min");
        createComponents();
        Logger.info("Create content index.");
        Suggest.createContent();
        Logger.info("Create suggest index.");
        Suggest.createSuggestFromContent();
        Logger.info("Finished setup suggest.");
    }

    @Override
    public void onStop(Application var1) {
        ComponentsUtil.runner.close();
        ComponentsUtil.runner.clean();
    }

    private void esRun() {
        ElasticsearchClusterRunner runner = new ElasticsearchClusterRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("index.number_of_shards", 1);
            settingsBuilder.put("index.number_of_replicas", 0);
            settingsBuilder.putArray("discovery.zen.ping.unicast.hosts", "localhost:9301-9399");
            settingsBuilder.put("plugin.types", "org.codelibs.elasticsearch.kuromoji.neologd.KuromojiNeologdPlugin,org.codelibs.elasticsearch.FessSuggestPlugin");
        }).build(newConfigs().clusterName("SuggesterTest").numOfNode(1));
        runner.ensureYellow();

        ComponentsUtil.runner = runner;
    }

    private void createComponents() {
        try {
            ComponentsUtil.contentIndexName = "test-content";
            ComponentsUtil.contentTypeName = "test";
            ComponentsUtil.suggester = Suggester.builder().build(ComponentsUtil.runner.client(), "test");
            ComponentsUtil.suggester.createIndexIfNothing();
        } catch (Exception e) {
            Logger.error("Failed to create components.", e);
        }
    }
}
