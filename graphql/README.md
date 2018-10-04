# Debezium - GraphQL Example

This demo shows how to build a GraphQL Subscription on top of Debezium Change Events.

The domain consists of `Order` objects that have among others a quantity field. These objects are stored
in a MySQL database. Debezium captures the changes in the database and publishes new orders to a Kafka Topic.

There are two applications:

1. The  _event-source_, that persists random orders in a MySQL database (simulates 'real' business)

2. The _aggregator_ consumes the messages from the Kafka topics and publishes new orders via a GraphQL API.
The _aggregator_ is a web app deployed to Thorntail.


## Preparations

Build data generator application and aggregator application:

```shell
mvn clean install -f event-source/pom.xml
mvn clean install -f aggregator/pom.xml
```

Start Kafka, Kafka Connect, MySQL, event source and aggregator:

```shell
export DEBEZIUM_VERSION=0.8
docker-compose up --build
```

Once you see the message "Waiting for source connector to be deployed" in the logs,
deploy the Debezium MySQL connector:

```shell
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @mysql-source.json
```

# Consume aggregated messages using GraphiQL

Once you see the message "WildFly Swarm is Ready" in the logs, open the UI in the browser: http://localhost:8079/graphiql.

It opens `GraphiQL`, a GraphQL API Browser. 

While writing your GraphQL queries in the editor, you can get code assist using `Ctrl+Space`.

![GraphiQL API Explorer](graphiql-screenshot.png)

## Example GraphQL Queries

Return the latest order that has been placed:
```
query { latestOrder { id quantity } }
```

Subscribe to _all_ new orders:
```
subscription {
  onNewOrder {
    id
    customerId
    quantity
  }
}
```

Subscribe to new orders that have a _quantity of at least 3_:
```
subscription {
  onNewOrder(withMinQuantity: 3) {
    id
    customerId
    quantity
  }
}
```


# Shut down the cluster

```shell
docker-compose down
```

# Locally testing the aggregator

1. Add `- ADVERTISED_HOST_NAME=<YOUR HOST IP>` to the `environment` section of the "kafka" service in _docker-compose.yaml_.

2. Run all services except the aggregator service:
```shell
docker-compose up --build connect event-source
```
(or run all services as described and then `docker-compose down aggregator`)

3. Run the aggregator from you IDE by running the class `org.wildfly.swarm.Swarm` from the `aggregator` project. 
Set the env variables `KAFKA_SERVICE_HOST` to <YOUR HOST IP> and `KAFKA_SERVICE_PORT` to `9092`

