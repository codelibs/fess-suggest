package jp.sf.fess.suggest.analysis;

import junit.framework.TestCase;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

public class SuggestTokenizerTest extends TestCase {
    public void test_GenerateToken() {
        final Reader rd = new StringReader("検索エンジン");
        SuggestTokenizer tokenizer = createToenizer(rd);
        try {
            tokenizer.reset();
            int count = 0;
            while(tokenizer.incrementToken()) {
                CharTermAttribute att = tokenizer.getAttribute(CharTermAttribute.class);
                SuggestReadingAttribute reading = tokenizer.getAttribute(SuggestReadingAttribute.class);
                switch (count) {
                    case 0:
                        assertEquals("検索", att.toString());
                        assertEquals("ケンサク", reading.toString());
                        break;
                    case 1:
                        assertEquals("エンジン", att.toString());
                        assertEquals("エンジン", reading.toString());
                        break;
                    case 2:
                        assertEquals("検索エンジン", att.toString());
                        assertEquals("ケンサクエンジン", reading.toString());
                        break;
                }
                count++;
            }
            assertEquals(3, count);
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    private SuggestTokenizer createToenizer(Reader rd) {
        //TODO Factory使う
        SuggestTokenizer.TermChecker termChecker = new SuggestTokenizer.TermChecker();
        // ex. start:名詞,middle:動詞
        final String includePartOfSpeech = "start:名詞,middle:名詞,動詞";
        if (includePartOfSpeech != null) {
            for (String text : includePartOfSpeech.split(",")) {
                text = text.trim();
                if (text.length() > 0) {
                    final String[] values = text.split(":");
                    if (values.length == 2) {
                        termChecker.includePartOfSpeech(values[0].trim(),
                                values[1].trim());
                    }
                }
            }
        }

        return new SuggestTokenizer(rd, 256, null, true,
                JapaneseTokenizer.Mode.NORMAL, termChecker, 1000);
    }
}
