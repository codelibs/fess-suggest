package org.codelibs.fess.suggest.normalizer;

public class HankakuKanaToZenkakuKana implements Normalizer {
    private static final char[] HANKAKU_KATAKANA = { '｡', '｢', '｣', '､', '･', 'ｦ', 'ｧ', 'ｨ', 'ｩ', 'ｪ', 'ｫ', 'ｬ', 'ｭ', 'ｮ', 'ｯ', 'ｰ', 'ｱ',
            'ｲ', 'ｳ', 'ｴ', 'ｵ', 'ｶ', 'ｷ', 'ｸ', 'ｹ', 'ｺ', 'ｻ', 'ｼ', 'ｽ', 'ｾ', 'ｿ', 'ﾀ', 'ﾁ', 'ﾂ', 'ﾃ', 'ﾄ', 'ﾅ', 'ﾆ', 'ﾇ', 'ﾈ', 'ﾉ', 'ﾊ',
            'ﾋ', 'ﾌ', 'ﾍ', 'ﾎ', 'ﾏ', 'ﾐ', 'ﾑ', 'ﾒ', 'ﾓ', 'ﾔ', 'ﾕ', 'ﾖ', 'ﾗ', 'ﾘ', 'ﾙ', 'ﾚ', 'ﾛ', 'ﾜ', 'ﾝ', 'ﾞ', 'ﾟ' };

    private static final char[] ZENKAKU_KATAKANA = { '。', '「', '」', '、', '・', 'ヲ', 'ァ', 'ィ', 'ゥ', 'ェ', 'ォ', 'ャ', 'ュ', 'ョ', 'ッ', 'ー', 'ア',
            'イ', 'ウ', 'エ', 'オ', 'カ', 'キ', 'ク', 'ケ', 'コ', 'サ', 'シ', 'ス', 'セ', 'ソ', 'タ', 'チ', 'ツ', 'テ', 'ト', 'ナ', 'ニ', 'ヌ', 'ネ', 'ノ', 'ハ',
            'ヒ', 'フ', 'ヘ', 'ホ', 'マ', 'ミ', 'ム', 'メ', 'モ', 'ヤ', 'ユ', 'ヨ', 'ラ', 'リ', 'ル', 'レ', 'ロ', 'ワ', 'ン', '゛', '゜' };

    private static final char HANKAKU_KATAKANA_FIRST_CHAR = HANKAKU_KATAKANA[0];

    private static final char HANKAKU_KATAKANA_LAST_CHAR = HANKAKU_KATAKANA[HANKAKU_KATAKANA.length - 1];

    @Override
    public String normalize(final String s, final String lang) {
        if (s.length() == 0) {
            return s;
        } else if (s.length() == 1) {
            return hankakuKatakanaToZenkakuKatakana(s.charAt(0)) + "";
        } else {
            StringBuffer sb = new StringBuffer(s);
            int i;
            for (i = 0; i < sb.length() - 1; i++) {
                char originalChar1 = sb.charAt(i);
                char originalChar2 = sb.charAt(i + 1);
                char margedChar = mergeChar(originalChar1, originalChar2);
                if (margedChar != originalChar1) {
                    sb.setCharAt(i, margedChar);
                    sb.deleteCharAt(i + 1);
                } else {
                    char convertedChar = hankakuKatakanaToZenkakuKatakana(originalChar1);
                    if (convertedChar != originalChar1) {
                        sb.setCharAt(i, convertedChar);
                    }
                }
            }
            if (i < sb.length()) {
                char originalChar1 = sb.charAt(i);
                char convertedChar = hankakuKatakanaToZenkakuKatakana(originalChar1);
                if (convertedChar != originalChar1) {
                    sb.setCharAt(i, convertedChar);
                }
            }
            return sb.toString();
        }
    }

    private static char hankakuKatakanaToZenkakuKatakana(char c) {
        if (c >= HANKAKU_KATAKANA_FIRST_CHAR && c <= HANKAKU_KATAKANA_LAST_CHAR) {
            return ZENKAKU_KATAKANA[c - HANKAKU_KATAKANA_FIRST_CHAR];
        } else {
            return c;
        }
    }

    public static char mergeChar(char c1, char c2) {
        if (c2 == 'ﾞ') {
            if ("ｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾊﾋﾌﾍﾎ".indexOf(c1) >= 0) {
                switch (c1) {
                case 'ｶ':
                    return 'ガ';
                case 'ｷ':
                    return 'ギ';
                case 'ｸ':
                    return 'グ';
                case 'ｹ':
                    return 'ゲ';
                case 'ｺ':
                    return 'ゴ';
                case 'ｻ':
                    return 'ザ';
                case 'ｼ':
                    return 'ジ';
                case 'ｽ':
                    return 'ズ';
                case 'ｾ':
                    return 'ゼ';
                case 'ｿ':
                    return 'ゾ';
                case 'ﾀ':
                    return 'ダ';
                case 'ﾁ':
                    return 'ヂ';
                case 'ﾂ':
                    return 'ヅ';
                case 'ﾃ':
                    return 'デ';
                case 'ﾄ':
                    return 'ド';
                case 'ﾊ':
                    return 'バ';
                case 'ﾋ':
                    return 'ビ';
                case 'ﾌ':
                    return 'ブ';
                case 'ﾍ':
                    return 'ベ';
                case 'ﾎ':
                    return 'ボ';
                }
            }
        } else if (c2 == 'ﾟ') {
            if ("ﾊﾋﾌﾍﾎ".indexOf(c1) > 0) {
                switch (c1) {
                case 'ﾊ':
                    return 'パ';
                case 'ﾋ':
                    return 'ピ';
                case 'ﾌ':
                    return 'プ';
                case 'ﾍ':
                    return 'ペ';
                case 'ﾎ':
                    return 'ポ';
                }
            }
        }
        return c1;
    }

}
