/*
 * Copyright 2012-2021 CodeLibs Project and the Others.
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
package org.codelibs.fesen.extension.analysis;

import org.apache.lucene.analysis.Tokenizer;
import org.codelibs.fesen.common.settings.Settings;
import org.codelibs.fesen.env.Environment;
import org.codelibs.fesen.index.IndexSettings;
import org.codelibs.fesen.index.analysis.AbstractTokenizerFactory;

/**
 * Factory for {@link NGramSynonymTokenizer}.
 */
public final class NGramSynonymTokenizerFactory extends AbstractTokenizerFactory {

    private final boolean ignoreCase;

    private final int n;

    private final String delimiters;

    private final boolean expand;

    private SynonymLoader synonymLoader = null;

    public NGramSynonymTokenizerFactory(final IndexSettings indexSettings, final Environment env, final String name,
            final Settings settings) {
        super(indexSettings, settings, name);
        ignoreCase = settings.getAsBoolean("ignore_case", true);
        n = settings.getAsInt("n", NGramSynonymTokenizer.DEFAULT_N_SIZE);
        delimiters = settings.get("delimiters", NGramSynonymTokenizer.DEFAULT_DELIMITERS);
        expand = settings.getAsBoolean("expand", true);

        settings.getAsBoolean("expand_ngram", false); // TODO remove

        synonymLoader = new SynonymLoader(env, settings, expand, SynonymLoader.getAnalyzer(ignoreCase));
        if (synonymLoader.getSynonymMap() == null) {
            if (settings.getAsList("synonyms", null) != null) {
                logger.warn("synonyms values are empty.");
            } else if (settings.get("synonyms_path") != null) {
                logger.warn("synonyms_path[{}] is empty.", settings.get("synonyms_path"));
            } else {
                logger.debug("No synonym data.");
            }
        }
    }

    @Override
    public Tokenizer create() {
        return new NGramSynonymTokenizer(n, delimiters, expand, ignoreCase, synonymLoader);
    }
}
