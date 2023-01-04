/*
 * Copyright 2012-2023 CodeLibs Project and the Others.
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
