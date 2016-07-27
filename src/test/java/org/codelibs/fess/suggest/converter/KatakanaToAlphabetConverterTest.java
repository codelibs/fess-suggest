package org.codelibs.fess.suggest.converter;

import junit.framework.TestCase;

public class KatakanaToAlphabetConverterTest extends TestCase {
    public void test_convert() {
        KatakanaToAlphabetConverter katakanaToAlphabetConverter = new KatakanaToAlphabetConverter();
        assertTrue(katakanaToAlphabetConverter.convert("ケンサク", null).contains("kennsaku"));
    }
}
