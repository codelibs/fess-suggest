package org.codelibs.fess.suggest.index;

import org.apache.lucene.analysis.Analyzer;
import org.codelibs.fess.suggest.constants.FieldNames;
import org.codelibs.fess.suggest.converter.ReadingConverter;
import org.codelibs.fess.suggest.entity.ElevateWord;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.index.contents.ContentsParser;
import org.codelibs.fess.suggest.index.contents.DefaultContentsParser;
import org.codelibs.fess.suggest.index.contents.document.DocumentReader;
import org.codelibs.fess.suggest.index.contents.querylog.QueryLogReader;
import org.codelibs.fess.suggest.index.writer.SuggestIndexWriter;
import org.codelibs.fess.suggest.index.writer.SuggestWriter;
import org.codelibs.fess.suggest.normalizer.Normalizer;
import org.codelibs.fess.suggest.settings.SuggestSettings;
import org.codelibs.fess.suggest.util.SuggestUtil;
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
    protected String[] ngWords;

    protected ReadingConverter readingConverter;
    protected Normalizer normalizer;
    protected Analyzer analyzer;

    protected ContentsParser contentsParser;
    protected SuggestWriter suggestWriter;

    public SuggestIndexer(final Client client, final String index, final String type, final ReadingConverter readingConverter,
            final Normalizer normalizer, final Analyzer analyzer, final SuggestSettings settings) {
        this.client = client;
        this.index = index;
        this.type = type;

        this.supportedFields = settings.array().get(SuggestSettings.DefaultKeys.SUPPORTED_FIELDS);
        this.ngWords = settings.ngword().get();
        this.tagFieldName = settings.getAsString(SuggestSettings.DefaultKeys.TAG_FIELD_NAME, "");
        this.roleFieldName = settings.getAsString(SuggestSettings.DefaultKeys.ROLE_FIELD_NAME, "");
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
        SuggestItem[] array = new SuggestItem[items.length];
        int size = 0;
        for (SuggestItem item : items) {
            if (!item.isNgWord(ngWords)) {
                array[size++] = item;
            }
        }
        SuggestItem[] newSizeArray = Arrays.copyOf(array, size);
        suggestWriter.write(client, settings, index, type, newSizeArray);
    }

    public void delete(final String id) {
        suggestWriter.delete(client, settings, index, type, id);
    }

    public void deleteByQuery(final String queryString) {
        suggestWriter.deleteByQuery(client, settings, index, type, queryString);
    }

    public void indexFromQueryString(final String queryString) {
        indexFromQueryString(new String[] { queryString });
    }

    public void indexFromQueryString(final String[] queryStrings) {
        try {
            final List<SuggestItem> items = new ArrayList<>(queryStrings.length * supportedFields.length);
            for (String queryString : queryStrings) {
                items.addAll(contentsParser.parseQueryString(queryString, supportedFields, readingConverter, normalizer));
            }
            index(items.toArray(new SuggestItem[items.size()]));
        } catch (Exception e) {
            throw new SuggesterException("Failed to index from query_string.", e);
        }
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

    public void indexFromDocument(final Map<String, Object>[] documents) {
        List<SuggestItem> items = new ArrayList<>(documents.length * supportedFields.length * 100); //TODO
        try {
            for (Map<String, Object> document : documents) {
                items.addAll(contentsParser.parseDocument(document, supportedFields, readingConverter, normalizer, analyzer));
            }
        } catch (Exception e) {
            throw new SuggesterException("Failed to index from document", e);
        }
        index(items.toArray(new SuggestItem[items.size()]));
    }

    public IndexingStatus indexFromDocument(final DocumentReader documentReader) {
        final IndexingStatus indexingStatus = new IndexingStatus();
        indexingStatus.running.set(true);
        indexingStatus.done.set(false);

        //TODO thread pool
        Thread th = new Thread(() -> {
            Map<String, Object> doc;
            while ((doc = documentReader.read()) != null) {
                //TODO
            }

            indexingStatus.running.set(false);
            indexingStatus.done.set(true);
        });

        th.start();
        return indexingStatus;
    }

    public void addNgWord(String ngWord) {
        deleteByQuery(FieldNames.TEXT + ":*\"" + ngWord + "\"*");
        settings.ngword().add(ngWord);
        ngWords = settings.ngword().get();
    }

    public void indexElevateWord(ElevateWord elevateWord) {
        settings.elevateWord().add(elevateWord);
        index(new SuggestItem(new String[] { elevateWord.getElevateWord() }, new String[][] { elevateWord.getReadings().toArray(
                new String[elevateWord.getReadings().size()]) }, 1, elevateWord.getBoost(), null, null, SuggestItem.Kind.USER));
    }

    public void deleteElevateWord(String elevateWord) {
        settings.elevateWord().delete(elevateWord);
        delete(SuggestUtil.createSuggestTextId(elevateWord));
    }

    public SuggestIndexer setIndex(String index) {
        this.index = index;
        return this;
    }

    public SuggestIndexer setType(String type) {
        this.type = type;
        return this;
    }

    public SuggestIndexer setSupportedFields(String[] supportedFields) {
        this.supportedFields = supportedFields;
        return this;
    }

    public SuggestIndexer setTagFieldName(String tagFieldName) {
        this.tagFieldName = tagFieldName;
        return this;
    }

    public SuggestIndexer setRoleFieldName(String roleFieldName) {
        this.roleFieldName = roleFieldName;
        return this;
    }

    public SuggestIndexer setReadingConverter(ReadingConverter readingConverter) {
        this.readingConverter = readingConverter;
        return this;
    }

    public SuggestIndexer setNormalizer(Normalizer normalizer) {
        this.normalizer = normalizer;
        return this;
    }

    public SuggestIndexer setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
        return this;
    }

    public SuggestIndexer setContentsParser(ContentsParser contentsParser) {
        this.contentsParser = contentsParser;
        return this;
    }

    public SuggestIndexer setSuggestWriter(SuggestWriter suggestWriter) {
        this.suggestWriter = suggestWriter;
        return this;
    }

    public static class IndexingStatus {
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
