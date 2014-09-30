package jp.sf.fess.suggest.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jp.sf.fess.suggest.SuggestConstants;

import org.apache.solr.common.SolrInputDocument;

public class SuggestItem {
    private volatile String text;

    private final List<String> readingList = Collections
        .synchronizedList(new ArrayList<String>());

    private final List<String> fieldNameList = Collections
        .synchronizedList(new ArrayList<String>()); // TODO Set?

    private final List<String> labels = Collections
        .synchronizedList(new ArrayList<String>()); // TODO Set?

    private final List<String> roles = Collections
        .synchronizedList(new ArrayList<String>()); // TODO Set?

    private volatile long count = 1;

    private volatile String expires;

    private volatile String expiresField;

    private volatile String segment;

    private volatile String segmentField;

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public List<String> getReadingList() {
        return readingList;
    }

    public void addReading(final String reading) {
        readingList.add(reading);
    }

    public List<String> getFieldNameList() {
        return fieldNameList;
    }

    public void addFieldName(final String fieldName) {
        fieldNameList.add(fieldName);
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(final List<String> labels) {
        this.labels.clear();
        for (final String label : labels) {
            this.labels.add(label);
        }
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(final List<String> roles) {
        this.roles.clear();
        for (final String label : roles) {
            this.roles.add(label);
        }
    }

    public long getCount() {
        return count;
    }

    public void setCount(final long count) {
        this.count = count;
    }

    public String getExpires() {
        return expires;
    }

    public void setExpires(final String expires) {
        this.expires = expires;
    }

    public String getExpiresField() {
        return expiresField;
    }

    public void setExpiresField(final String expiresField) {
        this.expiresField = expiresField;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(final String segment) {
        this.segment = segment;
    }

    public String getSegmentField() {
        return segmentField;
    }

    public void setSegmentField(final String segmentField) {
        this.segmentField = segmentField;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof SuggestItem) {
            final SuggestItem item = (SuggestItem) o;
            if (getDocumentId().equals(item.getDocumentId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getDocumentId().hashCode();
    }

    public SolrInputDocument toSolrInputDocument() {
        final SolrInputDocument doc = new SolrInputDocument();

        doc.setField(SuggestConstants.SuggestFieldNames.TEXT, text);
        for (final String reading : readingList) {
            doc.addField(SuggestConstants.SuggestFieldNames.READING, reading);
        }
        for (final String fieldName : fieldNameList) {
            doc.addField(SuggestConstants.SuggestFieldNames.FIELD_NAME,
                fieldName);
        }
        doc.setField(SuggestConstants.SuggestFieldNames.COUNT, count);
        doc.setField(expiresField, expires);
        doc.setField(segmentField, segment);
        for (final String label : labels) {
            doc.addField(SuggestConstants.SuggestFieldNames.LABELS, label);
        }
        for (final String role : roles) {
            doc.addField(SuggestConstants.SuggestFieldNames.ROLES, role);
        }
        doc.addField(SuggestConstants.SuggestFieldNames.ID, getDocumentId());

        return doc;
    }

    public String getDocumentId() {
        return text;
    }

    @Override
    public String toString() {
        return "SuggestItem [text=" + text + ", readingList=" + readingList
            + ", fieldNameList=" + fieldNameList + ", labels=" + labels
            + ", roles=" + roles + ", count=" + count + ", expires="
            + expires + ", expiresField=" + expiresField + ", segment="
            + segment + ", segmentField=" + segmentField + "]";
    }
}
