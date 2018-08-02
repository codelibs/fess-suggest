package org.codelibs.fess.suggest.converter;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.elasticsearch.common.Strings;

import com.ibm.icu.text.Transliterator;

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
        /* TODO
        if (initialized) {
            return;
        }

        if (tokenizerFactory == null) {
            final String path = System.getProperty(SuggestConstants.USER_DICT_PATH);
            final String encoding = System.getProperty(SuggestConstants.USER_DICT_ENCODING);
            final Map<String, String> args = new HashMap<>();
            args.put("mode", "normal");
            args.put("discardPunctuation", "false");
            if (Strings.isNullOrEmpty(path)) {
                args.put("userDictionary", path);
            }
            if (Strings.isNullOrEmpty(encoding)) {
                args.put("userDictionaryEncoding", encoding);
            }
            final JapaneseTokenizerFactory japaneseTokenizerFactory = new JapaneseTokenizerFactory(args);
            // TODO japaneseTokenizerFactory.inform(new FilesystemResourceLoader());
            tokenizerFactory = japaneseTokenizerFactory;
        }
        initialized = true;
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
                final int pos = inputStr.substring(offset).indexOf(term);
                if (pos > 0) {
                    final String tmp = inputStr.substring(offset, offset + pos);
                    kanaBuf.append(transliterator.transliterate(tmp));
                    offset += pos;
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
        //TODO return factory instanceof JapaneseTokenizerFactory;
        return false;
    }

    private TokenStream createTokenStream(final Reader rd) {
        return null;
        /* TODO
        if (tokenizerFactory instanceof JapaneseTokenizerFactory) {
            return tokenizerFactory.create();
        } else {
            return null;
        }
        */
    }

    protected String getReadingFromAttribute(final TokenStream stream) {
        return null;
        /*
        if (tokenizerFactory instanceof JapaneseTokenizerFactory) {
            final ReadingAttribute rdAttr = stream.getAttribute(ReadingAttribute.class);
            return rdAttr.getReading();
        } else {
            return null;
        }
        */
    }

}