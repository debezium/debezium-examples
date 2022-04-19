# Distributed Caching

This example demonstrates how to combine Debezium and Infinispan for an CQRS-style application design.

Multiple instances of the "Order" microservice (one in "London", one in "New York") use a shared Postgres database at a "Central" location as a system of record; i.e. all writes they do go against that Postgres database.
For performance reasons, read requests are done against local Infinispan caches.
These caches are kept in sync via Debezium.
Another service, "Cache Updater", is subscribed to the change event topics in Kafka and updates an Infinispan cache at "Central", from where the data is replicated to "London" and "New York" using Infinispan's cross-site replication feature.
In addition, the data view in Infinispan is denormalized (materialized join between purchase orders and order lines), so data can be obtained very efficiently.

![Solution Overview](architecture-overview.png)

## Building

Prepare the Java components by first performing a Maven build.

```console
$ mvn clean verify
```

## Environment

Setup the necessary environment variables:

```console
$ export DEBEZIUM_VERSION=1.8
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
    quay.io/debezium/tooling:1.2 \
    kafkacat -b kafka:9092 -C -o beginning -q \
    -t dbserver1.inventory.purchaseorder | jq .
```

Specify `dbserver1.inventory.orderline` as the topic name to examine the order line events.

Examine that the receiving service process the events:

```console
$ docker-compose logs cache-update-service
```

(Look for "Received '{PO]OL}'" messages in the logs)

## Infinispan

3 Infinispan nodes that run in 3 different sites via Docker Compose.

The Cache Update Service connects to "Central", for writes.
Order Service 1 to "NYC", for reads.
Order Service 2 to "LON", for reads.

From your local navigator, you can access the different Infinispan server web consoles at:

LON in `http://localhost:11222/console/`
NYC in `http://localhost:31222/console/`
Central in `http://localhost:41222/console/`

The "Central" 'orders' cache has to write backups to NYC and LON.

Test a query on the console:

```console
from caching.PurchaseOrder po where po.lineItems.status="CANCELLED"
```

Order Service 1 runs in `http://localhost:8080/`
Order Service 2 runs in `http://localhost:8081/`

```console
$ http GET http://localhost:8080/orders/1
$ http GET http://localhost:8081/orders/1
```

Add an order from the order service in LON or NYC.
Using the console, check the data available in every cluster.

## Running the Quarkus Services Locally in Dev Mode

When working on the Quarkus services, it's better to use the dev mode locally instead of rebuilding the container images all the time.
In order to do so, in the _docker-compose.yml_ file, set the `ADVERTISED_HOST_NAME` variable of the `kafka` service to the IP of your host machine.
Otherwise, the consuming application (_cache-update-service_) will not be able to connect Kafka.

Start all components besides the two services:

```console
$ docker-compose up --scale order-service-nyc=0 --scale order-service-lon=0 --scale cache-update-service=0
```

Then start the two services in dev mode:

```console
$ mvn compile quarkus:dev -f order-service/pom.xml
```

```console
$ mvn compile quarkus:dev -f cache-update-service/pom.xml
```

## Useful Commands

Getting a session in the Postgres DB of the "order" service:

```console
$ docker run --tty --rm -i \
    --network distributed-caching-network \
    quay.io/debezium/tooling:1.2 \
    bash -c 'pgcli postgresql://postgresuser:postgrespw@order-db:5432/orderdb'
```

E.g. to enable the before row state:

```sql
ALTER TABLE inventory.purchaseorder REPLICA IDENTITY FULL;
ALTER TABLE inventory.orderline REPLICA IDENTITY FULL;
```

E.g. to query for all purchase orders:

```sql
select * from inventory.purchaseorder po, inventory.orderline ol where ol.order_id = po.id;
```

Alternatively, you can access pgAdmin on http://localhost:5050.

List all Kafka topics:

```console
$ docker-compose exec kafka /kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list
```

Get the status of the CDC connector:

```console
$ http GET http://localhost:8083/connectors/order-connector/status
```

Delete the CDC connector:

```console
$ http DELETE http://localhost:8083/connectors/order-connector
```
