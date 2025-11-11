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
package org.codelibs.fess.suggest.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class SuggestDeleteResponseTest {

    @Test
    public void test_constructorWithoutErrors() throws Exception {
        SuggestDeleteResponse response = new SuggestDeleteResponse(null, 100);

        assertNotNull(response);
        assertEquals(100, response.getTook());
        assertFalse(response.hasError());
        assertNotNull(response.getErrors());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void test_constructorWithErrors() throws Exception {
        List<Throwable> errors = new ArrayList<>();
        errors.add(new Exception("Delete error 1"));
        errors.add(new Exception("Delete error 2"));

        SuggestDeleteResponse response = new SuggestDeleteResponse(errors, 100);

        assertNotNull(response);
        assertEquals(100, response.getTook());
        assertTrue(response.hasError());
        assertEquals(2, response.getErrors().size());
    }

    @Test
    public void test_constructorWithEmptyErrors() throws Exception {
        List<Throwable> errors = new ArrayList<>();

        SuggestDeleteResponse response = new SuggestDeleteResponse(errors, 100);

        assertNotNull(response);
        assertFalse(response.hasError());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void test_getTook() throws Exception {
        SuggestDeleteResponse response = new SuggestDeleteResponse(null, 250);

        assertEquals(250, response.getTook());
    }

    @Test
    public void test_hasError() throws Exception {
        SuggestDeleteResponse response1 = new SuggestDeleteResponse(null, 100);
        assertFalse(response1.hasError());

        List<Throwable> errors = new ArrayList<>();
        errors.add(new Exception("Test error"));
        SuggestDeleteResponse response2 = new SuggestDeleteResponse(errors, 100);
        assertTrue(response2.hasError());
    }

    @Test
    public void test_getErrors() throws Exception {
        List<Throwable> errors = new ArrayList<>();
        errors.add(new Exception("Error 1"));
        errors.add(new RuntimeException("Error 2"));

        SuggestDeleteResponse response = new SuggestDeleteResponse(errors, 100);

        assertNotNull(response.getErrors());
        assertEquals(2, response.getErrors().size());
    }

    @Test
    public void test_zeroTime() throws Exception {
        SuggestDeleteResponse response = new SuggestDeleteResponse(null, 0);

        assertEquals(0, response.getTook());
        assertFalse(response.hasError());
    }

    @Test
    public void test_multipleDifferentErrors() throws Exception {
        List<Throwable> errors = new ArrayList<>();
        errors.add(new Exception("Exception error"));
        errors.add(new RuntimeException("Runtime error"));
        errors.add(new IllegalArgumentException("Illegal argument error"));

        SuggestDeleteResponse response = new SuggestDeleteResponse(errors, 150);

        assertTrue(response.hasError());
        assertEquals(3, response.getErrors().size());
        assertEquals(150, response.getTook());
    }
}
