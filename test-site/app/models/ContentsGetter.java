package models;

import components.ComponentsUtil;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Client;
import play.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContentsGetter {
    HttpClient httpClient = HttpClientBuilder.create().build();

    public void create() {
        List<String> urls = getUrls();

        for(String url: urls) {
            try {
                HttpUriRequest request = new HttpGet(url);
                HttpResponse response = httpClient.execute(request);
                if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    Logger.warn("Error response:" + response.getStatusLine().getStatusCode());
                    continue;
                }
                String body = EntityUtils.toString(response.getEntity());

                Map<String, Object> source = new HashMap<>();
                source.put("content", extractHtml(body));
                ComponentsUtil.runner.client().prepareIndex()
                    .setIndex(ComponentsUtil.contentIndexName)
                    .setType(ComponentsUtil.contentTypeName)
                    .setCreate(true)
                    .setSource(source)
                    .execute()
                    .actionGet();
            } catch(Exception e) {
                Logger.warn(e.getMessage(), e);
            }
        }
    }

    protected String extractHtml(String html) {
        return html.replaceAll("<[^>]*>", "");
    }

    protected List<String> getUrls() {
        List<String> list = new ArrayList<>();
        list.add("http://wikiwiki.jp/kancolle/?%BB%B0%B7%A8%B2%FE");
        return list;
    }
}
