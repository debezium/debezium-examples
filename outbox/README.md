# Outbox Pattern

This example demonstrates the "outbox pattern", an approach for letting services communicate in an asynchronous and reliable fashion.
It accompanies [this post](https://debezium.io/blog/2019/02/19/reliable-microservices-data-exchange-with-the-outbox-pattern) on the Debezium blog.

The sending service ("order-service") produces events in an "outbox" event table within its own local database.
Debezium captures the additions to this table and streams the events to consumers via Apache Kafka.
The receiving service ("shipment-service-quarkus") receives these events (and would apply some processing based on them),
excluding any duplicate messages by comparing incoming event ids with already successfully consumed ids.

The receiving service ("shipment-service-quarkus") is implemented using the [Quarkus](https://quarkus.io) stack.
This allows to build a native binary of that service, resulting in significantly less memory usage and faster start-up than the JVM-based version.

**Update, December 20, 2019:** Another variant of the producing service has been added, "order-service-quarkus",
which is functionally the same as the original one, but is implemented using the [Quarkus](https://quarkus.io) stack.
This allows to build a native binary of that service, resulting in significantly less memory usage and faster start-up than the classic version (based on Thorntail).

## Execution

Prepare the Java components:

```console
$ mvn clean install -Pnative -Dnative-image.docker-build=true
```

Start all components:

```console
$ export DEBEZIUM_VERSION=0.10
$ docker-compose up --build
```

Register the Debezium Postgres connector (Non Quarkus):

```console
$ http PUT http://localhost:8083/connectors/outbox-connector/config < register-postgres.json
HTTP/1.1 201 Created
```

Register the Debezium Postgres connector (Quarkus):

```console
$ http PUT http://localhost:8083/connectors/outbox-connector-quarkus/config < register-postgres-quarkus.json
HTTP/1.1 201 Created
```

Place a "create order" request with the order service:

```console
$ http POST http://localhost:8080/order-service/rest/orders < resources/data/create-order-request.json
```

Place a "create order" request with the Quarkus order service:

```console
$ http POST http://localhost:8081/rest/orders < resources/data/create-order-request.json
```

Cancel one of the two order lines:

```console
$ http PUT http://localhost:8080/order-service/rest/orders/1/lines/2 < resources/data/cancel-order-line-request.json
```

Cancel one of the two order lines (Quarkus scenario):

```console
$ http PUT http://localhost:8081/rest/orders/1/lines/2 < resources/data/cancel-order-line-request.json
```

Examine the events produced by the service using _kafkacat_:

```console
$ docker run --tty --rm \
    --network outbox_default \
    debezium/tooling:1.0 \
    kafkacat -b kafka:9092 -C -o beginning -q \
    -f "{\"key\":%k, \"headers\":\"%h\"}\n%s\n" \
    -t order.events | jq .
```

Examine that the receiving services (Quarkus) process the events:

```console
$ docker-compose logs -f shipment-service-quarkus
```

(Look for "Processing '{OrderCreated|OrderLineUpdated}' event" messages in the logs)

## Useful Commands

Getting a session in the Postgres DB of the "order" service:

```console
$ docker run --tty --rm -i \
        --network outbox_default \
        debezium/tooling:1.0 \
        bash -c 'pgcli postgresql://postgresuser:postgrespw@order-db:5432/orderdb'
```        

Getting a session to the Postgres DB of the Quarkus "order" service:

```console
$ docker run --tty --rm -i \
        --network outbox_default \
        debezium/tooling:1.0 \
        bash -c 'pgcli postgresql://postgresuser:postgrespw@order-db-quarkus:5432/orderdb'        
```

E.g. to query for all purchase orders:

```sql
select * from inventory.purchaseorder po, inventory.orderline ol where ol.order_id = po.id;
```

Getting a session in the MariaDB DB of the "shipment-quarkus" service:

```console
$ docker run --tty --rm -i \
        --network outbox_default \
        debezium/tooling:1.0 \
        bash -c 'mycli mysql://mariadbuser:mariadbpw@shipment-db-quarkus:3306/shipmentdb'
```

E.g. to query for all shipments:

```sql
select * from Shipment;
```
