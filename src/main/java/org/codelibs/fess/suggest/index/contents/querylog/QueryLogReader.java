package org.codelibs.fess.suggest.index.contents.querylog;

public interface QueryLogReader {
    QueryLog read();

    void close();
}
