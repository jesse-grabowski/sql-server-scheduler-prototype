package com.grabowj.app.tasks;

import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class TaskGenerator {

    private final Scheduler taskScheduler;
    private final FluxSink<Task> taskSink;
    private final Disposable.Composite taskDisposables;
    private final Function<Integer, Publisher<Task>> taskHandler;

    public TaskGenerator(FluxSink<Task> taskSink, Function<Integer, Publisher<Task>> taskHandler) {
        this.taskScheduler = Schedulers.newSingle("task");
        this.taskDisposables = Disposables.composite();
        this.taskSink = taskSink;
        this.taskHandler = taskHandler;
        this.taskSink.onRequest(this::request);
        this.taskSink.onCancel(taskDisposables);
        this.taskSink.onDispose(taskDisposables);
    }

    private void request(long demand) {
        AtomicReference<Disposable> disposableAtomicReference = new AtomicReference<>();
        Disposable disposable = Flux.range(0, (int) demand)
                .flatMap(taskHandler)
                .subscribeOn(Schedulers.parallel())
                .publishOn(taskScheduler)
                .subscribe(
                        taskSink::next,
                        taskSink::error,
                        () -> {
                            Disposable d;
                            do {
                                d = disposableAtomicReference.get();
                            } while (d == null);
                            taskDisposables.remove(d);
                        },
                        Context.of(taskSink.contextView()));
        taskDisposables.add(disposable);
        disposableAtomicReference.set(disposable);
    }
}
