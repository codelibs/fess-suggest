package jp.sf.fess.suggest.normalizer;


import com.ibm.icu.text.Transliterator;

public class ICUNormalizer implements SuggestNormalizer {
    public String transliteratorId;

    protected Transliterator transliterator;

    @Override
    public String normalize(String text) {
        return transliterator.transliterate(text);
    }

    @Override
    public void start() {
        transliterator = Transliterator.getInstance(transliteratorId);
    }
}
