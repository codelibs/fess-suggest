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
package org.codelibs.opensearch.extension.analysis;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EnumMap;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Type;
import org.apache.lucene.analysis.ja.dict.Dictionary;
import org.apache.lucene.analysis.ja.dict.TokenInfoFST;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.util.AttributeSource;
import org.codelibs.opensearch.extension.kuromoji.index.analysis.KuromojiTokenizerFactory;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenizerFactory;

public class ReloadableKuromojiTokenizerFactory extends AbstractTokenizerFactory {

    private static final boolean VERBOSE = false; // debug

    protected static final Reader ILLEGAL_STATE_READER = new Reader() {
        @Override
        public int read(final char[] cbuf, final int off, final int len) {
            throw new IllegalStateException("TokenStream contract violation: reset()/close() call missing, "
                    + "reset() called multiple times, or subclass does not call super.reset(). "
                    + "Please see Javadocs of TokenStream class for more information about the correct consuming workflow.");
        }

        @Override
        public void close() {
        }
    };

    protected final Field inputPendingField;

    protected final Field userDictionaryField;

    protected final Field userFSTField;

    protected final Field userFSTReaderField;

    protected final Field dictionaryMapField;

    private final Environment env;

    private final Settings settings;

    private File reloadableFile = null;

    protected volatile long dictionaryTimestamp;

    private volatile long lastChecked;

    private long reloadInterval;

    private volatile UserDictionary userDictionary;

    private final Mode mode;

    private final boolean discartPunctuation;

    public ReloadableKuromojiTokenizerFactory(final IndexSettings indexSettings, final Environment env, final String name,
            final Settings settings) {
        super(indexSettings, settings, name);
        this.env = env;
        this.settings = settings;
        mode = KuromojiTokenizerFactory.getMode(settings);
        userDictionary = KuromojiTokenizerFactory.getUserDictionary(env, settings);
        discartPunctuation = settings.getAsBoolean("discard_punctuation", true);

        inputPendingField = getAccessibleField(Tokenizer.class, "inputPending");
        userDictionaryField = getAccessibleField(JapaneseTokenizer.class, "userDictionary");
        userFSTField = getAccessibleField(JapaneseTokenizer.class, "userFST");
        userFSTReaderField = getAccessibleField(JapaneseTokenizer.class, "userFSTReader");
        dictionaryMapField = getAccessibleField(JapaneseTokenizer.class, "dictionaryMap");

        dictionaryTimestamp = System.currentTimeMillis();

        final String monitoringFilePath = settings.get("user_dictionary");
        if (monitoringFilePath != null) {
            final Path path = env.configDir().resolve(monitoringFilePath);

            try {
                final File file = path.toFile();
                if (file.exists()) {
                    reloadableFile = file;
                    dictionaryTimestamp = reloadableFile.lastModified();

                    reloadInterval = settings.getAsTime("reload_interval", TimeValue.timeValueMinutes(1)).getMillis();

                    if (VERBOSE) {
                        System.out.println("Check " + reloadableFile.getAbsolutePath() + " (interval: " + reloadInterval + "ms)");
                    }
                }
            } catch (final Exception e) {
                throw new IllegalArgumentException("Could not access " + monitoringFilePath, e);
            }
        }

    }

    @Override
    public Tokenizer create() {
        return new TokenizerWrapper();
    }

    private void updateUserDictionary() {
        if (reloadableFile != null && System.currentTimeMillis() - lastChecked > reloadInterval) {
            lastChecked = System.currentTimeMillis();
            final long timestamp = reloadableFile.lastModified();
            if (timestamp != dictionaryTimestamp) {
                synchronized (reloadableFile) {
                    if (timestamp != dictionaryTimestamp) {
                        userDictionary = KuromojiTokenizerFactory.getUserDictionary(env, settings);
                        dictionaryTimestamp = timestamp;
                    }
                }
            }
        }
    }

    public final class TokenizerWrapper extends Tokenizer {

        private final JapaneseTokenizer tokenizer;

        private long tokenizerTimestamp;

        TokenizerWrapper() {
            super();

            tokenizerTimestamp = dictionaryTimestamp;
            tokenizer = new JapaneseTokenizer(userDictionary, discartPunctuation, mode);

            try {
                final Field attributesField = getAccessibleField(AttributeSource.class, "attributes");
                final Object attributesObj = attributesField.get(tokenizer);
                attributesField.set(this, attributesObj);

                final Field attributeImplsField = getAccessibleField(AttributeSource.class, "attributeImpls");
                final Object attributeImplsObj = attributeImplsField.get(tokenizer);
                attributeImplsField.set(this, attributeImplsObj);

                final Field currentStateField = getAccessibleField(AttributeSource.class, "currentState");
                final Object currentStateObj = currentStateField.get(tokenizer);
                currentStateField.set(this, currentStateObj);
            } catch (final Exception e) {
                throw new IllegalStateException("Failed to update the tokenizer.", e);
            }
        }

        @Override
        public void close() throws IOException {
            tokenizer.close();
        }

        @Override
        public void reset() throws IOException {
            updateUserDictionary();

            if (dictionaryTimestamp > tokenizerTimestamp) {
                if (VERBOSE) {
                    System.out.println("Update KuromojiTokenizer (" + tokenizerTimestamp + "," + dictionaryTimestamp + ")");
                }
                if (userDictionary != null) {
                    try {
                        tokenizerTimestamp = dictionaryTimestamp;
                        userDictionaryField.set(tokenizer, userDictionary);
                        final TokenInfoFST userFst = userDictionary.getFST();
                        userFSTField.set(tokenizer, userFst);
                        userFSTReaderField.set(tokenizer, userFst.getBytesReader());
                        @SuppressWarnings("unchecked")
                        final EnumMap<Type, Dictionary> dictionaryMap = (EnumMap<Type, Dictionary>) dictionaryMapField.get(tokenizer);
                        dictionaryMap.put(Type.USER, userDictionary);
                    } catch (final Exception e) {
                        throw new IllegalStateException("Failed to update the tokenizer.", e);
                    }
                }
            }

            final Reader inputPending = getInputPending();
            if (inputPending != ILLEGAL_STATE_READER) {
                tokenizer.setReader(inputPending);
            }
            tokenizer.reset();
        }

        @Override
        public boolean incrementToken() throws IOException {
            return tokenizer.incrementToken();
        }

        @Override
        public void end() throws IOException {
            tokenizer.end();
        }

        @Override
        public int hashCode() {
            return tokenizer.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return tokenizer.equals(obj);
        }

        @Override
        public String toString() {
            return tokenizer.toString();
        }

        private Reader getInputPending() {
            try {
                return (Reader) inputPendingField.get(this);
            } catch (final Exception e) {
                return null;
            }
        }

    }

    private static Field getAccessibleField(final Class<?> clazz, final String name) {
        return AccessController.doPrivileged((PrivilegedAction<Field>) () -> {
            try {
                final Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (final Exception e) {
                throw new IllegalArgumentException("Failed to load fields.", e);
            }
        });
    }
}
