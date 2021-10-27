# Saga Pattern

This example demonstrates how to implement the [Saga pattern](https://microservices.io/patterns/data/saga.html) for realizing distributed transactions across multiple microservices, in a safe and reliable way using change data capture.

Based on the [outbox pattern](https://debezium.io/blog/2019/02/19/reliable-microservices-data-exchange-with-the-outbox-pattern/),
this implementation avoids unsafe dual writes to a service's database and Apache Kafka by channeling all outgoing messages through the originating service's database and capturing them from there using CDC and Debezium.

There are three services involved:

* _order-service:_ originator and orchestrator of the Saga
* _customer-service:_ validates whether an incoming order is within the customer's credit limit and approves or rejects it accordingly
* _payment-service_ executes the payment associated to an incoming order

![Solution Overview](solution-overview.png)

## Running the Example

Build and start up:

```console
$ mvn clean verify
```

```console
$ export DEBEZIUM_VERSION=1.7
$ docker-compose up --build
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

HTTP/1.1 200 OK
Content-Length: 32
Content-Type: application/json

{
    "orderId": 1,
    "status": "CREATED"
}
```

Examine the emitted messages for `credit-approval` and `payment` in Apache Kafka:

```console
$ docker run --tty --rm \
    --network saga-network \
    debezium/tooling:1.1 \
    kafkacat -b kafka:9092 -C -o beginning -q \
    -f "{\"key\":%k, \"headers\":\"%h\"}\n%s\n" \
    -t credit-approval.request

{"key":17ef865e-39bf-404d-8d35-25c61ae0e082, "headers":"id=6ab3c538-5899-4a61-aa22-ebf5dee45b9d"}
{"order-id":1,"customer-id":456,"payment-due":59,"credit-card-no":"xxxx-yyyy-dddd-aaaa","type":"REQUEST"}
```

```console
$ docker run --tty --rm \
    --network saga-network \
    debezium/tooling:1.1 \
    kafkacat -b kafka:9092 -C -o beginning -q \
    -f "{\"key\":%k, \"headers\":\"%h\"}\n%s\n" \
    -t payment.request

{"key":17ef865e-39bf-404d-8d35-25c61ae0e082, "headers":"id=e88e463f-047d-49a9-be08-988a1552c571"}
{"order-id":1,"customer-id":456,"payment-due":59,"credit-card-no":"xxxx-yyyy-dddd-aaaa","type":"REQUEST"}
```

Examine the saga state in the order service's database:

```console
$ docker run --tty --rm -i \
        --network saga-network \
        debezium/tooling:1.1 \
        bash -c 'pgcli postgresql://orderuser:orderpw@order-db:5432/orderdb'

select * from purchaseorder.sagastate;

+--------------------------------------+------------------------------------------------------------------------------------------+----------+---------------------------------------------------+-----------------+-----------+
| id                                   | payload                                                                                  | status   | stepstate                            | type            | version   |
|--------------------------------------+------------------------------------------------------------------------------------------+----------+---------------------------------------------------+-----------------+-----------|
| 17ef865e-39bf-404d-8d35-25c61ae0e082 | {"order-id":1,"customer-id":456,"payment-due":59,"credit-card-no":"xxxx-yyyy-dddd-aaaa","type":"REQUEST"} | COMPLETED | {"credit-approval":"SUCCEEDED","payment":"SUCCEEDED"} | order-placement | 2         |
+--------------------------------------+------------------------------------------------------------------------------------------+----------+---------------------------------------------------+-----------------+-----------+
```

Alternatively, you also can access pgAdmin on http://localhost:5050 (pgadmin4@pgadmin.org/admin).

Place an order with an invalid credit card number (the payment service rejects any number that ends with "9999"):

```console
$ http POST http://localhost:8080/orders < requests/place-invalid-order1.json
```

Observe how the saga's state is `ABORTED`, with the `payment` step in state `FAILED`, and the `credit-approval` first in state `COMPENSATING`, then `COMPENSATED`.

Now place an order which exceeds the credit limit (customer 456 has an initial credit limit of $500.00, and this order exceeds this; alternatively, you can place the valid order request a number of times, until the accumlated value exceeds the limit):

```console
$ http POST http://localhost:8080/orders < requests/place-invalid-order2.json
```

Observe how the saga's state again is `ABORTED`, with the step states set accordingly.

Now stop the payment service and place a valid order again:

```console
$ docker-compose stop payment-service

$ http POST http://localhost:8080/orders < requests/place-order.json
```

Observe how the saga remains in state `STARTED`, with the `credit-approval` step in state `SUCCEEDED` and the `payment` step in state `STARTED`.
Start the payment service again (`docker-compose start payment-service`) and observe how the saga completes.

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

Register connector for logging the saga state:

```console
$ http PUT http://localhost:8083/connectors/order-sagastate-connector/config < register-sagastate-connector.json
```

Examining saga state log:

```console
$ docker run --tty --rm \
    --network saga-network \
    debezium/tooling:1.1 \
    kafkacat -b kafka:9092 -C -o beginning -q \
    -f "{\"key\":%k, \"headers\":\"%h\"}\n%s\n" \
    -t dbserver4.purchaseorder.sagastate
```

Examining saga execution via Jaeger tracing:

```console
$ open http://localhost:16686/
```
