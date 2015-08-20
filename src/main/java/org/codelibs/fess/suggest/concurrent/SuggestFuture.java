package org.codelibs.fess.suggest.concurrent;

import java.util.function.Consumer;

import org.codelibs.fess.suggest.request.Response;

public interface SuggestFuture<RESPONSE extends Response> {

    RESPONSE getResponse();

    SuggestFuture<RESPONSE> done(Consumer<RESPONSE> consumer);

    SuggestFuture<RESPONSE> error(Consumer<Throwable> consumer);

    void resolve(RESPONSE response, Throwable failure);

}