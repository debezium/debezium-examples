# Testcontainers

This example shows how to implement an integration test for your CDC set-up using [Testcontainers](https://www.testcontainers.org/).
It spins up Postgres, Apache Kafka and Kafka Connect (including the Debezium) connectors,
deploys an instance of the Debezium Postgres connector and runs some assertions against expected change events on the corresponding Kafka topic.

Please refer to the [Debezium documentation](https://debezium.io/documentation/reference/2.0/integrations/testcontainers.html) to learn more about its Testcontainers support.

## Prerequisites

* Java 11 development environment
* Local [Docker](https://www.docker.com/) installation

## Running the Example

```
mvn clean package
```
