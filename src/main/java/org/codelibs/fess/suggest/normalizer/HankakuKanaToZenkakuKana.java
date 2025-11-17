/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.suggest.normalizer;

/**
 * The HankakuKanaToZenkakuKana class implements the Normalizer interface to convert
 * half-width Katakana characters (Hankaku Kana) to full-width Katakana characters (Zenkaku Kana).
 * It also handles the conversion of voiced and semi-voiced sound marks.
 *
 * <p>This class provides the following functionalities:</p>
 * <ul>
 *   <li>Normalization of a string containing Hankaku Kana to Zenkaku Kana.</li>
 *   <li>Conversion of individual Hankaku Kana characters to Zenkaku Kana characters.</li>
 *   <li>Handling of voiced (ﾞ) and semi-voiced (ﾟ) sound marks to produce the correct Zenkaku Kana characters.</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * HankakuKanaToZenkakuKana normalizer = new HankakuKanaToZenkakuKana();
 * String normalized = normalizer.normalize("ｶﾞｷﾞｸﾞｹﾞｺﾞ", "field");
 * System.out.println(normalized); // Outputs: ガギグゲゴ
 * }
 * </pre>
 *
 * <p>Note: This class assumes that the input string contains valid Hankaku Kana characters
 * and does not perform any validation on the input.</p>
 *
 * @see Normalizer
 */
public class HankakuKanaToZenkakuKana implements Normalizer {
    /**
     * Constructs a new {@link HankakuKanaToZenkakuKana}.
     */
    public HankakuKanaToZenkakuKana() {
        // nothing
    }

    private static final char[] HANKAKU_KATAKANA = { '｡', '｢', '｣', '､', '･', 'ｦ', 'ｧ', 'ｨ', 'ｩ', 'ｪ', 'ｫ', 'ｬ', 'ｭ', 'ｮ', 'ｯ', 'ｰ', 'ｱ',
            'ｲ', 'ｳ', 'ｴ', 'ｵ', 'ｶ', 'ｷ', 'ｸ', 'ｹ', 'ｺ', 'ｻ', 'ｼ', 'ｽ', 'ｾ', 'ｿ', 'ﾀ', 'ﾁ', 'ﾂ', 'ﾃ', 'ﾄ', 'ﾅ', 'ﾆ', 'ﾇ', 'ﾈ', 'ﾉ', 'ﾊ',
            'ﾋ', 'ﾌ', 'ﾍ', 'ﾎ', 'ﾏ', 'ﾐ', 'ﾑ', 'ﾒ', 'ﾓ', 'ﾔ', 'ﾕ', 'ﾖ', 'ﾗ', 'ﾘ', 'ﾙ', 'ﾚ', 'ﾛ', 'ﾜ', 'ﾝ', 'ﾞ', 'ﾟ' };

    private static final char[] ZENKAKU_KATAKANA = { '。', '「', '」', '、', '・', 'ヲ', 'ァ', 'ィ', 'ゥ', 'ェ', 'ォ', 'ャ', 'ュ', 'ョ', 'ッ', 'ー', 'ア',
            'イ', 'ウ', 'エ', 'オ', 'カ', 'キ', 'ク', 'ケ', 'コ', 'サ', 'シ', 'ス', 'セ', 'ソ', 'タ', 'チ', 'ツ', 'テ', 'ト', 'ナ', 'ニ', 'ヌ', 'ネ', 'ノ', 'ハ',
            'ヒ', 'フ', 'ヘ', 'ホ', 'マ', 'ミ', 'ム', 'メ', 'モ', 'ヤ', 'ユ', 'ヨ', 'ラ', 'リ', 'ル', 'レ', 'ロ', 'ワ', 'ン', '゛', '゜' };

    private static final char HANKAKU_KATAKANA_FIRST_CHAR = HANKAKU_KATAKANA[0];

    private static final char HANKAKU_KATAKANA_LAST_CHAR = HANKAKU_KATAKANA[HANKAKU_KATAKANA.length - 1];

    @Override
    public String normalize(final String s, final String field, final String... langs) {
        if (s == null || s.length() == 0) {
            return s;
        }
        if (s.length() == 1) {
            return hankakuKatakanaToZenkakuKatakana(s.charAt(0)) + "";
        }
        final StringBuilder sb = new StringBuilder(s);
        int i;
        for (i = 0; i < sb.length() - 1; i++) {
            final char originalChar1 = sb.charAt(i);
            final char originalChar2 = sb.charAt(i + 1);
            final char margedChar = mergeChar(originalChar1, originalChar2);
            if (margedChar != originalChar1) {
                sb.setCharAt(i, margedChar);
                sb.deleteCharAt(i + 1);
            } else {
                final char convertedChar = hankakuKatakanaToZenkakuKatakana(originalChar1);
                if (convertedChar != originalChar1) {
                    sb.setCharAt(i, convertedChar);
                }
            }
        }
        if (i < sb.length()) {
            final char originalChar1 = sb.charAt(i);
            final char convertedChar = hankakuKatakanaToZenkakuKatakana(originalChar1);
            if (convertedChar != originalChar1) {
                sb.setCharAt(i, convertedChar);
            }
        }
        return sb.toString();
    }

    private static char hankakuKatakanaToZenkakuKatakana(final char c) {
        if (c >= HANKAKU_KATAKANA_FIRST_CHAR && c <= HANKAKU_KATAKANA_LAST_CHAR) {
            return ZENKAKU_KATAKANA[c - HANKAKU_KATAKANA_FIRST_CHAR];
        }
        return c;
    }

    /**
     * Merges two characters, handling voiced and semi-voiced sound marks.
     * @param c1 The first character.
     * @param c2 The second character.
     * @return The merged character.
     */
    public static char mergeChar(final char c1, final char c2) {
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
                default:
                    break;
                }
            }
        } else if (c2 == 'ﾟ' && "ﾊﾋﾌﾍﾎ".indexOf(c1) >= 0) {
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
            default:
                break;
            }
        }
        return c1;
    }

}
