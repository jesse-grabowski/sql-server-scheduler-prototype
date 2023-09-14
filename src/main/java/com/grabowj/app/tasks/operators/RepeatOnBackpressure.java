package com.grabowj.app.tasks.operators;

import com.grabowj.app.tasks.Task;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

public class RepeatOnBackpressure<T> {

    private final FluxSink<T> sink;
    private final Supplier<Publisher<T>> publisherSupplier;
    private final Disposable.Composite disposables;

    public RepeatOnBackpressure(FluxSink<T> sink, Supplier<Publisher<T>> publisherSupplier) {
        this.disposables = Disposables.composite();
        this.publisherSupplier = publisherSupplier;

        this.sink = sink;
        this.sink.onRequest(this::request);
        this.sink.onCancel(disposables);
        this.sink.onDispose(disposables);
    }

    private void request(long demand) {
        BaseSubscriber<T> subscriber = new FluxSinkSubscriber<>(sink, disposables);
        disposables.add(subscriber);
        Flux.range(0, (int) demand)
                .publishOn(Schedulers.parallel())
                .flatMap(i -> publisherSupplier.get())
                .contextWrite(sink.contextView())
                .subscribe(subscriber);
    }
}
