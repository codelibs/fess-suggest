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

import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.codelibs.analysis.ja.PatternConcatenationFilter;
import org.codelibs.fesen.common.settings.Settings;
import org.codelibs.fesen.env.Environment;
import org.codelibs.fesen.index.IndexSettings;
import org.codelibs.fesen.index.analysis.AbstractTokenFilterFactory;

public class PatternConcatenationFilterFactory extends AbstractTokenFilterFactory {

    private Pattern pattern1;

    private Pattern pattern2;

    public PatternConcatenationFilterFactory(final IndexSettings indexSettings, final Environment environment, final String name,
            final Settings settings) {
        super(indexSettings, name, settings);

        final String pattern1Str = settings.get("pattern1");
        final String pattern2Str = settings.get("pattern2", ".*");

        if (logger.isDebugEnabled()) {
            logger.debug("pattern1: {}, pattern2: {}", pattern1Str, pattern2Str);
        }
        if (pattern1Str != null) {
            pattern1 = Pattern.compile(pattern1Str);
            pattern2 = Pattern.compile(pattern2Str);
        }
    }

    @Override
    public TokenStream create(final TokenStream tokenStream) {
        return new PatternConcatenationFilter(tokenStream, pattern1, pattern2);
    }
}
