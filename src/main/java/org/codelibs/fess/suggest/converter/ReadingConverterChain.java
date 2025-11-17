/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A chain of {@link ReadingConverter} implementations that applies each converter in sequence to generate possible reading variations of a given text.
 * It maintains a list of ReadingConverter instances and iterates through them, applying each converter to the input text and accumulating the results.
 * The chain stops processing when the maximum number of readings is reached.
 */
public class ReadingConverterChain implements ReadingConverter {
    /**
     * Constructs a new {@link ReadingConverterChain}.
     */
    public ReadingConverterChain() {
        // nothing
    }

    private final List<ReadingConverter> converters = new ArrayList<>();

    @Override
    public void init() throws IOException {
        for (final ReadingConverter converter : converters) {
            converter.init();
        }
    }

    @Override
    public List<String> convert(final String text, final String field, final String... lang) throws IOException {
        // Use LinkedHashSet to maintain insertion order while eliminating duplicates
        final Set<String> resultSet = new LinkedHashSet<>();
        resultSet.add(text);

        // Start with the original text as the first input
        List<String> currentInputs = new ArrayList<>();
        currentInputs.add(text);

        // Apply each converter in sequence
        for (final ReadingConverter converter : converters) {
            final List<String> nextInputs = new ArrayList<>();

            // Process each input from the previous converter
            for (final String input : currentInputs) {
                if (resultSet.size() >= getMaxReadingNum()) {
                    break;
                }

                // Convert the input and collect results
                final List<String> results = converter.convert(input, field, lang);
                for (final String result : results) {
                    // Add to result set (automatically handles duplicates)
                    if (resultSet.add(result) && resultSet.size() <= getMaxReadingNum()) {
                        nextInputs.add(result);
                    }
                }
            }

            // Use the outputs of this converter as inputs for the next converter
            currentInputs = nextInputs;
        }

        return new ArrayList<>(resultSet);
    }

    /**
     * Adds a converter to the chain.
     * @param converter The converter to add.
     */
    public void addConverter(final ReadingConverter converter) {
        converters.add(converter);
    }
}
