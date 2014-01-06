package jp.sf.fess.suggest.converter;

import java.util.List;

public interface SuggestReadingConverter {
    public List<String> convert(String text);

    public void start();
}
