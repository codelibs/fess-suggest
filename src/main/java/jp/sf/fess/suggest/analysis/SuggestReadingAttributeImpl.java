package jp.sf.fess.suggest.analysis;


import org.apache.lucene.analysis.tokenattributes.CharTermAttributeImpl;

public class SuggestReadingAttributeImpl extends CharTermAttributeImpl implements SuggestReadingAttribute {
    public SuggestReadingAttributeImpl() {
        super();
    }
}
