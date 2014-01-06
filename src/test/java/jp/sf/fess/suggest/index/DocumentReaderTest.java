package jp.sf.fess.suggest.index;


import jp.sf.fess.suggest.TestUtils;
import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.entity.SuggestItem;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;
import junit.framework.TestCase;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.solr.common.SolrInputDocument;

import java.util.ArrayList;
import java.util.List;

public class DocumentReaderTest extends TestCase {
    public void test_getStandardDocumentItem() {
        SolrInputDocument doc = new SolrInputDocument();
        doc.setField("content", "検索エンジン");
        doc.setField("content2", "隣の客はよく柿食う客だ");
        List<String> targetFieldList = new ArrayList<String>();
        targetFieldList.add("content");
        targetFieldList.add("content2");
        List<String> labelFieldList = new ArrayList<String>();

        TokenizerFactory tokenizerFactory = TestUtils.getTokenizerFactory(TestUtils.getSuggestUpdateConfig());
        SuggestReadingConverter suggestReadingConverter = TestUtils.createConverter();
        SuggestNormalizer suggestNormalizer = TestUtils.createNormalizer();

        DocumentReader reader = new DocumentReader(tokenizerFactory, suggestReadingConverter,
                suggestNormalizer, doc, targetFieldList, labelFieldList, "", "");
        SuggestItem item;
        try {
            int count = 0;
            while((item = reader.next()) != null) {
                switch(count) {
                    case 0:
                        assertEquals("検索", item.getText());
                        assertTrue(item.getReadingList().contains("kensaku"));
                        assertTrue(item.getReadingList().contains("kennsaku"));
                        assertEquals("content", item.getFieldNameList().get(0));
                        break;
                    case 1:
                        assertEquals("エンジン", item.getText());
                        assertTrue(item.getReadingList().contains("ennjinn"));
                        assertTrue(item.getReadingList().contains("enjinn"));
                        assertEquals("content", item.getFieldNameList().get(0));
                        break;
                    case 2:
                        assertEquals("検索エンジン", item.getText());
                        assertTrue(item.getReadingList().contains("kennsakuennjinn"));
                        assertEquals("content", item.getFieldNameList().get(0));
                        break;
                    case 3:
                        assertEquals("隣", item.getText());
                        assertTrue(item.getReadingList().contains("tonari"));
                        assertEquals("content2", item.getFieldNameList().get(0));
                        break;
                    case 4:
                        assertEquals("客", item.getText());
                        assertTrue(item.getReadingList().contains("kyaku"));
                        assertEquals("content2", item.getFieldNameList().get(0));
                        break;
                    case 5:
                        assertEquals("柿", item.getText());
                        assertTrue(item.getReadingList().contains("kaki"));
                        assertEquals("content2", item.getFieldNameList().get(0));
                        break;
                    case 6:
                        assertEquals("客", item.getText());
                        assertTrue(item.getReadingList().contains("kyaku"));
                        assertEquals("content2", item.getFieldNameList().get(0));
                        break;
                }
                count++;
            }
            assertEquals(7,count);
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void test_getLabelTest() {
        SolrInputDocument doc = new SolrInputDocument();
        doc.setField("content", "検索エンジン");
        doc.addField("label", "label1");
        doc.addField("label", "label2");
        List<String> targetFieldList = new ArrayList<String>();
        targetFieldList.add("content");
        List<String> labelFieldList = new ArrayList<String>();
        labelFieldList.add("label");

        TokenizerFactory tokenizerFactory = TestUtils.getTokenizerFactory(TestUtils.getSuggestUpdateConfig());
        SuggestReadingConverter suggestReadingConverter = TestUtils.createConverter();
        SuggestNormalizer suggestNormalizer = TestUtils.createNormalizer();

        DocumentReader reader = new DocumentReader(tokenizerFactory, suggestReadingConverter,
                suggestNormalizer, doc, targetFieldList, labelFieldList, "","");
        SuggestItem item;
        try {
            int count = 0;
            while((item = reader.next()) != null) {
                switch(count) {
                    case 0:
                        assertEquals("検索", item.getText());
                        assertTrue(item.getReadingList().contains("kensaku"));
                        assertEquals("content", item.getFieldNameList().get(0));
                        for(String label: item.getLabels()) {
                            assertTrue(label.equals("label1") || label.equals("label2"));
                        }
                        break;
                    case 1:
                        assertEquals("エンジン", item.getText());
                        assertTrue(item.getReadingList().contains("ennjinn"));
                        assertEquals("content", item.getFieldNameList().get(0));
                        for(String label: item.getLabels()) {
                            assertTrue(label.equals("label1") || label.equals("label2"));
                        }
                        break;
                    case 2:
                        assertEquals("検索エンジン", item.getText());
                        assertTrue(item.getReadingList().contains("kennsakuennjinn"));
                        assertEquals("content", item.getFieldNameList().get(0));
                        for(String label: item.getLabels()) {
                            assertTrue(label.equals("label1") || label.equals("label2"));
                        }
                        break;
                }
                count++;
            }
            assertEquals(3,count);
        } catch(Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
