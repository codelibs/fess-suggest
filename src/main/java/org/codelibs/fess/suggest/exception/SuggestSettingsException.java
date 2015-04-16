package org.codelibs.fess.suggest.exception;

public class SuggestSettingsException extends RuntimeException {
    public SuggestSettingsException(String msg) {
        super(msg);
    }

    public SuggestSettingsException(Throwable cause) {
        super(cause);
    }

    public SuggestSettingsException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
