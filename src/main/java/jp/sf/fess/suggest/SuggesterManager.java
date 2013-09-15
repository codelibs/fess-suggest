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

import java.util.HashMap;
import java.util.Map;

public class SuggesterManager {

    protected Map<String, Suggester> suggesterMap = new HashMap<String, Suggester>();

    public void addSuggester(final String name, final Suggester suggester) {
        suggesterMap.put(name, suggester);
    }

    public Suggester getSuggester(final String name) {
        return suggesterMap.get(name);
    }
}
