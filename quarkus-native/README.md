# Quarkus Native image with Debezium

This example demonstrate how to create a superfast CDC with Quarkus built with graalVM. We will ingest data from a postgres database into quarkus application with Debezium.

## Disclaimer

The example suggested here serve solely as a **proof of concept** to demonstrate Debezium's capabilities. They are not intended for production use but rather to showcase its potential. Debezium and its surrounding third-party libraries are not designed to be fully native for all use cases. Instead, the example presented here focus on a specific **scenario**.

## Prerequisite

- GraalVM installed and configured appropriately (sdkman: 23.0.2-graal)
- Apache Maven 3.9.9
- A working container runtime (Docker or Podman)
- A working C development environment

You can use this link to set up your [working environment](https://quarkus.io/guides/building-native-image#configuring-c-development).

## Create a native build

```console
$ mvn clean package -Dnative
```

## Run the example

Start a postgres instance with docker compose:
```console
$ docker compose up
```

Start the application:
```console
$ ./debezium-native-1.0-SNAPSHOT-runner
```