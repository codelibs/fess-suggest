package controllers;

import components.ComponentsUtil;
import models.ContentsGetter;
import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.index.contents.document.DocumentReader;
import org.codelibs.fess.suggest.index.contents.document.ESSourceReader;
import org.codelibs.fess.suggest.request.suggest.SuggestResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.Map;

public class Suggest extends Controller {
    public static Result get() {
        Map<String, String[]> params = request().queryString();
        String[] callback = params.get("callback");
        String[] query = params.get("query");
        try {
            SuggestResponse response = ComponentsUtil.suggester.suggest()
                .setQuery(query[0])
                .execute();

            XContentBuilder builder  = JsonXContent.contentBuilder()
                .startObject()
                .startObject("response")
                .startArray("result")
                .startObject()
                .startArray("result");

            for(String word: response.getWords()) {
                builder.value(word);
            }

            String json = builder.endArray()
                .endObject()
                .endArray()
                .endObject()
                .endObject().string();
            if(callback != null && callback.length > 0) {
                json = callback[0] + '(' + json + ')';
            }
            return ok(json);
        } catch (Exception e) {
            return internalServerError(e.getMessage());
        }
    }

    public static Result createSuggestFromContent() {
        Suggester suggester = ComponentsUtil.suggester;
        DocumentReader reader = new ESSourceReader(ComponentsUtil.runner.client(),
            suggester.settings(),
            ComponentsUtil.contentIndexName,
            ComponentsUtil.contentTypeName);
        suggester.indexer().indexFromDocument(reader, false);
        suggester.refresh();
        return ok("Finished to create suggest from content");
    }



    public static Result createContent() {
        ContentsGetter getter = new ContentsGetter();
        getter.create();
        return ok("Finished to create content index;");
    }

}
