package components;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.fess.suggest.Suggester;

public class ComponentsUtil {
    public static String contentIndexName;

    public static String contentTypeName;

    public static ElasticsearchClusterRunner runner;

    public static Suggester suggester;
}
