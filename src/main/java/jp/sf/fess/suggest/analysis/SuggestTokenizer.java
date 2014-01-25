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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SuggestTokenizer extends Tokenizer {
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

    public SuggestTokenizer(final Reader input, final int bufferSize,
                            final UserDictionary userDictionaryPara,
                            final boolean discardPunctuationPara, final Mode modePara,
                            final TermChecker termChecker,
                            final int maxLength) {
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
        inputStr = "";

        try {
            String s = IOUtils.toString(input);
            if (s != null && s.length() > 0) {
                if (maxLength > 0 && s.length() > maxLength) {
                    s = truncateInput(s);
                }
                inputStr = s;
            }
        } catch (final IOException e) {
        }

        final Reader rd = new StringReader(inputStr);

        TokenStream stream = null;

        try {
            stream = new JapaneseTokenizer(rd, userDictionary,
                    discardPunctuation, tokenizerMode);

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
                    reading = att.toString();
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

        if (offset < termListByKuromoji.size()) {
            while (partOfSpeechList.get(offset).indexOf("名詞") == -1) {
                offset++;
                if (offset >= termListByKuromoji.size()) {
                    break;
                }
            }
        }

        if (offset < termListByKuromoji.size()) {
            termAtt.append(termListByKuromoji.get(offset));
            readingAtt.append(readingList.get(offset));
            offset++;
        } else {
            int tmpOffset = offset - termListByKuromoji.size();
            if (tmpOffset < termListByKuromoji.size()) {
                StringBuilder buffer = null;
                StringBuilder readingBuf = null;
                int end = 1;

                for (; tmpOffset < partOfSpeechList.size(); tmpOffset++) {
                    buffer = new StringBuilder();
                    readingBuf = new StringBuilder();
                    if (termChecker.check(partOfSpeechList.get(tmpOffset),
                            termListByKuromoji.get(tmpOffset), "start")) {
                        buffer.append(termListByKuromoji.get(tmpOffset));
                        readingBuf.append(readingList.get(tmpOffset));

                        for (int i = 1; tmpOffset + i < partOfSpeechList.size(); i++) {
                            if (termChecker.check(
                                    partOfSpeechList.get(tmpOffset + i),
                                    termListByKuromoji.get(tmpOffset + i),
                                    "middle")) {
                                if (inputStr
                                        .indexOf(buffer.toString()
                                                + termListByKuromoji
                                                .get(tmpOffset + i)) != -1) {
                                    buffer.append(termListByKuromoji
                                            .get(tmpOffset + i));
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
                        && tmpOffset < partOfSpeechList.size()
                        && buffer.length() > termListByKuromoji.get(tmpOffset)
                        .length()) {
                    termAtt.append(buffer.toString());
                    readingAtt.append(readingBuf.toString());
                } else {
                    return false;
                }
                offset = tmpOffset + termListByKuromoji.size() + end;
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
        private final Map<String, Map<String, List<String>>> paramMap = new HashMap<String, Map<String, List<String>>>(
                2);

        public TermChecker() {
            final Map<String, List<String>> startParamMap = new HashMap<String, List<String>>(
                    3);
            startParamMap.put("includePartOfSpeech", new ArrayList<String>());
            startParamMap.put("excludePartOfSpeech", new ArrayList<String>());
            startParamMap.put("includeCharTerm", new ArrayList<String>());
            paramMap.put("start", startParamMap);
            final Map<String, List<String>> middleParamMap = new HashMap<String, List<String>>(
                    3);
            middleParamMap.put("includePartOfSpeech", new ArrayList<String>());
            middleParamMap.put("excludePartOfSpeech", new ArrayList<String>());
            middleParamMap.put("includeCharTerm", new ArrayList<String>());
            paramMap.put("middle", middleParamMap);
        }

        public void includePartOfSpeech(final String mode, final String value) {
            updateParam(mode, "includePartOfSpeech", value);
        }

        public void excludePartOfSpeech(final String mode, final String value) {
            updateParam(mode, "excludePartOfSpeech", value);
        }

        public void includeCharTerm(final String mode, final String value) {
            updateParam(mode, "includeCharTerm", value);
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
                    .get("includePartOfSpeech");
            final List<String> excludePartOfSpeechList = modeParamMap
                    .get("excludePartOfSpeech");
            final List<String> includeCharTermList = modeParamMap
                    .get("includeCharTerm");

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
    }
}
