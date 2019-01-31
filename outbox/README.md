# Outbox Pattern

This example demonstrates the "outbox pattern", an approach for letting services communicate in an asynchronous and reliable fashion.
The sending services produces events in an "outbox" event table within its own local database.
Debezium captures the additions to this table and streams the events to consumers via Apache Kafka.

## Execution

Prepare the Java components:

    mvn clean install -f event-routing-smt/pom.xml
    mvn clean install -f order-service/pom.xml

Start all components:

    docker-compose up --build

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
