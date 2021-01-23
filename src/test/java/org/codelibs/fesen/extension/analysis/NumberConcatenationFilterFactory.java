package org.codelibs.fesen.extension.analysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.util.IOUtils;
import org.codelibs.analysis.ja.NumberConcatenationFilter;
import org.codelibs.fesen.common.settings.Settings;
import org.codelibs.fesen.env.Environment;
import org.codelibs.fesen.index.IndexSettings;
import org.codelibs.fesen.index.analysis.AbstractTokenFilterFactory;

public class NumberConcatenationFilterFactory extends AbstractTokenFilterFactory {

    private CharArraySet suffixWords;

    public NumberConcatenationFilterFactory(final IndexSettings indexSettings, final Environment environment,
            final String name, final Settings settings) {
        super(indexSettings, name, settings);

        final String suffixWordsPath = settings.get("suffix_words_path");

        if (suffixWordsPath != null) {
            final File suffixWordsFile = environment.configFile().resolve(suffixWordsPath).toFile();
            try (Reader reader = IOUtils.getDecodingReader(new FileInputStream(suffixWordsFile),
                    StandardCharsets.UTF_8)) {
                suffixWords = WordlistLoader.getWordSet(reader);
            } catch (final IOException e) {
                throw new IllegalArgumentException("Could not load " + suffixWordsFile.getAbsolutePath(), e);
            }
        } else {
            suffixWords = new CharArraySet(0, false);
        }
    }

    @Override
    public TokenStream create(final TokenStream tokenStream) {
        return new NumberConcatenationFilter(tokenStream, suffixWords);
    }
}
