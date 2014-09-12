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

package jp.sf.fess.suggest.converter;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.util.TokenizerFactory;

public class SuggestIntegrateConverter implements SuggestReadingConverter {
    List<SuggestReadingConverter> converterList = new ArrayList<SuggestReadingConverter>();

    public void addConverter(final SuggestReadingConverter converter) {
        converterList.add(converter);
    }

    @Override
    public List<String> convert(final String text) {
        final List<String> convertedStrings = new ArrayList<String>();
        for (final SuggestReadingConverter converter : converterList) {
            final List<String> list = converter.convert(text);
            convertedStrings.addAll(list);
        }
        return convertedStrings;
    }

    @Override
    public void start() {
        for (final SuggestReadingConverter converter : converterList) {
            converter.start();
        }
    }

    @Override
    public void setTokenizerFactory(final TokenizerFactory tokenizerFactory) {
        for (final SuggestReadingConverter converter : converterList) {
            converter.setTokenizerFactory(tokenizerFactory);
        }
    }

    public List<SuggestReadingConverter> getConverterList() {
        return converterList;
    }
}
