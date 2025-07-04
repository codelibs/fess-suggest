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
package org.codelibs.fess.suggest.index.contents.document;

import java.io.Closeable;
import java.util.Map;

/**
 * Interface for reading documents and extracting their contents into a map.
 * Implementations of this interface should provide the logic for reading
 * documents and converting them into a key-value structure.
 *
 * <p>This interface extends {@link java.io.Closeable}, so implementations
 * should also handle resource cleanup when the {@link #close()} method is called.</p>
 */
public interface DocumentReader extends Closeable {
    /**
     * Reads a document and returns its contents as a map.
     *
     * @return a map containing the document's data, or null if there are no more documents to read.
     */
    Map<String, Object> read();

    @Override
    void close();
}
