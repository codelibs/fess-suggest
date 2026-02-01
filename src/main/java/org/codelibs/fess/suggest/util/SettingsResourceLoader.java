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
package org.codelibs.fess.suggest.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Map;

import org.codelibs.core.lang.StringUtil;

/**
 * Utility class for loading JSON resource files for settings.
 * Centralizes resource loading logic to reduce code duplication across settings classes.
 *
 * <p>This class provides methods to load JSON resources from the classpath with optional
 * placeholder substitution.
 */
public final class SettingsResourceLoader {

    /** Default placeholder for dictionary path. */
    public static final String DICTIONARY_PATH_PLACEHOLDER = "${fess.dictionary.path}";

    /** System property name for dictionary path. */
    public static final String DICTIONARY_PATH_PROPERTY = "fess.dictionary.path";

    private SettingsResourceLoader() {
        // Utility class
    }

    /**
     * Loads a JSON resource file from the classpath.
     *
     * @param resourcePath The path to the resource file (e.g., "suggest_indices/suggest.json")
     * @return The content of the resource file as a string
     * @throws IOException If an I/O error occurs or the resource is not found
     */
    public static String loadJsonResource(final String resourcePath) throws IOException {
        return loadJsonResource(resourcePath, Collections.emptyMap());
    }

    /**
     * Loads a JSON resource file from the classpath with placeholder substitution.
     *
     * @param resourcePath The path to the resource file
     * @param substitutions A map of placeholder patterns to replacement values.
     *        Keys should be the literal placeholder strings (e.g., "${fess.dictionary.path}").
     * @return The content of the resource file with placeholders replaced
     * @throws IOException If an I/O error occurs or the resource is not found
     */
    public static String loadJsonResource(final String resourcePath, final Map<String, String> substitutions) throws IOException {
        return loadJsonResource(SettingsResourceLoader.class.getClassLoader(), resourcePath, substitutions);
    }

    /**
     * Loads a JSON resource file with the specified class loader.
     *
     * @param classLoader The class loader to use for loading the resource
     * @param resourcePath The path to the resource file
     * @param substitutions A map of placeholder patterns to replacement values
     * @return The content of the resource file with placeholders replaced
     * @throws IOException If an I/O error occurs or the resource is not found
     */
    public static String loadJsonResource(final ClassLoader classLoader, final String resourcePath, final Map<String, String> substitutions)
            throws IOException {
        final StringBuilder sb = new StringBuilder();
        try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }
        }

        String result = sb.toString();
        for (final Map.Entry<String, String> entry : substitutions.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Loads a JSON resource file with the default dictionary path substitution.
     * This is a convenience method that substitutes the ${fess.dictionary.path} placeholder
     * with the value from the system property "fess.dictionary.path".
     *
     * @param resourcePath The path to the resource file
     * @return The content of the resource file with the dictionary path placeholder replaced
     * @throws IOException If an I/O error occurs or the resource is not found
     */
    public static String loadJsonResourceWithDictionaryPath(final String resourcePath) throws IOException {
        final String dictionaryPath = System.getProperty(DICTIONARY_PATH_PROPERTY, StringUtil.EMPTY);
        return loadJsonResource(resourcePath, Collections.singletonMap(DICTIONARY_PATH_PLACEHOLDER, dictionaryPath));
    }

    /**
     * Loads a JSON resource file with the default dictionary path substitution using a specific class loader.
     *
     * @param classLoader The class loader to use for loading the resource
     * @param resourcePath The path to the resource file
     * @return The content of the resource file with the dictionary path placeholder replaced
     * @throws IOException If an I/O error occurs or the resource is not found
     */
    public static String loadJsonResourceWithDictionaryPath(final ClassLoader classLoader, final String resourcePath) throws IOException {
        final String dictionaryPath = System.getProperty(DICTIONARY_PATH_PROPERTY, StringUtil.EMPTY);
        return loadJsonResource(classLoader, resourcePath, Collections.singletonMap(DICTIONARY_PATH_PLACEHOLDER, dictionaryPath));
    }
}
