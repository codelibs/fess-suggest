package org.codelibs.fess.suggest.normalizer;

import org.codelibs.fess.suggest.util.SuggestUtil;
import org.junit.Test;

import static org.junit.Assert.*;

public class DefaultNormalizerTest {

    @Test
    public void test_normalize() throws Exception {
        Normalizer normalizer = SuggestUtil.createDefaultNormalizer();
        assertEquals(",.*[]「」abcケンサクabcdけんさく", normalizer.normalize(",.*[]「」ＡBCｹﾝｻｸabcdけんさく"));
    }
}
