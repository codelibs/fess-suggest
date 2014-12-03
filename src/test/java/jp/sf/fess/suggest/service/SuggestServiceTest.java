package jp.sf.fess.suggest.service;

import jp.sf.fess.suggest.FessSuggestTestCase;
import jp.sf.fess.suggest.SpellChecker;
import jp.sf.fess.suggest.SuggestConstants;
import jp.sf.fess.suggest.Suggester;
import jp.sf.fess.suggest.entity.SuggestResponse;
import jp.sf.fess.suggest.server.SuggestSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrDocumentList;

import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SuggestServiceTest extends FessSuggestTestCase {
    protected SuggestService service;

    SuggestSolrServer suggestSolrServer;

    public void setUp() throws Exception {
        super.setUp();
        startSolr();

        Suggester suggester = new Suggester();
        suggester.setConverter(createConverter());
        suggester.setNormalizer(createNormalizer());

        SpellChecker spellChecker = new SpellChecker();
        spellChecker.setConverter(createConverter());
        spellChecker.setNormalizer(createNormalizer());

        suggestSolrServer = new SuggestSolrServer(new HttpSolrServer(SOLR_URL));
        service = new SuggestService(suggester, spellChecker, suggestSolrServer);
        suggestSolrServer.deleteByQuery("*:*");
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        stopSolr();
    }



    public void test_addSolrParams() throws Exception {
        service.addSolrParams("q=content:hoge");
        service.commit();
        Thread.sleep(2 * 1000);
        SolrDocumentList list = suggestSolrServer.select("*:*");
        assertEquals(1, list.getNumFound());
    }

    public void test_addSolrParamsJapanese() throws Exception {
        String query = "q=content:" + URLEncoder.encode("柿の種", SuggestConstants.UTF_8);

        service.addSolrParams(query);
        service.commit();
        Thread.sleep(2 * 1000);
        SolrDocumentList list = suggestSolrServer.select("*:*");
        assertEquals(1, list.getNumFound());
        SuggestResponse response = service.getSuggestResponse("かき", null, null, null, 20);
        List sList = response.get("かき");
        assertEquals("柿の種", sList.get(0));
    }

    public void test_addMultipleWords() throws Exception {
        service.addSolrParams("q=content:hoge AND content:fuga");
        service.addSolrParams("q=content:hoge AND content:zzz");
        service.addSolrParams("q=content:hoge AND content:zzz");
        service.commit();
        Thread.sleep(2 * 1000);

        assertEquals(2, service.getSearchLogDocumentNum());
        assertEquals(0, service.getContentDocumentNum());

        SuggestResponse response = service.getSuggestResponse("hoge", null, null, null, 20);
        List sList = response.get("hoge");
        assertEquals("hoge zzz", sList.get(0));
        assertEquals("hoge fuga", sList.get(1));

        response = service.getSuggestResponse("hoge f", null, null, null, 20);
        sList = response.get("hoge f");
        assertEquals(1, sList.size());
        assertEquals("hoge fuga", sList.get(0));

    }

    public void test_addElevateWord() throws Exception {
        service.addElevateWord("hoge", null, null, null, 0);
        service.commit();
        Thread.sleep(2 * 1000);

        SuggestResponse response = service.getSuggestResponse("hoge", null, null, null, 20);
        List sList = response.get("hoge");
        assertEquals("hoge", sList.get(0));
    }

    public void test_updateBadWords() throws Exception {
        Set<String> badWords = new HashSet<>();
        badWords.add("hoge");
        service.updateBadWords(badWords);

        service.addSolrParams("q=content:hoge");
        service.addSolrParams("q=content:hoge AND content:zzz");
        service.commit();
        Thread.sleep(2 * 1000);

        SolrDocumentList list = suggestSolrServer.select("*:*");
        assertEquals(0, list.getNumFound());
    }

    public void test_deleteBadWords() throws Exception {
        service.addSolrParams("q=content:hoge");
        service.addSolrParams("q=content:zzz");
        service.addSolrParams("q=content:hoge AND content:zzz");
        service.addSolrParams("q=content:fuga AND content:zzz");
        service.commit();
        Thread.sleep(2 * 1000);

        SolrDocumentList list = suggestSolrServer.select("*:*");
        assertEquals(4, list.getNumFound());

        Set<String> badWords = new HashSet<>();
        badWords.add("hoge");
        service.updateBadWords(badWords);
        service.deleteBadWords();
        service.commit();
        Thread.sleep(2 * 1000);

        list = suggestSolrServer.select("*:*");
        assertEquals(2, list.getNumFound());

        badWords.add("zzz");
        service.updateBadWords(badWords);
        service.deleteBadWords();
        service.commit();
        Thread.sleep(2 * 1000);

        list = suggestSolrServer.select("*:*");
        assertEquals(0, list.getNumFound());

    }

}
