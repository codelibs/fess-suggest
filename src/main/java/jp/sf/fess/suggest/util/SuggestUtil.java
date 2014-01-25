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

package jp.sf.fess.suggest.util;


import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

public final class SuggestUtil {

    private SuggestUtil() {
    }

    public static SuggestReadingConverter createConverter(String className, Map<String, String> properties)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchFieldException {
        Class cls = Class.forName(className);
        Object obj = cls.newInstance();
        if (!(obj instanceof SuggestReadingConverter)) {
            throw new IllegalArgumentException("Not converter. " + className);
        }

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();
            Field field = cls.getField(fieldName);
            field.set(obj, fieldValue);
        }

        return (SuggestReadingConverter) obj;
    }

    public static SuggestNormalizer createNormalizer(String className, Map<String, String> properties)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchFieldException {
        Class cls = Class.forName(className);
        Object obj = cls.newInstance();
        if (!(obj instanceof SuggestNormalizer)) {
            throw new IllegalArgumentException("Not normalizer. " + className);
        }

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();
            Field field = cls.getField(fieldName);
            field.set(obj, fieldValue);
        }

        return (SuggestNormalizer) obj;
    }
}
