package org.codelibs.fess.suggest.analysis;

import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;

import java.util.List;

public interface SuggestAnalyzer {
    List<AnalyzeResponse.AnalyzeToken> analyze(String text, String lang);

    List<AnalyzeResponse.AnalyzeToken> analyzeAndReading(String text, String lang);
}
