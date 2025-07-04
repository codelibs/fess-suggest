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
 * Exception thrown when there is an issue with the suggestion index.
 * This exception extends {@link SuggesterException}.
 *
 * <p>This exception can be thrown with a message, a cause, or both.</p>
 *
 * <pre>
 * Example usage:
 * throw new SuggestIndexException("Indexing error occurred");
 * throw new SuggestIndexException(new IOException("IO error"));
 * throw new SuggestIndexException("Indexing error", new IOException("IO error"));
 * </pre>
 *
 * @see SuggesterException
 */
public class SuggestIndexException extends SuggesterException {

    private static final long serialVersionUID = -3792626439756997194L;

    /**
     * Constructs a new SuggestIndexException with the specified detail message.
     * @param msg The detail message.
     */
    public SuggestIndexException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new SuggestIndexException with the specified cause.
     * @param cause The cause.
     */
    public SuggestIndexException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new SuggestIndexException with the specified detail message and cause.
     * @param msg The detail message.
     * @param cause The cause.
     */
    public SuggestIndexException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

}
