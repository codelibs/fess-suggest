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

import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.transport.client.Client;

/**
 * SuggestBulkFileWriter is an implementation of the SuggestWriter interface.
 * This class is responsible for writing and deleting suggest items in bulk.
 *
 * <p>Currently, the methods in this class throw UnsupportedOperationException
 * as they are not yet implemented.</p>
 *
 * @see SuggestWriter
 */
public class SuggestBulkFileWriter implements SuggestWriter {
    @Override
    public SuggestWriterResult write(final Client client, final SuggestSettings settings, final String index, final SuggestItem[] items,
            final boolean update) {
        throw new UnsupportedOperationException("not yet.");
    }

    @Override
    public SuggestWriterResult delete(final Client client, final SuggestSettings settings, final String index, final String id) {
        throw new UnsupportedOperationException("not yet.");
    }

    @Override
    public SuggestWriterResult deleteByQuery(final Client client, final SuggestSettings settings, final String index,
            final QueryBuilder queryBuilder) {
        throw new UnsupportedOperationException("deleteByQuery is unsupported.");
    }
}
