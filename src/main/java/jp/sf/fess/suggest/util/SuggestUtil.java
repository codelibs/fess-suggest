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

import java.lang.reflect.Field;
import java.util.Map;

import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;

public final class SuggestUtil {

    private SuggestUtil() {
    }

    public static SuggestReadingConverter createConverter(
            final String className, final Map<String, String> properties)
            throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, NoSuchFieldException {
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

    public static SuggestNormalizer createNormalizer(final String className,
            final Map<String, String> properties)
            throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, NoSuchFieldException {
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
}
