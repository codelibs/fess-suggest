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
import org.apache.lucene.analysis.ja.tokenattributes.ReadingAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.FilesystemResourceLoader;
import org.apache.lucene.analysis.util.TokenizerFactory;

import com.ibm.icu.text.Transliterator;
import org.codelibs.fess.suggest.constants.SuggestConstants;

public class KatakanaConverter implements ReadingConverter {

    private final Transliterator transliterator = Transliterator.getInstance("Hiragana-Katakana");

    protected volatile boolean initialized = false;

    protected TokenizerFactory tokenizerFactory = null;

    public KatakanaConverter() {
    }

    public KatakanaConverter(TokenizerFactory tokenizerFactory) {
        if (isEnableTokenizer(tokenizerFactory)) {
            this.tokenizerFactory = tokenizerFactory;
        }
    }

    public void init() throws IOException {
        if (initialized) {
            return;
        }

        if (tokenizerFactory == null) {
            final String path = System.getProperty(SuggestConstants.USER_DICT_PATH);
            String encoding = System.getProperty(SuggestConstants.USER_DICT_ENCODING);
            Map<String, String> args = new HashMap<>();
            args.put("mode", "normal");
            args.put("discardPunctuation", "false");
            if (StringUtils.isNotBlank(path)) {
                args.put("userDictionary", path);
            }
            if (StringUtils.isNotBlank(encoding)) {
                args.put("userDictionaryEncoding", encoding);
            }
            JapaneseTokenizerFactory japaneseTokenizerFactory = new JapaneseTokenizerFactory(args);
            japaneseTokenizerFactory.inform(new FilesystemResourceLoader());
            tokenizerFactory = japaneseTokenizerFactory;
        }
        initialized = true;
    }

    @Override
    public List<String> convert(final String text) throws IOException {
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
        }

        return kanaBuf.toString();
    }

    protected boolean isEnableTokenizer(TokenizerFactory factory) {
        return factory instanceof JapaneseTokenizerFactory;
    }

    private TokenStream createTokenStream(Reader rd) {
        if (tokenizerFactory instanceof JapaneseTokenizerFactory) {
            return tokenizerFactory.create(rd);
        } else {
            return null;
        }
    }

    protected String getReadingFromAttribute(TokenStream stream) {
        if (tokenizerFactory instanceof JapaneseTokenizerFactory) {
            final ReadingAttribute rdAttr = stream.getAttribute(ReadingAttribute.class);
            return rdAttr.getReading();
        } else {
            return null;
        }
    }

}