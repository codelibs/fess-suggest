package org.codelibs.fess.suggest.util;

import java.util.ArrayList;
import java.util.List;

public class SuggestUtil {

    private SuggestUtil() {
    }

    public static String[] parseQuery(String query, String field) {
        List<String> words = new ArrayList<>();
        String[] elems = query.replace("(", " ").replace(")", " ").replaceAll(" +", " ").trim().split(" ");
        for (String elem : elems) {
            if (!elem.contains(":")) {
                continue;
            }
            String[] pair = elem.split(":");
            if (pair.length != 2 || !field.equals(pair[0])) {
                continue;
            }
            if (!words.contains(pair[1])) {
                words.add(pair[1]);
            }
        }
        return words.toArray(new String[words.size()]);
    }
}
