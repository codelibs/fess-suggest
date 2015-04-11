package org.codelibs.fess.suggest.index;

import org.apache.lucene.analysis.Analyzer;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.index.contents.ContentsParser;
import org.codelibs.fess.suggest.index.contents.DefaultContentsParser;
import org.codelibs.fess.suggest.index.contents.document.DocumentReader;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLogReader;
import org.codelibs.fess.suggest.index.writer.SuggestIndexWriter;
import org.codelibs.fess.suggest.index.writer.SuggestWriter;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.elasticsearch.client.Client;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SuggestIndexer {
    protected final Client client;
    protected String index;
    protected String type;
    protected SuggestSettings settings;

    protected String[] supportedFields;
    protected String tagFieldName;
    protected String roleFieldName;

    protected ReadingConverter readingConverter;
    protected Normalizer normalizer;
    protected Analyzer analyzer;

    protected ContentsParser contentsParser;
    protected SuggestWriter suggestWriter;

    public SuggestIndexer(final Client client, final String index, final String type, final String[] supportedField,
            final String tagFieldName, final String roleFieldName, final ReadingConverter readingConverter, final Normalizer normalizer,
            final Analyzer analyzer, final SuggestSettings settings) {
        this.client = client;
        this.index = index;
        this.type = type;
        this.supportedFields = supportedField;
        this.tagFieldName = tagFieldName;
        this.roleFieldName = roleFieldName;
        this.readingConverter = readingConverter;
        this.normalizer = normalizer;
        this.analyzer = analyzer;
        this.settings = settings;

        this.contentsParser = new DefaultContentsParser();
        this.suggestWriter = new SuggestIndexWriter();
    }

    //TODO return result
    public void index(final SuggestItem item) {
        index(new SuggestItem[] { item });
    }

    //TODO return result
    public void index(final SuggestItem[] items) {
        suggestWriter.write(client, settings, index, type, items);
    }

    public void indexFromQueryString(final String queryString) {
        indexFromQueryString(new String[] { queryString });
    }

    public void indexFromQueryString(final String[] queryStrings) {
        final List<SuggestItem> items = new ArrayList<>(queryStrings.length * supportedFields.length);
        for (String queryString : queryStrings) {
            items.addAll(contentsParser.parseQueryString(queryString, supportedFields, readingConverter, normalizer));
        }
        index(items.toArray(new SuggestItem[items.size()]));
    }

    public IndexingStatus indexFromDocument(final DocumentReader documentReader) {
        final IndexingStatus indexingStatus = new IndexingStatus();
        indexingStatus.running.set(true);
        indexingStatus.done.set(false);

        //TODO thread pool
        Thread th = new Thread(() -> {
            String doc;
            while ((doc = documentReader.read()) != null) {
                //TODO
            }

            indexingStatus.running.set(false);
            indexingStatus.done.set(true);
        });

        th.start();
        return indexingStatus;
    }

    public IndexingStatus indexFromQueryLog(final QueryLogReader queryLogReader) {
        final IndexingStatus indexingStatus = new IndexingStatus();
        indexingStatus.running.set(true);
        indexingStatus.done.set(false);

        //TODO thread pool
        Thread th = new Thread(() -> {
            int maxNum = 1000;

            List<String> queryStrings = new ArrayList<>(maxNum);
            String queryString = queryLogReader.read();
            while (queryString != null) {
                queryStrings.add(queryString);
                queryString = queryLogReader.read();
                if (queryString == null || queryStrings.size() >= maxNum) {
                    indexFromQueryString(queryStrings.toArray(new String[queryStrings.size()]));
                    queryStrings.clear();
                }
            }

            indexingStatus.running.set(false);
            indexingStatus.done.set(true);
        });

        th.start();
        return indexingStatus;
    }

    public SuggestIndexer index(String index) {
        this.index = index;
        return this;
    }

    public SuggestIndexer type(String type) {
        this.type = type;
        return this;
    }

    public SuggestIndexer supportedFields(String[] supportedFields) {
        this.supportedFields = supportedFields;
        return this;
    }

    public SuggestIndexer tagFieldName(String tagFieldName) {
        this.tagFieldName = tagFieldName;
        return this;
    }

    public SuggestIndexer roleFieldName(String roleFieldName) {
        this.roleFieldName = roleFieldName;
        return this;
    }

    public SuggestIndexer readingConverter(ReadingConverter readingConverter) {
        this.readingConverter = readingConverter;
        return this;
    }

    public SuggestIndexer normalizer(Normalizer normalizer) {
        this.normalizer = normalizer;
        return this;
    }

    public SuggestIndexer analyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
        return this;
    }

    public SuggestIndexer contentsParser(ContentsParser contentsParser) {
        this.contentsParser = contentsParser;
        return this;
    }

    public SuggestIndexer suggestWriter(SuggestWriter suggestWriter) {
        this.suggestWriter = suggestWriter;
        return this;
    }

    public class IndexingStatus {
        final protected AtomicBoolean running = new AtomicBoolean(false);
        final protected AtomicBoolean done = new AtomicBoolean(false);
        final protected List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        public boolean isStarted() {
            return running.get() || done.get();
        }

        public boolean isDone() {
            return done.get();
        }

        public boolean hasError() {
            return errors.size() > 0;
        }

        public List<Throwable> errors() {
            return errors;
        }
    }
}
