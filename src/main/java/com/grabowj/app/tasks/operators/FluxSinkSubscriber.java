package com.grabowj.app.tasks.operators;

import lombok.RequiredArgsConstructor;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.FluxSink;

@RequiredArgsConstructor
public class FluxSinkSubscriber<T> extends BaseSubscriber<T> {

    private final FluxSink<T> sink;
    private final Disposable.Composite disposables;

    @Override
    protected void hookOnNext(T event) {
        sink.next(event);
    }

    @Override
    protected void hookOnError(Throwable throwable) {
        sink.error(throwable);
    }

    @Override
    protected void hookOnComplete() {
        if (disposables != null) {
            disposables.remove(this);
        }
    }
}
