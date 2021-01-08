# Distributed Caching

This example demonstrates how to combine Debezium and Infinispan.

TODO: expand

## Building

Prepare the Java components by first performing a Maven build.

```console
$ mvn clean verify
```

## Environment

Setup the necessary environment variables:

```console
$ export DEBEZIUM_VERSION=1.4
```

The `DEBEZIUM_VERSION` specifies which version of Debezium artifacts should be used.
  
## Start the demo  

Start all components:

```console
$ docker-compose up --build
```

This executes all configurations set forth by the `docker-compose.yaml` file.

## Configure the Debezium connector

Register the connector that to stream outbox changes from the order service: 

```console
$ http PUT http://localhost:8083/connectors/order-connector/config < register-postgres.json

HTTP/1.1 201 Created
```

## Call the various REST-based APIs

Place a "create order" request with the order service:

```console
$ http POST http://localhost:8080/orders < resources/data/create-order-request.json
```

Cancel one of the two order lines:

```console
$ http PUT http://localhost:8080/orders/1/lines/2 < resources/data/cancel-order-line-request.json
```

## Review the Outcome

Examine the events produced by the service using _kafkacat_:

```console
$ docker run --tty --rm \
    --network distributed-caching-network \
    debezium/tooling:1.1 \
    kafkacat -b kafka:9092 -C -o beginning -q \
    -t dbserver1.inventory.purchaseorder | jq .
```

Specify `dbserver1.inventory.orderline` as the topic name to examine the order line events.

Examine that the receiving service process the events:

```console
$ docker-compose logs cache-update-service
```

(Look for "Received '{PO]OL}'" messages in the logs)

## Useful Commands

Getting a session in the Postgres DB of the "order" service:

```console
$ docker run --tty --rm -i \
    --network distributed-caching-network \
    debezium/tooling:1.1 \
    bash -c 'pgcli postgresql://postgresuser:postgrespw@order-db:5432/orderdb'
```

E.g. to query for all purchase orders:

```sql
select * from inventory.purchaseorder po, inventory.orderline ol where ol.order_id = po.id;
```

Alternatively, you can access pgAdmin on http://localhost:5050.

List all Kafka topics:

```console
$ docker-compose exec kafka /kafka/bin/kafka-topics.sh --zookeeper zookeeper:2181 --list
```

## Running the Quarkus Services Locally in Dev Mode

When working on the Quarkus services, it's better to use the dev mode locally instead of rebuilding the container images all the time.
In order to do so, in the _docker-compose.yml_ file, set the `ADVERTISED_HOST_NAME` variable of the `kafka` service to the IP of your host machine.
Otherwise, the consuming application (_cache-update-service_) will not be able to connect Kafka.

Start all components besides the two services:

```console
$ docker-compose up --build --scale order-service=0 --scale cache-update-service=0
```

Then start the two services in dev mode:

```console
$ mvn compile quarkus:dev -f order-service/pom.xml
```

```console
$ mvn compile quarkus:dev -f cache-update-service/pom.xml
```
