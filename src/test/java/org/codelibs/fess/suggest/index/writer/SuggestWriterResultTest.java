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
package org.codelibs.fess.suggest.index.writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SuggestWriterResultTest {

    @Test
    public void test_constructor() throws Exception {
        SuggestWriterResult result = new SuggestWriterResult();

        assertNotNull(result);
        assertFalse(result.hasFailure());
        assertNotNull(result.getFailures());
        assertEquals(0, result.getFailures().size());
    }

    @Test
    public void test_addFailure() throws Exception {
        SuggestWriterResult result = new SuggestWriterResult();

        Exception exception = new Exception("Test failure");
        result.addFailure(exception);

        assertTrue(result.hasFailure());
        assertEquals(1, result.getFailures().size());
        assertEquals(exception, result.getFailures().get(0));
    }

    @Test
    public void test_addMultipleFailures() throws Exception {
        SuggestWriterResult result = new SuggestWriterResult();

        Exception exception1 = new Exception("Failure 1");
        Exception exception2 = new RuntimeException("Failure 2");
        Exception exception3 = new IllegalArgumentException("Failure 3");

        result.addFailure(exception1);
        result.addFailure(exception2);
        result.addFailure(exception3);

        assertTrue(result.hasFailure());
        assertEquals(3, result.getFailures().size());
    }

    @Test
    public void test_hasFailure() throws Exception {
        SuggestWriterResult result = new SuggestWriterResult();

        assertFalse(result.hasFailure());

        result.addFailure(new Exception("Test"));

        assertTrue(result.hasFailure());
    }

    @Test
    public void test_getFailures() throws Exception {
        SuggestWriterResult result = new SuggestWriterResult();

        Exception exception1 = new Exception("Error 1");
        Exception exception2 = new RuntimeException("Error 2");

        result.addFailure(exception1);
        result.addFailure(exception2);

        assertNotNull(result.getFailures());
        assertEquals(2, result.getFailures().size());
        assertEquals(exception1, result.getFailures().get(0));
        assertEquals(exception2, result.getFailures().get(1));
    }

    @Test
    public void test_noFailures() throws Exception {
        SuggestWriterResult result = new SuggestWriterResult();

        assertFalse(result.hasFailure());
        assertEquals(0, result.getFailures().size());
    }

    @Test
    public void test_failuresList() throws Exception {
        SuggestWriterResult result = new SuggestWriterResult();

        for (int i = 0; i < 10; i++) {
            result.addFailure(new Exception("Error " + i));
        }

        assertEquals(10, result.getFailures().size());
        assertTrue(result.hasFailure());
    }

    @Test
    public void test_differentExceptionTypes() throws Exception {
        SuggestWriterResult result = new SuggestWriterResult();

        result.addFailure(new Exception("Exception"));
        result.addFailure(new RuntimeException("RuntimeException"));
        result.addFailure(new IllegalStateException("IllegalStateException"));
        result.addFailure(new NullPointerException("NullPointerException"));

        assertEquals(4, result.getFailures().size());
        assertTrue(result.getFailures().get(0) instanceof Exception);
        assertTrue(result.getFailures().get(1) instanceof RuntimeException);
        assertTrue(result.getFailures().get(2) instanceof IllegalStateException);
        assertTrue(result.getFailures().get(3) instanceof NullPointerException);
    }
}
