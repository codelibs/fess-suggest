/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.suggest.index.writer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The SuggestWriterResult class is used to store and manage the results of a suggest writer operation.
 * It keeps track of any failures that occur during the operation.
 */
public class SuggestWriterResult {
    /**
     * Constructs a new {@link SuggestWriterResult}.
     */
    public SuggestWriterResult() {
        // nothing
    }

    /**
     * A list of Throwables representing failures that occurred during the operation.
     */
    protected List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

    /**
     * Adds a Throwable to the list of failures.
     *
     * @param t the Throwable to add
     */
    public void addFailure(final Throwable t) {
        failures.add(t);
    }

    /**
     * Checks if there are any failures recorded.
     *
     * @return true if there is at least one failure, false otherwise
     */
    public boolean hasFailure() {
        return !failures.isEmpty();
    }

    /**
     * Returns the list of failures.
     *
     * @return a List of Throwable objects representing the failures
     */
    public List<Throwable> getFailures() {
        return failures;
    }

}
