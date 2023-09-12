package com.grabowj.app.tasks;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Void>> scheduleTasks(@RequestBody List<TaskDefinition> taskDefinitions) {
        return taskService.scheduleTasks(taskDefinitions).then(Mono.just(ResponseEntity.ok().build()));
    }

    @PostMapping(params = "count")
    public Mono<ResponseEntity<Void>> scheduleTasks(@RequestParam("count") int count, @RequestParam("delay") long delay) {
        return Flux.range(0, count)
                .map(i -> new TaskDefinition(UUID.randomUUID().toString(), delay))
                .window(200)
                .flatMap(flux -> flux.collectList().flatMap(taskService::scheduleTasks), 10)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

}
