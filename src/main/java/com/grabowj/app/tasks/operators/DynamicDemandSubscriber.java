package com.grabowj.app.tasks.operators;

import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DynamicDemandSubscriber<T> extends BaseSubscriber<DynamicDemandWrapper<T>> {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        executor.submit(() -> subscription.request(1));
    }

    @Override
    protected void hookOnNext(DynamicDemandWrapper<T> value) {
        boolean isSuccess = value != DynamicDemandWrapper.empty();
        executor.submit(() -> requestOnSignal(isSuccess));
    }

    @Override
    protected void hookOnError(Throwable throwable) {
        executor.submit(() -> requestOnSignal(false));
    }

    private void requestOnSignal(boolean isSuccess) {
        upstream().request(1);
    }

    @Override
    public void dispose() {
        executor.shutdown();
    }

    @Override
    public boolean isDisposed() {
        return executor.isTerminated();
    }
}
