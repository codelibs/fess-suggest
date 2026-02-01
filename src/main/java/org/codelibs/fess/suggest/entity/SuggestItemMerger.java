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
package org.codelibs.fess.suggest.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.codelibs.fess.suggest.constants.SuggestConstants;

/**
 * Utility class for merging SuggestItem objects.
 * Centralizes merge logic to reduce complexity in SuggestItem.
 *
 * <p>This class provides methods for merging two SuggestItem instances,
 * combining their fields, frequencies, and other attributes while
 * maintaining uniqueness where appropriate.
 */
public final class SuggestItemMerger {

    private SuggestItemMerger() {
        // Utility class
    }

    /**
     * Merges two SuggestItem instances into a new SuggestItem.
     *
     * <p>The merge follows these rules:
     * <ul>
     * <li>IDs must match, or an IllegalArgumentException is thrown</li>
     * <li>Text is taken from item1</li>
     * <li>Readings are merged, maintaining uniqueness</li>
     * <li>Fields, tags, languages, and roles are merged, maintaining uniqueness</li>
     * <li>Kinds are merged, maintaining uniqueness</li>
     * <li>Frequencies (queryFreq, docFreq) are summed</li>
     * <li>Timestamp and userBoost are taken from item2 (newer values)</li>
     * </ul>
     *
     * @param item1 The first SuggestItem (base item)
     * @param item2 The second SuggestItem (item to merge in)
     * @return A new merged SuggestItem
     * @throws IllegalArgumentException if item IDs don't match
     */
    public static SuggestItem merge(final SuggestItem item1, final SuggestItem item2) {
        if (!item1.getId().equals(item2.getId())) {
            throw new IllegalArgumentException("Item id is mismatch.");
        }

        final SuggestItem mergedItem = new SuggestItem();

        mergedItem.setId(item1.getId());
        mergedItem.setText(item1.getText());

        // Merge readings
        final int readingsLength = item1.getText().split(SuggestConstants.TEXT_SEPARATOR).length;
        final String[][] mergedReadings = new String[readingsLength][];
        for (int i = 0; i < readingsLength; i++) {
            mergedReadings[i] = mergeReadings(item1.getReadings(), item2.getReadings(), i);
        }
        mergedItem.setReadings(mergedReadings);

        // Merge arrays maintaining uniqueness
        mergedItem.setFields(mergeStringArrays(item1.getFields(), item2.getFields()));
        mergedItem.setTags(mergeStringArrays(item1.getTags(), item2.getTags()));
        mergedItem.setLanguages(mergeStringArrays(item1.getLanguages(), item2.getLanguages()));
        mergedItem.setRoles(mergeStringArrays(item1.getRoles(), item2.getRoles()));

        // Merge kinds
        mergedItem.setKinds(mergeKinds(item1.getKinds(), item2.getKinds()));

        // Take newer values from item2
        mergedItem.setTimestamp(item2.getTimestamp());
        mergedItem.setUserBoost(item2.getUserBoost());
        mergedItem.setEmptySource(item2.toEmptyMap());

        // Sum frequencies
        mergedItem.setQueryFreq(item1.getQueryFreq() + item2.getQueryFreq());
        mergedItem.setDocFreq(item1.getDocFreq() + item2.getDocFreq());

        return mergedItem;
    }

    /**
     * Merges readings at a specific index from two reading arrays.
     */
    private static String[] mergeReadings(final String[][] readings1, final String[][] readings2, final int index) {
        final List<String> merged = new ArrayList<>();

        if (readings1.length > index && readings1[index] != null) {
            Collections.addAll(merged, readings1[index]);
        }

        if (readings2.length > index && readings2[index] != null) {
            for (final String reading : readings2[index]) {
                if (!merged.contains(reading)) {
                    merged.add(reading);
                }
            }
        }

        return merged.toArray(new String[merged.size()]);
    }

    /**
     * Merges two string arrays, maintaining uniqueness and order.
     */
    private static String[] mergeStringArrays(final String[] array1, final String[] array2) {
        final Set<String> merged = new LinkedHashSet<>();

        if (array1 != null) {
            Collections.addAll(merged, array1);
        }

        if (array2 != null) {
            Collections.addAll(merged, array2);
        }

        return merged.toArray(new String[merged.size()]);
    }

    /**
     * Merges two Kind arrays, maintaining uniqueness.
     */
    private static SuggestItem.Kind[] mergeKinds(final SuggestItem.Kind[] kinds1, final SuggestItem.Kind[] kinds2) {
        final Set<SuggestItem.Kind> merged = new LinkedHashSet<>();

        if (kinds1 != null) {
            Collections.addAll(merged, kinds1);
        }

        if (kinds2 != null) {
            Collections.addAll(merged, kinds2);
        }

        return merged.toArray(new SuggestItem.Kind[merged.size()]);
    }
}
