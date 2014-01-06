package jp.sf.fess.suggest;


import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class Suggester {
    private final static String _AND_ = " AND ";

    private final static String _OR_ = " OR ";

    SuggestReadingConverter converter = null;

    SuggestNormalizer normalizer = null;

    public void setConverter(SuggestReadingConverter converter) {
        this.converter = converter;
    }

    public void setNormalizer(SuggestNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public String buildSuggestQuery(String query, List<String> targetFields, List<String> labels) {
        String q = buildQuery(query);
        if (StringUtils.isBlank(q)) {
            return "";
        }

        StringBuilder queryBuf = new StringBuilder(q);
        if (targetFields != null && targetFields.size() > 0) {
            queryBuf.append(_AND_);
            if (targetFields.size() >= 2) {
                queryBuf.append("(");
            }
            for (int i = 0; i < targetFields.size(); i++) {
                String fieldName = targetFields.get(i);
                if (i > 0) {
                    queryBuf.append(_OR_);
                }
                queryBuf.append(SuggestConstants.SuggestFieldNames.FIELD_NAME + ":" + fieldName);
            }
            if (targetFields.size() >= 2) {
                queryBuf.append(")");
            }
        }

        if(labels != null && labels.size() > 0) {
            queryBuf.append(_AND_);
            if(labels.size() >= 2) {
                queryBuf.append('(');
            }
            boolean isFirst = true;
            for(String label: labels) {
                if(!isFirst) {
                    queryBuf.append(_OR_);
                }
                queryBuf.append(SuggestConstants.SuggestFieldNames.LABELS + ":" + label);
                isFirst = false;
            }
            if(labels.size() >= 2) {
                queryBuf.append(')');
            }
        }

        return queryBuf.toString();
    }

    protected String buildQuery(String query) {
        String q = query;
        if (normalizer != null) {
            q = normalizer.normalize(q);
        }

        List<String> readingList = new ArrayList<String>();
        if (converter != null) {
            readingList = converter.convert(q);
        } else {
            readingList.add(q);
        }

        if (readingList.size() == 0) {
            return SuggestConstants.SuggestFieldNames.READING + ":" + query + "*";
        }

        StringBuilder queryBuf = new StringBuilder();
        if (readingList.size() >= 2) {
            queryBuf.append('(');
        }
        for (String reading : readingList) {
            if (queryBuf.length() > 1) {
                queryBuf.append(_OR_);
            }
            queryBuf.append(SuggestConstants.SuggestFieldNames.READING + ":" + reading + "*");
        }
        if (readingList.size() >= 2) {
            queryBuf.append(')');
        }

        return queryBuf.toString();
    }
}
