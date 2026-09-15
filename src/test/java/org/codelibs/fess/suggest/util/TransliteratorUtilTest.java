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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.UnaryOperator;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Transliterator;

import junit.framework.TestCase;

/**
 * TransliteratorUtil stands in for the ICU4J transliterators the reading converters used. ICU4J remains a test
 * dependency so that every case here compares against ICU's own output rather than against expectations written by
 * hand. Code points the running JDK does not define are skipped: they belong to a newer Unicode version than the
 * JDK's. Combining marks and look-alike characters are built from code points, because the formatter rewrites
 * Unicode escapes into characters that cannot be told apart on screen.
 */
public class TransliteratorUtilTest extends TestCase {

    private static final Transliterator HIRAGANA_KATAKANA = Transliterator.getInstance("Hiragana-Katakana");

    private static final Transliterator FULLWIDTH_HALFWIDTH = Transliterator.getInstance("Fullwidth-Halfwidth");

    private static final Transliterator ANY_LOWER = Transliterator.getInstance("Any-Lower");

    private static final String CAPITAL_SIGMA = codePoints(0x03A3);

    private static final int MAX_REPORTED = 20;

    public void test_hiraganaToKatakana_examples() {
        assertEquals("ケンサク", TransliteratorUtil.hiraganaToKatakana("けんさく"));
        assertEquals("ケンサク", TransliteratorUtil.hiraganaToKatakana("ｹﾝｻｸ"));
        // HIRAGANA KA followed by COMBINING KATAKANA-HIRAGANA VOICED SOUND MARK composes into KATAKANA GA.
        assertEquals(codePoints(0x30AC), TransliteratorUtil.hiraganaToKatakana(codePoints(0x304B, 0x3099)));
        assertEquals("ヨリ", TransliteratorUtil.hiraganaToKatakana("ゟ"));
        assertEquals("東京タワー", TransliteratorUtil.hiraganaToKatakana("東京たわー"));
        // A CJK compatibility ideograph is outside ICU's filter, so NFKC must not rewrite it into U+585A.
        assertEquals(codePoints(0xFA10), TransliteratorUtil.hiraganaToKatakana(codePoints(0xFA10)));
    }

    public void test_hiraganaToKatakana_everyCodePoint() {
        assertEveryCodePoint(HIRAGANA_KATAKANA, TransliteratorUtil::hiraganaToKatakana);
    }

    public void test_hiraganaToKatakana_pairs() {
        assertPairs(HIRAGANA_KATAKANA, TransliteratorUtil::hiraganaToKatakana);
    }

    public void test_hiraganaToKatakana_randomStrings() {
        assertRandomStrings(HIRAGANA_KATAKANA, TransliteratorUtil::hiraganaToKatakana);
    }

    public void test_fullwidthToHalfwidth_examples() {
        assertEquals("abcd", TransliteratorUtil.fullwidthToHalfwidth("ａｂｃｄ"));
        assertEquals("みかん ﾘﾝｺﾞ", TransliteratorUtil.fullwidthToHalfwidth("みかん" + codePoints(0x3000) + "リンゴ"));
        assertEquals(codePoints(0xFF76, 0xFF9E), TransliteratorUtil.fullwidthToHalfwidth(codePoints(0x30AC)));
    }

    public void test_fullwidthToHalfwidth_everyCodePoint() {
        assertEveryCodePoint(FULLWIDTH_HALFWIDTH, TransliteratorUtil::fullwidthToHalfwidth);
    }

    public void test_fullwidthToHalfwidth_pairs() {
        assertPairs(FULLWIDTH_HALFWIDTH, TransliteratorUtil::fullwidthToHalfwidth);
    }

    public void test_fullwidthToHalfwidth_randomStrings() {
        assertRandomStrings(FULLWIDTH_HALFWIDTH, TransliteratorUtil::fullwidthToHalfwidth);
    }

