package org.codelibs.fess.suggest.analysis;

import java.util.List;

import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;

public interface SuggestAnalyzer {
    List<AnalyzeResponse.AnalyzeToken> analyze(String text, String lang);

    List<AnalyzeResponse.AnalyzeToken> analyzeAndReading(String text, String lang);
}
