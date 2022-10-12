# Debezium CloudEvents Demo

This demo automatically deploys the topology of services as defined in the [Debezium Tutorial](https://debezium.io/docs/tutorial/).

## Preparations

```shell
export DEBEZIUM_VERSION=2.0
mvn clean install -f avro-data-extractor/pom.xml
docker-compose up --build
```

## CloudEvents Structured Mode with JSON for envelope and data

```shell
# Deploy Postgres connector
curl -i -X PUT -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/inventory-connector-json-json/config -d @register-postgres-json-json.json

# Consume messages from the Debezium topic
docker run --rm --tty \
  --network cloudevents-network \
  quay.io/debezium/tooling:1.2 \
  kafkacat -b kafka:9092 -C -o beginning -q \
  -t dbserver1.inventory.customers | jq .

# Modify records in the database via psql client
docker-compose  exec postgres env PGOPTIONS="--search_path=inventory" bash -c 'psql -U $POSTGRES_USER postgres'
```

In order to produce `data` values without the embedded JSON schema, add `"value.converter.json.schemas.enable" : "false"` to the connector configuration and `PUT` it again.

## CloudEvents Structured Mode with JSON for envelope and Avro for data

```shell
# Deploy Postgres connector
curl -i -X PUT -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/inventory-connector-json-avro/config -d @register-postgres-json-avro.json

# Consume messages from the Debezium topic
docker run --rm --tty \
  --network cloudevents-network \
  quay.io/debezium/tooling:1.2 \
  kafkacat -b kafka:9092 -C -o beginning -q \
  -t dbserver2.inventory.customers | jq .
```

Observe how the `data` field is (base64-encoded) Avro binary data.
A Kafka Streams application (see _avro-data-extractor_ directory) processes this topic and writes out the extracted Avro data to the `customers2` topic.
Examine its contents like so:

```shell
docker run --rm --tty \
  --network cloudevents-network \
  quay.io/debezium/tooling:1.2 \
  kafkacat -b kafka:9092 -C -o beginning -q -s value=avro -r http://schema-registry:8081 \
  -t customers2 | jq .
```

## CloudEvents Structured Mode with Avro for envelope and data

```shell
# Deploy Postgres connector
curl -i -X PUT -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/inventory-connector-avro-avro/config -d @register-postgres-avro-avro.json

# Consume messages from the Debezium topic:
docker run --rm --tty \
  --network cloudevents-network \
  quay.io/debezium/tooling:1.2 \
  kafkacat -b kafka:9092 -C -o beginning -q -s value=avro -r http://schema-registry:8081 \
  -t dbserver3.inventory.customers | jq .
```

Again the `data` field is an Avro-encoded binary itself.
The same stream processing application writes out that data to the `customers3` topic:

```shell
docker run --rm --tty \
  --network cloudevents-network \
  quay.io/debezium/tooling:1.2 \
  kafkacat -b kafka:9092 -C -o beginning -q -s value=avro -r http://schema-registry:8081 \
  -t customers2 | jq .
```

## CloudEvents Binary Mode

tbd.

## Clean-up

```shell
docker-compose down
```

## Debugging

Should you need to establish a remote debugging session into a deployed connector, add the following to the `environment` section of the `connect` in the Compose file service:

    - KAFKA_DEBUG=true
    - DEBUG_SUSPEND_FLAG=n

Also expose the debugging port 5005 under `ports`:

    - 5005:5005

You can then establish a remote debugging session from your IDE on localhost:5005.

## Useful Commands

Listing all topics:

```shell
docker-compose exec kafka /kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list
```
