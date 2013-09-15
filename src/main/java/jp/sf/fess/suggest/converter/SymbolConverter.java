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

package jp.sf.fess.suggest.converter;

import java.util.ArrayList;
import java.util.List;

public class SymbolConverter implements SuggestConverter {

    protected List<String> symbolList = new ArrayList<String>();

    protected String symbolPrefix;

    protected String symbolSuffix;

    public SymbolConverter() {
        this("__ID", "__");
    }

    public SymbolConverter(final String prefix, final String suffix) {
        symbolPrefix = prefix;
        symbolSuffix = suffix;
    }

    @Override
    public String convert(final String query) {

        String target = query;
        for (int i = 0; i < symbolList.size(); i++) {
            target = target.replace(symbolList.get(i),
                    symbolPrefix + Integer.valueOf(i) + symbolSuffix);
        }
        return target;
    }

    public void addSymbol(final String[] symbols) {
        if (symbols == null || symbols.length == 0) {
            return;
        }

        for (final String symbol : symbols) {
            symbolList.add(symbol);
        }
    }

    public void addSymbol(final List<String> symbols) {
        if (symbols == null || symbols.size() == 0) {
            return;
        }

        for (final String symbol : symbols) {
            symbolList.add(symbol);
        }
    }
}
