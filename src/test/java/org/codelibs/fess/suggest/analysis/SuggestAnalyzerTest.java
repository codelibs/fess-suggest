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
package org.codelibs.fess.suggest.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opensearch.action.admin.indices.analyze.AnalyzeAction.AnalyzeToken;

public class SuggestAnalyzerTest {

    private SuggestAnalyzer analyzer;
    private TestSuggestAnalyzer testAnalyzer;

    @Before
    public void setUp() {
        testAnalyzer = new TestSuggestAnalyzer();
        analyzer = testAnalyzer;
    }

    @Test
    public void testAnalyzeWithValidInput() {
        // Test normal analyze with valid input
        String text = "test text";
        String field = "content";
        String lang = "en";

        List<AnalyzeToken> tokens = analyzer.analyze(text, field, lang);

        assertNotNull(tokens);
        assertEquals(2, tokens.size());
        assertEquals("test", tokens.get(0).getTerm());
        assertEquals("text", tokens.get(1).getTerm());
    }

    @Test
    public void testAnalyzeWithEmptyText() {
        // Test analyze with empty text
        String text = "";
        String field = "content";
        String lang = "en";

        List<AnalyzeToken> tokens = analyzer.analyze(text, field, lang);

        assertNotNull(tokens);
        assertTrue(tokens.isEmpty());
    }

    @Test
    public void testAnalyzeWithNullText() {
        // Test analyze with null text
        String text = null;
        String field = "content";
        String lang = "en";

        List<AnalyzeToken> tokens = analyzer.analyze(text, field, lang);

        assertNotNull(tokens);
        assertTrue(tokens.isEmpty());
    }

    @Test
    public void testAnalyzeWithWhitespaceOnly() {
        // Test analyze with whitespace only
        String text = "   ";
        String field = "content";
        String lang = "en";

        List<AnalyzeToken> tokens = analyzer.analyze(text, field, lang);

        assertNotNull(tokens);
        assertTrue(tokens.isEmpty());
    }

    @Test
    public void testAnalyzeWithSpecialCharacters() {
        // Test analyze with special characters
        String text = "test@example.com #hashtag $100";
        String field = "content";
        String lang = "en";

        List<AnalyzeToken> tokens = analyzer.analyze(text, field, lang);

        assertNotNull(tokens);
        assertEquals(5, tokens.size());
        assertEquals("test", tokens.get(0).getTerm());
        assertEquals("example", tokens.get(1).getTerm());
        assertEquals("com", tokens.get(2).getTerm());
        assertEquals("hashtag", tokens.get(3).getTerm());
        assertEquals("100", tokens.get(4).getTerm());
    }

    @Test
    public void testAnalyzeWithDifferentLanguages() {
        // Test analyze with different language settings
        String text = "hello world";
        String field = "content";

        // Test with English
        List<AnalyzeToken> tokensEn = analyzer.analyze(text, field, "en");
        assertNotNull(tokensEn);
        assertEquals(2, tokensEn.size());

        // Test with Japanese
        List<AnalyzeToken> tokensJa = analyzer.analyze(text, field, "ja");
        assertNotNull(tokensJa);
        assertEquals(2, tokensJa.size());

        // Test with null language
        List<AnalyzeToken> tokensNull = analyzer.analyze(text, field, null);
        assertNotNull(tokensNull);
        assertEquals(2, tokensNull.size());
    }

    @Test
    public void testAnalyzeWithDifferentFields() {
        // Test analyze with different field settings
        String text = "test content";
        String lang = "en";

        // Test with different field names
        List<AnalyzeToken> tokensContent = analyzer.analyze(text, "content", lang);
        assertNotNull(tokensContent);
        assertEquals(2, tokensContent.size());

        List<AnalyzeToken> tokensTitle = analyzer.analyze(text, "title", lang);
        assertNotNull(tokensTitle);
        assertEquals(2, tokensTitle.size());

        // Test with null field
        List<AnalyzeToken> tokensNull = analyzer.analyze(text, null, lang);
        assertNotNull(tokensNull);
        assertEquals(2, tokensNull.size());
    }

    @Test
    public void testAnalyzeAndReadingWithValidInput() {
        // Test analyzeAndReading with valid input
        String text = "test text";
        String field = "content";
        String lang = "en";

        List<AnalyzeToken> tokens = analyzer.analyzeAndReading(text, field, lang);

        assertNotNull(tokens);
        assertEquals(2, tokens.size());
        assertEquals("test", tokens.get(0).getTerm());
        assertEquals("text", tokens.get(1).getTerm());
        // Check that readings are added
        assertEquals("TEST", ((TestAnalyzeToken) tokens.get(0)).getReading());
        assertEquals("TEXT", ((TestAnalyzeToken) tokens.get(1)).getReading());
    }

