package com.grabowj.app.tasks;

import com.grabowj.app.persistence.TaskDao;
import io.r2dbc.spi.R2dbcNonTransientException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {

    private final TaskDao taskDao;
    private final ReactiveTransactionManager reactiveTransactionManager;
    private final TaskSubscriber taskSubscriber;

    @PostConstruct
    public void start() {
        Flux.<Task>create(sink -> new TaskGenerator(sink, signal -> taskDao.dequeueTaskIfAvailable()
                .flatMap(task -> executeTask(task).onErrorResume(
                        Predicate.not(R2dbcNonTransientException.class::isInstance),
                        e -> scheduleTasks(List.of(new TaskDefinition(task.reference(), 10000)))
                                .thenReturn(Task.EMPTY)))
                .as(TransactionalOperator.create(reactiveTransactionManager)::transactional)
                .switchIfEmpty(Mono.just(Task.EMPTY))
                .onErrorReturn(Task.EMPTY)))
                .subscribe(taskSubscriber);
    }

    @Transactional
    public Mono<Void> scheduleTasks(List<TaskDefinition> tasks) {
        return taskDao.scheduleTasks(tasks);
    }

    private Mono<Task> executeTask(Task task) {
//        if (ThreadLocalRandom.current().nextInt(10) > 8) {
//            return Mono.error(new RuntimeException());
//        } else {
            return Mono.just(task);
//        }
    }


}
