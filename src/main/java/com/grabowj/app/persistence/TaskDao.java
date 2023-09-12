package com.grabowj.app.persistence;

import com.grabowj.app.tasks.Task;
import com.grabowj.app.tasks.TaskDefinition;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Metrics;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import io.r2dbc.spi.Statement;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiFunction;

@RequiredArgsConstructor
@Repository(TaskDao.BEAN_NAME)
public class TaskDao {

    public static final String BEAN_NAME = "taskDao";

    private static final BiFunction<Row, RowMetadata, Task> MAPPING_FUNCTION = (row, meta) -> Task.builder()
            .id(row.get("id", Long.class))
            .reference(row.get("reference", String.class))
            .triggerAt(row.get("trigger_at", LocalDateTime.class))
            .build();

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    @Timed(value = "dequeue_task")
    public Mono<Task> dequeueTaskIfAvailable() {
        return r2dbcEntityTemplate.getDatabaseClient().sql("""
                    DELETE TOP(1) tasks WITH (READPAST)
                    OUTPUT DELETED.*
                    WHERE trigger_at < GETUTCDATE();
                """)
                .map(MAPPING_FUNCTION)
                .one()
                .timeout(Duration.ofSeconds(5));
    }

    @Timed(value = "enqueue_task")
    public Mono<Void> scheduleTasks(List<TaskDefinition> tasks) {
        return r2dbcEntityTemplate.getDatabaseClient().inConnectionMany(connection -> {
            Statement statement = connection.createStatement("""
                INSERT INTO tasks (reference, trigger_at)
                VALUES (@reference, DATEADD(millisecond, @delayInMilliseconds, GETUTCDATE()))
            """);
            tasks.forEach(task -> statement
                        .bind("reference", task.reference())
                        .bind("delayInMilliseconds", task.delayInMilliseconds())
                        .add());
            return Flux.from(statement.execute());
        }).then().doOnSuccess(v -> Metrics.counter("task_scheduled").increment(tasks.size()))
                .timeout(Duration.ofSeconds(5));
    }
}
