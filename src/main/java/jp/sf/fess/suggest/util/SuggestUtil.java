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
