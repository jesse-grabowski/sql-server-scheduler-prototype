package com.grabowj.app.tasks;

import com.grabowj.app.persistence.TaskDao;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.prometheus.client.Counter;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/*
 * Use publishOn to ensure that only a single thread interacts with this subscriber.
 *
 * Uses the fibonacci sequence to scale demand as it won't scale until it has processed at least two results,
 * minimizing unprocessed requests.
 */
@Slf4j
@DependsOn({"r2dbcDatabaseClient", TaskDao.BEAN_NAME})
public class TaskSubscriber implements Subscriber<Task>, Disposable {

    private final long[] desiredDemandSequence;
    private final AtomicLong inFlightRequests;

    private volatile boolean isDisposed;
    private int desiredDemandIndex;

    private Subscription subscription;

    public TaskSubscriber(@Value("${app.scheduler.maximum-demand:89}") long maximumDemand) {
        this.isDisposed = false;
        this.inFlightRequests = new AtomicLong();
        this.desiredDemandIndex = 0;
        this.desiredDemandSequence = calculateDesiredDemandSequence(0, 0, 1, maximumDemand);
    }

    private long[] calculateDesiredDemandSequence(int depth, long valueA, long valueB, long max) {
        if (valueB > max) {
            return new long[depth];
        }

        long[] sequence = calculateDesiredDemandSequence(depth + 1, valueB, valueA + valueB, max);
        sequence[depth] = valueB;
        return sequence;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (this.subscription == null) {
            this.subscription = subscription;
            onEvent(false);
        } else {
            subscription.cancel();
        }
    }

    @Override
    public void onNext(Task task) {
        Objects.requireNonNull(task);

        if (task != Task.EMPTY) {
            Metrics.counter("task_executed").increment();
        }

        onEvent(task != Task.EMPTY);
    }

    @Override
    public void onError(Throwable throwable) {
        onEvent(false);
    }

    @Override
    public void onComplete() {
        this.isDisposed = true;
    }

    private void onEvent(boolean isRequestSuccessful) {
        long inFlight = inFlightRequests.decrementAndGet();

        if (this.isDisposed) {
            return;
        }

        if (isRequestSuccessful) {
            desiredDemandIndex = Math.min(desiredDemandIndex + 1, desiredDemandSequence.length - 1);
        } else {
            desiredDemandIndex = 0;
        }
        long desiredDemand = desiredDemandSequence[desiredDemandIndex];

        long delta = desiredDemand - inFlight;

        if (delta > 0) {
            inFlightRequests.addAndGet(delta);
            subscription.request(delta);
        }
    }

    @Override
    public void dispose() {
        this.isDisposed = true;
        if (subscription != null) {
            subscription.cancel();
        }
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }
}
