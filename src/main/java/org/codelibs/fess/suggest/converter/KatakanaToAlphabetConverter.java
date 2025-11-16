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
package org.codelibs.fess.suggest.converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.icu.text.Transliterator;

/**
 * Converts Katakana characters to their corresponding Alphabet representations.
 *
 * <p>
 * This class implements the {@link ReadingConverter} interface and provides a method to convert a given
 * Katakana string into a list of possible Alphabet readings. It uses a predefined mapping of Katakana
 * characters to their Alphabet equivalents, handling both single and double Katakana character combinations.
 * </p>
 *
 * <p>
 * The conversion process involves iterating through the input string, identifying Katakana characters,
 * and replacing them with their corresponding Alphabet representations based on the internal mapping.
 * When a Katakana character has multiple Alphabet representations, the converter generates multiple
 * possible readings.
 * </p>
 *
 * <p>
 * The class also utilizes ICU4J's {@link com.ibm.icu.text.Transliterator} to convert full-width characters
 * to half-width and to convert any characters to lowercase, ensuring consistency in the output.
 * </p>
 *
 * <p>
 * Example:
 * For the input "キャ", the converter might return a list containing "kya".
 * For the input "ジュ", the converter might return a list containing "zyu", "ju", and "jyu".
 * </p>
 *
 */
public class KatakanaToAlphabetConverter implements ReadingConverter {
    /** Static conversion map shared across all instances. */
    private static final Map<String, String[]> CONVERT_MAP = generateConvertMapping();

    /** Transliterator for full-width to half-width conversion. */
    protected Transliterator fullWidthHalfWidth;

    /** Transliterator for any-lower. */
    protected Transliterator anyLower;

    /**
     * Constructor for KatakanaToAlphabetConverter.
     */
    public KatakanaToAlphabetConverter() {
        fullWidthHalfWidth = Transliterator.getInstance("Fullwidth-Halfwidth");
        anyLower = Transliterator.getInstance("Any-Lower");
    }

    @Override
    public void init() throws IOException {
        // nothing
    }

    @Override
    public List<String> convert(final String text, final String field, final String... lang) {
        final List<String> list = new ArrayList<>();

        final List<StringBuilder> bufList = new ArrayList<>();
        bufList.add(new StringBuilder());
        for (int i = 0; i < text.length();) {
            String[] alphabets;
            if (i + 1 < text.length() && CONVERT_MAP.get(text.substring(i, i + 2)) != null) {
                alphabets = CONVERT_MAP.get(text.substring(i, i + 2));
                i += 2;
            } else {
                if (CONVERT_MAP.get(text.substring(i, i + 1)) != null) {
                    alphabets = CONVERT_MAP.get(text.substring(i, i + 1));
                } else {
                    alphabets = new String[] { text.substring(i, i + 1) };
                }
                i++;
            }

            final List<StringBuilder> originBufList = deepCopyBufList(bufList);
            for (int j = 0; j < alphabets.length; j++) {
                if (j == 0) {
                    for (final StringBuilder buf : bufList) {
                        buf.append(alphabets[j]);
                    }
                } else if (bufList.size() < getMaxReadingNum()) {
                    final List<StringBuilder> tmpBufList = deepCopyBufList(originBufList);
                    for (final StringBuilder buf : tmpBufList) {
                        buf.append(alphabets[j]);
                    }
                    bufList.addAll(tmpBufList);
                }
            }
        }

        for (final StringBuilder buf : bufList) {
            String s = fullWidthHalfWidth.transliterate(buf.toString());
            s = anyLower.transliterate(s);
            list.add(s);
        }

        return list;
    }

