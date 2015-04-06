package org.codelibs.fess.suggest.converter;

import java.util.List;

public interface ReadingConverter {
    default int getMaxReadingNum() {
        return 10;
    }

    List<String> convert(String text);
}
