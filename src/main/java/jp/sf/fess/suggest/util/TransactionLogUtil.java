package jp.sf.fess.suggest.util;


import com.google.common.io.Files;
import org.apache.solr.update.TransactionLog;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

public class TransactionLogUtil {
    private static final String PREFIX = "suggest-";

    public static TransactionLog createSuggestTransactionLog(File tlogFile, Collection<String> globalStrings, boolean openExisting)
            throws NoSuchMethodException, InstantiationException, IllegalAccessException, IOException,
            IllegalArgumentException, InvocationTargetException {
        long start = System.currentTimeMillis();
        File file = new File(tlogFile.getParent(), PREFIX + tlogFile.getName());
        Files.copy(tlogFile, file);
        System.out.println("create trans log " + (System.currentTimeMillis() - start) + "  " + file.getAbsolutePath());
        Class cls = TransactionLog.class;
        Constructor constructor = cls.getDeclaredConstructor(File.class, Collection.class, Boolean.TYPE);
        constructor.setAccessible(true);
        return (TransactionLog) constructor.newInstance(file, globalStrings, openExisting);
    }

    public static void clearSuggestTransactionLog(String dir) {
        File d = new File(dir);
        if (!d.isDirectory()) {
            return;
        }
        for (File f : d.listFiles()) {
            if (f.isFile() && f.getName().startsWith(PREFIX)) {
                if (!f.delete()) {
                    f.deleteOnExit();
                }
            }
        }
    }
}
