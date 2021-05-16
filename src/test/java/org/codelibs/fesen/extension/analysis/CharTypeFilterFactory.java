/*
 * Copyright 2009-2021 the CodeLibs Project and the Others.
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
