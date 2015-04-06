package org.codelibs.fess.suggest.converter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ReadingConverterChain implements ReadingConverter {
    private List<ReadingConverter> converters = new ArrayList<>();

    @Override
    public List<String> convert(String text) {
        Queue<String> queue = new LinkedList<>();
        queue.add(text);
        List<String> convertedTexts = new ArrayList<>(getMaxReadingNum());

        converters.forEach(converter -> {
            String s;
            while ((s = queue.poll()) != null && convertedTexts.size() <= getMaxReadingNum()) {
                List<String> results = converter.convert(s);
                convertedTexts.addAll(results);
            }
            queue.addAll(convertedTexts);
        });

        return convertedTexts;
    }

    public void addConverter(ReadingConverter converter) {
        converters.add(converter);
    }
}
