package org.codelibs.fess.suggest.index.contents.document;

import java.io.Closeable;
import java.util.Map;

public interface DocumentReader extends Closeable {
    Map<String, Object> read();

    @Override
    void close();
}
