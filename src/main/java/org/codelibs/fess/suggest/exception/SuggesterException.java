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
 * This class represents a custom exception for the suggester component.
 * It extends the {@link RuntimeException} class and provides constructors
 * to create an exception instance with a message, a cause, or both.
 *
 * <p>Usage examples:</p>
 * <pre>
 * throw new SuggesterException("An error occurred");
 * throw new SuggesterException(new IOException("IO error"));
 * throw new SuggesterException("An error occurred", new IOException("IO error"));
 * </pre>
 *
 * @see RuntimeException
 */
public class SuggesterException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new SuggesterException with the specified detail message.
     * @param msg The detail message.
     */
    public SuggesterException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new SuggesterException with the specified cause.
     * @param cause The cause.
     */
    public SuggesterException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new SuggesterException with the specified detail message and cause.
     * @param msg The detail message.
     * @param cause The cause.
     */
    public SuggesterException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
