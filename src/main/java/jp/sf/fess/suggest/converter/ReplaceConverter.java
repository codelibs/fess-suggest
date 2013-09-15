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

import java.util.LinkedHashMap;
import java.util.Map;

public class ReplaceConverter implements SuggestConverter {
    protected Map<String, String> replaceMap = new LinkedHashMap<String, String>();

    @Override
    public String convert(final String query) {
        String target = query;
        for (final Map.Entry<String, String> entry : replaceMap.entrySet()) {
            target = target.replaceAll(entry.getKey(), entry.getValue());
        }
        return target;
    }

    public void addReplaceString(final String before, final String after) {
        replaceMap.put(before, after);
    }
}
