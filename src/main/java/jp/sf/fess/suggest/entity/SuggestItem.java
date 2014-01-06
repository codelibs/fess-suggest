package jp.sf.fess.suggest.entity;

import jp.sf.fess.suggest.SuggestConstants;
import org.apache.commons.codec.binary.Base64;
import org.apache.solr.common.SolrInputDocument;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SuggestItem {
    private volatile String text;

    private List<String> readingList = Collections.synchronizedList(new ArrayList<String>());

    private List<String> fieldNameList = Collections.synchronizedList(new ArrayList<String>());

    private List<String> labels = Collections.synchronizedList(new ArrayList<String>());

    private volatile long count = 1;

    private volatile String expires;

    private volatile String expiresField;

    private volatile String segment;

    private volatile String segmentField;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getReadingList() {
        return readingList;
    }

    public void addReading(String reading) {
        this.readingList.add(reading);
    }

    public List<String> getFieldNameList() {
        return fieldNameList;
    }

    public void addFieldName(String fieldName) {
        this.fieldNameList.add(fieldName);
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
        this.expires = expires;
    }

    public String getExpiresField() {
        return expiresField;
    }

    public void setExpiresField(String expiresField) {
        this.expiresField = expiresField;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public String getSegmentField() {
        return segmentField;
    }

    public void setSegmentField(String segmentField) {
        this.segmentField = segmentField;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SuggestItem) {
            SuggestItem item = (SuggestItem) o;
            if (this.getDocumentId().equals(item.getDocumentId())) {
                return true;
            }
        }
        return false;
    }

    public SolrInputDocument toSolrInputDocument() {
        SolrInputDocument doc = new SolrInputDocument();

        doc.setField(SuggestConstants.SuggestFieldNames.TEXT, text);
        for (String reading : readingList) {
            doc.addField(SuggestConstants.SuggestFieldNames.READING, reading);
        }
        for (String fieldName : fieldNameList) {
            doc.addField(SuggestConstants.SuggestFieldNames.FIELD_NAME, fieldName);
        }
        doc.setField(SuggestConstants.SuggestFieldNames.COUNT, count);
        doc.setField(expiresField, expires);
        doc.setField(segmentField, segment);
        for (String label : labels) {
            doc.addField(SuggestConstants.SuggestFieldNames.LABELS, label);
        }
        doc.addField("id", getDocumentId());

        return doc;
    }

    public String getDocumentId() {
        return text;
    }
}
