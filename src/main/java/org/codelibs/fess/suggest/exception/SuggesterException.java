package org.codelibs.fess.suggest.exception;

public class SuggesterException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SuggesterException(final String msg) {
        super(msg);
    }

    public SuggesterException(final Throwable cause) {
        super(cause);
    }

    public SuggesterException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
