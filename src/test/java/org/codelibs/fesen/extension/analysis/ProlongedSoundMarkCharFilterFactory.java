package org.codelibs.fesen.extension.analysis;

import java.io.Reader;

import org.codelibs.analysis.ja.ProlongedSoundMarkCharFilter;
import org.codelibs.fesen.common.settings.Settings;
import org.codelibs.fesen.env.Environment;
import org.codelibs.fesen.index.IndexSettings;
import org.codelibs.fesen.index.analysis.AbstractCharFilterFactory;

public class ProlongedSoundMarkCharFilterFactory extends AbstractCharFilterFactory {
    private char replacement;

    public ProlongedSoundMarkCharFilterFactory(final IndexSettings indexSettings, final Environment env,
            final String name, final Settings settings) {
        super(indexSettings, name);
        final String value = settings.get("replacement");
        if (value == null || value.length() == 0) {
            replacement = '\u30fc';
        } else {
            replacement = value.charAt(0);
        }
    }

    @Override
    public Reader create(final Reader tokenStream) {
        return new ProlongedSoundMarkCharFilter(tokenStream, replacement);
    }

}
