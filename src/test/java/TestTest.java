import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.index.contents.document.ESSourceReader;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.Test;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;

public class TestTest {
    @Test
    public void test_performance() throws Exception {
        final String fessIndex = "fess.search";
        final String suggestIndex = "fess.suggest";

        Settings settings = Settings.builder()
            .put("cluster.name", "elasticsearch").build();
        Client client = new PreBuiltTransportClient(settings).addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9301));
        assertEquals(200, client.prepareSearch().setIndices("fess.search").setTypes("doc").execute().actionGet().getHits().totalHits());

        Suggester suggester = Suggester.builder().build(client, "fess");

        suggester.indexer().deleteAll();
        suggester.refresh();
        assertEquals(0, client.prepareSearch().setIndices("fess.suggest").setTypes(suggester.getType()).execute().actionGet().getHits().getTotalHits());

        Thread.sleep(1000 * 1000);

        CountDownLatch latch = new CountDownLatch(1);
        System.out.println("Index start");
        final long start = System.currentTimeMillis();
        suggester.indexer().indexFromDocument(
            () -> {
                final ESSourceReader reader =
                    new ESSourceReader(client, suggester.settings(), fessIndex,
                        "doc");
                reader.setScrollSize(1);
                return reader;
            }, 2, 1).then(response -> {
            suggester.refresh();
            System.out.println("success");
            latch.countDown();
        }).error(t -> {
            t.printStackTrace();
            latch.countDown();
        });
        latch.await();
        System.out.println("finished. tookTime:" + ((System.currentTimeMillis() - start) / 1000) + "秒");
        assertEquals(5118, client.prepareSearch().setIndices("fess.suggest").setTypes(suggester.getType()).execute().actionGet().getHits().getTotalHits());
    }
}
