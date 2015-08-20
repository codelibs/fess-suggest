package org.codelibs.fess.suggest.exception;

public class SuggestIndexException extends SuggesterException {

    private static final long serialVersionUID = -3792626439756997194L;

    public SuggestIndexException(final String msg) {
        super(msg);
    }

    public SuggestIndexException(final Throwable cause) {
        super(cause);
    }

    public SuggestIndexException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

}
