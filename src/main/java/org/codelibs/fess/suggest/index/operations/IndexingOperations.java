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
package org.codelibs.fess.suggest.index.operations;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggestIndexException;
import org.codelibs.fess.suggest.index.SuggestIndexResponse;
import org.codelibs.fess.suggest.index.writer.SuggestWriter;
import org.codelibs.fess.suggest.index.writer.SuggestWriterResult;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.opensearch.transport.client.Client;

/**
 * Internal operations class for core indexing functionality.
 * Handles indexing of SuggestItem objects with bad word filtering.
 *
 * <p>This class is package-private and intended for internal use by SuggestIndexer.
 */
public class IndexingOperations {

    private static final Logger logger = LogManager.getLogger(IndexingOperations.class);

    private final Client client;
    private final SuggestSettings settings;
    private final SuggestWriter suggestWriter;

    /**
     * Constructor.
     *
     * @param client The OpenSearch client
     * @param settings The suggest settings
     * @param suggestWriter The suggest writer for performing writes
     */
    public IndexingOperations(final Client client, final SuggestSettings settings, final SuggestWriter suggestWriter) {
        this.client = client;
        this.settings = settings;
        this.suggestWriter = suggestWriter;
    }

    /**
     * Indexes a single suggest item.
     *
     * @param index The index name
     * @param item The suggest item to index
     * @param badWords The list of bad words to filter against
     * @return The SuggestIndexResponse
     */
    public SuggestIndexResponse index(final String index, final SuggestItem item, final String[] badWords) {
        return index(index, new SuggestItem[] { item }, badWords);
    }

    /**
     * Indexes multiple suggest items with bad word filtering.
     *
     * @param index The index name
     * @param items The suggest items to index
     * @param badWords The list of bad words to filter against
     * @return The SuggestIndexResponse
     */
    public SuggestIndexResponse index(final String index, final SuggestItem[] items, final String[] badWords) {
        final SuggestItem[] filteredItems = Stream.of(items).filter(item -> !item.isBadWord(badWords)).toArray(SuggestItem[]::new);

        if (logger.isDebugEnabled()) {
            logger.debug("Indexing suggest items: index={}, totalItems={}, validItems={}, filteredByBadWords={}", index, items.length,
                    filteredItems.length, items.length - filteredItems.length);
        }

        try {
            final long start = System.currentTimeMillis();
            final SuggestWriterResult result = suggestWriter.write(client, settings, index, filteredItems, true);
            return new SuggestIndexResponse(items.length, items.length, result.getFailures(), System.currentTimeMillis() - start);
        } catch (final Exception e) {
            throw new SuggestIndexException("Failed to write items[" + items.length + "] to " + index, e);
        }
    }

    /**
     * Gets the SuggestWriter instance.
     *
     * @return The suggest writer
     */
    public SuggestWriter getSuggestWriter() {
        return suggestWriter;
    }
}
