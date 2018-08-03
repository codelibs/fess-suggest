package org.codelibs.fess.suggest.normalizer;

import com.ibm.icu.text.Transliterator;

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
