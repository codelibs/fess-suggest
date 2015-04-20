package models;

import components.ComponentsUtil;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.request.suggest.SuggestResponse;

public class SuggestIndex {
    public SuggestResponse suggest(String query) throws SuggesterException {
        return ComponentsUtil.suggester.suggest()
            .setQuery(query)
            .execute();
    }
}
