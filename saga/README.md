# Saga Pattern

This example demonstrates how to implement the [Saga pattern](https://microservices.io/patterns/data/saga.html) for realizing distributed transactions across multiple microservices, in a safe and reliable way using change data capture.

Similarly to the outbox pattern, this implementation avoids unsafe dual writes to a service's database and Apache Kafka by channeling all outgoing messages through the originating service's database and capturing them from there using CDC and Debezium.

There are three services involved:

* _order-service:_ originator and orchestrator of the Saga
* _customer-service:_ approves or rejects the customer's credit needed to fulfill an incoming order (tbd.)
* _payment-service_ executes the payment associated to an incoming order (tbd.)

## Running the Example

Build and start up:

```console
$ mvn compile quarkus:dev -f order-service/pom.xml
```

```console
$ docker-compose up
```

Register the connectors for the different services:

```console
$ http PUT http://localhost:8083/connectors/order-outbox-connector/config < register-order-connector.json
$ http PUT http://localhost:8083/connectors/payment-outbox-connector/config < register-payment-connector.json
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

+--------------------------------------+------------------------------------------------------------------------------------------+----------+-----------------------
----------------------------+-----------------+-----------+
| id                                   | payload                                                                                  | status   | stepstate
                            | type            | version   |
|--------------------------------------+------------------------------------------------------------------------------------------+----------+-----------------------
----------------------------+-----------------+-----------|
| d08b4e43-8522-4539-9aba-de4bb0dc1a8e | {"payment-due":59,"customer-id":456,"order-id":2,"credit-card-no":"xxxx-yyyy-dddd-aaaa"} | STARTED  | {"credit-approval":"ST
ARTED","payment":"STARTED"} | order-placement | 1         |
+--------------------------------------+------------------------------------------------------------------------------------------+----------+-----------------------
----------------------------+-----------------+-----------+
```

Emulate a response by the _customer-service_:

```console
$ http POST http://localhost:8080/orders/credit-approval status=SUCCEEDED saga-id:<obtained from database> message-id:d2b19439-b9aa-49f7-b6fe-e9d4c1d33e4
```

Observe how the Saga state gets updated accordingly. Do the same for the payment:

```console
$ http POST http://localhost:8080/orders/payment status=SUCCEEDED saga-id:<obtained from database> message-id:d2b19439-b9aa-49f7-b6fe-e9d4c1d33e5
```

Observe how the Saga is in state `COMPLETED` and the purchase order changed its state from `CREATED` to `PROCESSING`.

Create another order and emulate a failed response (status=FAILED) for the payment service.
This will trigger another outgoing message to abort the credit approval process.
Emulate a crediat approval response (status=ABORTED) and observe how the Saga is in state `ABORTED` and the purchase order in state `CANCELLED`.

Eventually,
actual service implementations will be provided for the customer and payment services,
which respond back via Kafka instead of HTTP.

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