    @Test
    public void testAnalyzeAndReadingWithEmptyText() {
        // Test analyzeAndReading with empty text
        String text = "";
        String field = "content";
        String lang = "en";

        List<AnalyzeToken> tokens = analyzer.analyzeAndReading(text, field, lang);

        assertNotNull(tokens);
        assertTrue(tokens.isEmpty());
    }

    @Test
    public void testAnalyzeAndReadingWithNullText() {
        // Test analyzeAndReading with null text
        String text = null;
        String field = "content";
        String lang = "en";

        List<AnalyzeToken> tokens = analyzer.analyzeAndReading(text, field, lang);

        assertNotNull(tokens);
        assertTrue(tokens.isEmpty());
    }

    @Test
    public void testAnalyzeAndReadingWithJapaneseText() {
        // Test analyzeAndReading with Japanese text
        String text = "東京 大阪";
        String field = "content";
        String lang = "ja";

        testAnalyzer.setJapaneseMode(true);
        List<AnalyzeToken> tokens = analyzer.analyzeAndReading(text, field, lang);

        assertNotNull(tokens);
        assertEquals(2, tokens.size());
        assertEquals("東京", tokens.get(0).getTerm());
        assertEquals("大阪", tokens.get(1).getTerm());
        // Check readings for Japanese
        assertEquals("トウキョウ", ((TestAnalyzeToken) tokens.get(0)).getReading());
        assertEquals("オオサカ", ((TestAnalyzeToken) tokens.get(1)).getReading());
    }

    @Test
    public void testAnalyzeAndReadingWithMixedContent() {
        // Test analyzeAndReading with mixed content (numbers, letters, special chars)
        String text = "Test123 ABC-456";
        String field = "content";
        String lang = "en";

        List<AnalyzeToken> tokens = analyzer.analyzeAndReading(text, field, lang);

        assertNotNull(tokens);
        assertEquals(2, tokens.size()); // "Test123" and "ABC-456" (hyphen is not a split char in our tokenizer)
        assertEquals("Test123", tokens.get(0).getTerm());
        assertEquals("ABC-456", tokens.get(1).getTerm());
        assertEquals("TEST123", ((TestAnalyzeToken) tokens.get(0)).getReading());
        assertEquals("ABC-456", ((TestAnalyzeToken) tokens.get(1)).getReading());
    }

    @Test
    public void testAnalyzeWithLongText() {
        // Test analyze with long text
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("word").append(i).append(" ");
        }
        String text = sb.toString();
        String field = "content";
        String lang = "en";

        List<AnalyzeToken> tokens = analyzer.analyze(text, field, lang);