    public void test_toLowerCase_examples() {
        assertEquals("abcd", TransliteratorUtil.toLowerCase("ABCD"));
        // GREEK CAPITAL SIGMA at the end of a word becomes SMALL FINAL SIGMA.
        assertEquals(codePoints(0x03BF, 0x03B4, 0x03BF, 0x03C2),
                TransliteratorUtil.toLowerCase(codePoints(0x039F, 0x0394, 0x039F, 0x03A3)));
        // A full stop is case-ignorable, so the sigma after "ALPHA ." still counts as final.
        final String alphaStopSigma = codePoints(0x0391, 0x002E, 0x03A3);
        assertEquals(ANY_LOWER.transliterate(alphaStopSigma), TransliteratorUtil.toLowerCase(alphaStopSigma));
    }

    public void test_toLowerCase_everyCodePoint() {
        assertEveryCodePoint(ANY_LOWER, TransliteratorUtil::toLowerCase);
    }

    public void test_toLowerCase_finalSigmaContexts() {
        final List<String> mismatches = new ArrayList<>();
        int count = 0;
        for (int cp = 0; cp <= Character.MAX_CODE_POINT; cp++) {
            if (!isComparable(cp)) {
                continue;
            }
            final String c = codePoints(cp);
            final String[] texts = { "A" + c + CAPITAL_SIGMA, "A" + CAPITAL_SIGMA + c, "A" + CAPITAL_SIGMA + c + "B", c + CAPITAL_SIGMA,
                    "A" + c + CAPITAL_SIGMA + "B" };
            for (final String text : texts) {
                count += compare(ANY_LOWER, TransliteratorUtil::toLowerCase, text, mismatches);
            }
        }
        assertNoMismatch(count, mismatches);
    }

    public void test_toLowerCase_randomStrings() {
        assertRandomStrings(ANY_LOWER, TransliteratorUtil::toLowerCase);
    }

    private static void assertEveryCodePoint(final Transliterator icu, final UnaryOperator<String> util) {
        final List<String> mismatches = new ArrayList<>();
        int count = 0;
        for (int cp = 0; cp <= Character.MAX_CODE_POINT; cp++) {
            if (isComparable(cp)) {
                count += compare(icu, util, codePoints(cp), mismatches);
            }
        }
        assertNoMismatch(count, mismatches);
    }

    private static void assertPairs(final Transliterator icu, final UnaryOperator<String> util) {
        final List<String> chars = new ArrayList<>();
        for (int cp = 0x0020; cp <= 0x007E; cp++) {
            chars.add(codePoints(cp));
        }
        for (int cp = 0x3000; cp <= 0x30FF; cp++) {
            chars.add(codePoints(cp));
        }
        for (int cp = 0xFF00; cp <= 0xFFEF; cp++) {
            chars.add(codePoints(cp));
        }
        for (final int cp : new int[] { 0x0301, 0x0307, 0x0345, 0x03A3, 0x1100, 0x3131, 0x32D0, 0x3300, 0xFA10, 0x6F22 }) {
            chars.add(codePoints(cp));
        }
        final List<String> mismatches = new ArrayList<>();
        int count = 0;
        for (final String a : chars) {
            for (final String b : chars) {
                count += compare(icu, util, a + b, mismatches);
            }
        }
        assertNoMismatch(count, mismatches);
    }

    private static void assertRandomStrings(final Transliterator icu, final UnaryOperator<String> util) {
        final List<Integer> defined = new ArrayList<>();
        for (int cp = 0; cp <= Character.MAX_CODE_POINT; cp++) {
            if (isComparable(cp)) {
                defined.add(cp);
            }
        }
        final int[] frequent = { 0x3042, 0x304B, 0x308F, 0x3099, 0x309A, 0x309B, 0x30AB, 0x30FC, 0xFF76, 0xFF9E, 0xFF9F, 0xFF21, 0x3000,
                0x0041, 0x0065, 0x0301, 0x0307, 0x0345, 0x03A3, 0x002E, 0x0027, 0x0020, 0x32D0, 0x3300, 0xFA10, 0x6F22 };
        final Random random = new Random(20260915L);
        final List<String> mismatches = new ArrayList<>();
        int count = 0;
        for (int i = 0; i < 100_000; i++) {
            final StringBuilder buf = new StringBuilder();
            final int length = 1 + random.nextInt(8);
            for (int j = 0; j < length; j++) {
                buf.appendCodePoint(i % 2 == 0 ? frequent[random.nextInt(frequent.length)] : defined.get(random.nextInt(defined.size())));
            }
            count += compare(icu, util, buf.toString(), mismatches);
        }
        assertNoMismatch(count, mismatches);
    }

