/*
 * Copyright 2009-2014 the CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.codelibs.fess.suggest;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.codelibs.fess.suggest.converter.SuggestReadingConverter;
import org.codelibs.fess.suggest.normalizer.SuggestNormalizer;

public class Suggester {
    private static final String _AND_ = " AND ";

    private static final String _OR_ = " OR ";

    private SuggestReadingConverter converter = null;

    private SuggestNormalizer normalizer = null;

    public void setConverter(final SuggestReadingConverter converter) {
        this.converter = converter;
    }

    public void setNormalizer(final SuggestNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public SuggestReadingConverter getConverter() {
        return converter;
    }

    public String buildSuggestQuery(final String query, final List<String> targetFields, final List<String> labels, final List<String> roles) {
        final String q = buildQuery(query);
        if (StringUtils.isBlank(q)) {
            return "";
        }

        final StringBuilder queryBuf = new StringBuilder(q.length() + 100);
        queryBuf.append(q);
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

    protected String buildQuery(final String query) {
        final StringBuilder queryBuf = new StringBuilder(100);
        String[] array = query.trim().replace("　", " ").replaceAll(" +", " ").split(" ");
        for (String q : array) {
            if (StringUtils.isBlank(q)) {
                continue;
            }

            if (normalizer != null) {
                q = normalizer.normalize(q);
            }
            q = ClientUtils.escapeQueryChars(q.trim());

            List<String> readingList;
            if (converter != null) {
                readingList = converter.convert(q);
            } else {
                readingList = new ArrayList<>();
                readingList.add(q);
            }

            if (readingList.isEmpty()) {
                continue;
            }

            if (queryBuf.length() > 0) {
                queryBuf.append(_AND_);
            }

            if (readingList.size() >= 2) {
                queryBuf.append('(');
            }
            boolean first = true;
            for (final String reading : readingList) {
                if (!first) {
                    queryBuf.append(_OR_);
                }
                first = false;
                queryBuf.append(SuggestConstants.SuggestFieldNames.READING);
                queryBuf.append(':');
                queryBuf.append(reading);
                queryBuf.append('*');
            }
            if (readingList.size() >= 2) {
                queryBuf.append(')');
            }
        }

        return queryBuf.toString();
    }
}
