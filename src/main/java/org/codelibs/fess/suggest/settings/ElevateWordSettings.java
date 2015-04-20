package org.codelibs.fess.suggest.settings;

import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.entity.ElevateWord;
import org.elasticsearch.client.Client;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElevateWordSettings {
    public static final String ELEVATE_WORD_SETTINGD_KEY = "elevateword";
    public static final String ELEVATE_WORD_BOOST = "boost";
    public static final String ELEVATE_WORD_READING = "reading";
    public static final String ELEVATE_WORD_FIELDS = "fields";

    protected ArraySettings arraySettings;

    protected ElevateWordSettings(final Client client, final String settingsIndexName, final String settingsId) {
        this.arraySettings = new ArraySettings(client, settingsIndexName, settingsId) {
            @Override
            protected String createArraySettingsIndexName(final String settingsIndexName) {
                return settingsIndexName + "-elevate";
            }
        };
    }

    @SuppressWarnings("unchecked")
    public ElevateWord[] get() {
        Map<String, Object>[] sourceArray =
                arraySettings.getFromArrayIndex(arraySettings.arraySettingsIndexName, arraySettings.settingsId, ELEVATE_WORD_SETTINGD_KEY);

        ElevateWord[] elevateWords = new ElevateWord[sourceArray.length];
        for (int i = 0; i < elevateWords.length; i++) {
            Object elevateWord = sourceArray[i].get(FieldNames.ARRAY_VALUE);
            Object boost = sourceArray[i].get(ELEVATE_WORD_BOOST);
            Object readings = sourceArray[i].get(ELEVATE_WORD_READING);
            Object fields = sourceArray[i].get(ELEVATE_WORD_FIELDS);
            if (elevateWord != null && boost != null && readings != null && fields != null) {
                elevateWords[i] =
                        new ElevateWord(elevateWord.toString(), Float.parseFloat(boost.toString()), (List) readings, (List) fields);
            }
        }
        return elevateWords;
    }

    public void add(ElevateWord elevateWord) {
        Map<String, Object> source = new HashMap<>();
        source.put(FieldNames.ARRAY_KEY, ELEVATE_WORD_SETTINGD_KEY);
        source.put(FieldNames.ARRAY_VALUE, elevateWord.getElevateWord());
        source.put(ELEVATE_WORD_BOOST, elevateWord.getBoost());
        source.put(ELEVATE_WORD_READING, elevateWord.getReadings());
        source.put(ELEVATE_WORD_FIELDS, elevateWord.getFields());
        source.put(FieldNames.TIMESTAMP, LocalDateTime.now());

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
