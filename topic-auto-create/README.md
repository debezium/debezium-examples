# Auto-creating Debezium Change Data Topics Example

## Prerequisites for this example

- Installed Docker and `docker-compose`
- Installed `curl` (and optionally `json_pp`)
- Cloned or downloaded this example

## Usage

You can find the documentation of the Change Data Topics Auto-creation in the Debezium Docs[] 

### Start Docker Containers

To spin up some example infrastructure like Kafka, Debezium and a Postgres database you
have to execute from this example directory (`./topic-auto-create` directory):

```bash
$ DEBEZIUM_VERSION=1.4 docker-compose up -d

  Creating network "topic-auto-create_default" with the default driver
  Creating topic-auto-create_zookeeper_1 ... done
  Creating topic-auto-create_postgres_1  ... done
  Creating topic-auto-create_kafka_1     ... done
  Creating topic-auto-create_connect_1   ... done
``` 

### Connector Config

Then we use the following connector config for a Postgres connector to capture the `inventory`
schema where the default config for topics should be `replication.factor = 1`, `partitions = 5`,
the topic should be key compacted with `cleanup.policy = "compact"`, and all messages should
be forced to GZIP compression on harddisk with `compression.type = "gzip"`.

Some topics of captured tables that have a name starting with `product` should go to topics
that have a retention time of 3 months / 90 days with `cleanup.policy": "delete"` and
`retention.ms = 7776000000`, `replication.factor = 1`, `partitions = 20`, and just use the
compression format that's chosen by the producer with `compression.type": "producer"`.

```json
{
    "name": "inventory-connector",
    "config": {
        "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
        "tasks.max": 1,
        "database.hostname": "postgres",
        "database.port": 5432,
        "database.user": "postgres",
        "database.password": "postgres",
        "database.dbname" : "postgres",
        "database.server.name": "dbserver1",
        "schema.include.list": "inventory",

        "topic.creation.default.replication.factor": 1,
        "topic.creation.default.partitions": 5,
        "topic.creation.default.cleanup.policy": "compact",
        "topic.creation.default.compression.type": "gzip",
        "topic.creation.default.delete.retention.ms" : 2592000000,

        "topic.creation.groups": "productlog",

        "topic.creation.productlog.include": "dbserver1\\.inventory\\.product.*",
        "topic.creation.productlog.replication.factor": 1,
        "topic.creation.productlog.partitions": 10,
        "topic.creation.productlog.cleanup.policy": "delete",
        "topic.creation.productlog.compression.type": "producer",
        "topic.creation.productlog.retention.ms": 7776000000
    }
}
```

### Sending connector config

```bash
$ curl -X PUT -H "Accept: application/json" -H "Content-Type: application/json" -d @connector.json "http://localhost:8083/connectors/inventory-connector/config" | json_pp

% Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100  2154  100  1009  100  1145   1318   1496 --:--:-- --:--:-- --:--:--  2815
{
   "config" : {
      "connector.class" : "io.debezium.connector.postgresql.PostgresConnector",
      "database.dbname" : "postgres",
      "database.hostname" : "postgres",
      "database.password" : "postgres",
      "database.port" : "5432",
      "database.server.name" : "dbserver1",
      "database.user" : "postgres",
      "name" : "inventory-connector",
      "schema.include.list" : "inventory",
      "tasks.max" : "1",
      "topic.creation.default.cleanup.policy" : "compact",
      "topic.creation.default.compression.type" : "gzip",
      "topic.creation.default.delete.retention.ms" : "2592000000",
      "topic.creation.default.partitions" : "5",
      "topic.creation.default.replication.factor" : "1",
      "topic.creation.groups" : "productlog",
      "topic.creation.productlog.cleanup.policy" : "delete",
      "topic.creation.productlog.compression.type" : "producer",
      "topic.creation.productlog.retention.ms" : "7776000000",
      "topic.creation.productlog.include" : "dbserver1\\.inventory\\.product.*",
      "topic.creation.productlog.partitions" : "10",
      "topic.creation.productlog.replication.factor" : "1"
   },
   "name" : "inventory-connector",
   "tasks" : [],
   "type" : "source"
}
``` 

### Validating the result

```bash
## Login into the Kafka container
$ docker exec -it topic-auto-create_kafka_1 bash

## Check the orders topic, if the `default` group config was applied
$ /kafka/bin/kafka-topics.sh --bootstrap-server $HOSTNAME:9092 --describe --topic dbserver1.inventory.orders

Topic: dbserver1.inventory.orders       PartitionCount: 5       ReplicationFactor: 1    Configs: compression.type=gzip,cleanup.policy=compact,segment.bytes=1073741824,delete.retention.ms=2592000000

## Check the products topic, if the `productlog` group config was applied
$ /kafka/bin/kafka-topics.sh --bootstrap-server $HOSTNAME:9092 --describe --topic dbserver1.inventory.products

Topic: dbserver1.inventory.products     PartitionCount: 10      ReplicationFactor: 1    Configs: compression.type=producer,cleanup.policy=delete,segment.bytes=1073741824,retention.ms=7776000000
```

### Cleanup

Exit from the docker container back to the example dir then destroy and remove the containers with: 

```bash
docker-compose down
```
