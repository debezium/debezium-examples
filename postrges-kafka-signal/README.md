# PostgreSQL Snapshot via Kafka event 

With the release of Debezium version 2.3, incremental snapshot can be [triggered in a different way](https://debezium.io/documentation/reference/2.3/configuration/signalling.html) and not only via an insert into signal table. Also Kafka signal, that was available only for MySQL with GTIDs enabled, is now an option for all connectors.

In this example, we create a simple setup in which we insert data into a PostgreSQL database, start a Kafka
Connector and finally trigger a snapshot by producing data into the signal topic.

Due to simplicity, we will start an environment without any security.

## Resources
* [Release Blog](https://debezium.io/blog/2023/06/27/Debezium-signaling-and-notifications/)
* [Enabling Kafka signaling channel](https://debezium.io/documentation/reference/stable/configuration/signalling.html#debezium-signaling-enabling-kafka-signaling-channel)
* [Debezium connector for PostgreSQL](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#debezium-postgresql-connector-kafka-signals-configuration-properties)
    * [Debezium connector Kafka signals configuration properties](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#debezium-postgresql-connector-kafka-signals-configuration-properties)

## Run
Start the environment
```
docker-compose up -d
```

Check if Connect contains the Debezium connector
```
curl -s -XGET http://localhost:8083/connector-plugins | jq '.[].class'
```

## PostgreSQL

Enter PostgreSQL
```
docker exec -i postgres psql -U myuser -d postgres
```

Create the data table
```
CREATE table characters (
        id INT PRIMARY KEY,
        first_name VARCHAR(50),
        last_name VARCHAR(50)
        );
INSERT INTO characters VALUES (1, 'luke', 'skywalker');
INSERT INTO characters VALUES (2, 'anakin', 'skywalker');
INSERT INTO characters VALUES (3, 'padmé', 'amidala');
SELECT * FROM characters;
```

Create Signal Table (still required)

```
CREATE TABLE debezium_signal (id VARCHAR(100) PRIMARY KEY, type VARCHAR(100) NOT NULL, data VARCHAR(2048) NULL);
```

## Connector

For the Kafka signal, we need to add to the connector configuration

```yaml
"signal.enabled.channels": "source,kafka",
"signal.kafka.topic": "signal-topic",
"signal.kafka.bootstrap.servers": "broker:29092"
```

⚠️ **Note:** You might need to add additional configuration due to security requirements of your setup. You can 
do this by passing the properties with the prefix `signals.consumer.*`


Deploy the connector
```shell
curl -X POST -H "Content-Type: application/json" --data @connector.json http://localhost:8083/connectors | jq
```

We consume the topic
```
kafka-console-consumer --bootstrap-server localhost:9092 --topic test.public.characters --from-beginning
```
and should see the 3 messages of the characters table.

## Trigger the Snapshot

### Insert manually (the old way)
```
INSERT INTO debezium_signal (id, type, data) VALUES ('ad-hoc', 'execute-snapshot', '{"data-collections": ["public.characters"],"type":"incremental"}');
```

### Produce into signal Kafka topic (the new event-driven way)


Produce into the signal topic
```
kafka-console-producer  --broker-list localhost:9092 --topic signal-topic  --property parse.key=true --property key.separator=":"
```
Ensure that the key equals the topic.prefix configuration.
```
test:{"type":"execute-snapshot","data": {"data-collections": ["public.characters"], "type": "INCREMENTAL"}}
```

Now we should see 9 messages in the topic (3 inserts + 3 from the manual snapshot + 3 from the Kafka snapshot).