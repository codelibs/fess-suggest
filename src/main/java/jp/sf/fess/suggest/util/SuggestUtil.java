package jp.sf.fess.suggest.util;


import jp.sf.fess.suggest.SuggestConstants;
import jp.sf.fess.suggest.solr.SuggestUpdateConfig;
import jp.sf.fess.suggest.converter.SuggestIntegrateConverter;
import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.entity.SuggestFieldInfo;
import jp.sf.fess.suggest.normalizer.SuggestIntegrateNormalizer;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.solr.common.util.DateUtil;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.update.TransactionLog;
import org.seasar.util.io.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class SuggestUtil {
    private static final Logger logger = LoggerFactory.getLogger(SuggestUtil.class);

    private static final String USER_DICT_PATH = "userDictionary";

    private static final String USER_DICT_ENCODING = "userDictionaryEncoding";

    public static String formatDate(Date date) {
        return DateUtil.getThreadLocalDateFormat().format(date);
    }

    public static SuggestReadingConverter createConverter(String className, Map<String, String> properties)
            throws Exception {
        Class cls = Class.forName(className);
        Object obj = cls.newInstance();
        if (!(obj instanceof SuggestReadingConverter)) {
            throw new IllegalArgumentException("Not converter. " + className);
        }

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();
            Field field = cls.getField(fieldName);
            field.set(obj, fieldValue);
        }

        return (SuggestReadingConverter) obj;
    }

    public static SuggestNormalizer createNormalizer(String className, Map<String, String> properties)
            throws Exception {
        Class cls = Class.forName(className);
        Object obj = cls.newInstance();
        if (!(obj instanceof SuggestNormalizer)) {
            throw new IllegalArgumentException("Not normalizer. " + className);
        }

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();
            Field field = cls.getField(fieldName);
            field.set(obj, fieldValue);
        }

        return (SuggestNormalizer) obj;
    }

    public static SuggestUpdateConfig getUpdateHandlerConfig(SolrConfig config) {
        SuggestUpdateConfig suggestUpdateConfig = new SuggestUpdateConfig();

        //setting config
        String solrUrl = config.getVal("updateHandler/solrUrl",
                false);
        if (StringUtils.isNotBlank(solrUrl)) {
            suggestUpdateConfig.setSolrUrl(solrUrl);
        }

        String solrUser = config.getVal("updateHandler/solrUser",
                false);
        if (StringUtils.isNotBlank(solrUser)) {
            suggestUpdateConfig.setSolrUser(solrUser);
        }
        String solrPassword = config.getVal("updateHandler/solrPassword",
                false);
        if (StringUtils.isNotBlank(solrPassword)) {
            suggestUpdateConfig.setSolrPassword(solrPassword);
        }
        String labelFields = config.getVal("updateHandler/labelFields",
                false);
        if (StringUtils.isNotBlank(labelFields)) {
            suggestUpdateConfig.setLabelFields(labelFields.trim().split(","));
        }

        String expiresField = config.getVal("updateHandler/expiresField",
                false);
        if (StringUtils.isNotBlank(expiresField)) {
            suggestUpdateConfig.setExpiresField(expiresField);
        }
        String segmentField = config.getVal("updateHandler/segmentField",
                false);
        if (StringUtils.isNotBlank(segmentField)) {
            suggestUpdateConfig.setSegmentField(segmentField);
        }
        String updateInterval = config.getVal("updateHandler/updateInterval",
                false);
        if (StringUtils.isNotBlank(updateInterval) && StringUtils.isNumeric(updateInterval)) {
            suggestUpdateConfig.setUpdateInterval(Long.parseLong(updateInterval));
        }

        NodeList nodeList = config.getNodeList("updateHandler/suggestFieldInfo", true);
        for (int i = 0; i < nodeList.getLength(); i++) {
            try {
                SuggestUpdateConfig.FieldConfig fieldConfig = new SuggestUpdateConfig.FieldConfig();
                Node fieldInfoNode = nodeList.item(i);
                NamedNodeMap fieldInfoAttributes = fieldInfoNode.getAttributes();
                Node fieldNameNode = fieldInfoAttributes.getNamedItem("fieldName");
                String fieldName = fieldNameNode.getNodeValue();
                if (StringUtils.isBlank(fieldName)) {
                    continue;
                }
                fieldConfig.setTargetFields(fieldName.trim().split(","));
                if (logger.isInfoEnabled()) {
                    for (String s : fieldConfig.getTargetFields()) {
                        logger.info("fieldName : " + s);
                    }
                }

                NodeList fieldInfoChilds = fieldInfoNode.getChildNodes();
                for (int j = 0; j < fieldInfoChilds.getLength(); j++) {
                    Node fieldInfoChildNode = fieldInfoChilds.item(j);
                    String fieldInfoChildNodeName = fieldInfoChildNode.getNodeName();

                    if ("tokenizerFactory".equals(fieldInfoChildNodeName)) {
                        SuggestUpdateConfig.TokenizerConfig tokenizerConfig =
                                new SuggestUpdateConfig.TokenizerConfig();

                        NamedNodeMap tokenizerFactoryAttributes = fieldInfoChildNode.getAttributes();
                        Node tokenizerClassNameNode = tokenizerFactoryAttributes.getNamedItem("class");
                        String tokenizerClassName = tokenizerClassNameNode.getNodeValue();
                        tokenizerConfig.setClassName(tokenizerClassName);
                        if (logger.isInfoEnabled()) {
                            logger.info("tokenizerFactory : " + tokenizerClassName);
                        }

                        Map<String, String> args = new HashMap<String, String>();
                        for (int k = 0; k < tokenizerFactoryAttributes.getLength(); k++) {
                            Node attribute = tokenizerFactoryAttributes.item(k);
                            String key = attribute.getNodeName();
                            String value = attribute.getNodeValue();
                            if (!"class".equals(key)) {
                                args.put(key, value);
                            }
                        }
                        if (!args.containsKey(USER_DICT_PATH)) {
                            args.put(USER_DICT_PATH, SuggestConstants.USER_DICT_PATH);
                            args.put(USER_DICT_ENCODING, SuggestConstants.USER_DICT_ENCODING);
                        }
                        tokenizerConfig.setArgs(args);

                        fieldConfig.setTokenizerConfig(tokenizerConfig);
                    } else if ("suggestReadingConverter".equals(fieldInfoChildNodeName)) {
                        NodeList converterNodeList = fieldInfoChildNode.getChildNodes();
                        for (int k = 0; k < converterNodeList.getLength(); k++) {
                            SuggestUpdateConfig.ConverterConfig converterConfig =
                                    new SuggestUpdateConfig.ConverterConfig();

                            Node converterNode = converterNodeList.item(k);
                            if (!"converter".equals(converterNode.getNodeName())) {
                                continue;
                            }

                            NamedNodeMap converterAttributes = converterNode.getAttributes();
                            Node classNameNode = converterAttributes.getNamedItem("class");
                            String className = classNameNode.getNodeValue();
                            converterConfig.setClassName(className);
                            if (logger.isInfoEnabled()) {
                                logger.info("converter : " + className);
                            }

                            Map<String, String> properties = new HashMap<String, String>();
                            for (int l = 0; l < converterAttributes.getLength(); l++) {
                                Node attribute = converterAttributes.item(l);
                                String key = attribute.getNodeName();
                                String value = attribute.getNodeValue();
                                if (!"class".equals(key)) {
                                    properties.put(key, value);
                                }
                            }
                            converterConfig.setProperties(properties);
                            if (logger.isInfoEnabled()) {
                                logger.info("converter properties = " + properties);
                            }
                            fieldConfig.addConverterConfig(converterConfig);
                        }
                    } else if ("suggestNormalizer".equals(fieldInfoChildNodeName)) {
                        NodeList normalizerNodeList = fieldInfoChildNode.getChildNodes();
                        for (int k = 0; k < normalizerNodeList.getLength(); k++) {
                            SuggestUpdateConfig.NormalizerConfig normalizerConfig =
                                    new SuggestUpdateConfig.NormalizerConfig();

                            Node normalizerNode = normalizerNodeList.item(k);
                            if (!"normalizer".equals(normalizerNode.getNodeName())) {
                                continue;
                            }

                            NamedNodeMap normalizerAttributes = normalizerNode.getAttributes();
                            Node classNameNode = normalizerAttributes.getNamedItem("class");
                            String className = classNameNode.getNodeValue();
                            normalizerConfig.setClassName(className);
                            if (logger.isInfoEnabled()) {
                                logger.info("normalizer : " + className);
                            }

                            Map<String, String> properties = new HashMap<String, String>();
                            for (int l = 0; l < normalizerAttributes.getLength(); l++) {
                                Node attribute = normalizerAttributes.item(l);
                                String key = attribute.getNodeName();
                                String value = attribute.getNodeValue();
                                if (!"class".equals(key)) {
                                    properties.put(key, value);
                                }
                            }
                            normalizerConfig.setProperties(properties);
                            if (logger.isInfoEnabled()) {
                                logger.info("normalize properties = " + properties);
                            }
                            fieldConfig.addNormalizerConfig(normalizerConfig);
                        }
                    }
                }

                suggestUpdateConfig.addFieldConfig(fieldConfig);
            } catch (Exception e) {
                logger.warn("debug error", e);
            }
        }

        return suggestUpdateConfig;
    }

    public static List<SuggestFieldInfo> getSuggestFieldInfoList(SuggestUpdateConfig config)
            throws Exception {
        List<SuggestFieldInfo> list =
                new ArrayList<SuggestFieldInfo>();

        for (SuggestUpdateConfig.FieldConfig fieldConfig : config.getFieldConfigList()) {
            try {
                List<String> fieldNameList = Arrays.asList(fieldConfig.getTargetFields());
                SuggestUpdateConfig.TokenizerConfig tokenizerConfig = fieldConfig.getTokenizerConfig();

                //create tokenizerFactory
                TokenizerFactory tokenizerFactory = null;
                if (tokenizerConfig != null) {
                    Class cls = Class.forName(tokenizerConfig.getClassName());
                    Constructor constructor = cls.getConstructor(Map.class);
                    tokenizerFactory = (TokenizerFactory) constructor.newInstance(tokenizerConfig.getArgs());
                }

                //create converter
                SuggestIntegrateConverter suggestIntegrateConverter = new SuggestIntegrateConverter();
                for (SuggestUpdateConfig.ConverterConfig converterConfig : fieldConfig.getConverterConfigList()) {
                    SuggestReadingConverter suggestReadingConverter =
                            SuggestUtil.createConverter(converterConfig.getClassName(), converterConfig.getProperties());
                    suggestIntegrateConverter.addConverter(suggestReadingConverter);
                }
                suggestIntegrateConverter.start();

                //create normalizer
                SuggestIntegrateNormalizer suggestIntegrateNormalizer = new SuggestIntegrateNormalizer();
                for (SuggestUpdateConfig.NormalizerConfig normalizerConfig : fieldConfig.getNormalizerConfigList()) {
                    SuggestNormalizer suggestNormalizer =
                            SuggestUtil.createNormalizer(normalizerConfig.getClassName(),
                                    normalizerConfig.getProperties());
                    suggestIntegrateNormalizer.addNormalizer(suggestNormalizer);
                }
                suggestIntegrateNormalizer.start();

                SuggestFieldInfo suggestFieldInfo =
                        new SuggestFieldInfo(fieldNameList, tokenizerFactory,
                                suggestIntegrateConverter, suggestIntegrateNormalizer);
                list.add(suggestFieldInfo);
            } catch (Exception e) {
                logger.warn("Failed to create Tokenizer." + fieldConfig.getTokenizerConfig().getClassName()
                        , e);
            }
        }
        return list;
    }
}
