package jp.sf.fess.suggest.index;


import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.entity.SuggestItem;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;
import jp.sf.fess.suggest.util.SuggestUtil;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class DocumentReader {
    private List<String> targetFields;

    private int fieldPos = 0;

    private final List<String> targetLabelFields;

    private final SolrInputDocument solrInputDocument;

    private Tokenizer tokenizer;

    private final TokenizerFactory tokenizerFactory;

    private final SuggestReadingConverter suggestReadingConverter;

    private final SuggestNormalizer suggestNormalizer;

    private final String expiresField;

    private final String segmentField;

    private final String expire;

    private final String segment;

    private boolean hasNext = true;

    public DocumentReader(TokenizerFactory tokenizerFactory, SuggestReadingConverter suggestReadingConverter,
                          SuggestNormalizer suggestNormalizer, SolrInputDocument solrInputDocument,
                          List<String> targetFields, List<String> targetLabelFields, String expiresFidld,
                          String segmentField) {
        this.solrInputDocument = solrInputDocument;
        this.targetFields = targetFields;
        this.targetLabelFields = targetLabelFields;
        this.tokenizerFactory = tokenizerFactory;
        this.expiresField = expiresFidld;
        this.segmentField = segmentField;
        this.suggestReadingConverter = suggestReadingConverter;
        this.suggestNormalizer = suggestNormalizer;

        Object expireObj = solrInputDocument.getFieldValue(expiresField);
        if (expireObj != null) {
            expire = expireObj.toString();
        } else {
            expire = SuggestUtil.formatDate(new Date());
        }

        Object segmentObj = solrInputDocument.getFieldValue(segmentField);
        if (segmentObj != null) {
            segment = segmentObj.toString();
        } else {
            segment = "";
        }
    }

    public SuggestItem next() throws IOException {
        if (tokenizerFactory == null) {
            String text = getNextFieldString();
            if (text == null) {
                return null;
            }
            SuggestItem item = createSuggestItem(text, targetFields.get(fieldPos));
            fieldPos++;
            return item;
        } else {
            while (hasNext) {
                if (tokenizer != null) {
                    if (tokenizer.incrementToken()) {
                        CharTermAttribute att = tokenizer.getAttribute(CharTermAttribute.class);
                        SuggestItem item = createSuggestItem(att.toString(), targetFields.get(fieldPos));
                        return item;
                    }
                    tokenizer.close();
                    tokenizer = null;
                    fieldPos++;
                }
                tokenizer = createTokenizer();
                if (tokenizer == null) {
                    hasNext = false;
                }
            }
        }
        return null;
    }

    private SuggestItem createSuggestItem(String text, String fieldName) {
        SuggestItem item = new SuggestItem();
        item.setExpiresField(expiresField);
        item.setExpires(expire);
        item.setSegmentField(segmentField);
        item.setSegment(segment);
        List<String> labels = item.getLabels();
        for (String label : targetLabelFields) {
            SolrInputField field = solrInputDocument.getField(label);
            if (field == null) {
                continue;
            }
            Collection<Object> valList = field.getValues();
            if (valList == null || valList.size() == 0) {
                continue;
            }

            for (Object val : valList) {
                labels.add(val.toString());
            }
            break;
        }

        item.addFieldName(fieldName);
        item.setText(text);
        if (suggestReadingConverter != null) {
            List<String> readingList = suggestReadingConverter.convert(item.getText());
            for (String reading : readingList) {
                item.addReading(reading.toString());
            }
        } else {
            item.addReading(text);
        }

        return item;
    }

    private Tokenizer createTokenizer() throws IOException {
        String nextFieldString = getNextFieldString();
        if (nextFieldString == null) {
            return null;
        }
        final Reader rd = new StringReader(nextFieldString);
        Tokenizer tokenizer = tokenizerFactory.create(rd);
        tokenizer.reset();
        return tokenizer;
    }

    private String getNextFieldString() {
        StringBuilder fieldValue = null;

        for (; fieldPos < targetFields.size(); fieldPos++) {
            String fieldName = targetFields.get(fieldPos);
            SolrInputField field = solrInputDocument.getField(fieldName);
            if (field == null) {
                continue;
            }
            Collection<Object> valList = field.getValues();
            if (valList == null || valList.size() == 0) {
                continue;
            }

            fieldValue = new StringBuilder();
            for (Object val : valList) {
                fieldValue.append(val.toString());
                fieldValue.append(' ');
            }
            break;
        }

        if (fieldValue == null) {
            return null;
        }

        String nextFieldString = fieldValue.toString();
        if (suggestNormalizer != null) {
            nextFieldString = suggestNormalizer.normalize(nextFieldString);
        }

        return nextFieldString;
    }
}
