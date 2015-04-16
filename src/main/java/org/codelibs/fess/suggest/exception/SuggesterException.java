package org.codelibs.fess.suggest.exception;

public class SuggesterException extends Exception {
    public SuggesterException(String msg) {
        super(msg);
    }

    public SuggesterException(Throwable cause) {
        super(cause);
    }

    public SuggesterException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
