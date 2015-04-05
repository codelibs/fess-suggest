package org.codelibs.fess.suggest.exception;

public class SuggestorException extends Exception {
    public SuggestorException(String msg) {
        super(msg);
    }

    public SuggestorException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
