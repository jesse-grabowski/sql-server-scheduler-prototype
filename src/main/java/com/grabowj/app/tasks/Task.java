package com.grabowj.app.tasks;

import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Builder(toBuilder = true)
public record Task(long id, String reference, LocalDateTime triggerAt) {

    public static final Task EMPTY = new Task(-1, "", LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC));

}
