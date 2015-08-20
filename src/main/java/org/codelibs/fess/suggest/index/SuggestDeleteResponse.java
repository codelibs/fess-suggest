package org.codelibs.fess.suggest.index;

import java.util.ArrayList;
import java.util.List;

public class SuggestDeleteResponse {
    protected final List<Throwable> errors = new ArrayList<>();
    protected final long took;

    protected SuggestDeleteResponse(final List<Throwable> errors, final long took) {
        this.took = took;
        if (errors != null && !errors.isEmpty()) {
            this.errors.addAll(errors);
        }
    }

    public boolean hasError() {
        return !errors.isEmpty();
    }

    public List<Throwable> getErrors() {
        return errors;
    }

    public long getTook() {
        return took;
    }
}
