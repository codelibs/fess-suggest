package jp.sf.fess.suggest.exception;

public class FessSuggestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FessSuggestException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public FessSuggestException(final String message) {
        super(message);
    }

    public FessSuggestException(final Throwable cause) {
        super(cause);
    }
}
