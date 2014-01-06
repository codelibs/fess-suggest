package jp.sf.fess.suggest.normalizer;


import junit.framework.TestCase;

public class ICUNormalizerTest extends TestCase {
    public void test_FullwidthHalfwidth() {
        ICUNormalizer normalizer = new ICUNormalizer();
        normalizer.transliteratorId = "Fullwidth-Halfwidth";
        normalizer.start();


        assertEquals("abcd", normalizer.normalize("ａｂｃｄ"));
        assertEquals("みかん", normalizer.normalize("みかん"));
        assertEquals("みかん ﾘﾝｺﾞ", normalizer.normalize("みかん　リンゴ"));
    }

    public void test_AnyLower() {
        ICUNormalizer normalizer = new ICUNormalizer();
        normalizer.transliteratorId = "Any-Lower";
        normalizer.start();

        assertEquals("abcd", normalizer.normalize("ABCD"));
    }

}
