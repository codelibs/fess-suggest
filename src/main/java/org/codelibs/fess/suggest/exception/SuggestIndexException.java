package org.codelibs.fess.suggest.exception;

public class SuggestIndexException extends SuggesterException {
    public SuggestIndexException(String msg) {
        super(msg);
    }

    public SuggestIndexException(Throwable cause) {
        super(cause);
    }

    public SuggestIndexException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
