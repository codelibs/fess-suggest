package org.codelibs.fess.suggest.converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ReadingConverterChain implements ReadingConverter {
    private List<ReadingConverter> converters = new ArrayList<>();

    @Override
    public void init() throws IOException {
        for (ReadingConverter converter : converters) {
            converter.init();
        }
    }

    @Override
    public List<String> convert(String text) throws IOException {
        Queue<String> queue = new LinkedList<>();
        queue.add(text);
        List<String> convertedTexts = new ArrayList<>(getMaxReadingNum());
        convertedTexts.add(text);

        for (ReadingConverter converter : converters) {
            String s;
            while ((s = queue.poll()) != null && convertedTexts.size() <= getMaxReadingNum()) {
                List<String> results = converter.convert(s);
                convertedTexts.addAll(results);
            }
            queue.addAll(convertedTexts);
        }

        return convertedTexts;
    }

    public void addConverter(ReadingConverter converter) {
        converters.add(converter);
    }
}
