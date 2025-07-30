# Quarkus Debezium Extension Quick Start

This project provides a Quarkus application that demonstrates how to use Debezium Extensions for Quarkus

This quick start is designed to help you:
- main features of Debezium Quarkus extension
- use Debezium inside Quarkus
- build a native Debezium

## Prerequisite

- JDK 21+ installed with `JAVA_HOME` configured appropriately
- Apache maven 3.9.9
- Quarkus version 3.25.0
- Docker or Podman
- Optionally Mandrel or GraalVM installed and configured appropriately if you want to build a native executable

## Development mode

You can use Quarkus development mode with `mvn quarkus:dev` that automatically start a docker image with `postgres`
and initialize the database with `init.sql`.

## Production mode

You can generate a jar file with `mvn clean install`. Before execute the quarkus jar with `java -jar  ./target/quarkus-app/quarkus-run.jar`, it's necessary a running postgres. You can use the docker compose with: `docker compose up`

## Native mode

To build a native image use: `mvn clean install -Dnative` and run it with `./target/debezium-quarkus-extension-service-1.0.0-SNAPSHOT-runner`. Please be sure to have followed [guideline](https://quarkus.io/guides/building-native-image) to prepare your environment for a native build.