# Outbox Pattern

This example demonstrates the "outbox pattern", an approach for letting services communicate in an asynchronous and reliable fashion.
It accompanies [this post](https://debezium.io/blog/2019/02/19/reliable-microservices-data-exchange-with-the-outbox-pattern) on the Debezium blog.

The sending service ("order-service") produces events in an "outbox" event table within its own local database.
Debezium captures the additions to this table and streams the events to consumers via Apache Kafka.
The receiving service ("shipment-service") receives these events (and would apply some processing based on them),
excluding any duplicate messages by comparing incoming event ids with already successfully consumed ids.

Both services are implemented using the [Quarkus](https://quarkus.io) stack.
This allows building a native binary of each service, resulting in significantly less memory usage and faster start-up than the JVM-based version.

## Building

Prepare the Java components by first performing a Maven build.

```console
$ mvn clean install -Pnative -Dnative-image.docker-build=true
```

This illustates the usage of Quarkus' `native` profile mode where the quarkus-maven-plugin will generate not only JVM-based artifacts but also native images.
The `native` profile and `native-image.docker-build` environment variable can be omitted if native image artifacts are not wanted.

## Environment

Setup the necessary environment variables

```console
$ export DEBEZIUM_VERSION=1.0
$ export QUARKUS_BUILD=native
```

The `DEBEZIUM_VERSION` specifies which version of Debezium artifacts should be used.
The `QUARKUS_BUILD` specifies whether docker-compose will build containers using Quarkus in JVM or Native modes.
The default is `jvm` for JVM mode but `native` can also be specified to build Quarkus native containers.
  
## Start the demo  

Start all components:

```console
$ docker-compose up --build
```

This executes all configurations set forth by the `docker-compose.yaml` file.

It's important to note that sometimes the order or shipment service may fail to start if the dependent database takes longer than expected to initialize.  If that happens, simply re-execute the command again and it will start the remaining services. 

## Configure the Debezium connector

Register the connector that to stream outbox changes from the order service: 

```console
$ http PUT http://localhost:8083/connectors/outbox-connector/config < register-postgres.json
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

## Review the outcome

Examine the events produced by the service using _kafkacat_:

```console
$ docker run --tty --rm \
    --network outbox_default \
    debezium/tooling:1.0 \
    kafkacat -b kafka:9092 -C -o beginning -q \
    -f "{\"key\":%k, \"headers\":\"%h\"}\n%s\n" \
    -t Order.events | jq .
```

Examine that the receiving service process the events:

```console
$ docker-compose logs -f shipment-service
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

Getting a session in the Postgres DB of the "shipment" service:

```console
$ docker run --tty --rm -i \
        --network outbox_default \
        debezium/tooling:1.0 \
        bash -c 'pgcli postgresql://postgresuser:postgrespw@shipment-db:5432/shipmentdb'
```

E.g. to query for all shipments:

```sql
select * from inventory.shipment;
```
