/*
 * Copyright 2012-2023 CodeLibs Project and the Others.
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
