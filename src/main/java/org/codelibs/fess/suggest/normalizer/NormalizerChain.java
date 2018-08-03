package org.codelibs.fess.suggest.normalizer;

import java.util.ArrayList;
import java.util.List;

public class NormalizerChain implements Normalizer {
    List<Normalizer> normalizers = new ArrayList<>();

    @Override
    public String normalize(final String text, final String field, final String... langs) {
        String tmp = text;
        for (final Normalizer normalizer : normalizers) {
            tmp = normalizer.normalize(tmp, field, langs);
        }
        return tmp;
    }

    public void add(final Normalizer normalizer) {
        normalizers.add(normalizer);
    }
}
