package jp.sf.fess.suggest.solr;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SuggestUpdateConfig {
    private String solrUrl = "";

    private String solrUser = "";

    private String solrPassword = "";

    private String[] labelFields = null;

    private long updateInterval = 10 * 1000;

    private String expiresField = "expires_dt";

    private String segmentField = "segment";

    private List<FieldConfig> fieldConfigList = new ArrayList<FieldConfig>();

    public String getSolrUrl() {
        return solrUrl;
    }

    public void setSolrUrl(String solrUrl) {
        this.solrUrl = solrUrl;
    }

    public long getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(long updateInterval) {
        this.updateInterval = updateInterval;
    }

    public String getExpiresField() {
        return expiresField;
    }

    public void setExpiresField(String expiresField) {
        this.expiresField = expiresField;
    }

    public String getSegmentField() {
        return segmentField;
    }

    public void setSegmentField(String segmentField) {
        this.segmentField = segmentField;
    }

    public String getSolrUser() {
        return solrUser;
    }

    public void setSolrUser(String solrUser) {
        this.solrUser = solrUser;
    }

    public String getSolrPassword() {
        return solrPassword;
    }

    public void setSolrPassword(String solrPassword) {
        this.solrPassword = solrPassword;
    }

    public String[] getLabelFields() {
        return labelFields;
    }

    public void setLabelFields(String[] labelFields) {
        this.labelFields = labelFields;
    }

    public List<FieldConfig> getFieldConfigList() {
        return fieldConfigList;
    }

    public void addFieldConfig(FieldConfig fieldConfig) {
        this.fieldConfigList.add(fieldConfig);
    }

    public static class FieldConfig {
        private String[] targetFields = new String[]{"content", "title"};

        private TokenizerConfig tokenizerConfig = null;

        private List<ConverterConfig> converterConfigList = new ArrayList<ConverterConfig>();

        private List<NormalizerConfig> normalizerConfigList = new ArrayList<NormalizerConfig>();

        public String[] getTargetFields() {
            return targetFields;
        }

        public void setTargetFields(String[] targetFields) {
            this.targetFields = targetFields;
        }

        public TokenizerConfig getTokenizerConfig() {
            return tokenizerConfig;
        }

        public void setTokenizerConfig(TokenizerConfig tokenizerConfig) {
            this.tokenizerConfig = tokenizerConfig;
        }

        public List<ConverterConfig> getConverterConfigList() {
            return converterConfigList;
        }

        public void addConverterConfig(ConverterConfig converterConfig) {
            this.converterConfigList.add(converterConfig);
        }

        public List<NormalizerConfig> getNormalizerConfigList() {
            return normalizerConfigList;
        }

        public void addNormalizerConfig(NormalizerConfig normalizerConfig) {
            this.normalizerConfigList.add(normalizerConfig);
        }
    }

    public static class TokenizerConfig {
        private String className = "jp.sf.fess.suggest.analysis.SuggestTokenizerFactory";

        Map<String, String> args;

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public Map<String, String> getArgs() {
            return args;
        }

        public void setArgs(Map<String, String> args) {
            this.args = args;
        }
    }

    public static class ConverterConfig {
        private String className;

        private Map<String, String> properties;

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }
    }

    public static class NormalizerConfig {
        private String className;

        private Map<String, String> properties;

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }
    }
}
