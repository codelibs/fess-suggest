package org.codelibs.fess.suggest.converter;

import junit.framework.TestCase;

public class KatakanaConverterTest extends TestCase {
    public void test_convert() {
        ReadingConverter converter = new KatakanaConverter();
        assertEquals("ケンサク", converter.convert("検索").get(0));
    }
}
