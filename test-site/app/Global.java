import components.ComponentsUtil;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.Suggester;
import play.Application;
import play.GlobalSettings;
import play.Logger;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;

public class Global extends GlobalSettings {
    @Override
    public void onStart(Application var1) {
        Logger.info("Start!!!");
        esRun();
        createComponents();
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
            settingsBuilder.put("index.number_of_replicas", 0);
            settingsBuilder.put("script.disable_dynamic", false);
            settingsBuilder.put("script.groovy.sandbox.enabled", true);
        }).build(newConfigs().ramIndexStore().numOfNode(1));
        runner.ensureYellow();

        ComponentsUtil.runner = runner;
    }

    private void createComponents() {
        try {
            ComponentsUtil.contentIndexName = "test-content";
            ComponentsUtil.contentTypeName = "test";
            ComponentsUtil.suggester = Suggester.builder().build(ComponentsUtil.runner.client(), "test");
        } catch (Exception e) {
            Logger.error("Failed to create components.", e);
        }
    }
}
