package org.codelibs.fess.suggest.converter;

import org.elasticsearch.client.Client;

import java.io.IOException;
import java.util.List;

public class AnalyzerConverter implements ReadingConverter {
    protected final Client client;

    public AnalyzerConverter(final Client client) {
        this.client = client;
    }

    @Override
    public void init() throws IOException {

    }

    @Override
    public List<String> convert(String text) throws IOException {

        return null;
    }
}
