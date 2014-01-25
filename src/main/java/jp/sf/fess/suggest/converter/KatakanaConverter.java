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

package jp.sf.fess.suggest.converter;


import com.ibm.icu.text.Transliterator;
import jp.sf.fess.suggest.SuggestConstants;
import jp.sf.fess.suggest.exception.FessSuggestException;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.analysis.ja.tokenattributes.ReadingAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.List;

public class KatakanaConverter implements SuggestReadingConverter {
    private final Transliterator transliterator = Transliterator
            .getInstance("Hiragana-Katakana");

    protected volatile UserDictionary userDictionary;

    protected volatile boolean initialized = false;

    protected void init() {
        if (initialized) {
            return;
        }

        synchronized (this) {
            if (initialized) {
                return;
            }

            final String path = System.getProperty(SuggestConstants.USER_DICT_PATH);
            if (path != null) {
                InputStream stream = null;
                try {
                    stream = new FileInputStream(path);
                    String encoding = System.getProperty(SuggestConstants.USER_DICT_ENCODING);
                    if (encoding == null) {
                        encoding = IOUtils.UTF_8;
                    }
                    final CharsetDecoder decoder = Charset.forName(encoding)
                            .newDecoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT);
                    final Reader reader = new InputStreamReader(stream, decoder);
                    userDictionary = new UserDictionary(reader);
                } catch (final Exception e) {
                    throw new FessSuggestException(e);
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (final IOException e) {
                        }
                    }
                }
            }

            initialized = true;
        }
    }

    @Override
    public void start() {
        init();
    }

    @Override
    public List<String> convert(String text) {
        List<String> readingList = new ArrayList<String>();
        try {
            readingList.add(toKatakana(text));
        } catch (Exception e) {

        }
        return readingList;
    }

    protected String toKatakana(String inputStr) throws IOException {
        StringBuilder kanaBuf = new StringBuilder();

        final Reader rd = new StringReader(inputStr);
        TokenStream stream = null;
        try {
            stream = new JapaneseTokenizer(rd, userDictionary,
                    false, JapaneseTokenizer.Mode.NORMAL);
            stream.reset();

            int offset = 0;
            while (stream.incrementToken()) {
                final CharTermAttribute att = stream
                        .getAttribute(CharTermAttribute.class);
                String term = att.toString();
                int pos = inputStr.substring(offset).indexOf(term);
                if (pos > 0) {
                    String tmp = inputStr.substring(offset, offset + pos);
                    kanaBuf.append(transliterator.transliterate(tmp));
                    offset += pos;
                }

                final ReadingAttribute rdAttr = stream
                        .getAttribute(ReadingAttribute.class);
                String reading;
                if (rdAttr.getReading() != null) {
                    reading = rdAttr.getReading();
                } else {
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

}
