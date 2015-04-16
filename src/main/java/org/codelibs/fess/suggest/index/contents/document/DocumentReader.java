package org.codelibs.fess.suggest.index.contents.document;

import java.util.Map;

public interface DocumentReader {
    Map<String, Object> read();

    void close();
}
