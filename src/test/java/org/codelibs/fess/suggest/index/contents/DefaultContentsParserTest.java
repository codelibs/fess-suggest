package org.codelibs.fess.suggest.index.contents;

import java.io.IOException;
import java.util.List;

import org.codelibs.fess.suggest.converter.KatakanaToAlphabetConverter;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.converter.ReadingConverterChain;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLog;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.normalizer.NormalizerChain;

import junit.framework.TestCase;

public class DefaultContentsParserTest extends TestCase {
    DefaultContentsParser defaultContentsParser = new DefaultContentsParser();
    String[] supportedFields = new String[] { "content", "title" };
    String[] tagFieldNames = new String[] { "label", "virtual_host" };
    String roleFieldName = "role";

    public void test_parseQueryLog() throws Exception {

        QueryLog queryLog = new QueryLog("content:検索エンジン", null);
        List<SuggestItem> items =
                defaultContentsParser.parseQueryLog(queryLog, supportedFields, tagFieldNames, roleFieldName,
                        createDefaultReadingConverter(), createDefaultNormalizer());
        SuggestItem item = items.get(0);
        assertEquals("検索エンジン", item.getText());
        assertEquals(SuggestItem.Kind.QUERY, item.getKinds()[0]);
        assertEquals(1, item.getQueryFreq());
    }

    public void test_parseQueryLog2Word() throws Exception {
        QueryLog queryLog = new QueryLog("content:検索エンジン AND content:柿", null);
        List<SuggestItem> items =
                defaultContentsParser.parseQueryLog(queryLog, supportedFields, tagFieldNames, roleFieldName,
                        createDefaultReadingConverter(), createDefaultNormalizer());
        assertEquals("検索エンジン 柿", items.get(0).getText());
    }

    public void test_parseQueryLogAndRole() throws Exception {
        QueryLog queryLog = new QueryLog("content:検索エンジン AND label:tag1", "role:role1");
        List<SuggestItem> items =
                defaultContentsParser.parseQueryLog(queryLog, supportedFields, tagFieldNames, roleFieldName,
                        createDefaultReadingConverter(), createDefaultNormalizer());
        SuggestItem item = items.get(0);
        assertEquals("検索エンジン", item.getText());
        assertEquals("tag1", item.getTags()[0]);
        assertEquals("role1", item.getRoles()[0]);
    }

    protected ReadingConverter createDefaultReadingConverter() throws IOException {
        ReadingConverterChain chain = new ReadingConverterChain();
        //chain.addConverter(new KatakanaConverter());
        chain.addConverter(new KatakanaToAlphabetConverter());
        chain.init();
        return chain;
    }

    protected Normalizer createDefaultNormalizer() {
        //TODO
        return new NormalizerChain();
    }

}
