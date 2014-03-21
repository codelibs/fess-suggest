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

package jp.sf.fess.suggest.analysis;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.icu.text.Transliterator;
import jp.sf.fess.suggest.io.AccessibleStringReader;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.analysis.ja.tokenattributes.PartOfSpeechAttribute;
import org.apache.lucene.analysis.ja.tokenattributes.ReadingAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuggestTokenizer extends Tokenizer {
    private static final String NOUN = "名詞";

    private static final String MIDDLE = "middle";

    private static final String START = "start";

    private static final Logger logger = LoggerFactory
            .getLogger(SuggestTokenizer.class);

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    private final SuggestReadingAttribute readingAtt = addAttribute(SuggestReadingAttribute.class);

    private String inputStr = "";

    private int offset = 0;

    private final List<String> termListByKuromoji = new ArrayList<String>();

    private final List<String> readingList = new ArrayList<String>();

    private final List<String> partOfSpeechList = new ArrayList<String>();

    private final List<String> suggestStringList = new ArrayList<String>();

    private final UserDictionary userDictionary;

    private final boolean discardPunctuation;

    private final Mode tokenizerMode;

    private final TermChecker termChecker;

    private final int maxLength;

    private final Transliterator transliterator = Transliterator
            .getInstance("Hiragana-Katakana");

    public SuggestTokenizer(final Reader input, final int bufferSize,
            final UserDictionary userDictionaryPara,
            final boolean discardPunctuationPara, final Mode modePara,
            final TermChecker termChecker, final int maxLength) {
        super(input);

        userDictionary = userDictionaryPara;
        discardPunctuation = discardPunctuationPara;
        tokenizerMode = modePara;
        termAtt.resizeBuffer(bufferSize);
        this.termChecker = termChecker;
        this.maxLength = maxLength;
    }

    public void initialize() {
        termListByKuromoji.clear();
        partOfSpeechList.clear();
        readingList.clear();
        suggestStringList.clear();
        offset = 0;

        try {
            String s;
            if (input instanceof AccessibleStringReader) {
                s = ((AccessibleStringReader) input).getString();
            } else {
                s = IOUtils.toString(input);
            }
            if (s != null && s.length() > 0) {
                if (maxLength > 0 && s.length() > maxLength) {
                    s = truncateInput(s);
                }
                inputStr = s;
            } else {
                inputStr = "";
            }
        } catch (final IOException e) {
            inputStr = "";
        }

        final Reader rd = new StringReader(inputStr);

        TokenStream stream = null;

        try {
            stream = new JapaneseTokenizer(rd, userDictionary,
                    discardPunctuation, tokenizerMode); // TODO reuse?

            stream.reset();
            while (stream.incrementToken()) {
                final CharTermAttribute att = stream
                        .getAttribute(CharTermAttribute.class);
                termListByKuromoji.add(att.toString());

                final PartOfSpeechAttribute psAtt = stream
                        .getAttribute(PartOfSpeechAttribute.class);
                final String pos = psAtt.getPartOfSpeech();
                partOfSpeechList.add(pos);

                final ReadingAttribute rdAttr = stream
                        .getAttribute(ReadingAttribute.class);

                String reading;
                if (rdAttr.getReading() != null) {
                    reading = rdAttr.getReading();
                } else {
                    reading = transliterator.transliterate(att.toString());
                }

                readingList.add(reading);

            }
        } catch (final Exception e) {
            logger.warn("JapaneseTokenizer stream error", e);
        } finally {
            try {
                input.reset();
            } catch (final Exception e) {
            }
            try {
                stream.end();
            } catch (final Exception e) {
            }
            try {
                rd.close();
            } catch (final Exception e) {
            }
        }
    }

    private String truncateInput(final String s) {
        int pos = maxLength;
        while (pos > 0) {
            final int ch = s.codePointAt(pos);
            if (!Character.isLetterOrDigit(ch)) {
                break;
            }
            pos--;
        }
        if (pos == 0) {
            pos = maxLength;
        }

        return s.substring(0, pos);
    }

    @Override
    public final boolean incrementToken() throws IOException {
        termAtt.setEmpty();
        readingAtt.setEmpty();

        final int termListByKuromojiSize = termListByKuromoji.size();

        if (offset < termListByKuromojiSize) {
            termAtt.append(termListByKuromoji.get(offset));
            readingAtt.append(readingList.get(offset));
            offset++;
        } else {
            int tmpOffset = offset - termListByKuromojiSize;
            if (tmpOffset < termListByKuromojiSize) {
                final StringBuilder buffer = new StringBuilder(100);
                final StringBuilder readingBuf = new StringBuilder(100);
                int end = 1;

                final int partOfSpeechListSize = partOfSpeechList.size();
                for (; tmpOffset < partOfSpeechListSize; tmpOffset++) {
                    buffer.setLength(0);
                    readingBuf.setLength(0);
                    final String termByKuromoji = termListByKuromoji
                            .get(tmpOffset);
                    if (termChecker.check(partOfSpeechList.get(tmpOffset),
                            termByKuromoji, START)) {
                        buffer.append(termByKuromoji);
                        readingBuf.append(readingList.get(tmpOffset));

                        for (int i = 1; tmpOffset + i < partOfSpeechListSize; i++) {
                            final String termPairByKuromoji = termListByKuromoji
                                    .get(tmpOffset + i);
                            if (termChecker.check(
                                    partOfSpeechList.get(tmpOffset + i),
                                    termPairByKuromoji, MIDDLE)) {
                                // TODO remove string conjunction
                                if (inputStr.indexOf(buffer.toString()
                                        + termPairByKuromoji) != -1) {
                                    buffer.append(termPairByKuromoji);
                                    readingBuf.append(readingList.get(tmpOffset
                                            + i));
                                    end++;
                                } else {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                        if (end > 1) {
                            break;
                        }
                    }
                }

                if (buffer != null
                        && tmpOffset < partOfSpeechListSize
                        && buffer.length() > termListByKuromoji.get(tmpOffset)
                                .length()) {
                    termAtt.append(buffer.toString());
                    readingAtt.append(readingBuf.toString());
                } else {
                    return false;
                }
                offset = tmpOffset + termListByKuromojiSize + end;
            } else {
                return false;
            }

        }
        return true;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        initialize();
    }

    public static class TermChecker {
        private static final String INCLUDE_CHAR_TERM = "includeCharTerm";

        private static final String EXCLUDE_PART_OF_SPEECH = "excludePartOfSpeech";

        private static final String INCLUDE_PART_OF_SPEECH = "includePartOfSpeech";

        private final Map<String, Map<String, List<String>>> paramMap = new HashMap<String, Map<String, List<String>>>(
                2);

        public TermChecker() {
            final Map<String, List<String>> startParamMap = new HashMap<String, List<String>>(
                    3);
            startParamMap.put(INCLUDE_PART_OF_SPEECH, new ArrayList<String>());
            startParamMap.put(EXCLUDE_PART_OF_SPEECH, new ArrayList<String>());
            startParamMap.put(INCLUDE_CHAR_TERM, new ArrayList<String>());
            paramMap.put(START, startParamMap);
            final Map<String, List<String>> middleParamMap = new HashMap<String, List<String>>(
                    3);
            middleParamMap.put(INCLUDE_PART_OF_SPEECH, new ArrayList<String>());
            middleParamMap.put(EXCLUDE_PART_OF_SPEECH, new ArrayList<String>());
            middleParamMap.put(INCLUDE_CHAR_TERM, new ArrayList<String>());
            paramMap.put(MIDDLE, middleParamMap);
        }

        public void includePartOfSpeech(final String mode, final String value) {
            updateParam(mode, INCLUDE_PART_OF_SPEECH, value);
        }

        public void excludePartOfSpeech(final String mode, final String value) {
            updateParam(mode, EXCLUDE_PART_OF_SPEECH, value);
        }

        public void includeCharTerm(final String mode, final String value) {
            updateParam(mode, INCLUDE_CHAR_TERM, value);
        }

        private void updateParam(final String mode, final String target,
                final String value) {
            final Map<String, List<String>> modeParamMap = paramMap.get(mode);
            if (modeParamMap != null) {
                final List<String> list = modeParamMap.get(target);
                if (list != null) {
                    list.add(value);
                }
            }
        }

        public boolean check(final String partOfSpeech,
                final String termByKuromoji, final String mode) {
            final Map<String, List<String>> modeParamMap = paramMap.get(mode);
            final List<String> includePartOfSpeechList = modeParamMap
                    .get(INCLUDE_PART_OF_SPEECH);
            final List<String> excludePartOfSpeechList = modeParamMap
                    .get(EXCLUDE_PART_OF_SPEECH);
            final List<String> includeCharTermList = modeParamMap
                    .get(INCLUDE_CHAR_TERM);

            boolean ret = false;
            for (int i = 0; i < includePartOfSpeechList.size(); i++) {
                if (partOfSpeech.indexOf(includePartOfSpeechList.get(i)) != -1) {
                    boolean isNg = false;
                    for (int j = 0; j < excludePartOfSpeechList.size(); j++) {
                        if (partOfSpeech
                                .indexOf(excludePartOfSpeechList.get(j)) != -1) {
                            isNg = true;
                        }
                    }
                    if (!isNg) {
                        ret = true;
                        break;
                    }
                }
            }

            if (!ret) {
                for (int i = 0; i < includeCharTermList.size(); i++) {
                    if (termByKuromoji.equals(includeCharTermList.get(i))) {
                        ret = true;
                        break;
                    }
                }
            }
            return ret;
        }

        @Override
        public String toString() {
            return "TermChecker [paramMap=" + paramMap + "]";
        }
    }
}
