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
package org.codelibs.fess.suggest.index.writer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.opensearch.client.Client;
import org.opensearch.index.query.QueryBuilder;

public interface SuggestWriter {
    /**
     * Writes the given suggest items to the specified index.
     *
     * @param client   the client used to interact with the search engine
     * @param settings the settings for the suggest feature
     * @param index    the name of the index where the suggest items will be written
     * @param items    an array of suggest items to be written to the index
     * @param update   a boolean flag indicating whether to update existing items
     * @return a result object containing information about the write operation
     */
    SuggestWriterResult write(Client client, SuggestSettings settings, String index, SuggestItem[] items, boolean update);

    /**
     * Deletes a suggestion from the specified index.
     *
     * @param client   the client to use for the operation
     * @param settings the settings to use for the operation
     * @param index    the name of the index from which to delete the suggestion
     * @param id       the ID of the suggestion to delete
     * @return a result object containing the outcome of the delete operation
     */
    SuggestWriterResult delete(Client client, SuggestSettings settings, String index, String id);

    /**
     * Deletes documents from the specified index based on the given query.
     *
     * @param client the OpenSearch client to use for the operation
     * @param settings the suggest settings to apply
     * @param index the name of the index from which documents will be deleted
     * @param queryBuilder the query that defines which documents to delete
     * @return a result object containing information about the delete operation
     */
    SuggestWriterResult deleteByQuery(Client client, SuggestSettings settings, String index, QueryBuilder queryBuilder);

    /**
     * Merges an array of SuggestItem objects by combining items with the same ID.
     *
     * <p>This method iterates through the provided array of SuggestItem objects and merges
     * items that have the same ID. The merged items are added to a new list, which is then
     * converted back to an array and returned.</p>
     *
     * @param items an array of SuggestItem objects to be merged
     * @return an array of merged SuggestItem objects
     */
    default SuggestItem[] mergeItems(final SuggestItem[] items) {
        final Set<String> mergedIdSet = new HashSet<>();
        final List<SuggestItem> mergedList = new ArrayList<>(items.length);

        for (final SuggestItem item1 : items) {
            final String item1Id = item1.getId();
            if (mergedIdSet.contains(item1Id)) {
                continue;
            }

            SuggestItem mergedItem = item1;
            for (final SuggestItem item2 : items) {
                if (item1.equals(item2)) {
                    continue;
                }

                final String item2Id = item2.getId();
                if (item1Id.equals(item2Id)) {
                    mergedItem = SuggestItem.merge(mergedItem, item2);
                    mergedIdSet.add(item1Id);
                }
            }
            mergedList.add(mergedItem);
        }

        return mergedList.toArray(new SuggestItem[mergedList.size()]);
    }
}
