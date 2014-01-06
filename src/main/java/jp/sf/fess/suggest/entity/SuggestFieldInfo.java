package jp.sf.fess.suggest.entity;

import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;
import org.apache.lucene.analysis.util.TokenizerFactory;

import java.util.List;

public class SuggestFieldInfo {
    private final List<String> fieldNameList;

    private final TokenizerFactory tokenizerFactory;

    private final SuggestReadingConverter suggestReadingConverter;

    private final SuggestNormalizer suggestNormalizer;

    public SuggestFieldInfo(List<String> fieldNameList, TokenizerFactory tokenizerFactory,
                            SuggestReadingConverter suggestReadingConverter,
                            SuggestNormalizer suggestNormalizer) {
        this.fieldNameList = fieldNameList;
        this.tokenizerFactory = tokenizerFactory;
        this.suggestReadingConverter = suggestReadingConverter;
        this.suggestNormalizer = suggestNormalizer;
    }

    public List<String> getFieldNameList() {
        return fieldNameList;
    }

    public TokenizerFactory getTokenizerFactory() {
        return tokenizerFactory;
    }

    public SuggestReadingConverter getSuggestReadingConverter() {
        return suggestReadingConverter;
    }

    public SuggestNormalizer getSuggestNormalizer() {
        return suggestNormalizer;
    }
}
