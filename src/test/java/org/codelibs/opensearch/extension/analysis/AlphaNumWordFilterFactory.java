/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
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

import org.apache.lucene.analysis.TokenStream;
import org.codelibs.analysis.en.AlphaNumWordFilter;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenFilterFactory;

public class AlphaNumWordFilterFactory extends AbstractTokenFilterFactory {

    private final int maxTokenLength;

    public AlphaNumWordFilterFactory(final IndexSettings indexSettings, final Environment environment, final String name,
            final Settings settings) {
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
