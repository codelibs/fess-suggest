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

/**
 * Represents the response of a suggest delete operation.
 * This class contains information about any errors that occurred during the operation
 * and the time taken to complete the operation.
 */
public class SuggestDeleteResponse {
    /** List of errors that occurred during the operation. */
    protected final List<Throwable> errors = new ArrayList<>();
    /** Time taken for the operation in milliseconds. */
    protected final long took;

    /**
     * Constructor for SuggestDeleteResponse.
     * @param errors A list of Throwables representing errors.
     * @param took The time taken for the operation in milliseconds.
     */
    protected SuggestDeleteResponse(final List<Throwable> errors, final long took) {
        this.took = took;
        if (errors != null && !errors.isEmpty()) {
            this.errors.addAll(errors);
        }
    }

    /**
     * Checks if the response has any errors.
     * @return True if there are errors, false otherwise.
     */
    public boolean hasError() {
        return !errors.isEmpty();
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
