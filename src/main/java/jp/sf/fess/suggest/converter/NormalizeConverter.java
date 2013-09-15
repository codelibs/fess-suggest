package jp.sf.fess.suggest.converter;

import com.ibm.icu.text.Normalizer;

public class NormalizeConverter implements SuggestConverter {

    @Override
    public String convert(final String query) {
        final String target = Normalizer.normalize(query.replaceAll(" +", " "),
                Normalizer.NFKC);

        return target;
    }
}
