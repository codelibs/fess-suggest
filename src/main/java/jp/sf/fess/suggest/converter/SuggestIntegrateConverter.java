package jp.sf.fess.suggest.converter;


import java.util.ArrayList;
import java.util.List;

public class SuggestIntegrateConverter implements SuggestReadingConverter {
    List<SuggestReadingConverter> converterList = new ArrayList<SuggestReadingConverter>();

    public void addConverter(SuggestReadingConverter converter) {
        converterList.add(converter);
    }

    @Override
    public List<String> convert(String text) {
        List<String> convertedStrings = new ArrayList<String>();
        for (SuggestReadingConverter converter : converterList) {
            List<String> list = converter.convert(text);
            convertedStrings.addAll(list);
        }
        return convertedStrings;
    }

    @Override
    public void start() {

    }
}
