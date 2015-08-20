package org.codelibs.fess.suggest.index;

import java.util.ArrayList;
import java.util.List;

import org.codelibs.fess.suggest.request.Response;

public class SuggestIndexResponse implements Response {
    protected final int numberOfSuggestDocs;
    protected final int numberOfInputDocs;
    protected final boolean hasError;
    protected final List<Throwable> errors = new ArrayList<>();
    protected final long took;

    protected SuggestIndexResponse(final int numberOfSuggestDocs, final int numberOfInputDocs, final List<Throwable> errors, final long took) {
        this.numberOfSuggestDocs = numberOfSuggestDocs;
        this.numberOfInputDocs = numberOfInputDocs;
        this.took = took;
        if (errors == null || errors.isEmpty()) {
            hasError = false;
        } else {
            hasError = true;
            errors.forEach(this.errors::add);
        }
    }

    public int getNumberOfSuggestDocs() {
        return numberOfSuggestDocs;
    }

    public int getNumberOfInputDocs() {
        return numberOfInputDocs;
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
