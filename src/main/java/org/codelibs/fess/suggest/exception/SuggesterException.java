package org.codelibs.fess.suggest.exception;

public class SuggesterException extends RuntimeException {
    public SuggesterException(String msg) {
        super(msg);
    }

    public SuggesterException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
