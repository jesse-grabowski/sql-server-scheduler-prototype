package com.grabowj.app.tasks;

import com.grabowj.app.persistence.TaskDao;
import com.grabowj.app.tasks.operators.DynamicDemandSubscriber;
import com.grabowj.app.tasks.operators.DynamicDemandWrapper;
import com.grabowj.app.tasks.operators.OperatorSupport;
import com.grabowj.app.tasks.operators.RepeatOnBackpressure;
import io.r2dbc.spi.R2dbcNonTransientException;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {

    private final TaskDao taskDao;
    private final ReactiveTransactionManager reactiveTransactionManager;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        OperatorSupport.repeatOnBackpressure(this::processSingleTask)
                .subscribe(new DynamicDemandSubscriber<>());
    }

    @Transactional
    public Mono<Void> scheduleTasks(List<TaskDefinition> tasks) {
        return taskDao.scheduleTasks(tasks);
    }

    private Mono<DynamicDemandWrapper<Task>> processSingleTask() {
        return taskDao.dequeueTaskIfAvailable()
                .doOnSuccess(task -> log.info("Processing task {}", task))
                .flatMap(task -> executeTask(task).onErrorResume(
                        Predicate.not(R2dbcNonTransientException.class::isInstance),
                        e -> scheduleTasks(List.of(new TaskDefinition(task.reference(), 10000)))
                                .thenReturn(Task.EMPTY)))
                .as(TransactionalOperator.create(reactiveTransactionManager)::transactional)
                .map(DynamicDemandWrapper::new)
                .onErrorReturn(DynamicDemandWrapper.empty())
                .defaultIfEmpty(DynamicDemandWrapper.empty());
    }

    private Mono<Task> executeTask(Task task) {
//        if (ThreadLocalRandom.current().nextInt(10) > 8) {
//            return Mono.error(new RuntimeException());
//        } else {
            return Mono.just(task).delayElement(Duration.ofSeconds(1));
//        }
    }


}
