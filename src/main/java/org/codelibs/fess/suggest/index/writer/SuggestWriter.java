package org.codelibs.fess.suggest.index.writer;

import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface SuggestWriter {
    SuggestWriterResult write(Client client, SuggestSettings settings, String index, String type, SuggestItem[] items);

    SuggestWriterResult delete(Client client, SuggestSettings settings, String index, String type, String id);

    SuggestWriterResult deleteByQuery(Client client, SuggestSettings settings, String index, String type, QueryBuilder queryBuilder);

    default SuggestItem[] mergeItems(SuggestItem[] items) {
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
