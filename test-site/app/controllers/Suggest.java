package controllers;

import models.*;
import org.codelibs.fess.suggest.request.suggest.SuggestResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.Map;

public class Suggest extends Controller {
    public static Result get() {
        Map<String, String[]> params = request().queryString();
        String[] callback = params.get("callback");
        String[] query = params.get("query");
        try {
            SuggestIndex suggestIndex = new SuggestIndex();
            SuggestResponse response = suggestIndex.suggest(query[0]);

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
        try {
            long start = System.currentTimeMillis();
            SuggestIndex suggestIndex = new SuggestIndex();
            suggestIndex.index();
            Logger.info("index took: " + ((System.currentTimeMillis() - start) / 1000) + "sec");
            return ok("Finished to create suggest from content");
        } catch (Exception e){
            Logger.error("Failed to create suggest index. ", e);
            return internalServerError();
        }
    }



    public static Result createContent() {
        ContentsCreator getter = new ContentsCreator();
        getter.create();
        return ok("Finished to create content index;");
    }

}
