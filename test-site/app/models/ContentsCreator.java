package models;

import components.*;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import play.Logger;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class ContentsCreator {

    int max = 10;

    public void create() {
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder = requestBuilder.setConnectTimeout(10000);
        requestBuilder = requestBuilder.setConnectionRequestTimeout(10000).setSocketTimeout(5000);
        HttpClient httpClient = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestBuilder.build()).build();

        Queue<String> queue = getUrls();

        String url;
        int count = 0;
        while((url = queue.poll()) != null && count < max) {

            try {
                Logger.info("crawling: " + url);
                HttpUriRequest request = new HttpGet(url);
                HttpResponse response = httpClient.execute(request);
                if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    Logger.warn("Error response:" + response.getStatusLine().getStatusCode());
                    EntityUtils.consume(response.getEntity());
                    continue;
                }
                String body = EntityUtils.toString(response.getEntity());
                addSubLink(queue, body, request);

                Map<String, Object> source = new HashMap<>();
                source.put("content", extractHtml(body));
                ComponentsUtil.runner.client().prepareIndex()
                    .setIndex(ComponentsUtil.contentIndexName)
                    .setType(ComponentsUtil.contentTypeName)
                    .setCreate(true)
                    .setSource(source)
                    .execute()
                    .actionGet();
                count++;
                Thread.sleep(500);
            } catch(Exception e) {
                Logger.warn(e.getMessage(), e);
            }
        }
    }

    protected void addSubLink(final Queue<String> queue, final String html, final HttpUriRequest request) {
        String schema = request.getURI().getScheme();
        String host = request.getURI().getHost();
        String originUri = request.getURI().toString();

        String h = html;
        final String tag = "<a href=\"";
        int linkPos;
        while((linkPos = h.indexOf(tag)) >=0) {
            h = h.substring(linkPos + tag.length());
            final int endpos = h.indexOf('"');
            String url = h.substring(0, endpos);
            if(url.contains("#")) {
                continue;
            }
            if(url.startsWith("http")) {
                queue.add(url);
            } else if(url.startsWith("//")) {
                queue.add(schema + ":" + url);
            } else if(url.startsWith("/")) {
                queue.add(schema + "://" + host + url);
            } else {
                queue.add(originUri + '/' + url);
            }
        }
    }

    protected String extractHtml(String html) {
        return html.replaceAll("<[^>]*>", "");
    }

    protected Queue<String> getUrls() {
        Queue<String> q = new LinkedBlockingQueue<>();
        q.add("http://ja.wikipedia.org/wiki/%E6%A4%9C%E7%B4%A2%E3%82%A8%E3%83%B3%E3%82%B8%E3%83%B3");
        q.add("http://ja.wikipedia.org/wiki/SHIROBAKO");
        q.add("http://fess.sourceforge.jp/ja/");
        return q;
    }
}
