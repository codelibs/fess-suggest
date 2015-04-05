package org.codelibs.fess.suggest;

public class SuggestSettings {
    public String index;
    public String type;
    public String[] supportedFields;
    public String tagFieldName;
    public String roleFieldName;

    //TODO converter settings

    //TODO normalizer settings

    public static SuggestSettings defaultSettings() {
        SuggestSettings settings = new SuggestSettings();
        settings.index = "suggest";
        settings.type = "item";
        settings.supportedFields = new String[]{"content"};
        settings.tagFieldName = "label";
        settings.roleFieldName = "role";
        return settings;
    }
}
