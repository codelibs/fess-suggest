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

import java.nio.file.Path;

import org.apache.lucene.analysis.TokenStream;
import org.codelibs.analysis.en.ReloadableKeywordMarkerFilter;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenFilterFactory;

public class ReloadableKeywordMarkerFilterFactory extends AbstractTokenFilterFactory {

    private final Path keywordPath;

    private final long reloadInterval;

    public ReloadableKeywordMarkerFilterFactory(final IndexSettings indexSettings, final Environment environment, final String name,
            final Settings settings) {
        super(indexSettings, name, settings);

        final String path = settings.get("keywords_path");
        if (path != null) {
            keywordPath = environment.configDir().resolve(path);
        } else {
            keywordPath = null;
        }

        reloadInterval = settings.getAsTime("reload_interval", TimeValue.timeValueMinutes(1)).getMillis();
    }

    @Override
    public TokenStream create(final TokenStream tokenStream) {
        if (keywordPath == null) {
            return tokenStream;
        }
        return new ReloadableKeywordMarkerFilter(tokenStream, keywordPath, reloadInterval);
    }

}