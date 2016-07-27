package org.codelibs.fess.suggest.converter;

import java.io.IOException;
import java.util.List;

public interface ReadingConverter {
    default int getMaxReadingNum() {
        return 10;
    }

    void init() throws IOException;

    List<String> convert(String text, String lang) throws IOException;
}
