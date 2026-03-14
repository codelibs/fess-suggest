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

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.suggest.analysis.SuggestAnalyzer;
import org.codelibs.fess.suggest.concurrent.Deferred;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.ElevateWord;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.contents.ContentsParser;
import org.codelibs.fess.suggest.index.contents.DefaultContentsParser;
import org.codelibs.fess.suggest.index.contents.document.DocumentReader;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLog;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLogReader;
import org.codelibs.fess.suggest.index.operations.ContentIndexingContext;
import org.codelibs.fess.suggest.index.operations.ContentIndexingOperations;
import org.codelibs.fess.suggest.index.operations.DeletionOperations;
import org.codelibs.fess.suggest.index.operations.IndexingOperations;
import org.codelibs.fess.suggest.index.operations.WordManagementOperations;
import org.codelibs.fess.suggest.index.writer.SuggestIndexWriter;
import org.codelibs.fess.suggest.index.writer.SuggestWriter;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.transport.client.Client;

/**
 * The SuggestIndexer class is responsible for indexing and managing suggest items in an OpenSearch index.
 * It provides methods to index, delete, and manage suggest items, including handling bad words and elevate words.
 *
 * <p>This class acts as a facade, delegating to internal operation classes for the actual work:
 * <ul>
 * <li>{@code IndexingOperations} - Core indexing functionality with bad word filtering</li>
 * <li>{@code DeletionOperations} - Deletion by ID, query, or kind</li>
 * <li>{@code WordManagementOperations} - Bad word and elevate word management</li>
 * <li>{@code ContentIndexingOperations} - Content-based indexing from query logs, documents, etc.</li>
 * </ul>
 *
 * <p>Constructor:
 * <ul>
 * <li>{@link #SuggestIndexer(Client, String, ReadingConverter, ReadingConverter, Normalizer, SuggestAnalyzer, SuggestSettings, ExecutorService)}
 * </ul>
 *
 * <p>Methods:
 * <ul>
 * <li>{@link #index(SuggestItem)} - Index a single suggest item.</li>
 * <li>{@link #index(SuggestItem[])} - Index multiple suggest items.</li>
 * <li>{@link #delete(String)} - Delete a suggest item by ID.</li>
 * <li>{@link #deleteByQuery(String)} - Delete suggest items by query string.</li>
 * <li>{@link #deleteByQuery(QueryBuilder)} - Delete suggest items by query builder.</li>
 * <li>{@link #deleteAll()} - Delete all suggest items.</li>
 * <li>{@link #deleteDocumentWords()} - Delete document words.</li>
 * <li>{@link #deleteQueryWords()} - Delete query words.</li>
 * <li>{@link #indexFromQueryLog(QueryLog)} - Index from a single query log.</li>
 * <li>{@link #indexFromQueryLog(QueryLog[])} - Index from multiple query logs.</li>
 * <li>{@link #indexFromQueryLog(QueryLogReader, int, long)} - Index from query log reader with specified document per request and request interval.</li>
 * <li>{@link #indexFromDocument(Map[])} - Index from an array of documents.</li>
 * <li>{@link #indexFromSearchWord(String, String[], String[], String[], int, String[])} - Index from search word.</li>
 * <li>{@link #addBadWord(String, boolean)} - Add a bad word and optionally apply it.</li>
 * <li>{@link #deleteBadWord(String)} - Delete a bad word.</li>
 * <li>{@link #addElevateWord(ElevateWord, boolean)} - Add an elevate word and optionally apply it.</li>
 * <li>{@link #deleteElevateWord(String, boolean)} - Delete an elevate word and optionally apply it.</li>
 * <li>{@link #restoreElevateWord()} - Restore elevate words.</li>
 * <li>{@link #deleteOldWords(ZonedDateTime)} - Delete old words based on a threshold date.</li>
 * <li>{@link #setIndexName(String)} - Set the index name.</li>
 * <li>{@link #setSupportedFields(String[])} - Set the supported fields.</li>
 * <li>{@link #setTagFieldNames(String[])} - Set the tag field names.</li>
 * <li>{@link #setRoleFieldName(String)} - Set the role field name.</li>
 * <li>{@link #setReadingConverter(ReadingConverter)} - Set the reading converter.</li>
 * <li>{@link #setNormalizer(Normalizer)} - Set the normalizer.</li>
 * <li>{@link #setAnalyzer(SuggestAnalyzer)} - Set the analyzer.</li>
 * <li>{@link #setContentsParser(ContentsParser)} - Set the contents parser.</li>
 * <li>{@link #setSuggestWriter(SuggestWriter)} - Set the suggest writer.</li>
 * </ul>
 *
 * <p>Fields:
 * <ul>
 * <li>{@link #logger} - Logger instance.</li>
 * <li>{@link #client} - OpenSearch client.</li>
 * <li>{@link #index} - Index name.</li>
 * <li>{@link #settings} - Suggest settings.</li>
 * <li>{@link #supportedFields} - Supported fields for suggestions.</li>
 * <li>{@link #tagFieldNames} - Tag field names.</li>
 * <li>{@link #roleFieldName} - Role field name.</li>
 * <li>{@link #langFieldName} - Language field name.</li>
 * <li>{@link #badWords} - List of bad words.</li>
 * <li>{@link #parallel} - Flag for parallel processing.</li>
 * <li>{@link #readingConverter} - Reading converter.</li>
 * <li>{@link #contentsReadingConverter} - Contents reading converter.</li>
 * <li>{@link #normalizer} - Normalizer.</li>
 * <li>{@link #analyzer} - Suggest analyzer.</li>
 * <li>{@link #contentsParser} - Contents parser.</li>
 * <li>{@link #suggestWriter} - Suggest writer.</li>
 * <li>{@link #threadPool} - Executor service for thread pool.</li>
 * </ul>
 */
