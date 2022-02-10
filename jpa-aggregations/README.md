# Debezium Hibernate Aggregate Materialization Demo

This demo shows how to materialize consistent aggregates (e.g. a customer and all their addresses) using a PoC-level Hibernate ORM extension.
This extension persists materialized aggregates (represented as JSON) in a dedicated table, `aggregates`.
Debezium is set up to capture changes from this table and stream them into Kafka.
An SMT (single message transform) is used to expand the aggregate's JSON into typed Kafka Connect records and route them into a dedicated topic per aggregate root type.
The Elasticsearch sink connector is used to consume these records and persist the structured aggregate into Elasticsearch.

## Usage

How to run:

```shell
# Make sure that you have maven installed. Use command: sudo apt install maven
cd json-smt-es && mvn clean install && cd ../

# Start the DB, Kafka Connect, Elasticsearch etc.
export DEBEZIUM_VERSION=1.8
docker-compose up --build

# Register MySQL connector to capture changes from the "aggregates" table
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @source.json
```

Import the _jpa-test_ project into your IDE and execute `JpaAggregationTest`.

```shell
# Observe changes to the aggregate topic while applying more changes to customers using the test class above
docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic customers-complete
```

```shell
# Register ES sink connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @es-sink-aggregates.json
```

Examine contents of the Elasticsearch index while you alter the customer data:

```shell
curl -i -X GET -H "Accept:application/json" http://localhost:9200/customers-complete/_search?pretty
```
End the application:

```shell
# Shut down the cluster
docker-compose down
```
