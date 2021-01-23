package org.codelibs.fesen.extension.analysis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ja.tokenattributes.PartOfSpeechAttribute;
import org.codelibs.analysis.ja.PosConcatenationFilter;
import org.codelibs.fesen.common.settings.Settings;
import org.codelibs.fesen.env.Environment;
import org.codelibs.fesen.index.IndexSettings;
import org.codelibs.fesen.index.analysis.AbstractTokenFilterFactory;
import org.codelibs.fesen.index.analysis.Analysis;

public class PosConcatenationFilterFactory extends AbstractTokenFilterFactory {

    private final Set<String> posTags = new HashSet<>();

    public PosConcatenationFilterFactory(final IndexSettings indexSettings, final Environment environment,
            final String name, final Settings settings) {
        super(indexSettings, name, settings);

        final List<String> tagList = Analysis.getWordList(environment, settings, "tags");
        if (tagList != null) {
            posTags.addAll(tagList);
        }
    }

    @Override
    public TokenStream create(final TokenStream tokenStream) {
        final PartOfSpeechAttribute posAtt = tokenStream.addAttribute(PartOfSpeechAttribute.class);
        return new PosConcatenationFilter(tokenStream, posTags, () -> posAtt.getPartOfSpeech());
    }
}
