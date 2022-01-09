/*
 * Copyright 2012-2022 CodeLibs Project and the Others.
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
package org.codelibs.fess.suggest.normalizer;

import java.util.ArrayList;
import java.util.List;

public class NormalizerChain implements Normalizer {
    List<Normalizer> normalizers = new ArrayList<>();

    @Override
    public String normalize(final String text, final String field, final String... langs) {
        String tmp = text;
        for (final Normalizer normalizer : normalizers) {
            tmp = normalizer.normalize(tmp, field, langs);
        }
        return tmp;
    }

    public void add(final Normalizer normalizer) {
        normalizers.add(normalizer);
    }
}
