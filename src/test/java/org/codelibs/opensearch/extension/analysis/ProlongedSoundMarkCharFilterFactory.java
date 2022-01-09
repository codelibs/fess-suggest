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

import java.io.Reader;

import org.codelibs.analysis.ja.ProlongedSoundMarkCharFilter;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractCharFilterFactory;

public class ProlongedSoundMarkCharFilterFactory extends AbstractCharFilterFactory {
    private char replacement;

    public ProlongedSoundMarkCharFilterFactory(final IndexSettings indexSettings, final Environment env, final String name,
            final Settings settings) {
        super(indexSettings, name);
        final String value = settings.get("replacement");
        if (value == null || value.length() == 0) {
            replacement = '\u30fc';
        } else {
            replacement = value.charAt(0);
        }
    }

    @Override
    public Reader create(final Reader tokenStream) {
        return new ProlongedSoundMarkCharFilter(tokenStream, replacement);
    }

}
