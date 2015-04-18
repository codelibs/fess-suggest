package org.codelibs.fess.suggest.normalizer;

import java.util.ArrayList;
import java.util.List;

public class NormalizerChain implements Normalizer {
    List<Normalizer> normalizers = new ArrayList<>();

    @Override
    public String normalize(String text) {
        String tmp = text;
        for (Normalizer normalizer : normalizers) {
            tmp = normalizer.normalize(tmp);
        }
        return tmp;
    }

    public void add(Normalizer normalizer) {
        normalizers.add(normalizer);
    }
}
