package org.codelibs.fess.suggest.settings;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.client.Client;

public class NgWordSettings {
    public static final String NG_WORD_SETTINGD_KEY = "ngword";

    protected ArraySettings arraySettings;

    protected NgWordSettings(final Client client, final String settingsIndexName, final String settingsId) {
        this.arraySettings = new ArraySettings(client, settingsIndexName, settingsId) {
            @Override
            protected String createArraySettingsIndexName(final String settingsIndexName) {
                return settingsIndexName + "-ngword";
            }
        };
    }

    public String[] get() {
        return arraySettings.get(NG_WORD_SETTINGD_KEY);
    }

    public void add(String ngWord) {
        String validationError = getValidationError(ngWord);
        if (validationError != null) {
            throw new IllegalArgumentException("Validation error. " + validationError);
        }
        arraySettings.add(NG_WORD_SETTINGD_KEY, ngWord);
    }

    public void delete(String ngWord) {
        arraySettings.delete(NG_WORD_SETTINGD_KEY, ngWord);
    }

    public void deleteAll() {
        arraySettings.delete(NG_WORD_SETTINGD_KEY);
    }

    protected String getValidationError(String ngWord) {
        if (StringUtils.isBlank(ngWord)) {
            return "ngWord was empty.";
        }
        if (ngWord.contains(" ") || ngWord.contains("  ")) {
            return "ngWord contains space.";
        }
        return null;
    }
}
