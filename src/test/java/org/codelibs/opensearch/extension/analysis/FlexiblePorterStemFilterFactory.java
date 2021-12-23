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
package org.codelibs.opensearch.extension.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.codelibs.analysis.en.FlexiblePorterStemFilter;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenFilterFactory;

public class FlexiblePorterStemFilterFactory extends AbstractTokenFilterFactory {

    private final boolean step1;

    private final boolean step2;

    private final boolean step3;

    private final boolean step4;

    private final boolean step5;

    private final boolean step6;

    public FlexiblePorterStemFilterFactory(final IndexSettings indexSettings, final Environment environment, final String name,
            final Settings settings) {
        super(indexSettings, name, settings);

        step1 = settings.getAsBoolean("step1", true);
        step2 = settings.getAsBoolean("step2", true);
        step3 = settings.getAsBoolean("step3", true);
        step4 = settings.getAsBoolean("step4", true);
        step5 = settings.getAsBoolean("step5", true);
        step6 = settings.getAsBoolean("step6", true);
    }

    @Override
    public TokenStream create(final TokenStream tokenStream) {
        return new FlexiblePorterStemFilter(tokenStream, step1, step2, step3, step4, step5, step6);
    }

}