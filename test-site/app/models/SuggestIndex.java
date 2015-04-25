package models;

import components.ComponentsUtil;
import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.index.contents.document.DocumentReader;
import org.codelibs.fess.suggest.index.contents.document.ESSourceReader;
import org.codelibs.fess.suggest.request.suggest.SuggestResponse;

public class SuggestIndex {
    public SuggestResponse suggest(String query) throws SuggesterException {
        return ComponentsUtil.suggester.suggest()
            .setQuery(query)
            .execute();
    }

    public void index() throws SuggesterException {
        Suggester suggester = ComponentsUtil.suggester;
        DocumentReader reader = new ESSourceReader(ComponentsUtil.runner.client(),
            suggester.settings(),
            ComponentsUtil.contentIndexName,
            ComponentsUtil.contentTypeName);
        suggester.indexer().indexFromDocument(reader, false, 2);
        suggester.refresh();
    }
}
