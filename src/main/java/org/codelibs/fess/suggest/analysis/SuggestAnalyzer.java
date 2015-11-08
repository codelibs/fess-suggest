package org.codelibs.fess.suggest.analysis;

import java.util.List;

public interface SuggestAnalyzer {
    List<String> analyze(String text);
}
