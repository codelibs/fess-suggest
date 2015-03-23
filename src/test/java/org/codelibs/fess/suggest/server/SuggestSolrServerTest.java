package org.codelibs.fess.suggest.server;

/*
 * Copyright 2009-2014 the CodeLibs Project and the Others.
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

import org.apache.solr.common.SolrInputDocument;
import org.codelibs.fess.suggest.FessSuggestTestCase;
import org.codelibs.fess.suggest.server.SuggestSolrServer;

public class SuggestSolrServerTest extends FessSuggestTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();
        startSolr();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        stopSolr();
    }

    public void test_add() {
        final SuggestSolrServer suggestSolrServer = new SuggestSolrServer(SOLR_URL);
        final SolrInputDocument doc = new SolrInputDocument();
        doc.setField("content_s", "aaaa");
        doc.setField("id", "1");

        try {
            suggestSolrServer.deleteAll();
            suggestSolrServer.add(doc);
            suggestSolrServer.commit();
            assertTrue(suggestSolrServer.select("*:*").getNumFound() == 1);
            suggestSolrServer.deleteAll();
            suggestSolrServer.commit();
            assertTrue(suggestSolrServer.select("*:*").getNumFound() == 0);
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }
}