    private static Map<String, String[]> generateConvertMapping() {
        final Map<String, String[]> map = new HashMap<>();

        map.put("ア", new String[] { "a" });
        map.put("イ", new String[] { "i" });
        map.put("ウ", new String[] { "u" });
        map.put("エ", new String[] { "e" });
        map.put("オ", new String[] { "o" });

        map.put("カ", new String[] { "ka" });
        map.put("キ", new String[] { "ki" });
        map.put("ク", new String[] { "ku" });
        map.put("ケ", new String[] { "ke" });
        map.put("コ", new String[] { "ko" });

        map.put("サ", new String[] { "sa" });
        map.put("シ", new String[] { "si", "shi" });
        map.put("ス", new String[] { "su" });
        map.put("セ", new String[] { "se" });
        map.put("ソ", new String[] { "so" });

        map.put("タ", new String[] { "ta" });
        map.put("チ", new String[] { "ti", "chi" });
        map.put("ツ", new String[] { "tu", "tsu" });
        map.put("テ", new String[] { "te" });
        map.put("ト", new String[] { "to" });

        map.put("ナ", new String[] { "na" });
        map.put("ニ", new String[] { "ni" });
        map.put("ヌ", new String[] { "nu" });
        map.put("ネ", new String[] { "ne" });
        map.put("ノ", new String[] { "no" });

        map.put("ハ", new String[] { "ha" });
        map.put("ヒ", new String[] { "hi" });
        map.put("フ", new String[] { "hu", "fu" });
        map.put("ヘ", new String[] { "he" });
        map.put("ホ", new String[] { "ho" });

        map.put("マ", new String[] { "ma" });
        map.put("ミ", new String[] { "mi" });
        map.put("ム", new String[] { "mu" });
        map.put("メ", new String[] { "me" });
        map.put("モ", new String[] { "mo" });

        map.put("ヤ", new String[] { "ya" });
        map.put("ユ", new String[] { "yu" });
        map.put("ヨ", new String[] { "yo" });

        map.put("ラ", new String[] { "ra" });
        map.put("リ", new String[] { "ri" });
        map.put("ル", new String[] { "ru" });
        map.put("レ", new String[] { "re" });
        map.put("ロ", new String[] { "ro" });

        map.put("ワ", new String[] { "wa" });
        map.put("ヲ", new String[] { "wo" });
        map.put("ン", new String[] { "nn" });

        map.put("ウィ", new String[] { "wi" });
        map.put("ウェ", new String[] { "we" });
        map.put("ウォ", new String[] { "wo" });

        map.put("ガ", new String[] { "ga" });
        map.put("ギ", new String[] { "gi" });
        map.put("グ", new String[] { "gu" });
        map.put("ゲ", new String[] { "ge" });
        map.put("ゴ", new String[] { "go" });

        map.put("ザ", new String[] { "za" });
        map.put("ジ", new String[] { "zi", "ji" });
        map.put("ズ", new String[] { "zu" });
        map.put("ゼ", new String[] { "ze" });
        map.put("ゾ", new String[] { "zo" });

        map.put("ダ", new String[] { "da" });
        map.put("ヂ", new String[] { "di" });
        map.put("ヅ", new String[] { "du" });
        map.put("デ", new String[] { "de" });
        map.put("ド", new String[] { "do" });

        map.put("ティ", new String[] { "ti", "thi" });
        map.put("ディ", new String[] { "di", "dhi" });
        map.put("デュ", new String[] { "dyu" });
        map.put("トゥ", new String[] { "tu", "twu" });
        map.put("ドゥ", new String[] { "du", "dwu" });

        map.put("バ", new String[] { "ba" });
        map.put("ビ", new String[] { "bi" });
        map.put("ブ", new String[] { "bu" });
        map.put("ベ", new String[] { "be" });
        map.put("ボ", new String[] { "bo" });

        map.put("パ", new String[] { "pa" });
        map.put("ピ", new String[] { "pi" });
        map.put("プ", new String[] { "pu" });
        map.put("ペ", new String[] { "pe" });
        map.put("ポ", new String[] { "po" });

        map.put("ヴァ", new String[] { "va" });
        map.put("ヴィ", new String[] { "vi" });
        map.put("ヴ", new String[] { "vu" });
        map.put("ヴェ", new String[] { "ve" });
        map.put("ヴォ", new String[] { "vo" });

        map.put("ギャ", new String[] { "gya" });
        map.put("ギュ", new String[] { "gyu" });
        map.put("ギョ", new String[] { "gyo" });
        map.put("ギェ", new String[] { "gye" });

        map.put("ジャ", new String[] { "zya", "ja", "jya" });
        map.put("ジュ", new String[] { "zyu", "ju", "jyu" });
        map.put("ジョ", new String[] { "zyo", "jo", "jyo" });
        map.put("ジェ", new String[] { "zye", "je", "jye" });

        map.put("キャ", new String[] { "kya" });
        map.put("キュ", new String[] { "kyu" });
        map.put("キョ", new String[] { "kyo" });

        map.put("シャ", new String[] { "sya", "sha" });
        map.put("シュ", new String[] { "syu", "shu" });
        map.put("ショ", new String[] { "syo", "sho" });
        map.put("シェ", new String[] { "sye", "she" });

        map.put("チャ", new String[] { "tya", "cha" });
        map.put("チュ", new String[] { "tyu", "chu" });
        map.put("チョ", new String[] { "tyo", "cho" });
        map.put("チェ", new String[] { "tye", "che" });

        map.put("ニャ", new String[] { "nya" });
        map.put("ニュ", new String[] { "nyu" });
        map.put("ニョ", new String[] { "nyo" });

        map.put("ヒャ", new String[] { "hya" });
        map.put("ヒュ", new String[] { "hyu" });
        map.put("ヒョ", new String[] { "hyo" });

        map.put("フャ", new String[] { "fya" });
        map.put("フュ", new String[] { "fyu" });
        map.put("フョ", new String[] { "fyo" });

        map.put("ファ", new String[] { "fa" });
        map.put("フィ", new String[] { "fi" });
        map.put("フェ", new String[] { "fe" });
        map.put("フォ", new String[] { "fo" });

        map.put("ミャ", new String[] { "mya" });
        map.put("ミュ", new String[] { "myu" });
        map.put("ミョ", new String[] { "myo" });

        map.put("リャ", new String[] { "rya" });
        map.put("リュ", new String[] { "ryu" });
        map.put("リョ", new String[] { "ryo" });

        map.put("ァ", new String[] { "a" });
        map.put("ィ", new String[] { "i" });
        map.put("ゥ", new String[] { "u" });
        map.put("ェ", new String[] { "e" });
        map.put("ォ", new String[] { "o" });
        map.put("ャ", new String[] { "ya" });
        map.put("ュ", new String[] { "yu" });
        map.put("ョ", new String[] { "yo" });
        map.put("ッ", new String[] { "tu", "tsu" });

        map.put("ツァ", new String[] { "tsa" });
        map.put("ツィ", new String[] { "tsi" });
        map.put("ツェ", new String[] { "tse" });
        map.put("ツォ", new String[] { "tso" });

        return map;
    }

    private List<StringBuilder> deepCopyBufList(final List<StringBuilder> bufList) {
        final List<StringBuilder> list = new ArrayList<>();
        bufList.forEach(buf -> list.add(new StringBuilder(buf.toString())));
        return list;
    }

}