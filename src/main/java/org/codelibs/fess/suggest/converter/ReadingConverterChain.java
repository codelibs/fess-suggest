package org.codelibs.fess.suggest.converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ReadingConverterChain implements ReadingConverter {
    private final List<ReadingConverter> converters = new ArrayList<>();

    @Override
    public void init() throws IOException {
        for (final ReadingConverter converter : converters) {
            converter.init();
        }
    }

    @Override
    public List<String> convert(final String text, final String field, final String... lang) throws IOException {
        final Queue<String> queue = new LinkedList<>();
        queue.add(text);
        final List<String> convertedTexts = new ArrayList<>(getMaxReadingNum());
        convertedTexts.add(text);

        for (final ReadingConverter converter : converters) {
            String s;
            while ((s = queue.poll()) != null && convertedTexts.size() <= getMaxReadingNum()) {
                final List<String> results = converter.convert(s, field, lang);
                convertedTexts.addAll(results);
            }
            queue.addAll(convertedTexts);
        }

        return convertedTexts;
    }

    public void addConverter(final ReadingConverter converter) {
        converters.add(converter);
    }
}
