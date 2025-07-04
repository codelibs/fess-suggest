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
package org.codelibs.fess.suggest.index;

import java.util.ArrayList;
import java.util.List;

import org.codelibs.fess.suggest.request.Response;

/**
 * Represents the response of a suggest index operation.
 * This class contains information about the number of suggest documents,
 * the number of input documents, any errors that occurred during the operation,
 * and the time taken to complete the operation.
 */
public class SuggestIndexResponse implements Response {
    /** The number of suggest documents. */
    protected final int numberOfSuggestDocs;
    /** The number of input documents. */
    protected final int numberOfInputDocs;
    /** Flag indicating if there are errors. */
    protected final boolean hasError;
    /** List of errors. */
    protected final List<Throwable> errors = new ArrayList<>();
    /** Time taken for the operation in milliseconds. */
    protected final long took;

    /**
     * Constructor for SuggestIndexResponse.
     * @param numberOfSuggestDocs The number of suggest documents.
     * @param numberOfInputDocs The number of input documents.
     * @param errors A list of Throwables representing errors.
     * @param took The time taken for the operation in milliseconds.
     */
    protected SuggestIndexResponse(final int numberOfSuggestDocs, final int numberOfInputDocs, final List<Throwable> errors,
            final long took) {
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

    /**
     * Returns the number of suggest documents.
     * @return The number of suggest documents.
     */
    public int getNumberOfSuggestDocs() {
        return numberOfSuggestDocs;
    }

    /**
     * Returns the number of input documents.
     * @return The number of input documents.
     */
    public int getNumberOfInputDocs() {
        return numberOfInputDocs;
    }

    /**
     * Checks if there are any errors.
     * @return True if there are errors, false otherwise.
     */
    public boolean hasError() {
        return hasError;
    }

    /**
     * Returns the list of errors.
     * @return The list of errors.
     */
    public List<Throwable> getErrors() {
        return errors;
    }

    /**
     * Returns the time taken for the operation.
     * @return The time taken in milliseconds.
     */
    public long getTook() {
        return took;
    }
}
