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
package org.codelibs.fess.suggest.converter;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.TokenizerFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.opensearch.core.common.Strings;

import com.ibm.icu.text.Transliterator;

/**
 * {@link KatakanaConverter} converts input strings to katakana representation.
 * It utilizes a transliterator to convert hiragana to katakana and can optionally
 * use a tokenizer to process the input.
 *
 * <p>
 * The class provides methods to initialize the converter, convert strings, and
 * check if a tokenizer is enabled. It also includes methods to create a token
 * stream and extract reading information from the stream's attributes, although
 * the tokenizer-related functionality is currently commented out.
 * </p>
 */
public class KatakanaConverter implements ReadingConverter {

    protected final Transliterator transliterator = Transliterator.getInstance("Hiragana-Katakana");

    protected volatile boolean initialized = false;

    protected TokenizerFactory tokenizerFactory = null;

    public KatakanaConverter() {
        // nothing
    }

    public KatakanaConverter(final TokenizerFactory tokenizerFactory) {
        if (isEnableTokenizer(tokenizerFactory)) {
            this.tokenizerFactory = tokenizerFactory;
        }
    }

    @Override
    public void init() throws IOException {
        /*
         * TODO if (initialized) { return; }
         *
         * if (tokenizerFactory == null) { final String path = System.getProperty(SuggestConstants.USER_DICT_PATH);
         * final String encoding = System.getProperty(SuggestConstants.USER_DICT_ENCODING); final Map<String, String>
         * args = new HashMap<>(); args.put("mode", "normal"); args.put("discardPunctuation", "false"); if
         * (Strings.isNullOrEmpty(path)) { args.put("userDictionary", path); } if (Strings.isNullOrEmpty(encoding)) {
         * args.put("userDictionaryEncoding", encoding); } final JapaneseTokenizerFactory japaneseTokenizerFactory = new
         * JapaneseTokenizerFactory(args); // TODO japaneseTokenizerFactory.inform(new FilesystemResourceLoader());
         * tokenizerFactory = japaneseTokenizerFactory; } initialized = true;
         */
    }

    @Override
    public List<String> convert(final String text, final String field, final String... langs) throws IOException {
        final List<String> readingList = new ArrayList<>();
        readingList.add(toKatakana(text));
        return readingList;
    }

    protected String toKatakana(final String inputStr) throws IOException {
        final StringBuilder kanaBuf = new StringBuilder();

        final Reader rd = new StringReader(inputStr);
        try (TokenStream stream = createTokenStream(rd)) {
            if (stream == null) {
                throw new IOException("Invalid tokenizer.");
            }
            stream.reset();

            int offset = 0;
            while (stream.incrementToken()) {
                final CharTermAttribute att = stream.getAttribute(CharTermAttribute.class);
                final String term = att.toString();
                final int pos = inputStr.indexOf(term, offset);
                if (pos > 0) {
                    final String tmp = inputStr.substring(offset, pos);
                    kanaBuf.append(transliterator.transliterate(tmp));
                    offset = pos;
                } else if (pos == -1) {
                    continue;
                }

                String reading = getReadingFromAttribute(stream);
                if (Strings.isNullOrEmpty(reading)) {
                    reading = transliterator.transliterate(att.toString());
                }
                kanaBuf.append(reading);
                offset += term.length();
            }
        }

        return kanaBuf.toString();
    }

    protected boolean isEnableTokenizer(final TokenizerFactory factory) {
        // TODO return factory instanceof JapaneseTokenizerFactory;
        return false;
    }

    private TokenStream createTokenStream(final Reader rd) {
        return null;
        /*
         * TODO if (tokenizerFactory instanceof JapaneseTokenizerFactory) { return tokenizerFactory.create(); } else {
         * return null; }
         */
    }

    protected String getReadingFromAttribute(final TokenStream stream) {
        return null;
        /*
         * if (tokenizerFactory instanceof JapaneseTokenizerFactory) { final ReadingAttribute rdAttr =
         * stream.getAttribute(ReadingAttribute.class); return rdAttr.getReading(); } else { return null; }
         */
    }

}