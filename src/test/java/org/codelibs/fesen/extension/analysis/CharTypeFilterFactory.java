package org.codelibs.fesen.extension.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.codelibs.analysis.ja.CharTypeFilter;
import org.codelibs.fesen.common.settings.Settings;
import org.codelibs.fesen.env.Environment;
import org.codelibs.fesen.index.IndexSettings;
import org.codelibs.fesen.index.analysis.AbstractTokenFilterFactory;

public class CharTypeFilterFactory extends AbstractTokenFilterFactory {

    private final boolean alphabetic;

    private final boolean digit;

    private final boolean letter;

    public CharTypeFilterFactory(final IndexSettings indexSettings, final Environment environment, final String name,
            final Settings settings) {
        super(indexSettings, name, settings);

        alphabetic = settings.getAsBoolean("alphabetic", true);
        digit = settings.getAsBoolean("digit", true);
        letter = settings.getAsBoolean("letter", true);
    }

    @Override
    public TokenStream create(final TokenStream tokenStream) {
        return new CharTypeFilter(tokenStream, alphabetic, digit, letter);
    }
}
