package jp.sf.fess.suggest.normalizer;


public interface SuggestNormalizer {
    String normalize(String text);

    void start();
}
