package org.codelibs.fesen.extension.analysis;

import java.nio.file.Path;

import org.apache.lucene.analysis.TokenStream;
import org.codelibs.analysis.en.ReloadableStopFilter;
import org.codelibs.fesen.common.settings.Settings;
import org.codelibs.fesen.common.unit.TimeValue;
import org.codelibs.fesen.env.Environment;
import org.codelibs.fesen.index.IndexSettings;
import org.codelibs.fesen.index.analysis.AbstractTokenFilterFactory;

public class ReloadableStopFilterFactory extends AbstractTokenFilterFactory {

    private final Path stopwordPath;

    private final long reloadInterval;

    private final boolean ignoreCase;

    public ReloadableStopFilterFactory(final IndexSettings indexSettings, final Environment environment,
            final String name, final Settings settings) {
        super(indexSettings, name, settings);

        final String path = settings.get("stopwords_path");
        if (path != null) {
            stopwordPath = environment.configFile().resolve(path);
        } else {
            stopwordPath = null;
        }

        ignoreCase = settings.getAsBoolean("ignore_case", false);
        reloadInterval = settings.getAsTime("reload_interval", TimeValue.timeValueMinutes(1)).getMillis();
    }

    @Override
    public TokenStream create(final TokenStream tokenStream) {
        if (stopwordPath == null) {
            return tokenStream;
        }
        return new ReloadableStopFilter(tokenStream, stopwordPath, ignoreCase, reloadInterval);
    }

}