    /**
     * TransliteratorUtil reads Unicode data from the JDK: the general categories it tests, the Lowercase and Uppercase
     * properties, and the Hiragana and Katakana scripts. Where the running JDK's Unicode version disagrees with
     * ICU4J's on that data, the implementation follows the JDK by design, so those code points are not compared.
     * Logic derived from the data, such as the case-ignorable list, is deliberately left out, so that a mistake there
     * still fails.
     */
    private static final boolean[] UNICODE_DATA_DIFFERS = new boolean[Character.MAX_CODE_POINT + 1];

    private static final List<String> UNICODE_DATA_DIFFERENCES = findUnicodeDataDifferences();

    public void test_unicodeDataDifferences_areFew() {
        assertTrue("JDK and ICU4J disagree on " + UNICODE_DATA_DIFFERENCES, UNICODE_DATA_DIFFERENCES.size() <= 16);
    }

    private static List<String> findUnicodeDataDifferences() {
        final int[][] categories = { { Character.NON_SPACING_MARK, UCharacterCategory.NON_SPACING_MARK },
                { Character.ENCLOSING_MARK, UCharacterCategory.ENCLOSING_MARK }, { Character.FORMAT, UCharacterCategory.FORMAT },
                { Character.MODIFIER_LETTER, UCharacterCategory.MODIFIER_LETTER },
                { Character.MODIFIER_SYMBOL, UCharacterCategory.MODIFIER_SYMBOL },
                { Character.TITLECASE_LETTER, UCharacterCategory.TITLECASE_LETTER } };
        final List<String> differences = new ArrayList<>();
        for (int cp = 0; cp <= Character.MAX_CODE_POINT; cp++) {
            if (!Character.isDefined(cp)) {
                continue;
            }
            boolean differs = Character.isLowerCase(cp) != UCharacter.hasBinaryProperty(cp, UProperty.LOWERCASE)
                    || Character.isUpperCase(cp) != UCharacter.hasBinaryProperty(cp, UProperty.UPPERCASE)
                    || (Character.UnicodeScript.of(cp) == Character.UnicodeScript.HIRAGANA) != (UScript.getScript(cp) == UScript.HIRAGANA)
                    || (Character.UnicodeScript.of(cp) == Character.UnicodeScript.KATAKANA) != (UScript.getScript(cp) == UScript.KATAKANA);
            for (final int[] category : categories) {
                differs |= (Character.getType(cp) == category[0]) != (UCharacter.getType(cp) == category[1]);
            }
            if (differs) {
                UNICODE_DATA_DIFFERS[cp] = true;
                differences.add(String.format("U+%04X", cp));
            }
        }
        return differences;
    }

    private static boolean isComparable(final int cp) {
        return Character.isDefined(cp) && Character.getType(cp) != Character.SURROGATE && !UNICODE_DATA_DIFFERS[cp];
    }

    private static String codePoints(final int... cps) {
        return new String(cps, 0, cps.length);
    }

    private static int compare(final Transliterator icu, final UnaryOperator<String> util, final String text,
            final List<String> mismatches) {
        final String expected = icu.transliterate(text);
        final String actual = util.apply(text);
        if (expected.equals(actual)) {
            return 0;
        }
        if (mismatches.size() < MAX_REPORTED) {
            mismatches.add("input=[" + hex(text) + "] icu=[" + hex(expected) + "] actual=[" + hex(actual) + "]");
        }
        return 1;
    }

    private static void assertNoMismatch(final int count, final List<String> mismatches) {
        if (count > 0) {
            fail(count + " inputs differ from ICU, for example:\n  " + String.join("\n  ", mismatches));
        }
    }

    private static String hex(final String text) {
        final StringBuilder buf = new StringBuilder();
        text.codePoints().forEach(cp -> buf.append(String.format("U+%04X ", cp)));
        return buf.toString().trim();
    }
}
