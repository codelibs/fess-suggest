package org.codelibs.fess.suggest.util;

import junit.framework.TestCase;

import java.util.List;
import java.util.Map;

import org.codelibs.fess.suggest.util.SuggestUtil;

public class SuggestUtilTest extends TestCase {
    public void test_parseSolrParams() throws Exception {
        Map<String, String> params = SuggestUtil.parseSolrParams("q=content:%e3%81%82%e3%81%84%e3%81%82%e3%81%84&fq=role:guest");
        assertEquals("content:あいあい", params.get("q"));
        assertEquals("role:guest", params.get("fq"));
    }

    public void test_parseQuery() throws Exception {
        List<String> words = SuggestUtil.parseQuery("content:hoge", "content");
        assertEquals("hoge", words.get(0));

        words = SuggestUtil.parseQuery("content:hoge AND content:fuga", "content");
        assertEquals("hoge", words.get(0));
        assertEquals("fuga", words.get(1));

        words = SuggestUtil.parseQuery("(content:hoge AND content:fuga) OR (content:hoge AND content:zzz)", "content");
        assertEquals("hoge", words.get(0));
        assertEquals("fuga", words.get(1));
        assertEquals("zzz", words.get(2));
    }
}
