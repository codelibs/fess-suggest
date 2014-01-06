package jp.sf.fess.suggest.entity;

import jp.sf.fess.suggest.SuggestConstants;
import jp.sf.fess.suggest.TestUtils;
import jp.sf.fess.suggest.util.SuggestUtil;
import junit.framework.TestCase;
import org.apache.solr.common.SolrInputDocument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class SuggestItemTest extends TestCase {
    public void test_equals_True() {
        SuggestItem item1 = new SuggestItem();
        item1.setText("test");
        item1.addReading("テスト");
        item1.addFieldName("content");

        SuggestItem item2 = new SuggestItem();
        item2.setText("test");
        item2.addReading("テスト");
        item2.addFieldName("content");
        item2.setLabels(new ArrayList<String>());

        assertTrue(item1.equals(item2));
    }

    public void test_equals_False() {
        SuggestItem item1 = new SuggestItem();
        item1.setText("test");
        item1.addReading("テスト");
        item1.addFieldName("content");

        SuggestItem item2 = new SuggestItem();
        item2.setText("test2");
        item2.addReading("テスト");
        item2.addFieldName("content");
        assertFalse(item1.equals(item2));

        item2.setText("test");
        item2.addReading("テスト");
        item2.addFieldName("content2");
        assertTrue(item1.equals(item2));
    }

    public void test_toSolrInputDocument() {
        SuggestItem item1 = new SuggestItem();
        item1.setText("test");
        item1.addReading("テスト");
        item1.addReading("テスト2");
        item1.addFieldName("content");
        String date = SuggestUtil.formatDate(new Date());
        item1.setExpires(date);
        item1.setExpiresField(TestUtils.getSuggestUpdateConfig().getExpiresField());
        item1.setCount(10);
        List<String> labels = Arrays.asList(new String[]{"label1", "label2"});
        item1.setLabels(labels);

        SolrInputDocument doc = item1.toSolrInputDocument();
        assertEquals("test", doc.getFieldValue(SuggestConstants.SuggestFieldNames.TEXT));
        assertEquals("テスト", doc.getFieldValue(SuggestConstants.SuggestFieldNames.READING));
        assertEquals("content", doc.getFieldValue(SuggestConstants.SuggestFieldNames.FIELD_NAME));
        assertEquals(date, doc.getFieldValue(TestUtils.getSuggestUpdateConfig().getExpiresField())
                .toString());
        assertEquals(10, Integer.parseInt(doc.getFieldValue(SuggestConstants.SuggestFieldNames.COUNT)
                .toString()));
        assertTrue(labels.equals(doc.getFieldValues(SuggestConstants.SuggestFieldNames.LABELS)));
        assertEquals("test", doc.getFieldValue("id"));
    }
}
