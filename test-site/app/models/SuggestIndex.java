package models;

import components.*;
import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.index.contents.document.DocumentReader;
import org.codelibs.fess.suggest.index.contents.document.ESSourceReader;
import org.codelibs.fess.suggest.request.suggest.SuggestResponse;

public class SuggestIndex {
    public SuggestResponse suggest(String query) {
        return ComponentsUtil.suggester.suggest()
            .setQuery(query)
            .execute().getResponse();
    }

    public void index() {
        Suggester suggester = ComponentsUtil.suggester;
        DocumentReader reader = new ESSourceReader(ComponentsUtil.runner.client(),
            suggester.settings(),
            ComponentsUtil.contentIndexName,
            ComponentsUtil.contentTypeName);
        suggester.indexer().indexFromDocument(reader, 2, 100).getResponse();
        suggester.refresh();
    }
}
