package org.codelibs.fesen.extension.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.codelibs.analysis.ja.KanjiNumberFilter;
import org.codelibs.fesen.common.settings.Settings;
import org.codelibs.fesen.env.Environment;
import org.codelibs.fesen.index.IndexSettings;
import org.codelibs.fesen.index.analysis.AbstractTokenFilterFactory;

public class KanjiNumberFilterFactory extends AbstractTokenFilterFactory {

    public KanjiNumberFilterFactory(final IndexSettings indexSettings, final Environment environment, final String name,
            final Settings settings) {
        super(indexSettings, name, settings);
    }

    @Override
    public TokenStream create(final TokenStream tokenStream) {
        return new KanjiNumberFilter(tokenStream);
    }

}
