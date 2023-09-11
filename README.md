# SQLServer-Based Scheduler Prototype

Proof-of-concept application for a task executor using SQLServer.

## Prerequisites

* Docker (For running database and dashboards)
* Java 17
* Maven

## Getting Started

Start the database container:

```
./start-sql-server.sh
```

Start the application:

```
mvn spring-boot:run
```

Start Prometheus and Grafana. Your browser should automatically navigate to the appropriate Grafana dashboard, you can log in using the credentials `admin`/`admin`:

```
./start-monitoring.sh
```

## Exercising the Application

The following POST call may be utilized to bulk schedule tasks for testing.

* `count` - The number of tasks to schedule
* `delay` - The delay after which each task will be executed

For example, the following command will schedule one million tasks with an execution delay of 5 seconds.

```
curl --location --request POST 'http://localhost:8080/tasks?count=1000000&delay=5000'
```

## Performance

Detailed performance tuning and testing has not been performed, but rudimentary testing with the settings in this repo indicate a simulataneous task write/read throughput 
of 6000/80 per second, with read-only performance reaching up to 500 task invocations per second. Note that the application was exercised with simple tasks that simulated
a 200ms delay against a local database. Production usage will vary with task duration and network latency.

![image](https://github.com/jesse-grabowski/sql-server-scheduler-prototype/assets/2453853/0fa11371-190b-4df8-841e-db38f943f065)
