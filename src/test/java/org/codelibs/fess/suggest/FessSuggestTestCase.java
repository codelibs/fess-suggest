package org.codelibs.fess.suggest;

import junit.framework.TestCase;

import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.codelibs.fess.suggest.converter.AlphabetConverter;
import org.codelibs.fess.suggest.converter.SuggestIntegrateConverter;
import org.codelibs.fess.suggest.converter.SuggestReadingConverter;
import org.codelibs.fess.suggest.normalizer.SuggestIntegrateNormalizer;
import org.codelibs.fess.suggest.normalizer.SuggestNormalizer;
import org.codelibs.fess.suggest.util.SuggestUtil;

import java.util.HashMap;
import java.util.Map;

public class FessSuggestTestCase extends TestCase {
    protected static final String SOLR_URL = "http://localhost:8181/solr/core1-suggest";

    protected static final String CONTEXT_PATH = "/solr";

    protected static final int PORT = 8181;

    protected static JettySolrRunner jettySolrRunner;

    public void startSolr() {
        if (jettySolrRunner != null) {
            return;
        }

        jettySolrRunner = new JettySolrRunner("./solr", CONTEXT_PATH, PORT);
        try {
            jettySolrRunner.start();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public void stopSolr() {
        if (jettySolrRunner == null) {
            return;
        }
        try {
            jettySolrRunner.stop();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        jettySolrRunner = null;
    }

    public static SuggestReadingConverter createConverter() {
        final SuggestIntegrateConverter suggestIntegrateConverter = new SuggestIntegrateConverter();
        suggestIntegrateConverter.addConverter(new AlphabetConverter());
        suggestIntegrateConverter.start();
        return suggestIntegrateConverter;
    }

    public static SuggestNormalizer createNormalizer() {
        final SuggestIntegrateNormalizer suggestIntegrateNormalizer = new SuggestIntegrateNormalizer();

        try {
            Map<String, String> map = new HashMap<String, String>();
            suggestIntegrateNormalizer.addNormalizer(SuggestUtil.createNormalizer(
                    "org.codelibs.fess.suggest.normalizer.FullWidthToHalfWidthAlphabetNormalizer", map));
            map = new HashMap<String, String>();
            map.put("transliteratorId", "Any-Lower");
            suggestIntegrateNormalizer.addNormalizer(SuggestUtil
                    .createNormalizer("org.codelibs.fess.suggest.normalizer.ICUNormalizer", map));
        } catch (final Exception e) {
            e.printStackTrace();
        }
        suggestIntegrateNormalizer.start();
        return suggestIntegrateNormalizer;
    }

    public void test_dmy() {
        assertTrue(true);
    }

}
