package org.codelibs.fesen.extension.analysis;

import java.io.Reader;

import org.codelibs.analysis.ja.IterationMarkCharFilter;
import org.codelibs.fesen.common.settings.Settings;
import org.codelibs.fesen.env.Environment;
import org.codelibs.fesen.index.IndexSettings;
import org.codelibs.fesen.index.analysis.AbstractCharFilterFactory;

public class IterationMarkCharFilterFactory extends AbstractCharFilterFactory {

    public IterationMarkCharFilterFactory(final IndexSettings indexSettings, final Environment env, final String name,
            final Settings settings) {
        super(indexSettings, name);
    }

    @Override
    public Reader create(final Reader tokenStream) {
        return new IterationMarkCharFilter(tokenStream);
    }

}
