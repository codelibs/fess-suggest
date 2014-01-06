package jp.sf.fess.suggest.normalizer;


public interface SuggestNormalizer {
    public String normalize(String text);

    public void start();
}
