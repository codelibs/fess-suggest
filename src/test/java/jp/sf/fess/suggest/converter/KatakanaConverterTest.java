package jp.sf.fess.suggest.converter;


import junit.framework.TestCase;

public class KatakanaConverterTest extends TestCase {
    public void test_toKatakana() {
        KatakanaConverter converter = new KatakanaConverter();
        converter.start();
        try {
            assertEquals("アイウエオ", converter.convert("あいうえお").get(0));
            assertEquals("アイウエオ", converter.convert("ｱｲｳｴｵ").get(0));
            assertEquals("ザジズゼゾ", converter.convert("ｻﾞｼﾞｽﾞｾﾞｿﾞ").get(0));
            assertEquals("ケンサクエンジン", converter.convert("検索エンジン").get(0));
            assertEquals("apple", converter.convert("apple").get(0));
            assertEquals("ミカン リンゴ", converter.convert("みかん りんご").get(0));
            assertEquals("appleジュース", converter.convert("appleじゅーす").get(0));
            assertEquals("1234ジャ1234", converter.convert("1234じゃ1234").get(0));
            assertEquals("スモモモモモモモモノウチ", converter.convert("すもももももももものうち").get(0));
            assertEquals("トナリノキャクハヨクカキクウキャクダ。イヌモアルケバボウニアタル。",
                    converter.convert("隣の客はよく柿くう客だ。犬もあるけば棒にあたる。").get(0));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
