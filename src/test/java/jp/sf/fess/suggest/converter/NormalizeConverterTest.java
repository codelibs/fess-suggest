package jp.sf.fess.suggest.converter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class NormalizeConverterTest {

    @Test
    public void convert() {
        final NormalizeConverter normalizeConveter = new NormalizeConverter();
        assertThat(normalizeConveter.convert("123ABC１２３ＡＢＣアイウエオｱｲｳｴｵ"),
                is("123ABC123ABCアイウエオアイウエオ"));
    }
}
