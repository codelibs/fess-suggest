package jp.sf.fess.suggest.solr;


import jp.sf.fess.suggest.SuggestConstants;
import jp.sf.fess.suggest.Suggester;
import jp.sf.fess.suggest.TestUtils;
import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.entity.SuggestFieldInfo;
import jp.sf.fess.suggest.index.SuggestSolrServer;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;
import jp.sf.fess.suggest.util.SuggestUtil;
import jp.sf.fess.suggest.util.TransactionLogUtil;
import junit.framework.TestCase;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.update.TransactionLog;

import java.io.File;
import java.util.*;

public class SuggestUpdateControllerTest extends TestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();
        TestUtils.startJerrySolrRunner();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        TestUtils.stopJettySolrRunner();
    }

    public void test_updateAndSuggest() {
        try {
            SuggestSolrServer suggestSolrServer = TestUtils.createSuggestSolrServer();
            suggestSolrServer.deleteAll();
            SuggestUpdateConfig config = TestUtils.getSuggestUpdateConfig();
            SuggestUpdateController controller = new SuggestUpdateController(config,
                    getSuggestFieldInfoList(config, false));
            controller.start();

            SolrInputDocument doc = new SolrInputDocument();
            doc.setField("content", "Fess は「5 分で簡単に構築可能な全文検索サーバー」です。Java 実行環境があればどの OS でも実行可能です。Fess は Apache ライセンスで提供され、無料 (フリーソフト) でご利用いただけます。\n" +
                    "\n" +
                    "Seasar2 ベースで構築され、検索エンジン部分には 2 億ドキュメントもインデックス可能と言われる Solr を利用しています。 ドキュメントクロールには S2Robot を利用することで、Web やファイルシステムに対するクロールが可能になり、MS Office 系のドキュメントや zip などの圧縮ファイルも検索対象とすることができます。");
            doc.setField(config.getExpiresField(), SuggestUtil.formatDate(new Date()));
            controller.add(doc);
            controller.commit();
            Thread.sleep(5 * 1000);

            //assert
            assertTrue(suggestSolrServer.select("*:*").getNumFound() > 10);
            assertTrue(suggestSolrServer.select(SuggestConstants.SuggestFieldNames.READING + ":jav*")
                    .getNumFound() > 0);
            assertTrue(suggestSolrServer.select(SuggestConstants.SuggestFieldNames.READING + ":kensakuenjinn*")
                    .getNumFound() > 0);
            assertTrue(suggestSolrServer.select(SuggestConstants.SuggestFieldNames.READING + ":inde*")
                    .getNumFound() > 0);

            //suggest check
            Suggester suggester = new Suggester();
            suggester.setNormalizer(TestUtils.createNormalizer());
            suggester.setConverter(TestUtils.createConverter());

            String q = suggester.buildSuggestQuery("jav", Arrays.asList(new String[]{"content"}), null);
            assertTrue(suggestSolrServer.select(q).getNumFound() > 0);
            q = suggester.buildSuggestQuery("kensakuenj", Arrays.asList(new String[]{"content"}), null);
            assertTrue(suggestSolrServer.select(q).getNumFound() > 0);
            q = suggester.buildSuggestQuery("inde", Arrays.asList(new String[]{"content"}), null);
            assertTrue(suggestSolrServer.select(q).getNumFound() > 0);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void test_update_multifield() {
        SuggestSolrServer suggestSolrServer = TestUtils.createSuggestSolrServer();

        try {
            suggestSolrServer.deleteAll();

            SuggestUpdateConfig config = TestUtils.getSuggestUpdateConfig();
            SuggestUpdateController controller = new SuggestUpdateController(config,
                    getSuggestFieldInfoList(config, true));
            controller.start();
            SolrInputDocument doc = new SolrInputDocument();
            doc.setField("content", "Fess は「5 分で簡単に構築可能な全文検索サーバー」です。Java 実行環境があればどの OS でも実行可能です。Fess は Apache ライセンスで提供され、無料 (フリーソフト) でご利用いただけます。\n" +
                    "\n" +
                    "Seasar2 ベースで構築され、検索エンジン部分には 2 億ドキュメントもインデックス可能と言われる Solr を利用しています。 ドキュメントクロールには S2Robot を利用することで、Web やファイルシステムに対するクロールが可能になり、MS Office 系のドキュメントや zip などの圧縮ファイルも検索対象とすることができます。");
            doc.setField("title", "Fessについての説明　page");
            doc.setField(config.getExpiresField(), SuggestUtil.formatDate(new Date()));
            controller.add(doc);
            controller.commit();
            Thread.sleep(5 * 1000);

            //assert
            assertTrue(suggestSolrServer.select("*:*").getNumFound() > 10);
            assertEquals(1, suggestSolrServer.select(SuggestConstants.SuggestFieldNames.READING + ":jav*")
                    .getNumFound());
            assertTrue(suggestSolrServer.select(SuggestConstants.SuggestFieldNames.READING + ":kensakuenjinn*")
                    .getNumFound() > 0);
            assertTrue(suggestSolrServer.select(SuggestConstants.SuggestFieldNames.READING + ":inde*")
                    .getNumFound() > 0);
            assertTrue(suggestSolrServer.select(SuggestConstants.SuggestFieldNames.READING + ":fessnituitenose*")
                    .getNumFound() > 0);

            //suggester check
            Suggester suggester = new Suggester();
            suggester.setNormalizer(TestUtils.createNormalizer());
            suggester.setConverter(TestUtils.createConverter());

            String q = suggester.buildSuggestQuery("fessnituitenosetumei",Arrays.asList(new String[]{"title"}), null);
            assertEquals(1, suggestSolrServer.select(q).getNumFound());
            q = suggester.buildSuggestQuery("fessnituitenosetumei",Arrays.asList(new String[]{"content"}), null);
            assertEquals(0, suggestSolrServer.select(q).getNumFound());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void test_ttl_delete() {
        SuggestSolrServer suggestSolrServer = TestUtils.createSuggestSolrServer();

        try {
            suggestSolrServer.deleteAll();

            SuggestUpdateConfig config = TestUtils.getSuggestUpdateConfig();
            SuggestUpdateController controller = new SuggestUpdateController(config,
                    getSuggestFieldInfoList(config, false));
            controller.start();

            String prevDate = SuggestUtil.formatDate(new Date());

            SolrInputDocument doc = new SolrInputDocument();
            doc.setField("content", "りんご");
            doc.setField(config.getExpiresField(), prevDate);
            controller.add(doc);
            controller.commit();
            Thread.sleep(5 * 1000);
            assertTrue(suggestSolrServer.select("*:*").getNumFound() == 1);
            SolrInputDocument doc2 = new SolrInputDocument();
            doc2.setField("content", "みかん");
            doc2.setField(config.getExpiresField(), SuggestUtil.formatDate(new Date()));
            controller.add(doc2);
            controller.commit();
            Thread.sleep(5 * 1000);

            //assert
            assertTrue(suggestSolrServer.select("*:*").getNumFound() == 2);
            assertTrue(suggestSolrServer.select(SuggestConstants.SuggestFieldNames.READING
                    + ":mikan").getNumFound() == 1);
            assertTrue(suggestSolrServer.select(SuggestConstants.SuggestFieldNames.READING
                    + ":rinngo").getNumFound() == 1);


            controller.deleteByQuery(config.getExpiresField()
                    + ":[* TO "
                            + prevDate
                            + "] NOT segment:hogehoge");
            controller.commit();
            Thread.sleep(5 * 1000);

            //assert
            assertTrue(suggestSolrServer.select("*:*").getNumFound() == 1);
            assertTrue(suggestSolrServer.select(SuggestConstants.SuggestFieldNames.READING
                    + ":mikan").getNumFound() == 1);
            assertTrue(suggestSolrServer.select(SuggestConstants.SuggestFieldNames.READING
                    + ":rinngo").getNumFound() == 0);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void test_segment_delete() {
        SuggestSolrServer suggestSolrServer = TestUtils.createSuggestSolrServer();

        try {
            suggestSolrServer.deleteAll();

            SuggestUpdateConfig config = TestUtils.getSuggestUpdateConfig();
            SuggestUpdateController controller = new SuggestUpdateController(config,
                    getSuggestFieldInfoList(config, false));
            controller.start();

            SolrInputDocument doc = new SolrInputDocument();
            doc.setField("content", "りんご");
            doc.setField(config.getSegmentField(), "1");
            controller.add(doc);
            controller.commit();
            Thread.sleep(5 * 1000);
            assertTrue(suggestSolrServer.select("*:*").getNumFound() == 1);
            SolrInputDocument doc2 = new SolrInputDocument();
            doc2.setField("content", "みかん");
            doc2.setField(config.getSegmentField(), "2");
            controller.add(doc2);
            controller.commit();
            Thread.sleep(5 * 1000);

            //assert
            assertTrue(suggestSolrServer.select("*:*").getNumFound() == 2);
            assertTrue(suggestSolrServer.select(SuggestConstants.SuggestFieldNames.READING
                    + ":mikan").getNumFound() == 1);
            assertTrue(suggestSolrServer.select(SuggestConstants.SuggestFieldNames.READING
                    + ":ringo").getNumFound() == 1);


            controller.deleteByQuery(config.getSegmentField() + ":1");
            controller.commit();
            Thread.sleep(5 * 1000);

            //assert
            assertTrue(suggestSolrServer.select("*:*").getNumFound() == 1);
            assertTrue(suggestSolrServer.select(SuggestConstants.SuggestFieldNames.READING
                    + ":mikan").getNumFound() == 1);
            assertTrue(suggestSolrServer.select(SuggestConstants.SuggestFieldNames.READING
                    + ":ringo").getNumFound() == 0);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }


    public void test_incrementCount() {
        SuggestSolrServer suggestSolrServer = TestUtils.createSuggestSolrServer();

        try {
            suggestSolrServer.deleteAll();
            suggestSolrServer.commit();

            SuggestUpdateConfig config = TestUtils.getSuggestUpdateConfig();
            SuggestUpdateController controller = new SuggestUpdateController(config,
                    getSuggestFieldInfoList(config, false));
            controller.addLabelFieldName("label");
            controller.start();

            SolrInputDocument doc = new SolrInputDocument();
            for(int i=0;i<5;i++) {
                doc.setField("content", "みかん");
                doc.setField("label", "label" + i);
                doc.setField(config.getExpiresField(), SuggestUtil.formatDate(new Date()));
                controller.add(doc);
                Thread.sleep(5 * 1000);
            }
            controller.commit();
            Thread.sleep(5 * 1000);
            SolrDocumentList solrDocuments = suggestSolrServer.select("*:*");
            assertEquals(1, solrDocuments.getNumFound());
            SolrDocument solrDocument = solrDocuments.get(0);
            Object count = solrDocument.getFieldValue(SuggestConstants.SuggestFieldNames.COUNT);
            assertEquals("5", count.toString());
            Collection<Object> labels = solrDocument.getFieldValues(SuggestConstants.SuggestFieldNames.LABELS);
            assertEquals(5, labels.size());
            for(int i=0;i<5;i++) {
                assertTrue(labels.contains("label" + i));
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private List<SuggestFieldInfo> getSuggestFieldInfoList(SuggestUpdateConfig config, boolean multi) {
        List<SuggestFieldInfo> list =
                new ArrayList<SuggestFieldInfo>();

        List<String> fieldNameList = new ArrayList<String>();
        fieldNameList.add("content");

        TokenizerFactory tokenizerFactory = TestUtils.getTokenizerFactory(config);
        SuggestReadingConverter suggestReadingConverter = TestUtils.createConverter();
        SuggestNormalizer suggestNormalizer = TestUtils.createNormalizer();

        SuggestFieldInfo suggestFieldInfo =
                new SuggestFieldInfo(fieldNameList, tokenizerFactory, suggestReadingConverter, suggestNormalizer);
        list.add(suggestFieldInfo);

        if(multi) {
            List<String> fieldNameList2 = new ArrayList<String>();
            fieldNameList2.add("title");

            SuggestReadingConverter suggestReadingConverter2 = TestUtils.createConverter();
            SuggestNormalizer suggestNormalizer2 = TestUtils.createNormalizer();

            SuggestFieldInfo suggestFieldInfo2 =
                    new SuggestFieldInfo(fieldNameList2, null, suggestReadingConverter2, suggestNormalizer2);
            list.add(suggestFieldInfo2);
        }

        return list;
    }

    public void test_UpdateFromTransactionLog() {
        SuggestSolrServer suggestSolrServer = TestUtils.createSuggestSolrServer();

        try {
            suggestSolrServer.deleteAll();
            suggestSolrServer.commit();

            SuggestUpdateConfig config = TestUtils.getSuggestUpdateConfig();
            SuggestUpdateController controller = new SuggestUpdateController(config,
                    getSuggestFieldInfoList(config, false));
            controller.addLabelFieldName("label");
            controller.start();

            File classPath = new File(this.getClass().getClassLoader().getResource("").getPath());
            File file = new File(classPath, "tlog.0000000000000000059");
            TransactionLog translog = TransactionLogUtil.createSuggestTransactionLog(file, null, true);
            controller.addTransactionLog(translog);

            Thread.sleep(10 * 1000);
            assertTrue(suggestSolrServer.select("*:*").getNumFound() > 100);
            assertTrue(suggestSolrServer.select("reading_s_m:kensa*").getNumFound() > 0);

            controller.close();
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
