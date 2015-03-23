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

package org.codelibs.fess.suggest.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codelibs.fess.suggest.SuggestConstants;
import org.codelibs.fess.suggest.converter.SuggestReadingConverter;
import org.codelibs.fess.suggest.normalizer.SuggestNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SuggestUtil {
    private static final Logger logger = LoggerFactory.getLogger(SuggestUtil.class);

    private SuggestUtil() {
    }

    public static SuggestReadingConverter createConverter(final String className, final Map<String, String> properties)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchFieldException {
        final Class cls = Class.forName(className);
        final Object obj = cls.newInstance();
        if (!(obj instanceof SuggestReadingConverter)) {
            throw new IllegalArgumentException("Not converter. " + className);
        }

        for (final Map.Entry<String, String> entry : properties.entrySet()) {
            final String fieldName = entry.getKey();
            final String fieldValue = entry.getValue();
            final Field field = cls.getField(fieldName);
            field.set(obj, fieldValue);
        }

        return (SuggestReadingConverter) obj;
    }

    public static SuggestNormalizer createNormalizer(final String className, final Map<String, String> properties)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchFieldException {
        final Class cls = Class.forName(className);
        final Object obj = cls.newInstance();
        if (!(obj instanceof SuggestNormalizer)) {
            throw new IllegalArgumentException("Not normalizer. " + className);
        }

        for (final Map.Entry<String, String> entry : properties.entrySet()) {
            final String fieldName = entry.getKey();
            final String fieldValue = entry.getValue();
            final Field field = cls.getField(fieldName);
            field.set(obj, fieldValue);
        }

        return (SuggestNormalizer) obj;
    }

    public static Map<String, String> parseSolrParams(String solrParams) {
        Map<String, String> map = new HashMap<>();

        String[] params = solrParams.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length != 2) {
                continue;
            }
            String key = keyValue[0];
            String value;
            try {
                value = URLDecoder.decode(keyValue[1], SuggestConstants.UTF_8);
            } catch (Exception e) {
                value = keyValue[1];
            }
            if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                map.put(key, value);
            }
        }

        return map;
    }

    public static List<String> parseQuery(String query, String field) {
        List<String> words = new ArrayList<>();
        String[] elems = query.replace("(", " ").replace(")", " ").replaceAll(" +", " ").trim().split(" ");
        for (String elem : elems) {
            if (elem.indexOf(":") == -1) {
                continue;
            }
            String[] pair = elem.split(":");
            if (pair.length != 2 || !field.equals(pair[0])) {
                continue;
            }
            if (!words.contains(pair[1])) {
                words.add(pair[1]);
            }
        }
        return words;
    }
}
