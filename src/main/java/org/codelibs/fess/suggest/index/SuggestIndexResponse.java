/*
 * Copyright 2012-2024 CodeLibs Project and the Others.
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
    protected final int numberOfSuggestDocs;
    protected final int numberOfInputDocs;
    protected final boolean hasError;
    protected final List<Throwable> errors = new ArrayList<>();
    protected final long took;

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