public class SuggestIndexer {
    private final static Logger logger = LogManager.getLogger(SuggestIndexer.class);

    /** The suggest analyzer. */
    protected SuggestAnalyzer analyzer;
    /** Bad words. */
    protected String[] badWords;
    /** The OpenSearch client. */
    protected final Client client;
    /** The contents parser. */
    protected ContentsParser contentsParser;
    /** The contents reading converter. */
    protected ReadingConverter contentsReadingConverter;
    /** The index name. */
    protected String index;
    /** The language field name. */
    protected String langFieldName;
    /** The normalizer. */
    protected Normalizer normalizer;
    /** Flag for parallel processing. */
    protected boolean parallel;
    /** The reading converter. */
    protected ReadingConverter readingConverter;
    /** The role field name. */
    protected String roleFieldName;
    /** The suggest settings. */
    protected SuggestSettings settings;
    /** The suggest writer. */
    protected SuggestWriter suggestWriter;
    /** Supported fields for suggestions. */
    protected String[] supportedFields;
    /** Tag field names. */
    protected String[] tagFieldNames;
    /** The thread pool. */
    protected ExecutorService threadPool;

    // Internal operation classes
    private IndexingOperations indexingOps;
    private DeletionOperations deletionOps;
    private WordManagementOperations wordMgmtOps;
    private ContentIndexingOperations contentOps;

    /** Flag indicating that operations need to be re-initialized. Not thread-safe: setters are configuration-time only. */
    private volatile boolean operationsStale = false;

