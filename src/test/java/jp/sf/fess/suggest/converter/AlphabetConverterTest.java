package jp.sf.fess.suggest.converter;


import junit.framework.TestCase;

public class AlphabetConverterTest extends TestCase {
    public void test_convert() {
        SuggestReadingConverter converter = new AlphabetConverter();
        converter.start();

        assertEquals("hai", converter.convert("はい").get(0));
        assertTrue(converter.convert("てつや").contains("tetuya"));
        assertTrue(converter.convert("てつや").contains("tetsuya"));
        assertTrue(converter.convert("徹夜").contains("tetuya"));
        assertTrue(converter.convert("徹夜").contains("tetsuya"));
        assertTrue(converter.convert("ﾃﾂﾔ").contains("tetuya"));
        assertTrue(converter.convert("ﾃﾂﾔ").contains("tetsuya"));

        assertEquals(2, converter.convert("ファイナンス").size());
        assertTrue(converter.convert("ファイナンス").contains("fainannsu"));
        assertTrue(converter.convert("ファイナンス").contains("fainansu"));

        assertEquals(4, converter.convert("化粧水つける").size());
        assertTrue(converter.convert("化粧水つける").contains("kesyousuitukeru"));
        assertTrue(converter.convert("化粧水つける").contains("keshousuitsukeru"));
        assertTrue(converter.convert("化粧水つける").contains("kesyousuitukeru"));
        assertTrue(converter.convert("化粧水つける").contains("keshousuitsukeru"));
    }
}
