# Outbox Pattern

This example demonstrates the "outbox pattern", an approach for letting services communicate in an asynchronous and reliable fashion.
The sending service ("order-service") produces events in an "outbox" event table within its own local database.
Debezium captures the additions to this table and streams the events to consumers via Apache Kafka.
The receiving service ("shipment-service") receives these events (and would apply some processing based on them),
excluding any duplicate messages by comparing incoming event ids with already successfully consumed ids.

## Execution

Prepare the Java components:

    mvn clean install

Start all components:

    export DEBEZIUM_VERSION=0.9
    docker-compose up --build

Register the Debezium Postgres connector:

    cat register-postgres.json  | http POST http://localhost:8083/connectors/

Place a "create order" request with the order service:

    cat resources/data/create-order-request.json | http POST http://localhost:8080/order-service/rest/orders

Cancel one of the two order lines:

    cat resources/data/cancel-order-line-request.json | http PUT http://localhost:8080/order-service/rest/orders/1/lines/2

Examine the events produced by the service via the Apache Kafka console consumer:

    docker run --tty --rm \
        --network outbox-quarkus_default \
        debezium/tooling:1.0 \
        kafkacat -b kafka:9092 -C -o beginning -q \
        -t OrderEvents | jq .

Examine that the receiving service processes the events:

    docker-compose logs -f shipment-service

(Look for "Processing '{OrderCreated|OrderLineUpdated}' event" messages in the log)

## Useful Commands

Getting a session in the Postgres DB of the "order" service:

    docker run --tty --rm -i \
        --network outbox-quarkus_default \
        debezium/tooling:1.0 \
        bash -c 'pgcli postgresql://postgresuser:postgrespw@order-db:5432/orderdb'

E.g. to query for all purchase orders:

    select * from inventory.purchaseorder po, inventory.orderline ol where ol.order_id = po.id;

Getting a session in the MySQL DB of the "shipment" service:

    docker run --tty --rm -i \
        --network outbox-quarkus_default \
        debezium/tooling:1.0 \
        bash -c 'mycli mysql://mariadbuser:mariadbpw@shipment-db:3306/shipmentdb'

E.g. to query for all shipments:

    select * from Shipment;
