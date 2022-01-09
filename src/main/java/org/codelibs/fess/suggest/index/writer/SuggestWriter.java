/*
 * Copyright 2012-2022 CodeLibs Project and the Others.
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
    SuggestWriterResult write(Client client, SuggestSettings settings, String index, SuggestItem[] items, boolean update);

    SuggestWriterResult delete(Client client, SuggestSettings settings, String index, String id);

    SuggestWriterResult deleteByQuery(Client client, SuggestSettings settings, String index, QueryBuilder queryBuilder);

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
