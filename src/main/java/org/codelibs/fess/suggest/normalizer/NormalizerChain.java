package org.codelibs.fess.suggest.normalizer;

import java.util.ArrayList;
import java.util.List;

public class NormalizerChain implements Normalizer {
    List<Normalizer> normalizers = new ArrayList<>();

    @Override
    public String normalize(final String text, final String lang) {
        String tmp = text;
        for (final Normalizer normalizer : normalizers) {
            tmp = normalizer.normalize(tmp, lang);
        }
        return tmp;
    }

    public void add(final Normalizer normalizer) {
        normalizers.add(normalizer);
    }
}
