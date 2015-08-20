package org.codelibs.fess.suggest.index.contents.querylog;

import java.io.Closeable;

public interface QueryLogReader extends Closeable {
    QueryLog read();

    @Override
    void close();
}
