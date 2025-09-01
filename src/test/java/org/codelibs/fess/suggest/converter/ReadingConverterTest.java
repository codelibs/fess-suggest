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
package org.codelibs.fess.suggest.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class ReadingConverterTest {

    private ReadingConverter converter;
    private TestReadingConverter testConverter;

    @Before
    public void setUp() {
        testConverter = new TestReadingConverter();
        converter = testConverter;
    }

    @Test
    public void testGetMaxReadingNum() {
        // Test default max reading num
        assertEquals(10, converter.getMaxReadingNum());
    }

    @Test
    public void testInitNormal() throws IOException {
        // Test normal initialization
        converter.init();
        assertTrue(testConverter.isInitialized());
    }

    @Test
    public void testInitMultipleTimes() throws IOException {
        // Test multiple initialization calls
        converter.init();
        assertTrue(testConverter.isInitialized());

        // Call init again
        converter.init();
        assertTrue(testConverter.isInitialized());
        assertEquals(2, testConverter.getInitCount());
    }

    @Test
    public void testInitWithException() {
        // Test initialization with exception
        TestReadingConverterWithException errorConverter = new TestReadingConverterWithException();
        errorConverter.setThrowExceptionOnInit(true);

        try {
            errorConverter.init();
            fail("Should throw IOException");
        } catch (IOException e) {
            assertEquals("Init failed", e.getMessage());
        }
    }

    @Test
    public void testConvertWithNormalText() throws IOException {
        // Test convert with normal text
        converter.init();
        String text = "hello world";
        String field = "content";

        List<String> readings = converter.convert(text, field, "en");

        assertNotNull(readings);
        assertEquals(1, readings.size());
        assertEquals("HELLO WORLD", readings.get(0));
    }

    @Test
    public void testConvertWithEmptyText() throws IOException {
        // Test convert with empty text
        converter.init();
        String text = "";
        String field = "content";

        List<String> readings = converter.convert(text, field, "en");

        assertNotNull(readings);
        assertTrue(readings.isEmpty());
    }

    @Test
    public void testConvertWithNullText() throws IOException {
        // Test convert with null text
        converter.init();
        String text = null;
        String field = "content";

        List<String> readings = converter.convert(text, field, "en");

        assertNotNull(readings);
        assertTrue(readings.isEmpty());
    }

    @Test
    public void testConvertWithMultipleLanguages() throws IOException {
        // Test convert with multiple languages
        converter.init();
        String text = "test";
        String field = "content";

        List<String> readings = converter.convert(text, field, "en", "ja", "fr");

        assertNotNull(readings);
        assertEquals(3, readings.size());
        assertEquals("TEST_en", readings.get(0));
        assertEquals("TEST_ja", readings.get(1));
        assertEquals("TEST_fr", readings.get(2));
    }

    @Test
    public void testConvertWithNoLanguages() throws IOException {
        // Test convert with no languages specified
        converter.init();
        String text = "test";
        String field = "content";

        List<String> readings = converter.convert(text, field);

        assertNotNull(readings);
        assertEquals(1, readings.size());
        assertEquals("TEST", readings.get(0));
    }

    @Test
    public void testConvertWithNullField() throws IOException {
        // Test convert with null field
        converter.init();
        String text = "test";
        String field = null;

        List<String> readings = converter.convert(text, field, "en");

        assertNotNull(readings);
        assertEquals(1, readings.size());
        assertEquals("TEST", readings.get(0));
    }

    @Test
    public void testConvertWithSpecialCharacters() throws IOException {
        // Test convert with special characters
        converter.init();
        String text = "test@#$%123";
        String field = "content";

        List<String> readings = converter.convert(text, field, "en");

        assertNotNull(readings);
        assertEquals(1, readings.size());
        assertEquals("TEST@#$%123", readings.get(0));
    }

    @Test
    public void testConvertWithJapaneseText() throws IOException {
        // Test convert with Japanese text
        testConverter.setJapaneseMode(true);
        converter.init();
        String text = "東京";
        String field = "content";

        List<String> readings = converter.convert(text, field, "ja");

        assertNotNull(readings);
        assertEquals(1, readings.size());
        assertEquals("トウキョウ", readings.get(0));
    }

    @Test
    public void testConvertWithoutInit() throws IOException {
        // Test convert without initialization
        String text = "test";
        String field = "content";

        List<String> readings = converter.convert(text, field, "en");

        assertNotNull(readings);
        assertEquals(1, readings.size());
        assertEquals("TEST", readings.get(0));
        assertFalse(testConverter.isInitialized());
    }

    @Test
    public void testConvertWithException() throws IOException {
        // Test convert with exception
        TestReadingConverterWithException errorConverter = new TestReadingConverterWithException();
        errorConverter.init();
        errorConverter.setThrowExceptionOnConvert(true);

        try {
            errorConverter.convert("test", "field", "en");
            fail("Should throw IOException");
        } catch (IOException e) {
            assertEquals("Convert failed", e.getMessage());
        }
    }

    @Test
    public void testConvertWithMaxReadingLimit() throws IOException {
        // Test that readings respect max reading limit
        TestReadingConverterWithLimit limitConverter = new TestReadingConverterWithLimit();
        limitConverter.init();

        assertEquals(5, limitConverter.getMaxReadingNum());

        String text = "test";
        String field = "content";

        // Request more languages than max reading num
        List<String> readings = limitConverter.convert(text, field, "en", "ja", "fr", "de", "es", "it", "pt");

        assertNotNull(readings);
        assertEquals(5, readings.size()); // Should be limited to 5
    }

    @Test
    public void testConvertWithWhitespace() throws IOException {
        // Test convert with various whitespace
        converter.init();
        String text = "  test  \n\t  text  ";
        String field = "content";

        List<String> readings = converter.convert(text, field, "en");

        assertNotNull(readings);
        assertEquals(1, readings.size());
        assertEquals("  TEST  \n\t  TEXT  ", readings.get(0));
    }

    @Test
    public void testConvertWithLongText() throws IOException {
        // Test convert with long text
        converter.init();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("word").append(i).append(" ");
        }
        String text = sb.toString();
        String field = "content";

        List<String> readings = converter.convert(text, field, "en");

        assertNotNull(readings);
        assertEquals(1, readings.size());
        assertEquals(text.toUpperCase(), readings.get(0));
    }

    @Test
    public void testMultipleConverterInstances() throws IOException {
        // Test that multiple converter instances work independently
        ReadingConverter converter1 = new TestReadingConverter();
        ReadingConverter converter2 = new TestReadingConverter();

        converter1.init();
        converter2.init();

        String text = "test";
        String field = "content";

        List<String> readings1 = converter1.convert(text, field, "en");
        List<String> readings2 = converter2.convert(text, field, "ja");

        assertNotNull(readings1);
        assertNotNull(readings2);
        assertEquals("TEST", readings1.get(0));
        assertEquals("TEST", readings2.get(0));
    }

    @Test
    public void testConvertWithEmptyLanguageArray() throws IOException {
        // Test convert with empty language array
        converter.init();
        String text = "test";
        String field = "content";
        String[] langs = new String[0];

        List<String> readings = converter.convert(text, field, langs);

        assertNotNull(readings);
        assertEquals(1, readings.size());
        assertEquals("TEST", readings.get(0));
    }

    @Test
    public void testConvertWithNullLanguageArray() throws IOException {
        // Test convert with null language array
        converter.init();
        String text = "test";
        String field = "content";
        String[] langs = null;

        List<String> readings = converter.convert(text, field, langs);

        assertNotNull(readings);
        assertEquals(1, readings.size());
        assertEquals("TEST", readings.get(0));
    }

    /**
     * Test implementation of ReadingConverter for testing purposes
     */
    private static class TestReadingConverter implements ReadingConverter {

        private boolean initialized = false;
        private int initCount = 0;
        private boolean japaneseMode = false;

        public void setJapaneseMode(boolean japaneseMode) {
            this.japaneseMode = japaneseMode;
        }

        public boolean isInitialized() {
            return initialized;
        }

        public int getInitCount() {
            return initCount;
        }

        @Override
        public void init() throws IOException {
            initialized = true;
            initCount++;
        }

        @Override
        public List<String> convert(String text, String field, String... langs) throws IOException {
            if (text == null || text.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> readings = new ArrayList<>();

            if (langs == null || langs.length == 0) {
                readings.add(text.toUpperCase());
            } else {
                for (String lang : langs) {
                    if (japaneseMode && "ja".equals(lang)) {
                        // Simulate Japanese conversion
                        if ("東京".equals(text)) {
                            readings.add("トウキョウ");
                        } else {
                            readings.add(text.toUpperCase() + "_" + lang);
                        }
                    } else if (langs.length > 1) {
                        readings.add(text.toUpperCase() + "_" + lang);
                    } else {
                        readings.add(text.toUpperCase());
                    }
                }
            }

            return readings;
        }
    }

    /**
     * Test implementation that throws exceptions
     */
    private static class TestReadingConverterWithException implements ReadingConverter {

        private boolean throwExceptionOnInit = false;
        private boolean throwExceptionOnConvert = false;

        public void setThrowExceptionOnInit(boolean throwException) {
            this.throwExceptionOnInit = throwException;
        }

        public void setThrowExceptionOnConvert(boolean throwException) {
            this.throwExceptionOnConvert = throwException;
        }

        @Override
        public void init() throws IOException {
            if (throwExceptionOnInit) {
                throw new IOException("Init failed");
            }
        }

        @Override
        public List<String> convert(String text, String field, String... langs) throws IOException {
            if (throwExceptionOnConvert) {
                throw new IOException("Convert failed");
            }
            return Collections.emptyList();
        }
    }

    /**
     * Test implementation with custom max reading limit
     */
    private static class TestReadingConverterWithLimit implements ReadingConverter {

        @Override
        public int getMaxReadingNum() {
            return 5;
        }

        @Override
        public void init() throws IOException {
            // Do nothing
        }

        @Override
        public List<String> convert(String text, String field, String... langs) throws IOException {
            if (text == null || text.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> readings = new ArrayList<>();
            int limit = Math.min(langs.length, getMaxReadingNum());

            for (int i = 0; i < limit; i++) {
                readings.add(text.toUpperCase() + "_" + langs[i]);
            }

            return readings;
        }
    }
}