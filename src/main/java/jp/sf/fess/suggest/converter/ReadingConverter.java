/*
 * Copyright 2009-2013 the Fess Project and the Others.
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.LinkedHashMap;
import java.util.Map;

import jp.sf.fess.suggest.FessSuggestException;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.analysis.ja.tokenattributes.ReadingAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.text.Transliterator;

public class ReadingConverter implements SuggestConverter {
    private static final Logger logger = LoggerFactory
            .getLogger(ReadingConverter.class);

    private static final String USER_DICT_ENCODING = "fess.user.dict.encoding";

    private static final String USER_DICT_PATH = "fess.user.dict.path";

    protected volatile UserDictionary userDictionary;

    protected volatile boolean initialized = false;

    private final Transliterator transliterator = Transliterator
            .getInstance("Hiragana-Katakana");

    protected void init() {
        if (initialized) {
            return;
        }

        synchronized (this) {
            if (initialized) {
                return;
            }

            final String path = System.getProperty(USER_DICT_PATH);
            if (path != null) {
                InputStream stream = null;
                try {
                    stream = new FileInputStream(path);
                    String encoding = System.getProperty(USER_DICT_ENCODING);
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
    public String convert(final String query) {
        init();

        final Map<String, String> termMap = new LinkedHashMap<String, String>();

        TokenStream stream = null;
        try {
            stream = new JapaneseTokenizer(new StringReader(query),
                    userDictionary, true, JapaneseTokenizer.Mode.NORMAL);

            stream.reset();
            while (stream.incrementToken()) {
                final CharTermAttribute att = stream
                        .getAttribute(CharTermAttribute.class);
                final String term = att.toString();

                final ReadingAttribute rdAttr = stream
                        .getAttribute(ReadingAttribute.class);
                final String reading = rdAttr.getReading();

                if (StringUtils.isNotBlank(reading)) {
                    termMap.put(term, reading);
                } else {
                    termMap.put(term, transliterator.transliterate(term));
                }
            }
        } catch (final Exception e) {
            logger.warn("JapaneseTokenizer stream error", e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (final Exception e) {
                }
            }
        }

        final StringBuilder buf = new StringBuilder();

        int pos = 0;
        for (final Map.Entry<String, String> entry : termMap.entrySet()) {
            final String term = entry.getKey();
            final String reading = entry.getValue();
            final int index = query.indexOf(term, pos);
            if (index - pos > 0) {
                buf.append(transliterator.transliterate(query.substring(pos,
                        index)));
            }
            buf.append(reading);
            pos = index + term.length();
        }
        if (pos < query.length()) {
            buf.append(query.substring(pos));
        }

        return buf.toString();
    }

}
