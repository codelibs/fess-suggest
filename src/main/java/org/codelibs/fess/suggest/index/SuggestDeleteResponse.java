package org.codelibs.fess.suggest.index;

import java.util.ArrayList;
import java.util.List;

public class SuggestDeleteResponse {
    protected final boolean hasError;
    protected final List<Throwable> errors = new ArrayList<>();
    protected final long took;

    protected SuggestDeleteResponse(final List<Throwable> errors, final long took) {
        this.took = took;
        if (errors == null || errors.isEmpty()) {
            hasError = false;
        } else {
            hasError = true;
            errors.forEach(this.errors::add);
        }
    }

    public boolean hasError() {
        return hasError;
    }

    public List<Throwable> getErrors() {
        return errors;
    }

    public long getTook() {
        return took;
    }
}
