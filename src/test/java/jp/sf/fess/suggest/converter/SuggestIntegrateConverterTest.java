package jp.sf.fess.suggest.converter;


import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class SuggestIntegrateConverterTest extends TestCase {
    public void test_convert() {
        SuggestIntegrateConverter converter = new SuggestIntegrateConverter();
        converter.addConverter(new SuggestReadingConverter() {
            @Override
            public List<String> convert(String text) {
                List<String> list = new ArrayList<String>();
                list.add("abc");
                list.add("123");
                return list;
            }

            @Override
            public void start() {
            }
        });

        converter.addConverter(new SuggestReadingConverter() {
            @Override
            public List<String> convert(String text) {
                List<String> list = new ArrayList<String>();
                list.add("あああ");
                list.add("いいい");
                return list;
            }

            @Override
            public void start() {
            }
        });

        converter.start();

        List<String> list = converter.convert("");
        assertEquals("abc", list.get(0));
        assertEquals("123", list.get(1));
        assertEquals("あああ", list.get(2));
        assertEquals("いいい", list.get(3));
    }
}
