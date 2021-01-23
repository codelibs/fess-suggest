package org.codelibs.fesen.extension.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.DisableGraphAttribute;
import org.codelibs.fesen.common.settings.Settings;
import org.codelibs.fesen.env.Environment;
import org.codelibs.fesen.index.IndexSettings;
import org.codelibs.fesen.index.analysis.AbstractTokenFilterFactory;

public class DisableGraphFilterFactory extends AbstractTokenFilterFactory {

    public DisableGraphFilterFactory(final IndexSettings indexSettings, final Environment environment,
            final String name, final Settings settings) {
        super(indexSettings, name, settings);
    }

    @Override
    public TokenStream create(final TokenStream tokenStream) {
        tokenStream.addAttribute(DisableGraphAttribute.class);
        return tokenStream;
    }
}
