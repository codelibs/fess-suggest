package org.codelibs.fess.suggest.request.suggest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SuggestRequestTest {
    @Test
    public void test_isHiraganaQuery() throws Exception {
        SuggestRequest request = new SuggestRequest();
        assertTrue(request.isHiraganaQuery("あ"));
        assertTrue(request.isHiraganaQuery("あおぞら"));
        assertTrue(request.isHiraganaQuery("けんさく"));
        assertTrue(request.isHiraganaQuery("あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよをわん"));
        assertTrue(request.isHiraganaQuery("がぎぐげござじずぜぞだぢづでどばびぶべぼ"));
        assertTrue(request.isHiraganaQuery("ぁぃぅぇぉっゃゅょ"));

        assertFalse(request.isHiraganaQuery("こ犬"));
        assertFalse(request.isHiraganaQuery("abc"));
        assertFalse(request.isHiraganaQuery("カキク"));
        assertFalse(request.isHiraganaQuery("あカ"));
        assertFalse(request.isHiraganaQuery("アか"));
        assertFalse(request.isHiraganaQuery("abcあ"));
    }
}
