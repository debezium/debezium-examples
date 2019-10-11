# Outbox Pattern

This example demonstrates the "outbox pattern", an approach for letting services communicate in an asynchronous and reliable fashion.
It accompanies [this post](https://debezium.io/blog/2019/02/19/reliable-microservices-data-exchange-with-the-outbox-pattern) on the Debezium blog.

The sending service ("order-service") produces events in an "outbox" event table within its own local database.
Debezium captures the additions to this table and streams the events to consumers via Apache Kafka.
The receiving service ("shipment-service") receives these events (and would apply some processing based on them),
excluding any duplicate messages by comparing incoming event ids with already successfully consumed ids.

**Update, March 3, 2019:** Another variant of the receiving service has been added, "shipment-service-quarkus",
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

Register the Debezium Postgres connector:

```console
$ http PUT http://localhost:8083/connectors/outbox-connector/config < register-postgres.json
HTTP/1.1 201 Created
```

Place a "create order" request with the order service:

```console
$ http POST http://localhost:8080/order-service/rest/orders < resources/data/create-order-request.json
```

Cancel one of the two order lines:

```console
$ http PUT http://localhost:8080/order-service/rest/orders/1/lines/2 < resources/data/cancel-order-line-request.json
```

Examine the events produced by the service via the Apache Kafka console consumer:

```console
$ docker run --tty --rm \
    --network outbox_default \
    debezium/tooling:1.0 \
    kafkacat -b kafka:9092 -C -o beginning -q \
    -f "{\"key\":%k, \"headers\":\"%h\"}\n%s\n" \
    -t order.events | jq .
```

Examine that the receiving services process the events:

```console
$ docker-compose logs -f shipment-service
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

E.g. to query for all purchase orders:

```sql
select * from inventory.purchaseorder po, inventory.orderline ol where ol.order_id = po.id;
```

Getting a session in the MySQL DB of the "shipment" service:

```console
$ docker run --tty --rm -i \
        --network outbox_default \
        debezium/tooling:1.0 \
        bash -c 'mycli mysql://mysqluser:mysqlpw@shipment-db:3306/inventory'
```

E.g. to query for all shipments:

```sql
select * from Shipment;
```

Getting a session in the MariaDB DB of the "shipment-quarkus" service:

```console
$ docker run --tty --rm -i \
        --network outbox_default \
        debezium/tooling:1.0 \
        bash -c 'mycli mysql://mariadbuser:mariadbpw@shipment-db-quarkus:3306/shipmentdb'
```
