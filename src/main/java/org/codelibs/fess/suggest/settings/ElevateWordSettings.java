/*
 * Copyright 2012-2024 CodeLibs Project and the Others.
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
package org.codelibs.fess.suggest.settings;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.entity.ElevateWord;
import org.opensearch.client.Client;

/**
 * The ElevateWordSettings class manages the settings for elevate words in the suggestion system.
 * It provides methods to retrieve, add, and delete elevate words from the settings.
 *
 * <p>Constants:</p>
 * <ul>
 *   <li>{@code ELEVATE_WORD_SETTINGD_KEY} - The key for elevate word settings.</li>
 *   <li>{@code ELEVATE_WORD_BOOST} - The key for elevate word boost value.</li>
 *   <li>{@code ELEVATE_WORD_READING} - The key for elevate word readings.</li>
 *   <li>{@code ELEVATE_WORD_FIELDS} - The key for elevate word fields.</li>
 *   <li>{@code ELEVATE_WORD_TAGS} - The key for elevate word tags.</li>
 *   <li>{@code ELEVATE_WORD_ROLES} - The key for elevate word roles.</li>
 * </ul>
 *
 * <p>Methods:</p>
 * <ul>
 *   <li>{@code get()} - Retrieves an array of elevate words from the settings.</li>
 *   <li>{@code add(ElevateWord elevateWord)} - Adds a new elevate word to the settings.</li>
 *   <li>{@code delete(String elevateWord)} - Deletes an elevate word from the settings.</li>
 *   <li>{@code deleteAll()} - Deletes all elevate words from the settings.</li>
 * </ul>
 *
 * <p>Protected Constructor:</p>
 * <ul>
 *   <li>{@code ElevateWordSettings(SuggestSettings settings, Client client, String settingsIndexName, String settingsId)} -
 *       Initializes the ElevateWordSettings with the provided settings, client, index name, and settings ID.</li>
 * </ul>
 *
 * <p>Dependencies:</p>
 * <ul>
 *   <li>{@code ArraySettings} - Used to manage the array settings for elevate words.</li>
 *   <li>{@code SuggestSettings} - The suggestion settings.</li>
 *   <li>{@code Client} - The client used for accessing the settings.</li>
 * </ul>
 *
 * <p>Logging:</p>
 * <ul>
 *   <li>Uses {@code Logger} to log debug information when adding, deleting, or deleting all elevate words.</li>
 * </ul>
 */
public class ElevateWordSettings {
    private static final Logger logger = LogManager.getLogger(ElevateWordSettings.class);

    public static final String ELEVATE_WORD_SETTINGD_KEY = "elevateword";
    public static final String ELEVATE_WORD_BOOST = "boost";
    public static final String ELEVATE_WORD_READING = "reading";
    public static final String ELEVATE_WORD_FIELDS = "fields";
    public static final String ELEVATE_WORD_TAGS = "tags";
    public static final String ELEVATE_WORD_ROLES = "roles";

    protected ArraySettings arraySettings;

    protected ElevateWordSettings(final SuggestSettings settings, final Client client, final String settingsIndexName,
            final String settingsId) {
        arraySettings = new ArraySettings(settings, client, settingsIndexName, settingsId) {
            @Override
            protected String createArraySettingsIndexName(final String settingsIndexName) {
                return settingsIndexName + "_elevate";
            }
        };
    }

    public ElevateWord[] get() {
        final Map<String, Object>[] sourceArray =
                arraySettings.getFromArrayIndex(arraySettings.arraySettingsIndexName, arraySettings.settingsId, ELEVATE_WORD_SETTINGD_KEY);

        final ElevateWord[] elevateWords = new ElevateWord[sourceArray.length];
        for (int i = 0; i < elevateWords.length; i++) {
            final Object elevateWord = sourceArray[i].get(FieldNames.ARRAY_VALUE);
            final Object boost = sourceArray[i].get(ELEVATE_WORD_BOOST);
            @SuppressWarnings("unchecked")
            final List<String> readings = (List<String>) sourceArray[i].get(ELEVATE_WORD_READING);
            @SuppressWarnings("unchecked")
            final List<String> fields = (List<String>) sourceArray[i].get(ELEVATE_WORD_FIELDS);
            @SuppressWarnings("unchecked")
            final List<String> tags = (List<String>) sourceArray[i].get(ELEVATE_WORD_TAGS);
            @SuppressWarnings("unchecked")
            final List<String> roles = (List<String>) sourceArray[i].get(ELEVATE_WORD_ROLES);
            if (elevateWord != null && boost != null && readings != null && fields != null) {
                elevateWords[i] =
                        new ElevateWord(elevateWord.toString(), Float.parseFloat(boost.toString()), readings, fields, tags, roles);
            }
        }
        return elevateWords;
    }

    public void add(final ElevateWord elevateWord) {
        if (logger.isDebugEnabled()) {
            logger.debug("Add elevateword. {}  elevateword: {}", arraySettings.arraySettingsIndexName, elevateWord.getElevateWord());
        }

        final Map<String, Object> source = new HashMap<>();
        source.put(FieldNames.ARRAY_KEY, ELEVATE_WORD_SETTINGD_KEY);
        source.put(FieldNames.ARRAY_VALUE, elevateWord.getElevateWord());
        source.put(ELEVATE_WORD_BOOST, elevateWord.getBoost());
        source.put(ELEVATE_WORD_READING, elevateWord.getReadings());
        source.put(ELEVATE_WORD_FIELDS, elevateWord.getFields());
        source.put(ELEVATE_WORD_TAGS, elevateWord.getTags());
        source.put(ELEVATE_WORD_ROLES, elevateWord.getRoles());
        source.put(FieldNames.TIMESTAMP, DateTimeFormatter.ISO_INSTANT.format(ZonedDateTime.now()));

        arraySettings.addToArrayIndex(arraySettings.arraySettingsIndexName, arraySettings.settingsId,
                arraySettings.createId(ELEVATE_WORD_SETTINGD_KEY, elevateWord.getElevateWord()), source);
    }

    public void delete(final String elevateWord) {
        if (logger.isDebugEnabled()) {
            logger.debug("Delete elevateword. {} elevateword: {}", arraySettings.arraySettingsIndexName, elevateWord);
        }
        arraySettings.delete(ELEVATE_WORD_SETTINGD_KEY, elevateWord);
    }

    public void deleteAll() {
        if (logger.isDebugEnabled()) {
            logger.debug("Delete all elevateword. {}", arraySettings.arraySettingsIndexName);
        }
        arraySettings.delete(ELEVATE_WORD_SETTINGD_KEY);
    }

}
