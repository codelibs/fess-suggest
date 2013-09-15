/*
 * Copyright 2009-2013 the Fess Project and the Others.
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

package jp.sf.fess.suggest;

import java.util.ArrayList;
import java.util.List;

import jp.sf.fess.suggest.converter.SuggestConverter;

public class Suggester {

    protected final List<SuggestConverter> preQueryConverterList = new ArrayList<SuggestConverter>();

    protected final List<SuggestConverter> queryConverterList = new ArrayList<SuggestConverter>();

    protected final List<SuggestConverter> resultConverterList = new ArrayList<SuggestConverter>();

    public String wordSeprator = SuggestConstants.WORD_SEPARATOR;

    public void addQueryConverter(final SuggestConverter converter) {
        queryConverterList.add(converter);
    }

    public void addPreQueryConverter(final SuggestConverter converter) {
        preQueryConverterList.add(converter);
    }

    public void addResultConverter(final SuggestConverter converter) {
        resultConverterList.add(converter);
    }

    public String convertQuery(final String query) {

        String target = query;
        for (final SuggestConverter conveter : preQueryConverterList) {
            target = conveter.convert(target);
        }

        final List<String> queryList = new ArrayList<String>();
        for (final SuggestConverter converter : queryConverterList) {
            String convertedQuery = converter.convert(target);
            if (convertedQuery != null) {
                convertedQuery = convertedQuery.trim();
                if (convertedQuery.length() > 0) {
                    queryList.add(convertedQuery);
                }
            }
        }

        if (queryList.size() == 0) {
            queryList.add(target);
        }

        final StringBuilder resultStrBuff = new StringBuilder(255);

        for (int i = 0; i < queryList.size(); i++) {
            if (i > 0) {
                resultStrBuff.append(' ');
            }

            resultStrBuff.append(queryList.get(i));
        }

        return resultStrBuff.toString();
    }

    public String convertResultString(final String suggestTerm) {
        final String[] strArray = suggestTerm.split(wordSeprator);

        String target;

        if (strArray.length == 1) {
            target = strArray[0];
        } else if (strArray.length == 2) {
            target = strArray[1];
        } else {
            return SuggestConstants.EMPTY_STRING;
        }

        for (final SuggestConverter converter : resultConverterList) {
            target = converter.convert(target);
        }
        return target;
    }
}
