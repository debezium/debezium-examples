# Saga Pattern

This example demonstrates how to implement the [Saga pattern](https://microservices.io/patterns/data/saga.html) for realizing distributed transactions across multiple microservices, in a safe and reliable way using change data capture.

Similarly to the outbox pattern, this implementation avoids unsafe dual writes to a service's database and Apache Kafka by channeling all outgoing messages through the originating service's database and capturing them from there using CDC and Debezium.

There are three services involved:

* _order-service:_ originator and orchestrator of the Saga
* _customer-service:_ approves or rejects the customer's credit needed to fulfill an incoming order
* _payment-service_ executes the payment associated to an incoming order

## Running the Example

Build and start up:

Note: the services are not part of the Docker Compose set-up yet. Instead, run them locally as described below.

```console
$ mvn clean verify
```

```console
$ docker-compose up
```

Register the connectors for the different services:

```console
$ http PUT http://localhost:8083/connectors/order-outbox-connector/config < register-order-connector.json
$ http PUT http://localhost:8083/connectors/payment-outbox-connector/config < register-payment-connector.json
$ http PUT http://localhost:8083/connectors/credit-outbox-connector/config < register-credit-connector.json
```

Place an order:

```console
$ http POST http://localhost:8080/orders < requests/place-order.json
```

Examine the emitted messages for `payment` and `credit-approval` in Apache Kafka:

```console
$ docker run --tty --rm \
    --network saga-network \
    debezium/tooling:1.1 \
    kafkacat -b kafka:9092 -C -o beginning -q \
    -f "{\"key\":%k, \"headers\":\"%h\"}\n%s\n" \
    -t payment.request

$ docker run --tty --rm \
    --network saga-network \
    debezium/tooling:1.1 \
    kafkacat -b kafka:9092 -C -o beginning -q \
    -f "{\"key\":%k, \"headers\":\"%h\"}\n%s\n" \
    -t credit-approval.request
```

Examine the saga state in the order service's database:

```console
$ docker run --tty --rm -i \
        --network saga-network \
        debezium/tooling:1.1 \
        bash -c 'pgcli postgresql://todouser:todopw@order-db:5432/tododb'

select * from todo.sagastate;

+--------------------------------------+------------------------------------------------------------------------------------------+----------+---------------------------------------------------+-----------------+-----------+
| id                                   | payload                                                                                  | status   | stepstate                            | type            | version   |
|--------------------------------------+------------------------------------------------------------------------------------------+----------+---------------------------------------------------+-----------------+-----------|
| d08b4e43-8522-4539-9aba-de4bb0dc1a8e | {"payment-due":59,"customer-id":456,"order-id":2,"credit-card-no":"xxxx-yyyy-dddd-aaaa"} | COMPLETED  | {"credit-approval":"SUCCEEDED","payment":"SUCCEEDED"} | order-placement | 1         |
+--------------------------------------+------------------------------------------------------------------------------------------+----------+---------------------------------------------------+-----------------+-----------+
```

Place an order with an invalid credit card number (the payment service rejects any number that ends with "9999"):

```console
$ http POST http://localhost:8080/orders < requests/place-invalid-order1.json
```

Observe how the saga's state is `ABORTED`, with the `payment` step `FAILED` and the `credit-approval` step `ABORTED`.

Now place an order which exceeds the credit limit (the customer service rejects any value over 5000):

```console
$ http POST http://localhost:8080/orders < requests/place-invalid-order2.json
```

Observe how the saga's state again is `ABORTED`, with the step states set accordingly.

Now stop the payment service and place a valid order again. Observe how the saga remains in state `STARTED`, with the `credit-approval` step `SUCCEEDED` and the `payment` step `STARTED`.
Start the payment service again and observe how the saga completes.

## Running Locally

Set the ADVERTISED_HOST_NAME env variable of the _kafka_ service in _docker-compose.yml_ to the address of your host machine.

```console
$ docker-compose up --build --scale order-service=0 --scale payment-service=0 --scale customer-service=0
```

```console
$ mvn compile quarkus:dev -f order-service/pom.xml
```

```console
$ mvn compile quarkus:dev -f payment-service/pom.xml
```

```console
$ mvn compile quarkus:dev -f customer-service/pom.xml
```

## Misc. Commands

Listing all topics:

```console
$ docker-compose exec kafka /kafka/bin/kafka-topics.sh --zookeeper zookeeper:2181 --list
```