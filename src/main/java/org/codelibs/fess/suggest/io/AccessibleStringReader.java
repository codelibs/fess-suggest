package org.codelibs.fess.suggest.io;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;

public class AccessibleStringReader extends Reader {
    private Reader reader;

    private String str;

    public AccessibleStringReader(final String s) {
        super();
        reader = new StringReader(s);
        str = s;
    }

    public String getString() {
        return str;
    }

    public void setString(final String s) {
        reader = new StringReader(s);
        str = s;
    }

    @Override
    public int hashCode() {
        return reader.hashCode();
    }

    @Override
    public int read(final CharBuffer target) throws IOException {
        return reader.read(target);
    }

    @Override
    public int read() throws IOException {
        return reader.read();
    }

    @Override
    public int read(final char[] cbuf) throws IOException {
        return reader.read(cbuf);
    }

    @Override
    public boolean equals(final Object obj) {
        return reader.equals(obj);
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        return reader.read(cbuf, off, len);
    }

    @Override
    public long skip(final long n) throws IOException {
        return reader.skip(n);
    }

    @Override
    public boolean ready() throws IOException {
        return reader.ready();
    }

    @Override
    public boolean markSupported() {
        return reader.markSupported();
    }

    @Override
    public void mark(final int readAheadLimit) throws IOException {
        reader.mark(readAheadLimit);
    }

    @Override
    public void reset() throws IOException {
        reader.reset();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public String toString() {
        return reader.toString();
    }
}
