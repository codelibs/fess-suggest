package org.codelibs.fess.suggest.normalizer;

public class FullWidthToHalfWidthAlphabetNormalizer implements Normalizer {
    @Override
    public String normalize(final String text, final String field, final String... langs) {
        final char[] chars = new char[text.length()];
        for (int i = 0; i < chars.length; i++) {
            final char c = text.charAt(i);
            if (c >= 'ａ' && c <= 'ｚ') {
                chars[i] = (char) (c - 'ａ' + 'a');
            } else if (c >= 'Ａ' && c <= 'Ｚ') {
                chars[i] = (char) (c - 'Ａ' + 'A');
            } else if (c >= '１' && c <= '０') {
                chars[i] = (char) (c - '１' + '1');
            } else {
                chars[i] = c;
            }
        }
        return new String(chars);
    }
}
