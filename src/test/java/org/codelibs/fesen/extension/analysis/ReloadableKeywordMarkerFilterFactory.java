package org.codelibs.fesen.extension.analysis;

import java.nio.file.Path;

import org.apache.lucene.analysis.TokenStream;
import org.codelibs.analysis.en.ReloadableKeywordMarkerFilter;
import org.codelibs.fesen.common.settings.Settings;
import org.codelibs.fesen.common.unit.TimeValue;
import org.codelibs.fesen.env.Environment;
import org.codelibs.fesen.index.IndexSettings;
import org.codelibs.fesen.index.analysis.AbstractTokenFilterFactory;

public class ReloadableKeywordMarkerFilterFactory extends AbstractTokenFilterFactory {

    private final Path keywordPath;

    private final long reloadInterval;

    public ReloadableKeywordMarkerFilterFactory(final IndexSettings indexSettings, final Environment environment,
            final String name, final Settings settings) {
        super(indexSettings, name, settings);

        final String path = settings.get("keywords_path");
        if (path != null) {
            keywordPath = environment.configFile().resolve(path);
        } else {
            keywordPath = null;
        }

        reloadInterval = settings.getAsTime("reload_interval", TimeValue.timeValueMinutes(1)).getMillis();
    }

    @Override
    public TokenStream create(final TokenStream tokenStream) {
        if (keywordPath == null) {
            return tokenStream;
        }
        return new ReloadableKeywordMarkerFilter(tokenStream, keywordPath, reloadInterval);
    }

}