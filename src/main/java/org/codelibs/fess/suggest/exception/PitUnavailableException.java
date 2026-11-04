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
package org.codelibs.fess.suggest.exception;

/**
 * Exception thrown when a point in time (PIT) context could not be created because no shard of
 * the target index is available.
 *
 * <p>This case is common right after another process has created the index: the search engine
 * reports the index as yellow before its primary shard has actually started, so a PIT created on
 * it is answered without an ID. A dedicated subclass lets callers retry this specific failure
 * while it is still starting, instead of having to parse the error message.</p>
 *
 * @see SuggesterException
 * @see org.codelibs.fess.suggest.util.PitOperationHelper#createPit
 * @see org.codelibs.fess.suggest.util.PitOperationHelper#readWithRetry
 */
public class PitUnavailableException extends SuggesterException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new PitUnavailableException with the specified detail message.
     * @param msg The detail message.
     */
    public PitUnavailableException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new PitUnavailableException with the specified cause.
     * @param cause The cause.
     */
    public PitUnavailableException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new PitUnavailableException with the specified detail message and cause.
     * @param msg The detail message.
     * @param cause The cause.
     */
    public PitUnavailableException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
