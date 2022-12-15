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
package org.codelibs.opensearch.extension.kuromoji.index.analysis;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.analysis.ja.util.CSVUtil;
import org.opensearch.OpenSearchException;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenizerFactory;
import org.opensearch.index.analysis.Analysis;

public class KuromojiTokenizerFactory extends AbstractTokenizerFactory {

    private static final String USER_DICT_PATH_OPTION = "user_dictionary";
    private static final String USER_DICT_RULES_OPTION = "user_dictionary_rules";
    private static final String NBEST_COST = "nbest_cost";
    private static final String NBEST_EXAMPLES = "nbest_examples";

    private final UserDictionary userDictionary;
    private final Mode mode;
    private final String nBestExamples;
    private final int nBestCost;

    private boolean discartPunctuation;

    public KuromojiTokenizerFactory(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        super(indexSettings, settings, name);
        mode = getMode(settings);
        userDictionary = getUserDictionary(env, settings);
        discartPunctuation = settings.getAsBoolean("discard_punctuation", true);
        nBestCost = settings.getAsInt(NBEST_COST, -1);
        nBestExamples = settings.get(NBEST_EXAMPLES);
    }

    private static String parse(String rule, Set<String> dup) {
        String[] values = CSVUtil.parse(rule);
        if (dup.add(values[0]) == false) {
            throw new IllegalArgumentException("Found duplicate term [" + values[0] + "] in user dictionary.");
        }
        return rule;
    }

    public static UserDictionary getUserDictionary(Environment env, Settings settings) {
        if (settings.get(USER_DICT_PATH_OPTION) != null && settings.get(USER_DICT_RULES_OPTION) != null) {
            throw new IllegalArgumentException(
                    "It is not allowed to use [" + USER_DICT_PATH_OPTION + "] in conjunction" + " with [" + USER_DICT_RULES_OPTION + "]");
        }
        try {
            Set<String> dup = new HashSet<>();
            List<String> ruleList =
                    Analysis.parseWordList(env, settings, USER_DICT_PATH_OPTION, USER_DICT_RULES_OPTION, s -> parse(s, dup));
            if (ruleList == null || ruleList.isEmpty()) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            for (String line : ruleList) {
                sb.append(line).append(System.lineSeparator());
            }
            return UserDictionary.open(new StringReader(sb.toString()));
        } catch (IOException e) {
            throw new OpenSearchException("failed to load kuromoji user dictionary", e);
        }
    }

    public static JapaneseTokenizer.Mode getMode(Settings settings) {
        JapaneseTokenizer.Mode mode = JapaneseTokenizer.DEFAULT_MODE;
        String modeSetting = settings.get("mode", null);
        if (modeSetting != null) {
            if ("search".equalsIgnoreCase(modeSetting)) {
                mode = JapaneseTokenizer.Mode.SEARCH;
            } else if ("normal".equalsIgnoreCase(modeSetting)) {
                mode = JapaneseTokenizer.Mode.NORMAL;
            } else if ("extended".equalsIgnoreCase(modeSetting)) {
                mode = JapaneseTokenizer.Mode.EXTENDED;
            }
        }
        return mode;
    }

    @Override
    public Tokenizer create() {
        JapaneseTokenizer t = new JapaneseTokenizer(userDictionary, discartPunctuation, mode);
        int nBestCost = this.nBestCost;
        if (nBestExamples != null) {
            nBestCost = Math.max(nBestCost, t.calcNBestCost(nBestExamples));
        }
        t.setNBestCost(nBestCost);
        return t;
    }

}
