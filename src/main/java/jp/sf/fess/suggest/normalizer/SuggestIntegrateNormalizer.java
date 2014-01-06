package jp.sf.fess.suggest.normalizer;


import java.util.ArrayList;
import java.util.List;

public class SuggestIntegrateNormalizer implements SuggestNormalizer {
    private List<SuggestNormalizer> suggestNormalizerList = new ArrayList<SuggestNormalizer>();

    public void addNormalizer(SuggestNormalizer normalizer) {
        suggestNormalizerList.add(normalizer);
    }

    @Override
    public String normalize(String text) {
        String s = text;
        for (SuggestNormalizer normalizer : suggestNormalizerList) {
            s = normalizer.normalize(s);
        }
        return s;
    }

    @Override
    public void start() {
        for (SuggestNormalizer normalizer : suggestNormalizerList) {
            normalizer.start();
        }
    }
}
