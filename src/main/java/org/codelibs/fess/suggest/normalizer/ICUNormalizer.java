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

import com.ibm.icu.text.Transliterator;

/**
 * ICUNormalizer is a class that implements the Normalizer interface and provides
 * functionality to normalize text using ICU4J's Transliterator.
 *
 * <p>This class uses a specified Transliterator to perform text normalization.
 * The Transliterator is initialized with a given ID during the construction of
 * the ICUNormalizer instance.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * ICUNormalizer normalizer = new ICUNormalizer("Any-Latin; NFD; [:Nonspacing Mark:] Remove; NFC");
 * String normalizedText = normalizer.normalize("text to normalize", "field");
 * </pre>
 *
 * @see com.ibm.icu.text.Transliterator
 */
public class ICUNormalizer implements Normalizer {
    protected Transliterator transliterator;

    public ICUNormalizer(final String transliteratorId) {
        transliterator = Transliterator.getInstance(transliteratorId);
    }

    @Override
    public String normalize(final String text, final String field, final String... langs) {
        return transliterator.transliterate(text);
    }
}
