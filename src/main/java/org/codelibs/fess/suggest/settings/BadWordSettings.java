/*
 * Copyright 2012-2023 CodeLibs Project and the Others.
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.fess.suggest.exception.SuggestSettingsException;
import org.opensearch.client.Client;
import org.opensearch.core.common.Strings;

public class BadWordSettings {
    private static final Logger logger = LogManager.getLogger(BadWordSettings.class);

    public static final String BAD_WORD_SETTINGD_KEY = "badword";

    protected ArraySettings arraySettings;

    protected static String[] defaultWords = null;

    protected BadWordSettings(final SuggestSettings settings, final Client client, final String settingsIndexName,
            final String settingsId) {
        this.arraySettings = new ArraySettings(settings, client, settingsIndexName, settingsId) {
            @Override
            protected String createArraySettingsIndexName(final String settingsIndexName) {
                return settingsIndexName + "_badword";
            }
        };
    }

    public String[] get(final boolean includeDefault) {
        final String[] badWords = arraySettings.get(BAD_WORD_SETTINGD_KEY);
        if (!includeDefault) {
            return badWords;
        }

        if (defaultWords == null) {
            updateDefaultBadwords();
        }
        final String[] concat = new String[defaultWords.length + badWords.length];
        System.arraycopy(badWords, 0, concat, 0, badWords.length);
        System.arraycopy(defaultWords, 0, concat, badWords.length, defaultWords.length);
        return concat;
    }

    public void add(final String badWord) {
        if (logger.isDebugEnabled()) {
            logger.debug("Add badword. {} badword: {}", arraySettings.arraySettingsIndexName, badWord);
        }

        final String validationError = getValidationError(badWord);
        if (validationError != null) {
            throw new IllegalArgumentException("Validation error. " + validationError);
        }
        arraySettings.add(BAD_WORD_SETTINGD_KEY, badWord);
    }

    public void delete(final String badWord) {
        if (logger.isDebugEnabled()) {
            logger.debug("Delete badword. {} badword: {}", arraySettings.arraySettingsIndexName, badWord);
        }
        arraySettings.delete(BAD_WORD_SETTINGD_KEY, badWord);
    }

    public void deleteAll() {
        if (logger.isDebugEnabled()) {
            logger.debug("Add all badword. {}", arraySettings.arraySettingsIndexName);
        }
        arraySettings.delete(BAD_WORD_SETTINGD_KEY);
    }

    protected String getValidationError(final String badWord) {
        if (Strings.isNullOrEmpty(badWord)) {
            return "badWord was empty.";
        }
        if (badWord.contains(" ") || badWord.contains("  ")) {
            return "badWord contains space.";
        }
        return null;
    }

    protected static void updateDefaultBadwords() {
        if (defaultWords != null) {
            return;
        }

        final List<String> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("suggest_settings/default-badwords.txt")))) {

            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() > 0 && !line.startsWith("#")) {
                    list.add(line.trim());
                }
            }
        } catch (final Exception e) {
            throw new SuggestSettingsException("Failed to load default badwords.", e);
        }
        defaultWords = list.toArray(new String[list.size()]);
    }
}
