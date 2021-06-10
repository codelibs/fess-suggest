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
package org.codelibs.fesen.extension.kuromoji.index.analysis;

import java.io.Reader;

import org.apache.lucene.analysis.ja.JapaneseIterationMarkCharFilter;
import org.codelibs.fesen.common.settings.Settings;
import org.codelibs.fesen.env.Environment;
import org.codelibs.fesen.index.IndexSettings;
import org.codelibs.fesen.index.analysis.AbstractCharFilterFactory;
import org.codelibs.fesen.index.analysis.NormalizingCharFilterFactory;

public class KuromojiIterationMarkCharFilterFactory extends AbstractCharFilterFactory implements NormalizingCharFilterFactory {

    private final boolean normalizeKanji;
    private final boolean normalizeKana;

    public KuromojiIterationMarkCharFilterFactory(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        super(indexSettings, name);
        normalizeKanji = settings.getAsBoolean("normalize_kanji", JapaneseIterationMarkCharFilter.NORMALIZE_KANJI_DEFAULT);
        normalizeKana = settings.getAsBoolean("normalize_kana", JapaneseIterationMarkCharFilter.NORMALIZE_KANA_DEFAULT);
    }

    @Override
    public Reader create(Reader reader) {
        return new JapaneseIterationMarkCharFilter(reader, normalizeKanji, normalizeKana);
    }

}
