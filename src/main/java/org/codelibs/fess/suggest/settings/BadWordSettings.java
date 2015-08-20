package org.codelibs.fess.suggest.settings;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.base.Strings;

public class BadWordSettings {
    public static final String BAD_WORD_SETTINGD_KEY = "badword";

    protected ArraySettings arraySettings;

    protected BadWordSettings(final Client client, final String settingsIndexName, final String settingsId) {
        this.arraySettings = new ArraySettings(client, settingsIndexName, settingsId) {
            @Override
            protected String createArraySettingsIndexName(final String settingsIndexName) {
                return settingsIndexName + "-badword";
            }
        };
    }

    public String[] get() {
        return arraySettings.get(BAD_WORD_SETTINGD_KEY);
    }

    public void add(final String badWord) {
        final String validationError = getValidationError(badWord);
        if (validationError != null) {
            throw new IllegalArgumentException("Validation error. " + validationError);
        }
        arraySettings.add(BAD_WORD_SETTINGD_KEY, badWord);
    }

    public void delete(final String badWord) {
        arraySettings.delete(BAD_WORD_SETTINGD_KEY, badWord);
    }

    public void deleteAll() {
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
}
