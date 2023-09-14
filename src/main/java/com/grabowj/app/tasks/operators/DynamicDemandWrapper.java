package com.grabowj.app.tasks.operators;

public record DynamicDemandWrapper<T>(T value) {

    private static final DynamicDemandWrapper EMPTY = new DynamicDemandWrapper(null);

    public static <T> DynamicDemandWrapper<T> empty() {
        return EMPTY;
    }

}
