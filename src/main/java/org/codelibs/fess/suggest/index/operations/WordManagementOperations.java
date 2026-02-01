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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.entity.ElevateWord;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.SuggestDeleteResponse;
import org.codelibs.fess.suggest.index.SuggestIndexResponse;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fess.suggest.util.SuggestUtil;
import org.opensearch.index.query.QueryBuilders;

/**
 * Internal operations class for bad word and elevate word management.
 * Handles adding, deleting, and restoring special words.
 *
 * <p>This class is package-private and intended for internal use by SuggestIndexer.
 */
public class WordManagementOperations {

    private static final Logger logger = LogManager.getLogger(WordManagementOperations.class);

    private final SuggestSettings settings;
    private final Normalizer normalizer;
    private final IndexingOperations indexingOps;
    private final DeletionOperations deletionOps;
    private final Supplier<String[]> badWordsSupplier;

    /**
     * Constructor.
     *
     * @param settings The suggest settings
     * @param normalizer The normalizer for word normalization
     * @param indexingOps The indexing operations for writing items
     * @param deletionOps The deletion operations for removing items
     * @param badWordsSupplier Supplier to get current bad words array (for updating after changes)
     */
    public WordManagementOperations(final SuggestSettings settings, final Normalizer normalizer, final IndexingOperations indexingOps,
            final DeletionOperations deletionOps, final Supplier<String[]> badWordsSupplier) {
        this.settings = settings;
        this.normalizer = normalizer;
        this.indexingOps = indexingOps;
        this.deletionOps = deletionOps;
        this.badWordsSupplier = badWordsSupplier;
    }

    /**
     * Adds a bad word.
     *
     * @param index The index name
     * @param badWord The bad word to add
     * @param apply Whether to apply the change immediately (delete matching items)
     * @return The SuggestDeleteResponse
     */
    public SuggestDeleteResponse addBadWord(final String index, final String badWord, final boolean apply) {
        final String normalized = normalizer.normalize(badWord, "");
        settings.badword().add(normalized);
        if (apply) {
            return deletionOps.deleteByQuery(index, QueryBuilders.wildcardQuery(FieldNames.TEXT, "*" + normalized + "*"));
        }
        return new SuggestDeleteResponse(null, 0);
    }

    /**
     * Deletes a bad word.
     *
     * @param badWord The bad word to delete
     */
    public void deleteBadWord(final String badWord) {
        settings.badword().delete(normalizer.normalize(badWord, ""));
    }

    /**
     * Adds an elevate word.
     *
     * @param index The index name
     * @param elevateWord The elevate word to add
     * @param apply Whether to apply the change immediately (index the word)
     * @return The SuggestIndexResponse
     */
    public SuggestIndexResponse addElevateWord(final String index, final ElevateWord elevateWord, final boolean apply) {
        final String normalizedWord = normalizer.normalize(elevateWord.getElevateWord(), "");
        final List<String> normalizedReadings =
                elevateWord.getReadings().stream().map(reading -> normalizer.normalize(reading, "")).collect(Collectors.toList());
        final ElevateWord normalized = new ElevateWord(normalizedWord, elevateWord.getBoost(), normalizedReadings, elevateWord.getFields(),
                elevateWord.getTags(), elevateWord.getRoles());
        settings.elevateWord().add(normalized);
        if (apply) {
            final SuggestItem item = normalized.toSuggestItem();
            return indexingOps.index(index, item, badWordsSupplier.get());
        }
        return new SuggestIndexResponse(0, 0, null, 0);
    }

    /**
     * Deletes an elevate word.
     *
     * @param index The index name
     * @param elevateWord The elevate word to delete
     * @param apply Whether to apply the change immediately (delete from index)
     * @return The SuggestDeleteResponse
     */
    public SuggestDeleteResponse deleteElevateWord(final String index, final String elevateWord, final boolean apply) {
        final String normalized = normalizer.normalize(elevateWord, "");
        settings.elevateWord().delete(normalized);
        if (apply) {
            return deletionOps.delete(index, SuggestUtil.createSuggestTextId(normalized));
        }
        return new SuggestDeleteResponse(null, 0);
    }

    /**
     * Restores elevate words.
     *
     * @param index The index name
     * @return The SuggestIndexResponse
     */
    public SuggestIndexResponse restoreElevateWord(final String index) {
        if (logger.isInfoEnabled()) {
            logger.info("Restoring elevate words: index={}", index);
        }
        final long start = System.currentTimeMillis();
        int numberOfSuggestDocs = 0;
        int numberOfInputDocs = 0;

        final ElevateWord[] elevateWords = settings.elevateWord().get();
        final List<Throwable> errors = new ArrayList<>(elevateWords.length);
        for (final ElevateWord elevateWord : elevateWords) {
            final SuggestIndexResponse res = addElevateWord(index, elevateWord, true);
            numberOfSuggestDocs += res.getNumberOfSuggestDocs();
            numberOfInputDocs += res.getNumberOfInputDocs();
            errors.addAll(res.getErrors());
        }
        return new SuggestIndexResponse(numberOfSuggestDocs, numberOfInputDocs, errors, System.currentTimeMillis() - start);
    }
}
