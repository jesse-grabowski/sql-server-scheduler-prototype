package com.grabowj.app.tasks;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class TaskSubscriberTest extends SubscriberWhiteboxVerification<Task> {

    public TaskSubscriberTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<Task> createSubscriber(WhiteboxSubscriberProbe<Task> probe) {
        return new TaskSubscriber(50) {
            @Override
            public void onSubscribe(final Subscription s) {
                super.onSubscribe(s);

                // register a successful Subscription, and create a Puppet,
                // for the WhiteboxVerification to be able to drive its tests:
                probe.registerOnSubscribe(new SubscriberPuppet() {

                    @Override
                    public void triggerRequest(long elements) {
                        s.request(elements);
                    }

                    @Override
                    public void signalCancel() {
                        s.cancel();
                    }
                });
            }

            @Override
            public void onNext(Task element) {
                // in addition to normal Subscriber work that you're testing, register onNext with the probe
                super.onNext(element);
                probe.registerOnNext(element);
            }

            @Override
            public void onError(Throwable cause) {
                // in addition to normal Subscriber work that you're testing, register onError with the probe
                super.onError(cause);
                probe.registerOnError(cause);
            }

            @Override
            public void onComplete() {
                // in addition to normal Subscriber work that you're testing, register onComplete with the probe
                super.onComplete();
                probe.registerOnComplete();
            }
        };
    }

    @Override
    public Task createElement(int i) {
        return Task.builder().id(i).reference("hello").triggerAt(LocalDateTime.now()).build();
    }
}