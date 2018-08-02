package org.codelibs.fess.suggest.normalizer;

public interface Normalizer {
    String normalize(String text, String field, String... langs);
}
