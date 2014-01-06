package jp.sf.fess.suggest;


import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class SuggesterTest extends TestCase {
    public void test_buildQuery() {
        Suggester suggester = new Suggester();
        suggester.setNormalizer(new SuggestNormalizer() {
            @Override
            public String normalize(String text) {
                return text.replace("りんご", "リンゴ");
            }

            @Override
            public void start() {
            }
        });

        suggester.setConverter(new SuggestReadingConverter() {
            @Override
            public List<String> convert(String text) {
                List<String> list = new ArrayList<String>();
                list.add(text);
                list.add(text.replace("みかん", "ミカン"));
                return list;
            }

            @Override
            public void start() {
            }
        });

        String field = SuggestConstants.SuggestFieldNames.READING;
        String query = suggester.buildSuggestQuery("りんごとみかん", null, null);
        assertEquals("(" + field + ":リンゴとみかん* OR " + field + ":リンゴとミカン*)", query);
    }
}
