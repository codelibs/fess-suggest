package jp.sf.fess.suggest.normalizer;


import jp.sf.fess.suggest.util.SuggestUtil;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class SuggestIntegrateNormalizerTest extends TestCase {
    public void test_normalize() {
        try {
            SuggestIntegrateNormalizer normalizer = new SuggestIntegrateNormalizer();
            Map<String, String> map = new HashMap<String, String>();

            map.put("transliteratorId", "Fullwidth-Halfwidth");
            SuggestNormalizer icuNormalizer =
                    SuggestUtil.createNormalizer("jp.sf.fess.suggest.normalizer.ICUNormalizer", map);
            normalizer.addNormalizer(icuNormalizer);

            map = new HashMap<String, String>();
            map.put("transliteratorId", "Any-Lower");
            icuNormalizer = SuggestUtil.createNormalizer("jp.sf.fess.suggest.normalizer.ICUNormalizer",
                    map);
            normalizer.addNormalizer(icuNormalizer);
            normalizer.start();

            assertEquals("abcd", normalizer.normalize("ABCD"));
            assertEquals("abcd aaa", normalizer.normalize("abcd　ＡＡＡ"));
        } catch(Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
