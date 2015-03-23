/*
 * Copyright 2009-2014 the CodeLibs Project and the Others.
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

package org.codelibs.fess.suggest.converter;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ja.JapaneseTokenizerFactory;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.analysis.ja.tokenattributes.ReadingAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.FilesystemResourceLoader;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.codelibs.fess.suggest.SuggestConstants;
import org.codelibs.fess.suggest.analysis.SuggestReadingAttribute;
import org.codelibs.fess.suggest.analysis.SuggestTokenizerFactory;

import com.ibm.icu.text.Transliterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KatakanaConverter implements SuggestReadingConverter {
    private static final Logger logger = LoggerFactory.getLogger(KatakanaConverter.class);

    private final Transliterator transliterator = Transliterator.getInstance("Hiragana-Katakana");

    protected volatile UserDictionary userDictionary;

    protected volatile boolean initialized = false;

    protected TokenizerFactory tokenizerFactory = null;

    protected void init() {
        if (initialized) {
            return;
        }

        if (tokenizerFactory == null) {
            final String path = System.getProperty(SuggestConstants.USER_DICT_PATH);
            String encoding = System.getProperty(SuggestConstants.USER_DICT_ENCODING);
            Map<String, String> args = new HashMap<String, String>();
            args.put("mode", "normal");
            args.put("discardPunctuation", "false");
            if (StringUtils.isNotBlank(path)) {
                args.put("userDictionary", path);
            }
            if (StringUtils.isNotBlank(encoding)) {
                args.put("userDictionaryEncoding", encoding);
            }
            JapaneseTokenizerFactory japaneseTokenizerFactory = new JapaneseTokenizerFactory(args);
            try {
                japaneseTokenizerFactory.inform(new FilesystemResourceLoader());
            } catch (Exception e) {
                logger.warn("Failed to initialize.", e);
            }
            tokenizerFactory = japaneseTokenizerFactory;
        }
        initialized = true;
    }

    @Override
    public void start() {
        init();
    }

    @Override
    public void setTokenizerFactory(TokenizerFactory tokenizerFactory) {
        if (isEnableTokenizer(tokenizerFactory)) {
            this.tokenizerFactory = tokenizerFactory;
        } else {
            logger.warn("Invalid tokenizerFactory. " + tokenizerFactory.getClass().getName());
        }
    }

    @Override
    public List<String> convert(final String text) {
        final List<String> readingList = new ArrayList<String>();
        try {
            readingList.add(toKatakana(text));
        } catch (final Exception e) {

        }
        return readingList;
    }

    protected String toKatakana(final String inputStr) throws IOException {
        final StringBuilder kanaBuf = new StringBuilder();

        final Reader rd = new StringReader(inputStr);
        TokenStream stream = null;
        try {
            stream = createTokenStream(rd);
            stream.reset();

            int offset = 0;
            while (stream.incrementToken()) {
                final CharTermAttribute att = stream.getAttribute(CharTermAttribute.class);
                final String term = att.toString();
                final int pos = inputStr.substring(offset).indexOf(term);
                if (pos > 0) {
                    final String tmp = inputStr.substring(offset, offset + pos);
                    kanaBuf.append(transliterator.transliterate(tmp));
                    offset += pos;
                } else if (pos == -1) {
                    continue;
                }

                String reading = getReadingFromAttribute(stream);
                if (StringUtils.isBlank(reading)) {
                    reading = transliterator.transliterate(att.toString());
                }
                kanaBuf.append(reading);
                offset += term.length();
            }
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        return kanaBuf.toString();
    }

    protected boolean isEnableTokenizer(TokenizerFactory factory) {
        if (factory instanceof JapaneseTokenizerFactory || factory instanceof SuggestTokenizerFactory) {
            return true;
        }
        return false;
    }

    private TokenStream createTokenStream(Reader rd) {
        if (tokenizerFactory instanceof JapaneseTokenizerFactory) {
            return tokenizerFactory.create(rd);
        } else if (tokenizerFactory instanceof SuggestTokenizerFactory) {
            SuggestTokenizerFactory suggestTokenizerFactory = (SuggestTokenizerFactory) tokenizerFactory;
            return suggestTokenizerFactory.create(rd, true);
        } else {
            return null;
        }
    }

    protected String getReadingFromAttribute(TokenStream stream) {
        if (tokenizerFactory instanceof JapaneseTokenizerFactory) {
            final ReadingAttribute rdAttr = stream.getAttribute(ReadingAttribute.class);
            return rdAttr.getReading();
        } else if (tokenizerFactory instanceof SuggestTokenizerFactory) {
            final SuggestReadingAttribute rdAttr = stream.getAttribute(SuggestReadingAttribute.class);
            return rdAttr.toString();
        } else {
            return null;
        }
    }

}
