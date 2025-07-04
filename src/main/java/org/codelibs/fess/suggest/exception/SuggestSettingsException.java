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
 * Exception thrown when there is an issue with the suggest settings.
 * This exception extends {@link RuntimeException}, so it is an unchecked exception.
 *
 * <p>There are three constructors available for this exception:</p>
 * <ul>
 *   <li>{@link #SuggestSettingsException(String)}: Constructs a new exception with the specified detail message.</li>
 *   <li>{@link #SuggestSettingsException(Throwable)}: Constructs a new exception with the specified cause.</li>
 *   <li>{@link #SuggestSettingsException(String, Throwable)}: Constructs a new exception with the specified detail message and cause.</li>
 * </ul>
 *
 * @see RuntimeException
 */
public class SuggestSettingsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new SuggestSettingsException with the specified detail message.
     * @param msg The detail message.
     */
    public SuggestSettingsException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new SuggestSettingsException with the specified cause.
     * @param cause The cause.
     */
    public SuggestSettingsException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new SuggestSettingsException with the specified detail message and cause.
     * @param msg The detail message.
     * @param cause The cause.
     */
    public SuggestSettingsException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
