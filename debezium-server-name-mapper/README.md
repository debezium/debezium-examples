# Custom Topic Name Mapper

This example demonstrates how to develop and deploy a bean that would allow the developer to implement a custom topic naming policy.

The implementation of the custom naming policy is in the class `PrefixingNameMapper`.
The class is marked as `@Dependent` and it is injected into the configured sink adaptor class.
It exposes a configuration option named `mapper.prefix` which defines a string that is inserted before the original topic name.

The configuration option is configured either via `application.properties` configuration file or via other ways defined in the Microprofile Config specification, e.g. an environment variable.

As an example, when setting `mapper.prefix` to the value `superprefix`, then a message intended to be delivered to the topic `server.schema.table` would be routed to topic `superprefix.server.schema.table`.

## Topology

This demo uses Apache Pulsar as the sink and the standard Debezium example PostgreSQL database.
Both Apache Pulsar and the source database are deployed via Docker Compose file.

## How to run

From terminal start the source database and the sink system:

```
$ export DEBEZIUM_VERSION=2.1
$ docker compose up
```

In another terminal build the custom naming policy class and the runner JAR to start the application:

```
$ mvn clean install
```

Start the application:

```
$ java -jar target/quarkus-app/quarkus-run.jar
```

In another terminal check the created topics:

```
docker compose exec pulsar bin/pulsar-admin broker-stats topics -i
```

The resulting topic list should contain for example a topic named

```
"persistent://public/default/superprefix.tutorial.inventory.customers": {
"publishers": [
{
    "msgRateIn": 0.332,
    "msgThroughputIn": 815.825,
    "averageMsgSize": 2452.0,
    "address": "/192.168.16.1:43258",
    "producerId": 0,
    "producerName": "standalone-0-2",
    "connectedSince": "2020-06-15T07:20:59.189Z",
    "clientVersion": "2.5.2",
    "metadata": {}
}
],
"replication": {},
"subscriptions": {},
"producerCount": 1,
"averageMsgSize": 2452.500,
"msgRateIn": 0.332,
"msgRateOut": 0.0,
"msgThroughputIn": 815.825,
"msgThroughputOut": 0.0,
"storageSize": 9810,
"backlogSize": 0,
"pendingAddEntriesCount": 0
},
```

So the topic name is prepended with `superprefix` string that is defined in the `application.properties` file.
