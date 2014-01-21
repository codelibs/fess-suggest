package jp.sf.fess.suggest.converter;

import java.util.List;

public interface SuggestReadingConverter {
    List<String> convert(String text);

    void start();
}
