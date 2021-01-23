package org.codelibs.fesen.extension.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.codelibs.analysis.en.AlphaNumWordFilter;
import org.codelibs.fesen.common.settings.Settings;
import org.codelibs.fesen.env.Environment;
import org.codelibs.fesen.index.IndexSettings;
import org.codelibs.fesen.index.analysis.AbstractTokenFilterFactory;

public class AlphaNumWordFilterFactory extends AbstractTokenFilterFactory {

    private final int maxTokenLength;

    public AlphaNumWordFilterFactory(final IndexSettings indexSettings, final Environment environment,
            final String name, final Settings settings) {
        super(indexSettings, name, settings);

        maxTokenLength = settings.getAsInt("max_token_length", AlphaNumWordFilter.DEFAULT_MAX_TOKEN_LENGTH);
    }

    @Override
    public TokenStream create(final TokenStream tokenStream) {
        final AlphaNumWordFilter alphaNumWordFilter = new AlphaNumWordFilter(tokenStream);
        alphaNumWordFilter.setMaxTokenLength(maxTokenLength);
        return alphaNumWordFilter;
    }
}
