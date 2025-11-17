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
package org.codelibs.fess.suggest.normalizer;

import java.util.ArrayList;
import java.util.List;

/**
 * The NormalizerChain class implements the Normalizer interface and allows chaining multiple normalizers together.
 * It applies each normalizer in the order they were added to the chain.
 *
 * <p>Usage example:</p>
 * <pre>
 * NormalizerChain chain = new NormalizerChain();
 * chain.add(new SomeNormalizer());
 * chain.add(new AnotherNormalizer());
 * String normalizedText = chain.normalize("input text", "field", "en");
 * </pre>
 *
 * <p>Methods:</p>
 * <ul>
 *   <li>{@link #normalize(String, String, String...)} - Applies all added normalizers to the input text.</li>
 *   <li>{@link #add(Normalizer)} - Adds a new normalizer to the chain.</li>
 * </ul>
 *
 * <p>Fields:</p>
 * <ul>
 *   <li>{@code normalizers} - A list of normalizers to be applied in sequence.</li>
 * </ul>
 */
public class NormalizerChain implements Normalizer {
    /**
     * Constructs a new {@link NormalizerChain}.
     */
    public NormalizerChain() {
        // nothing
    }

    private final List<Normalizer> normalizers = new ArrayList<>();

    @Override
    public String normalize(final String text, final String field, final String... langs) {
        if (text == null) {
            return null;
        }
        String tmp = text;
        for (final Normalizer normalizer : normalizers) {
            tmp = normalizer.normalize(tmp, field, langs);
            if (tmp == null) {
                return null;
            }
        }
        return tmp;
    }

    /**
     * Adds a normalizer to the chain.
     * @param normalizer The normalizer to add.
     * @throws IllegalArgumentException if normalizer is null
     */
    public void add(final Normalizer normalizer) {
        if (normalizer == null) {
            throw new IllegalArgumentException("normalizer must not be null");
        }
        normalizers.add(normalizer);
    }
}
