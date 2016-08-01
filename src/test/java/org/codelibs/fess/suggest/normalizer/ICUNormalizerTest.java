package org.codelibs.fess.suggest.normalizer;

import junit.framework.TestCase;

public class ICUNormalizerTest extends TestCase {
    public void test_FullwidthHalfwidth() {
        ICUNormalizer normalizer = new ICUNormalizer("Fullwidth-Halfwidth");

        assertEquals("abcd", normalizer.normalize("ａｂｃｄ", null));
        assertEquals("みかん", normalizer.normalize("みかん", null));
        assertEquals("みかん ﾘﾝｺﾞ", normalizer.normalize("みかん　リンゴ", null));
    }

    public void test_AnyLower() {
        ICUNormalizer normalizer = new ICUNormalizer("Any-Lower");
        assertEquals("abcd", normalizer.normalize("ABCD", null));
    }

    public void test_HalfwidthFullwidth() {
        ICUNormalizer normalizer = new ICUNormalizer("Halfwidth-Fullwidth");
        assertEquals("ケンサク", normalizer.normalize("ｹﾝｻｸ", null));
    }

}