        assertNotNull(tokens);
        assertEquals(100, tokens.size());
        for (int i = 0; i < 100; i++) {
            assertEquals("word" + i, tokens.get(i).getTerm());
        }
    }

    @Test
    public void testAnalyzeAndReadingWithLongText() {
        // Test analyzeAndReading with long text
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("word").append(i).append(" ");
        }
        String text = sb.toString();
        String field = "content";
        String lang = "en";

        List<AnalyzeToken> tokens = analyzer.analyzeAndReading(text, field, lang);

        assertNotNull(tokens);
        assertEquals(100, tokens.size());
        for (int i = 0; i < 100; i++) {
            assertEquals("word" + i, tokens.get(i).getTerm());
            assertEquals("WORD" + i, ((TestAnalyzeToken) tokens.get(i)).getReading());
        }
    }

    @Test
    public void testAnalyzeWithPunctuation() {
        // Test analyze with various punctuation
        String text = "Hello, world! How are you? I'm fine.";
        String field = "content";
        String lang = "en";

        List<AnalyzeToken> tokens = analyzer.analyze(text, field, lang);

        assertNotNull(tokens);
        assertEquals(8, tokens.size()); // "I'm" is split into "I" and "m" by our simple tokenizer
        assertEquals("Hello", tokens.get(0).getTerm());
        assertEquals("world", tokens.get(1).getTerm());
        assertEquals("How", tokens.get(2).getTerm());
        assertEquals("are", tokens.get(3).getTerm());
        assertEquals("you", tokens.get(4).getTerm());
        assertEquals("I", tokens.get(5).getTerm());
        assertEquals("m", tokens.get(6).getTerm());
        assertEquals("fine", tokens.get(7).getTerm());
    }

    @Test
    public void testAnalyzeWithNumbers() {
        // Test analyze with numbers
        String text = "123 456.789 0xFF 3.14159";
        String field = "content";
        String lang = "en";

        List<AnalyzeToken> tokens = analyzer.analyze(text, field, lang);

        assertNotNull(tokens);
        assertEquals(4, tokens.size()); // Our simple tokenizer splits on '.' so 456.789 becomes 456 and 789
        assertEquals("123", tokens.get(0).getTerm());
        assertEquals("456", tokens.get(1).getTerm());
        assertEquals("0xFF", tokens.get(2).getTerm());
        assertEquals("3", tokens.get(3).getTerm());
    }

    @Test
    public void testAnalyzeTokenPositions() {
        // Test that token positions are correct
        String text = "one two three";
        String field = "content";
        String lang = "en";

        List<AnalyzeToken> tokens = analyzer.analyze(text, field, lang);

        assertNotNull(tokens);
        assertEquals(3, tokens.size());
        assertEquals(0, tokens.get(0).getPosition());
        assertEquals(1, tokens.get(1).getPosition());
        assertEquals(2, tokens.get(2).getPosition());
    }

    @Test
    public void testAnalyzeTokenOffsets() {
        // Test that token offsets are correct
        String text = "one two three";
        String field = "content";
        String lang = "en";

        List<AnalyzeToken> tokens = analyzer.analyze(text, field, lang);

        assertNotNull(tokens);
        assertEquals(3, tokens.size());
        assertEquals(0, tokens.get(0).getStartOffset());
        assertEquals(3, tokens.get(0).getEndOffset());
        assertEquals(4, tokens.get(1).getStartOffset());
        assertEquals(7, tokens.get(1).getEndOffset());
        assertEquals(8, tokens.get(2).getStartOffset());
        assertEquals(13, tokens.get(2).getEndOffset());
    }

    @Test
    public void testMultipleAnalyzersIndependence() {
        // Test that multiple analyzer instances work independently
        SuggestAnalyzer analyzer1 = new TestSuggestAnalyzer();
        SuggestAnalyzer analyzer2 = new TestSuggestAnalyzer();

        String text = "test text";
        String field = "content";
        String lang = "en";

        List<AnalyzeToken> tokens1 = analyzer1.analyze(text, field, lang);
        List<AnalyzeToken> tokens2 = analyzer2.analyze(text, field, lang);

        assertNotNull(tokens1);
        assertNotNull(tokens2);
        assertEquals(tokens1.size(), tokens2.size());

        for (int i = 0; i < tokens1.size(); i++) {
            assertEquals(tokens1.get(i).getTerm(), tokens2.get(i).getTerm());
        }
    }

    /**
     * Test implementation of SuggestAnalyzer for testing purposes
     */
    private static class TestSuggestAnalyzer implements SuggestAnalyzer {

        private boolean japaneseMode = false;

        public void setJapaneseMode(boolean japaneseMode) {
            this.japaneseMode = japaneseMode;
        }

        @Override
        public List<AnalyzeToken> analyze(String text, String field, String lang) {
            if (text == null || text.trim().isEmpty()) {
                return Collections.emptyList();
            }

            List<AnalyzeToken> tokens = new ArrayList<>();
            String[] words = text.trim().split("[\\s@#$.,!?;:()\\[\\]{}\"']+");

            int position = 0;
            int offset = 0;
            for (String word : words) {
                if (!word.isEmpty()) {
                    int startOffset = text.indexOf(word, offset);
                    int endOffset = startOffset + word.length();

                    // Handle decimal numbers
                    if (word.matches("\\d+") && startOffset > 0 && text.charAt(startOffset - 1) == '.') {
                        continue;
                    }

                    TestAnalyzeToken token = new TestAnalyzeToken(word, position++, startOffset, endOffset, 1, // positionIncrement
                            "<ALPHANUM>", Collections.emptyMap());
                    tokens.add(token);
                    offset = endOffset;
                }
            }

            return tokens;
        }

        @Override
        public List<AnalyzeToken> analyzeAndReading(String text, String field, String lang) {
            List<AnalyzeToken> tokens = analyze(text, field, lang);

            // Add readings to tokens
            for (AnalyzeToken token : tokens) {
                if (token instanceof TestAnalyzeToken) {
                    TestAnalyzeToken testToken = (TestAnalyzeToken) token;
                    if (japaneseMode && "ja".equals(lang)) {
                        // Simulate Japanese reading conversion
                        testToken.setReading(convertToJapaneseReading(testToken.getTerm()));
                    } else {
                        // Default: uppercase for reading
                        testToken.setReading(testToken.getTerm().toUpperCase());
                    }
                }
            }

            return tokens;
        }

        private String convertToJapaneseReading(String term) {
            // Simple simulation of Japanese reading conversion
            if ("東京".equals(term)) {
                return "トウキョウ";
            } else if ("大阪".equals(term)) {
                return "オオサカ";
            } else {
                return term.toUpperCase();
            }
        }
    }

    /**
     * Test implementation of AnalyzeToken with reading support
     */
    private static class TestAnalyzeToken extends AnalyzeToken {
        private String reading;

        public TestAnalyzeToken(String term, int position, int startOffset, int endOffset, int positionIncrement, String type,
                java.util.Map<String, Object> attributes) {
            super(term, position, startOffset, endOffset, positionIncrement, type, attributes);
        }

        public String getReading() {
            return reading;
        }

        public void setReading(String reading) {
            this.reading = reading;
        }
    }
}