    /**
     * Constructor for SuggestIndexer.
     * @param client The OpenSearch client.
     * @param index The index name.
     * @param readingConverter The reading converter.
     * @param contentsReadingConverter The contents reading converter.
     * @param normalizer The normalizer.
     * @param analyzer The suggest analyzer.
     * @param settings The suggest settings.
     * @param threadPool The thread pool.
     */
    public SuggestIndexer(final Client client, final String index, final ReadingConverter readingConverter,
            final ReadingConverter contentsReadingConverter, final Normalizer normalizer, final SuggestAnalyzer analyzer,
            final SuggestSettings settings, final ExecutorService threadPool) {
        this.client = client;
        this.index = index;

        supportedFields = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS);
        badWords = settings.badword().get(true);
        tagFieldNames = settings.getAsString(SuggestSettings.DefaultKeys.TAG_FIELD_NAME, StringUtil.EMPTY).split(",");
        roleFieldName = settings.getAsString(SuggestSettings.DefaultKeys.ROLE_FIELD_NAME, StringUtil.EMPTY);
        langFieldName = settings.getAsString(SuggestSettings.DefaultKeys.LANG_FIELD_NAME, StringUtil.EMPTY);
        parallel = settings.getAsBoolean(SuggestSettings.DefaultKeys.PARALLEL_PROCESSING, false);
        this.readingConverter = readingConverter;
        this.contentsReadingConverter = contentsReadingConverter;
        this.normalizer = normalizer;
        this.analyzer = analyzer;
        this.settings = settings;

        contentsParser = new DefaultContentsParser();
        suggestWriter = new SuggestIndexWriter();

        this.threadPool = threadPool;

