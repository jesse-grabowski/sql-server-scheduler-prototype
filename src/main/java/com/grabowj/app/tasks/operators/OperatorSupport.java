package com.grabowj.app.tasks.operators;

import lombok.experimental.UtilityClass;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

@UtilityClass
public class OperatorSupport {

    public static <T> Flux<T> repeatOnBackpressure(Supplier<Publisher<T>> publisherSupplier) {
        return Flux.create(sink -> new RepeatOnBackpressure<>(sink, publisherSupplier));
    }

}
