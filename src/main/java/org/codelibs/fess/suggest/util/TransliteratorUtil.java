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
package org.codelibs.fess.suggest.util;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Text transforms the reading converters need, implemented with the JDK alone.
 * <p>
 * Each method reproduces an ICU4J transliterator that fess-suggest used before, so that the readings written to
 * existing suggest indices do not change: {@link #hiraganaToKatakana(String)} is {@code Hiragana-Katakana},
 * {@link #fullwidthToHalfwidth(String)} is {@code Fullwidth-Halfwidth} and {@link #toLowerCase(String)} is
 * {@code Any-Lower}. TransliteratorUtilTest holds all three to ICU4J's output for every code point the running JDK
 * defines. The Unicode data itself comes from the JDK, so where the JDK's Unicode version and ICU4J's disagree about a
 * character's properties, or the JDK does not know the character yet, the JDK's view applies.
 * </p>
 * <p>
 * Code points are written as integers throughout: the formatter rewrites Unicode escapes into the characters
 * themselves, and combining marks are unreadable that way.
 * </p>
 */
public final class TransliteratorUtil {

    /**
     * The rules of ICU4J's {@code Fullwidth-Halfwidth} transliterator: a source code point followed by the one or two
     * code points it becomes. Every rule rewrites one BMP code point without context, so a lookup table reproduces it.
     */
    private static final int[][] HALFWIDTH_RULES = { { 0x1100, 0xFFA1 }, { 0x1101, 0xFFA2 }, { 0x1102, 0xFFA4 }, { 0x1103, 0xFFA7 },
            { 0x1104, 0xFFA8 }, { 0x1105, 0xFFA9 }, { 0x1106, 0xFFB1 }, { 0x1107, 0xFFB2 }, { 0x1108, 0xFFB3 }, { 0x1109, 0xFFB5 },
            { 0x110A, 0xFFB6 }, { 0x110B, 0xFFB7 }, { 0x110C, 0xFFB8 }, { 0x110D, 0xFFB9 }, { 0x110E, 0xFFBA }, { 0x110F, 0xFFBB },
            { 0x1110, 0xFFBC }, { 0x1111, 0xFFBD }, { 0x1112, 0xFFBE }, { 0x111A, 0xFFB0 }, { 0x1121, 0xFFB4 }, { 0x1160, 0xFFA0 },
            { 0x1161, 0xFFC2 }, { 0x1162, 0xFFC3 }, { 0x1163, 0xFFC4 }, { 0x1164, 0xFFC5 }, { 0x1165, 0xFFC6 }, { 0x1166, 0xFFC7 },
            { 0x1167, 0xFFCA }, { 0x1168, 0xFFCB }, { 0x1169, 0xFFCC }, { 0x116A, 0xFFCD }, { 0x116B, 0xFFCE }, { 0x116C, 0xFFCF },
            { 0x116D, 0xFFD2 }, { 0x116E, 0xFFD3 }, { 0x116F, 0xFFD4 }, { 0x1170, 0xFFD5 }, { 0x1171, 0xFFD6 }, { 0x1172, 0xFFD7 },
            { 0x1173, 0xFFDA }, { 0x1174, 0xFFDB }, { 0x1175, 0xFFDC }, { 0x11AA, 0xFFA3 }, { 0x11AC, 0xFFA5 }, { 0x11AD, 0xFFA6 },
            { 0x11B0, 0xFFAA }, { 0x11B1, 0xFFAB }, { 0x11B2, 0xFFAC }, { 0x11B3, 0xFFAD }, { 0x11B4, 0xFFAE }, { 0x11B5, 0xFFAF },
            { 0x2190, 0xFFE9 }, { 0x2191, 0xFFEA }, { 0x2192, 0xFFEB }, { 0x2193, 0xFFEC }, { 0x2502, 0xFFE8 }, { 0x25A0, 0xFFED },
            { 0x25CB, 0xFFEE }, { 0x3000, 0x0020 }, { 0x3001, 0xFF64 }, { 0x3002, 0xFF61 }, { 0x300C, 0xFF62 }, { 0x300D, 0xFF63 },
            { 0x3099, 0xFF9E }, { 0x309A, 0xFF9F }, { 0x30A1, 0xFF67 }, { 0x30A2, 0xFF71 }, { 0x30A3, 0xFF68 }, { 0x30A4, 0xFF72 },
            { 0x30A5, 0xFF69 }, { 0x30A6, 0xFF73 }, { 0x30A7, 0xFF6A }, { 0x30A8, 0xFF74 }, { 0x30A9, 0xFF6B }, { 0x30AA, 0xFF75 },
            { 0x30AB, 0xFF76 }, { 0x30AC, 0xFF76, 0xFF9E }, { 0x30AD, 0xFF77 }, { 0x30AE, 0xFF77, 0xFF9E }, { 0x30AF, 0xFF78 },
            { 0x30B0, 0xFF78, 0xFF9E }, { 0x30B1, 0xFF79 }, { 0x30B2, 0xFF79, 0xFF9E }, { 0x30B3, 0xFF7A }, { 0x30B4, 0xFF7A, 0xFF9E },
            { 0x30B5, 0xFF7B }, { 0x30B6, 0xFF7B, 0xFF9E }, { 0x30B7, 0xFF7C }, { 0x30B8, 0xFF7C, 0xFF9E }, { 0x30B9, 0xFF7D },
            { 0x30BA, 0xFF7D, 0xFF9E }, { 0x30BB, 0xFF7E }, { 0x30BC, 0xFF7E, 0xFF9E }, { 0x30BD, 0xFF7F }, { 0x30BE, 0xFF7F, 0xFF9E },
            { 0x30BF, 0xFF80 }, { 0x30C0, 0xFF80, 0xFF9E }, { 0x30C1, 0xFF81 }, { 0x30C2, 0xFF81, 0xFF9E }, { 0x30C3, 0xFF6F },
            { 0x30C4, 0xFF82 }, { 0x30C5, 0xFF82, 0xFF9E }, { 0x30C6, 0xFF83 }, { 0x30C7, 0xFF83, 0xFF9E }, { 0x30C8, 0xFF84 },
            { 0x30C9, 0xFF84, 0xFF9E }, { 0x30CA, 0xFF85 }, { 0x30CB, 0xFF86 }, { 0x30CC, 0xFF87 }, { 0x30CD, 0xFF88 }, { 0x30CE, 0xFF89 },
            { 0x30CF, 0xFF8A }, { 0x30D0, 0xFF8A, 0xFF9E }, { 0x30D1, 0xFF8A, 0xFF9F }, { 0x30D2, 0xFF8B }, { 0x30D3, 0xFF8B, 0xFF9E },
            { 0x30D4, 0xFF8B, 0xFF9F }, { 0x30D5, 0xFF8C }, { 0x30D6, 0xFF8C, 0xFF9E }, { 0x30D7, 0xFF8C, 0xFF9F }, { 0x30D8, 0xFF8D },
            { 0x30D9, 0xFF8D, 0xFF9E }, { 0x30DA, 0xFF8D, 0xFF9F }, { 0x30DB, 0xFF8E }, { 0x30DC, 0xFF8E, 0xFF9E },
            { 0x30DD, 0xFF8E, 0xFF9F }, { 0x30DE, 0xFF8F }, { 0x30DF, 0xFF90 }, { 0x30E0, 0xFF91 }, { 0x30E1, 0xFF92 }, { 0x30E2, 0xFF93 },
            { 0x30E3, 0xFF6C }, { 0x30E4, 0xFF94 }, { 0x30E5, 0xFF6D }, { 0x30E6, 0xFF95 }, { 0x30E7, 0xFF6E }, { 0x30E8, 0xFF96 },
            { 0x30E9, 0xFF97 }, { 0x30EA, 0xFF98 }, { 0x30EB, 0xFF99 }, { 0x30EC, 0xFF9A }, { 0x30ED, 0xFF9B }, { 0x30EF, 0xFF9C },
            { 0x30F2, 0xFF66 }, { 0x30F3, 0xFF9D }, { 0x30F4, 0xFF73, 0xFF9E }, { 0x30F7, 0xFF9C, 0xFF9E }, { 0x30FA, 0xFF66, 0xFF9E },
            { 0x30FB, 0xFF65 }, { 0x30FC, 0xFF70 }, { 0xFF01, 0x0021 }, { 0xFF02, 0x0022 }, { 0xFF03, 0x0023 }, { 0xFF04, 0x0024 },
            { 0xFF05, 0x0025 }, { 0xFF06, 0x0026 }, { 0xFF07, 0x0027 }, { 0xFF08, 0x0028 }, { 0xFF09, 0x0029 }, { 0xFF0A, 0x002A },
            { 0xFF0B, 0x002B }, { 0xFF0C, 0x002C }, { 0xFF0D, 0x002D }, { 0xFF0E, 0x002E }, { 0xFF0F, 0x002F }, { 0xFF10, 0x0030 },
            { 0xFF11, 0x0031 }, { 0xFF12, 0x0032 }, { 0xFF13, 0x0033 }, { 0xFF14, 0x0034 }, { 0xFF15, 0x0035 }, { 0xFF16, 0x0036 },
            { 0xFF17, 0x0037 }, { 0xFF18, 0x0038 }, { 0xFF19, 0x0039 }, { 0xFF1A, 0x003A }, { 0xFF1B, 0x003B }, { 0xFF1C, 0x003C },
            { 0xFF1D, 0x003D }, { 0xFF1E, 0x003E }, { 0xFF1F, 0x003F }, { 0xFF20, 0x0040 }, { 0xFF21, 0x0041 }, { 0xFF22, 0x0042 },
            { 0xFF23, 0x0043 }, { 0xFF24, 0x0044 }, { 0xFF25, 0x0045 }, { 0xFF26, 0x0046 }, { 0xFF27, 0x0047 }, { 0xFF28, 0x0048 },
            { 0xFF29, 0x0049 }, { 0xFF2A, 0x004A }, { 0xFF2B, 0x004B }, { 0xFF2C, 0x004C }, { 0xFF2D, 0x004D }, { 0xFF2E, 0x004E },
            { 0xFF2F, 0x004F }, { 0xFF30, 0x0050 }, { 0xFF31, 0x0051 }, { 0xFF32, 0x0052 }, { 0xFF33, 0x0053 }, { 0xFF34, 0x0054 },
            { 0xFF35, 0x0055 }, { 0xFF36, 0x0056 }, { 0xFF37, 0x0057 }, { 0xFF38, 0x0058 }, { 0xFF39, 0x0059 }, { 0xFF3A, 0x005A },
            { 0xFF3B, 0x005B }, { 0xFF3C, 0x005C }, { 0xFF3D, 0x005D }, { 0xFF3E, 0x005E }, { 0xFF3F, 0x005F }, { 0xFF40, 0x0060 },
            { 0xFF41, 0x0061 }, { 0xFF42, 0x0062 }, { 0xFF43, 0x0063 }, { 0xFF44, 0x0064 }, { 0xFF45, 0x0065 }, { 0xFF46, 0x0066 },
            { 0xFF47, 0x0067 }, { 0xFF48, 0x0068 }, { 0xFF49, 0x0069 }, { 0xFF4A, 0x006A }, { 0xFF4B, 0x006B }, { 0xFF4C, 0x006C },
            { 0xFF4D, 0x006D }, { 0xFF4E, 0x006E }, { 0xFF4F, 0x006F }, { 0xFF50, 0x0070 }, { 0xFF51, 0x0071 }, { 0xFF52, 0x0072 },
            { 0xFF53, 0x0073 }, { 0xFF54, 0x0074 }, { 0xFF55, 0x0075 }, { 0xFF56, 0x0076 }, { 0xFF57, 0x0077 }, { 0xFF58, 0x0078 },
            { 0xFF59, 0x0079 }, { 0xFF5A, 0x007A }, { 0xFF5B, 0x007B }, { 0xFF5C, 0x007C }, { 0xFF5D, 0x007D }, { 0xFF5E, 0x007E },
            { 0xFFE0, 0x00A2 }, { 0xFFE1, 0x00A3 }, { 0xFFE2, 0x00AC }, { 0xFFE3, 0x00AF }, { 0xFFE4, 0x00A6 }, { 0xFFE5, 0x00A5 },
            { 0xFFE6, 0x20A9 }, };

    private static final Map<Character, String> HALFWIDTH_FORMS = new HashMap<>(HALFWIDTH_RULES.length * 2);

    static {
        for (final int[] rule : HALFWIDTH_RULES) {
            HALFWIDTH_FORMS.put((char) rule[0], new String(rule, 1, rule.length - 1));
        }
    }

    private static final int CAPITAL_SIGMA = 0x03A3;

    private static final char SMALL_SIGMA = (char) 0x03C3;

    private static final char SMALL_FINAL_SIGMA = (char) 0x03C2;

    private static final int COMBINING_VOICED_SOUND_MARK = 0x3099;

    private TransliteratorUtil() {
    }

    /**
     * Converts hiragana to katakana, the way ICU4J's {@code Hiragana-Katakana} transliterator does.
     * <p>
     * That transliterator only looks at ASCII, kana, halfwidth katakana and nonspacing marks. Each run of such
     * characters is normalized to NFKC (so halfwidth katakana become fullwidth and a kana absorbs a following
     * combining voiced sound mark), hiragana are mapped to katakana, and the result is composed again with NFC.
     * Everything else, including the CJK compatibility ideographs that NFKC would rewrite, is left as it is.
     * </p>
     *
     * @param text the text to convert
     * @return the converted text
     */
    public static String hiraganaToKatakana(final String text) {
        final StringBuilder buf = new StringBuilder(text.length());
        int runStart = -1;
        int i = 0;
        while (i < text.length()) {
            final int cp = text.codePointAt(i);
            if (isKanaTransliterationTarget(cp)) {
                if (runStart < 0) {
                    runStart = i;
                }
            } else {
                if (runStart >= 0) {
                    appendKatakana(buf, text.substring(runStart, i));
                    runStart = -1;
                }
                buf.appendCodePoint(cp);
            }
            i += Character.charCount(cp);
        }
        if (runStart >= 0) {
            appendKatakana(buf, text.substring(runStart));
        }
        return buf.toString();
    }

    /**
     * Converts fullwidth characters to their halfwidth forms, the way ICU4J's {@code Fullwidth-Halfwidth}
     * transliterator does. A katakana with a voiced sound mark becomes two halfwidth characters.
     *
     * @param text the text to convert
     * @return the converted text
     */
    public static String fullwidthToHalfwidth(final String text) {
        final StringBuilder buf = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            final String halfwidth = HALFWIDTH_FORMS.get(c);
            if (halfwidth != null) {
                buf.append(halfwidth);
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    /**
     * Converts text to lower case with the root locale, the way ICU4J's {@code Any-Lower} transliterator does.
     * <p>
     * {@link String#toLowerCase(Locale)} already applies the language-independent mappings. The one conditional
     * mapping it decides differently is the final form of capital sigma, so each sigma is resolved here with the
     * Final_Sigma condition of the Unicode Standard (section 3.13).
     * </p>
     *
     * @param text the text to convert
     * @return the converted text
     */
    public static String toLowerCase(final String text) {
        int sigma = text.indexOf(CAPITAL_SIGMA);
        if (sigma < 0) {
            return text.toLowerCase(Locale.ROOT);
        }
        final StringBuilder buf = new StringBuilder(text.length());
        int segmentStart = 0;
        while (sigma >= 0) {
            buf.append(text.substring(segmentStart, sigma).toLowerCase(Locale.ROOT));
            buf.append(isFinalSigma(text, sigma) ? SMALL_FINAL_SIGMA : SMALL_SIGMA);
            segmentStart = sigma + 1;
            sigma = text.indexOf(CAPITAL_SIGMA, segmentStart);
        }
        buf.append(text.substring(segmentStart).toLowerCase(Locale.ROOT));
        return buf.toString();
    }

    /**
     * The filter of ICU4J's {@code Hiragana-Katakana} transliterator: U+0000 to U+007E, U+3001, U+3002, U+3099,
     * U+309A, U+30A1 to U+30FC, U+FF61 to U+FF9F, and the Hiragana script, the Katakana script and nonspacing marks,
     * except U+309B and U+309C.
     */
    private static boolean isKanaTransliterationTarget(final int cp) {
        if (cp == 0x309B || cp == 0x309C) {
            return false;
        }
        if (cp <= 0x007E || cp == 0x3001 || cp == 0x3002 || cp == 0x3099 || cp == 0x309A || cp >= 0x30A1 && cp <= 0x30FC
                || cp >= 0xFF61 && cp <= 0xFF9F) {
            return true;
        }
        final Character.UnicodeScript script = Character.UnicodeScript.of(cp);
        return script == Character.UnicodeScript.HIRAGANA || script == Character.UnicodeScript.KATAKANA
                || Character.getType(cp) == Character.NON_SPACING_MARK;
    }

    private static void appendKatakana(final StringBuilder buf, final String run) {
        final String normalized = Normalizer.normalize(run, Normalizer.Form.NFKC);
        final StringBuilder katakana = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            final char c = normalized.charAt(i);
            if (c >= 0x308F && c <= 0x3092 && i + 1 < normalized.length() && normalized.charAt(i + 1) == COMBINING_VOICED_SOUND_MARK) {
                // WA, WI, WE and WO with a voiced sound mark have no precomposed hiragana, but VA, VI, VE and VO exist.
                katakana.append((char) (c - 0x308F + 0x30F7));
                i++;
            } else if (c >= 0x3041 && c <= 0x3094 || c == 0x309D || c == 0x309E) {
                katakana.append((char) (c + 0x60));
            } else {
                katakana.append(c);
            }
        }
        buf.append(Normalizer.normalize(katakana, Normalizer.Form.NFC));
    }

    /**
     * Final_Sigma: the sigma is preceded by a cased letter and zero or more case-ignorable characters, and is not
     * followed by zero or more case-ignorable characters and a cased letter. A character that is both cased and
     * case-ignorable counts as case-ignorable, as it does in ICU4J.
     */
    private static boolean isFinalSigma(final String text, final int index) {
        boolean precededByCasedLetter = false;
        int i = index;
        while (i > 0) {
            final int cp = text.codePointBefore(i);
            i -= Character.charCount(cp);
            if (!isCaseIgnorable(cp)) {
                precededByCasedLetter = isCased(cp);
                break;
            }
        }
        if (!precededByCasedLetter) {
            return false;
        }
        i = index + 1;
        while (i < text.length()) {
            final int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (!isCaseIgnorable(cp)) {
                return !isCased(cp);
            }
        }
        return true;
    }

    private static boolean isCased(final int cp) {
        return Character.isLowerCase(cp) || Character.isUpperCase(cp) || Character.isTitleCase(cp);
    }

    /**
     * Case_Ignorable: general category Mn, Me, Cf, Lm or Sk, or Word_Break MidLetter, MidNumLet or Single_Quote. The
     * JDK exposes no Word_Break property, so those code points are listed.
     */
    private static boolean isCaseIgnorable(final int cp) {
        switch (Character.getType(cp)) {
        case Character.NON_SPACING_MARK:
        case Character.ENCLOSING_MARK:
        case Character.FORMAT:
        case Character.MODIFIER_LETTER:
        case Character.MODIFIER_SYMBOL:
            return true;
        default:
            break;
        }
        switch (cp) {
        case 0x0027: // Single_Quote
        case 0x002E: // MidNumLet
        case 0x2018:
        case 0x2019:
        case 0x2024:
        case 0xFE52:
        case 0xFF07:
        case 0xFF0E:
        case 0x003A: // MidLetter
        case 0x00B7:
        case 0x0387:
        case 0x055F:
        case 0x05F4:
        case 0x2027:
        case 0xFE13:
        case 0xFE55:
        case 0xFF1A:
            return true;
        default:
            return false;
        }
    }
}
