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

/**
 * Represents the response of a suggest delete operation.
 * This class contains information about any errors that occurred during the operation
 * and the time taken to complete the operation.
 */
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
