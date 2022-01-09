/*
 * Copyright 2012-2022 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.opensearch.extension.analysis;

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
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenFilterFactory;

public class NumberConcatenationFilterFactory extends AbstractTokenFilterFactory {

    private CharArraySet suffixWords;

    public NumberConcatenationFilterFactory(final IndexSettings indexSettings, final Environment environment, final String name,
            final Settings settings) {
        super(indexSettings, name, settings);

        final String suffixWordsPath = settings.get("suffix_words_path");

        if (suffixWordsPath != null) {
            final File suffixWordsFile = environment.configFile().resolve(suffixWordsPath).toFile();
            try (Reader reader = IOUtils.getDecodingReader(new FileInputStream(suffixWordsFile), StandardCharsets.UTF_8)) {
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
