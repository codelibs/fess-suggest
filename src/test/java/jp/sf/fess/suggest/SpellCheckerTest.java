package jp.sf.fess.suggest;

import jp.sf.fess.suggest.converter.SuggestReadingConverter;
import jp.sf.fess.suggest.normalizer.SuggestNormalizer;
import junit.framework.TestCase;
import org.apache.lucene.analysis.util.TokenizerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpellCheckerTest extends TestCase {
    public void test_buildQuery() {
        SpellChecker spellChecker = new SpellChecker();
        spellChecker.setNormalizer(new SuggestNormalizer() {
            @Override
            public String normalize(String text) {
                return text.replace("りんご", "リンゴ");
            }

            @Override
            public void start() {
            }
        });

        spellChecker.setConverter(new SuggestReadingConverter() {
            @Override
            public List<String> convert(String text) {
                List<String> list = new ArrayList<String>();
                list.add(text);
                list.add(text.replace("みかん", "ミカン"));
                return list;
            }

            @Override
            public void start() {
            }

            @Override
            public void setTokenizerFactory(TokenizerFactory tokenizerFactory) {
            }
        });

        List<String> targetFields = Arrays.asList(new String[]{"field1", "field2"});
        List<String> labels = Arrays.asList(new String[]{"label1", "label2"});
        List<String> roles = Arrays.asList(new String[]{"role1", "role2"});

        String readingField = SuggestConstants.SuggestFieldNames.READING;
        String field = SuggestConstants.SuggestFieldNames.FIELD_NAME;
        String labelField = SuggestConstants.SuggestFieldNames.LABELS;
        String roleField = SuggestConstants.SuggestFieldNames.ROLES;
        String query = spellChecker.buildSpellCheckQuery("りんごとみかん", targetFields, labels, roles);
        assertEquals(readingField + ":リンゴとみかん~0.5 AND " +
                "(" + field + ":field1 OR " + field + ":field2) AND (" +
                labelField + ":label1 OR " + labelField + ":label2) AND (" +
                roleField + ":role1 OR " + roleField + ":role2)",
                query);
    }
}
