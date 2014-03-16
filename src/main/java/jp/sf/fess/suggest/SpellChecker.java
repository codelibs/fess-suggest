package jp.sf.fess.suggest;

import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;
import org.apache.solr.client.solrj.util.ClientUtils;

import java.util.ArrayList;
import java.util.List;

public class SpellChecker {
    private static final String _AND_ = " AND ";

    private static final String _OR_ = " OR ";

    public float fuzzyValue = 0.5F;

    private SuggestReadingConverter converter = null;

    private SuggestNormalizer normalizer = null;

    public void setFuzzyValue(final float fuzzyValue) {
        this.fuzzyValue = fuzzyValue;
    }

    public void setConverter(final SuggestReadingConverter converter) {
        this.converter = converter;
    }

    public void setNormalizer(final SuggestNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public String buildSpellCheckQuery(final String query,
                                       final List<String> targetFields, final List<String> labels,
                                       final List<String> roles) {
        return buildSpellCheckQuery(query, fuzzyValue, targetFields, labels, roles);
    }

    public String buildSpellCheckQuery(final String query, final float fuzzy,
                                       final List<String> targetFields, final List<String> labels,
                                       final List<String> roles) {
        StringBuilder queryBuf = new StringBuilder();

        String q = query;
        if (normalizer != null) {
            q = normalizer.normalize(q);
        }
        q = ClientUtils.escapeQueryChars(q.trim());

        List<String> readingList = new ArrayList<String>();
        if (converter != null) {
            readingList = converter.convert(q);
        } else {
            readingList.add(q);
        }

        if(readingList.isEmpty()) {
            queryBuf.append(SuggestConstants.SuggestFieldNames.READING)
                    .append(':')
                    .append(q);
        } else {
            queryBuf.append(SuggestConstants.SuggestFieldNames.READING)
                .append(':')
                .append(readingList.get(0));
        }
        queryBuf.append('~')
                .append(fuzzy);

        if (targetFields != null && !targetFields.isEmpty()) {
            queryBuf.append(_AND_);
            if (targetFields.size() >= 2) {
                queryBuf.append("(");
            }
            for (int i = 0; i < targetFields.size(); i++) {
                final String fieldName = targetFields.get(i);
                if (i > 0) {
                    queryBuf.append(_OR_);
                }
                queryBuf.append(SuggestConstants.SuggestFieldNames.FIELD_NAME);
                queryBuf.append(':');
                queryBuf.append(fieldName);
            }
            if (targetFields.size() >= 2) {
                queryBuf.append(')');
            }
        }

        if (labels != null && !labels.isEmpty()) {
            queryBuf.append(_AND_);
            if (labels.size() >= 2) {
                queryBuf.append('(');
            }
            boolean isFirst = true;
            for (final String label : labels) {
                if (!isFirst) {
                    queryBuf.append(_OR_);
                }
                queryBuf.append(SuggestConstants.SuggestFieldNames.LABELS);
                queryBuf.append(':');
                queryBuf.append(label);
                isFirst = false;
            }
            if (labels.size() >= 2) {
                queryBuf.append(')');
            }
        }

        if (roles != null && !roles.isEmpty()) {
            queryBuf.append(_AND_);
            if (roles.size() >= 2) {
                queryBuf.append('(');
            }
            boolean isFirst = true;
            for (final String role : roles) {
                if (!isFirst) {
                    queryBuf.append(_OR_);
                }
                queryBuf.append(SuggestConstants.SuggestFieldNames.ROLES);
                queryBuf.append(':');
                queryBuf.append(role);
                isFirst = false;
            }
            if (roles.size() >= 2) {
                queryBuf.append(')');
            }
        }

        return queryBuf.toString();
    }

}