        initializeOperations();
    }

    /**
     * Initializes the internal operation classes.
     */
    private void initializeOperations() {
        indexingOps = new IndexingOperations(client, settings, suggestWriter);
        deletionOps = new DeletionOperations(client, settings, suggestWriter);
        wordMgmtOps = new WordManagementOperations(settings, normalizer, indexingOps, deletionOps, this::getBadWords);
        contentOps = new ContentIndexingOperations(client, settings, threadPool, indexingOps, contentsParser, analyzer, readingConverter,
                contentsReadingConverter, normalizer, parallel);
        operationsStale = false;
    }

    /**
     * Ensures operations are initialized and up-to-date.
     * Called before any operation method to lazily re-initialize if a setter has been called.
     */
    private void ensureOperations() {
        if (operationsStale) {
            initializeOperations();
        }
    }

    /**
     * Creates a ContentIndexingContext from the current state of this indexer.
     * @return A new ContentIndexingContext.
     */
    private ContentIndexingContext createContext() {
        return new ContentIndexingContext(index, supportedFields, tagFieldNames, roleFieldName, langFieldName, badWords);
    }

    /**
     * Gets the current bad words array.
     * @return The bad words array.
     */
    private String[] getBadWords() {
        return badWords;
    }

    /**
     * Indexes a single suggest item.
     * @param item The suggest item to index.
     * @return The SuggestIndexResponse.
     */
    public SuggestIndexResponse index(final SuggestItem item) {
        ensureOperations();
        return indexingOps.index(index, item, badWords);
    }

    /**
     * Indexes multiple suggest items.
     * @param items The suggest items to index.
     * @return The SuggestIndexResponse.
     */
    public SuggestIndexResponse index(final SuggestItem[] items) {
        ensureOperations();
        return indexingOps.index(index, items, badWords);
    }

    /**
     * Deletes a suggest item by ID.
     * @param id The ID of the item to delete.
     * @return The SuggestDeleteResponse.
     */
    public SuggestDeleteResponse delete(final String id) {
        ensureOperations();
        return deletionOps.delete(index, id);
    }

    /**
     * Deletes suggest items by a query string.
     * @param queryString The query string.
     * @return The SuggestDeleteResponse.
     */
    public SuggestDeleteResponse deleteByQuery(final String queryString) {
        ensureOperations();
        return deletionOps.deleteByQuery(index, queryString);
    }

    /**
     * Deletes suggest items by a query builder.
     * @param queryBuilder The query builder.
     * @return The SuggestDeleteResponse.
     */
    public SuggestDeleteResponse deleteByQuery(final QueryBuilder queryBuilder) {
        ensureOperations();
        return deletionOps.deleteByQuery(index, queryBuilder);
    }

    /**
     * Deletes all suggest items.
     * @return The SuggestDeleteResponse.
     */
    public SuggestDeleteResponse deleteAll() {
        ensureOperations();
        final SuggestDeleteResponse response = deletionOps.deleteAll(index);
        restoreElevateWord();
        return response;
    }

    /**
     * Deletes document words.
     * @return The SuggestDeleteResponse.
     */
    public SuggestDeleteResponse deleteDocumentWords() {
        ensureOperations();
        return deletionOps.deleteDocumentWords(index);
    }

    /**
     * Deletes query words.
     * @return The SuggestDeleteResponse.
     */
    public SuggestDeleteResponse deleteQueryWords() {
        ensureOperations();
        return deletionOps.deleteQueryWords(index);
    }

    /**
     * Indexes a single query log.
     * @param queryLog The query log to index.
     * @return The SuggestIndexResponse.
     */
    public SuggestIndexResponse indexFromQueryLog(final QueryLog queryLog) {
        ensureOperations();
        return contentOps.indexFromQueryLog(createContext(), queryLog);
    }

    /**
     * Indexes multiple query logs.
     * @param queryLogs The query logs to index.
     * @return The SuggestIndexResponse.
     */
    public SuggestIndexResponse indexFromQueryLog(final QueryLog[] queryLogs) {
        ensureOperations();
        return contentOps.indexFromQueryLog(createContext(), queryLogs);
    }

    /**
     * Indexes documents from a query log reader asynchronously.
     * @param queryLogReader The query log reader.
     * @param docPerReq The number of documents to process per request.
     * @param requestInterval The interval between requests.
     * @return A Promise that will be resolved with the SuggestIndexResponse or rejected with an error.
     */
    public Deferred<SuggestIndexResponse>.Promise indexFromQueryLog(final QueryLogReader queryLogReader, final int docPerReq,
            final long requestInterval) {
        ensureOperations();
        return contentOps.indexFromQueryLog(createContext(), queryLogReader, docPerReq, requestInterval);
    }

    /**
     * Indexes documents from an array of maps.
     * @param documents The documents to index.
     * @return The SuggestIndexResponse.
     */
    public SuggestIndexResponse indexFromDocument(final Map<String, Object>[] documents) {
        ensureOperations();
        return contentOps.indexFromDocument(createContext(), documents);
    }

    /**
     * Indexes documents from a DocumentReader asynchronously.
     * @param reader The supplier for DocumentReader.
     * @param docPerReq The number of documents to process per request.
     * @param waitController The runnable to control waiting between requests.
     * @return A Promise that will be resolved with the SuggestIndexResponse or rejected with an error.
     */
    public Deferred<SuggestIndexResponse>.Promise indexFromDocument(final Supplier<DocumentReader> reader, final int docPerReq,
            final Runnable waitController) {
        ensureOperations();
        return contentOps.indexFromDocument(createContext(), reader, docPerReq, waitController);
    }

    /**
     * Indexes a search word.
     * @param searchWord The search word.
     * @param fields The fields.
     * @param tags The tags.
     * @param roles The roles.
     * @param num The number.
     * @param langs The languages.
     * @return The SuggestIndexResponse.
     */
    public SuggestIndexResponse indexFromSearchWord(final String searchWord, final String[] fields, final String[] tags,
            final String[] roles, final int num, final String[] langs) {
        ensureOperations();
        return contentOps.indexFromSearchWord(index, searchWord, fields, tags, roles, num, langs, badWords);
    }

    /**
     * Adds a bad word.
     * @param badWord The bad word to add.
     * @param apply Whether to apply the change immediately.
     * @return The SuggestDeleteResponse.
     */
    public SuggestDeleteResponse addBadWord(final String badWord, final boolean apply) {
        ensureOperations();
        final SuggestDeleteResponse response = wordMgmtOps.addBadWord(index, badWord, apply);
        badWords = settings.badword().get(true);
        return response;
    }

    /**
     * Deletes a bad word.
     * @param badWord The bad word to delete.
     */
    public void deleteBadWord(final String badWord) {
        ensureOperations();
        wordMgmtOps.deleteBadWord(badWord);
    }

    /**
     * Adds an elevate word.
     * @param elevateWord The elevate word to add.
     * @param apply Whether to apply the change immediately.
     * @return The SuggestIndexResponse.
     */
    public SuggestIndexResponse addElevateWord(final ElevateWord elevateWord, final boolean apply) {
        ensureOperations();
        return wordMgmtOps.addElevateWord(index, elevateWord, apply);
    }

    /**
     * Deletes an elevate word.
     * @param elevateWord The elevate word to delete.
     * @param apply Whether to apply the change immediately.
     * @return The SuggestDeleteResponse.
     */
    public SuggestDeleteResponse deleteElevateWord(final String elevateWord, final boolean apply) {
        ensureOperations();
        return wordMgmtOps.deleteElevateWord(index, elevateWord, apply);
    }

    /**
     * Restores elevate words.
     * @return The SuggestIndexResponse.
     */
    public SuggestIndexResponse restoreElevateWord() {
        ensureOperations();
        return wordMgmtOps.restoreElevateWord(index);
    }

    /**
     * Deletes old words based on a threshold date.
     * @param threshold The threshold date.
     * @return The SuggestDeleteResponse.
     */
    public SuggestDeleteResponse deleteOldWords(final ZonedDateTime threshold) {
        ensureOperations();
        return deletionOps.deleteOldWords(index, threshold);
    }

    /**
     * Sets the index name.
     * @param index The index name.
     * @return This SuggestIndexer instance.
     */
    public SuggestIndexer setIndexName(final String index) {
        this.index = index;
        return this;
    }

    /**
     * Sets the supported fields.
     * @param supportedFields The supported fields.
     * @return This SuggestIndexer instance.
     */
    public SuggestIndexer setSupportedFields(final String[] supportedFields) {
        this.supportedFields = supportedFields;
        return this;
    }

    /**
     * Sets the tag field names.
     * @param tagFieldNames The tag field names.
     * @return This SuggestIndexer instance.
     */
    public SuggestIndexer setTagFieldNames(final String[] tagFieldNames) {
        this.tagFieldNames = tagFieldNames;
        return this;
    }

    /**
     * Sets the role field name.
     * @param roleFieldName The role field name.
     * @return This SuggestIndexer instance.
     */
    public SuggestIndexer setRoleFieldName(final String roleFieldName) {
        this.roleFieldName = roleFieldName;
        return this;
    }

    /**
     * Sets the reading converter.
     * @param readingConverter The reading converter.
     * @return This SuggestIndexer instance.
     */
    public SuggestIndexer setReadingConverter(final ReadingConverter readingConverter) {
        this.readingConverter = readingConverter;
        operationsStale = true;
        return this;
    }

    /**
     * Sets the normalizer.
     * @param normalizer The normalizer.
     * @return This SuggestIndexer instance.
     */
    public SuggestIndexer setNormalizer(final Normalizer normalizer) {
        this.normalizer = normalizer;
        operationsStale = true;
        return this;
    }

    /**
     * Sets the analyzer.
     * @param analyzer The analyzer.
     * @return This SuggestIndexer instance.
     */
    public SuggestIndexer setAnalyzer(final SuggestAnalyzer analyzer) {
        this.analyzer = analyzer;
        operationsStale = true;
        return this;
    }

    /**
     * Sets the contents parser.
     * @param contentsParser The contents parser.
     * @return This SuggestIndexer instance.
     */
    public SuggestIndexer setContentsParser(final ContentsParser contentsParser) {
        this.contentsParser = contentsParser;
        operationsStale = true;
        return this;
    }

    /**
     * Sets the suggest writer.
     * @param suggestWriter The suggest writer.
     * @return This SuggestIndexer instance.
     */
    public SuggestIndexer setSuggestWriter(final SuggestWriter suggestWriter) {
        this.suggestWriter = suggestWriter;
        operationsStale = true;
        return this;
    }

}
