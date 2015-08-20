package org.codelibs.fess.suggest.index.writer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SuggestWriterResult {
    protected List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

    public void addFailure(final Throwable t) {
        failures.add(t);
    }

    public boolean hasFailure() {
        return !failures.isEmpty();
    }

    public List<Throwable> getFailures() {
        return failures;
    }

}
