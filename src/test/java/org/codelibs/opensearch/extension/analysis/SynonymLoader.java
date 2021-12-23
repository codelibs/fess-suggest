/*
 * Copyright 2012-2021 CodeLibs Project and the Others.
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
package org.codelibs.opensearch.extension.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.synonym.SolrSynonymParser;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.synonym.WordnetSynonymParser;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.env.Environment;
import org.opensearch.index.analysis.Analysis;

public class SynonymLoader {
    private File reloadableFile = null;

    private final Analyzer analyzer;

    private final Settings settings;

    private final boolean expand;

    private long reloadInterval = 0;

    private final Environment env;

    private volatile long lastModified;

    private volatile long lastChecked;

    private volatile SynonymMap synonymMap;

    public SynonymLoader(final Environment env, final Settings settings, final boolean expand, final Analyzer analyzer) {
        this.env = env;
        this.settings = settings;
        this.expand = expand;
        this.analyzer = analyzer;

        createSynonymMap(false);
    }

    public boolean isUpdate(final long time) {
        if (System.currentTimeMillis() - lastChecked > reloadInterval) {
            lastChecked = System.currentTimeMillis();
            final long timestamp = reloadableFile.lastModified();
            if (timestamp != time) {
                synchronized (reloadableFile) {
                    if (timestamp != lastModified) {
                        createSynonymMap(true);
                        return true;
                    }
                }
            }
        }

        if (lastModified != time) {
            return true;
        }

        return false;
    }

    public SynonymMap getSynonymMap() {
        return synonymMap;
    }

    protected void createSynonymMap(final boolean reload) {
        try (Reader rulesReader = getReader(reload)) {
            if (rulesReader instanceof StringReader && ((StringReader) rulesReader).toString().length() == 0) {
                synonymMap = null;
                return;
            }

            SynonymMap.Builder parser = null;

            if ("wordnet".equalsIgnoreCase(settings.get("format"))) {
                parser = new WordnetSynonymParser(true, expand, analyzer);
                ((WordnetSynonymParser) parser).parse(rulesReader);
            } else {
                parser = new SolrSynonymParser(true, expand, analyzer);
                ((SolrSynonymParser) parser).parse(rulesReader);
            }

            final SynonymMap localSynonymMap = parser.build();
            if (localSynonymMap.fst == null) {
                synonymMap = null;
                return;
            }

            synonymMap = localSynonymMap;

            if (reloadableFile != null) {
                lastModified = reloadableFile.lastModified();
            } else {
                lastModified = System.currentTimeMillis();
            }

        } catch (final Exception e) {
            throw new IllegalArgumentException("failed to build synonyms", e);
        }
    }

    private Reader getReader(final boolean reload) throws IOException {
        if (reload) {
            if (reloadableFile == null) {
                throw new IllegalArgumentException("reloadableFile is null.");
            }
            return new InputStreamReader(new FileInputStream(reloadableFile), StandardCharsets.UTF_8);
        }

        Reader reader = null;
        if (settings.getAsList("synonyms", null) != null) {
            final List<String> rules = Analysis.getWordList(env, settings, "synonyms");
            final StringBuilder sb = new StringBuilder();
            for (final String line : rules) {
                sb.append(line).append(System.getProperty("line.separator"));
            }
            reader = new StringReader(sb.toString());
        } else if (settings.get("synonyms_path") != null) {
            if (settings.getAsBoolean("dynamic_reload", false)) {
                final String filePath = settings.get("synonyms_path", null);

                if (filePath == null) {
                    throw new IllegalArgumentException("synonyms_path is not found.");
                }

                final Path path = env.configFile().resolve(filePath);

                try {
                    final File file = path.toFile();
                    if (file.exists()) {
                        reloadableFile = file;
                    }
                    reader = new BufferedReader(new InputStreamReader(path.toUri().toURL().openStream(), StandardCharsets.UTF_8));
                } catch (final Exception e) {
                    throw new IllegalArgumentException("Failed to read " + filePath, e);
                }

                reloadInterval = settings.getAsTime("reload_interval", TimeValue.timeValueMinutes(1)).getMillis();

            } else {
                reader = Analysis.getReaderFromFile(env, settings, "synonyms_path");
            }
        } else {
            reader = new StringReader("");
        }

        return reader;
    }

    public boolean isReloadable() {
        return reloadableFile != null;
    }

    public long getLastModified() {
        return lastModified;
    }

    protected static Analyzer getAnalyzer(final boolean ignoreCase) {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(final String fieldName) {
                final Tokenizer tokenizer = new KeywordTokenizer();
                final TokenStream stream = ignoreCase ? new LowerCaseFilter(tokenizer) : tokenizer;
                return new TokenStreamComponents(tokenizer, stream);
            }
        };
    }
}
