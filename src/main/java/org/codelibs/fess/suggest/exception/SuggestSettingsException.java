package org.codelibs.fess.suggest.exception;

public class SuggestSettingsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SuggestSettingsException(final String msg) {
        super(msg);
    }

    public SuggestSettingsException(final Throwable cause) {
        super(cause);
    }

    public SuggestSettingsException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
