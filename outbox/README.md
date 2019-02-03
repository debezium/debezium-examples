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

    docker-compose up --build

Register the Debezium Postgres connector:

    curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-postgres.json

Place a "create order" request with the order service:

    cat resources/data/create-order-request.json | http POST http://localhost:8080/order-service/rest/orders

Cancel one of the two order lines:

    cat resources/data/cancel-order-line-request.json | http PUT http://localhost:8080/order-service/rest/orders/1/lines/2

Examine the events produced by the service via the Apache Kafka console consumer:

    docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh \
        --bootstrap-server kafka:9092 \
        --from-beginning \
        --property print.key=true \
        --topic Order

Examine that the receiving service processes the events:

    docker-compose logs -f shipment-service

(Look for "Processing '{OrderCreated|OrderLineUpdated}' event" messages in the log)

## Useful Commands

Getting a session in the Postgres DB of the "order" service:

    docker-compose exec order-db env PGOPTIONS="--search_path=inventory" bash -c 'psql -U $POSTGRES_USER orderdb'

E.g. to query for all purchase orders:

    select * from purchaseorder po, orderline ol where ol.order_id = po.id;

Getting a session in the MySQL DB of the "shipment" service:

    docker-compose exec shipment-db bash -c 'mysql -u $MYSQL_USER -p$MYSQL_PASSWORD inventory'

E.g. to query for all shipments:

    select * from Shipment;
