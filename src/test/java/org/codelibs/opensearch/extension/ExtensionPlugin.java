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
package org.codelibs.opensearch.extension;

import java.util.HashMap;
import java.util.Map;

import org.codelibs.opensearch.extension.analysis.AlphaNumWordFilterFactory;
import org.codelibs.opensearch.extension.analysis.CharTypeFilterFactory;
import org.codelibs.opensearch.extension.analysis.DisableGraphFilterFactory;
import org.codelibs.opensearch.extension.analysis.FlexiblePorterStemFilterFactory;
import org.codelibs.opensearch.extension.analysis.IterationMarkCharFilterFactory;
import org.codelibs.opensearch.extension.analysis.KanjiNumberFilterFactory;
import org.codelibs.opensearch.extension.analysis.NGramSynonymTokenizerFactory;
import org.codelibs.opensearch.extension.analysis.NumberConcatenationFilterFactory;
import org.codelibs.opensearch.extension.analysis.PatternConcatenationFilterFactory;
import org.codelibs.opensearch.extension.analysis.PosConcatenationFilterFactory;
import org.codelibs.opensearch.extension.analysis.ProlongedSoundMarkCharFilterFactory;
import org.codelibs.opensearch.extension.analysis.ReloadableKeywordMarkerFilterFactory;
import org.codelibs.opensearch.extension.analysis.ReloadableStopFilterFactory;
import org.codelibs.opensearch.extension.analysis.StopTokenPrefixFilterFactory;
import org.codelibs.opensearch.extension.analysis.StopTokenSuffixFilterFactory;
import org.codelibs.opensearch.extension.kuromoji.index.analysis.JapaneseStopTokenFilterFactory;
import org.codelibs.opensearch.extension.kuromoji.index.analysis.KuromojiBaseFormFilterFactory;
import org.codelibs.opensearch.extension.kuromoji.index.analysis.KuromojiIterationMarkCharFilterFactory;
import org.codelibs.opensearch.extension.kuromoji.index.analysis.KuromojiKatakanaStemmerFactory;
import org.codelibs.opensearch.extension.kuromoji.index.analysis.KuromojiNumberFilterFactory;
import org.codelibs.opensearch.extension.kuromoji.index.analysis.KuromojiPartOfSpeechFilterFactory;
import org.codelibs.opensearch.extension.kuromoji.index.analysis.KuromojiReadingFormFilterFactory;
import org.codelibs.opensearch.extension.kuromoji.index.analysis.KuromojiTokenizerFactory;
import org.opensearch.index.analysis.CharFilterFactory;
import org.opensearch.index.analysis.TokenFilterFactory;
import org.opensearch.index.analysis.TokenizerFactory;
import org.opensearch.indices.analysis.AnalysisModule.AnalysisProvider;
import org.opensearch.plugins.AnalysisPlugin;
import org.opensearch.plugins.Plugin;

public class ExtensionPlugin extends Plugin implements AnalysisPlugin {

    @Override
    public Map<String, AnalysisProvider<CharFilterFactory>> getCharFilters() {
        final Map<String, AnalysisProvider<CharFilterFactory>> extra = new HashMap<>();
        extra.put("iteration_mark", IterationMarkCharFilterFactory::new);
        extra.put("prolonged_sound_mark", ProlongedSoundMarkCharFilterFactory::new);
        extra.put("reloadable_kuromoji_iteration_mark", KuromojiIterationMarkCharFilterFactory::new);
        return extra;
    }

    @Override
    public Map<String, AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
        final Map<String, AnalysisProvider<TokenFilterFactory>> extra = new HashMap<>();
        extra.put("reloadable_kuromoji_baseform", KuromojiBaseFormFilterFactory::new);
        extra.put("reloadable_kuromoji_part_of_speech", KuromojiPartOfSpeechFilterFactory::new);
        extra.put("reloadable_kuromoji_readingform", KuromojiReadingFormFilterFactory::new);
        extra.put("reloadable_kuromoji_stemmer", KuromojiKatakanaStemmerFactory::new);
        extra.put("reloadable_kuromoji_number", KuromojiNumberFilterFactory::new);
        extra.put("reloadable_ja_stop", JapaneseStopTokenFilterFactory::new);
        extra.put("kanji_number", KanjiNumberFilterFactory::new);
        extra.put("kuromoji_pos_concat", PosConcatenationFilterFactory::new);
        extra.put("char_type", CharTypeFilterFactory::new);
        extra.put("number_concat", NumberConcatenationFilterFactory::new);
        extra.put("pattern_concat", PatternConcatenationFilterFactory::new);
        extra.put("stop_prefix", StopTokenPrefixFilterFactory::new);
        extra.put("stop_suffix", StopTokenSuffixFilterFactory::new);
        extra.put("reloadable_keyword_marker", ReloadableKeywordMarkerFilterFactory::new);
        extra.put("reloadable_stop", ReloadableStopFilterFactory::new);
        extra.put("flexible_porter_stem", FlexiblePorterStemFilterFactory::new);
        extra.put("alphanum_word", AlphaNumWordFilterFactory::new);
        extra.put("disable_graph", DisableGraphFilterFactory::new);
        return extra;
    }

    @Override
    public Map<String, AnalysisProvider<TokenizerFactory>> getTokenizers() {
        final Map<String, AnalysisProvider<TokenizerFactory>> extra = new HashMap<>();
        extra.put("reloadable_kuromoji_tokenizer", KuromojiTokenizerFactory::new);
        extra.put("reloadable_kuromoji", KuromojiTokenizerFactory::new);
        extra.put("ngram_synonym", NGramSynonymTokenizerFactory::new);
        return extra;
    }

}
