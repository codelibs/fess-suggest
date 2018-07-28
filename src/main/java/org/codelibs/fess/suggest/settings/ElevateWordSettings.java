package org.codelibs.fess.suggest.settings;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.entity.ElevateWord;
import org.elasticsearch.client.Client;

public class ElevateWordSettings {
    public static final String ELEVATE_WORD_SETTINGD_KEY = "elevateword";
    public static final String ELEVATE_WORD_BOOST = "boost";
    public static final String ELEVATE_WORD_READING = "reading";
    public static final String ELEVATE_WORD_FIELDS = "fields";
    public static final String ELEVATE_WORD_TAGS = "tags";
    public static final String ELEVATE_WORD_ROLES = "roles";

    protected ArraySettings arraySettings;

    protected ElevateWordSettings(final SuggestSettings settings, final Client client, final String settingsIndexName,
            final String settingsId) {
        this.arraySettings = new ArraySettings(settings, client, settingsIndexName, settingsId) {
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
        arraySettings.delete(ELEVATE_WORD_SETTINGD_KEY, elevateWord);
    }

    public void deleteAll() {
        arraySettings.delete(ELEVATE_WORD_SETTINGD_KEY);
    }

}
