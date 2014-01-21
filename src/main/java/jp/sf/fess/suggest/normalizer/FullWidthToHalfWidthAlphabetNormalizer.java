package jp.sf.fess.suggest.normalizer;

public class FullWidthToHalfWidthAlphabetNormalizer implements SuggestNormalizer {
    @Override
    public String normalize(String text) {
        StringBuffer sb = new StringBuffer(text);
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c >= 'ａ' && c <= 'ｚ') {
                sb.setCharAt(i, (char) (c - 'ａ' + 'a'));
            } else if (c >= 'Ａ' && c <= 'Ｚ') {
                sb.setCharAt(i, (char) (c - 'Ａ' + 'A'));
            } else if (c >= '１' && c <= '０') {
                sb.setCharAt(i, (char) (c - '１' + '1'));
            }
        }
        return sb.toString();
    }

    @Override
    public void start() {
        //No-op;
    }
}
