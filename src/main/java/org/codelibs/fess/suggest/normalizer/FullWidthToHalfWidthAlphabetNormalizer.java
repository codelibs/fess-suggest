package org.codelibs.fess.suggest.normalizer;

public class FullWidthToHalfWidthAlphabetNormalizer implements Normalizer {
    @Override
    public String normalize(final String text) {
        final StringBuilder sb = new StringBuilder(text);
        for (int i = 0; i < sb.length(); i++) {
            final char c = sb.charAt(i);
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
}
