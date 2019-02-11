# Debezium KStreams - WebSockets Example

This demo shows how to use KStreams to join two CDC event streams created by Debezium,
do some calculation on the joined stream and push the merged events to a client using WebSockets.

The domain is that of orders that belong to given categories.
There's an application _event-source_, which persists random orders.

The application has two tables:

* `categories`: Product categories
* `orders`: orders with a given category and a (random) sales prices

Debezium is used to capture changes to the two tables in the application's underlying MySQL database.

The _aggregator_ application runs KStreams to join orders with categories,
group the events by category name and accumulate the sales price per category in time windows of 5 seconds.

The aggregated values are pushed to WebSockets.
For that purpose, the aggregator application exposes a WebSockets endpoint using Thorntail.

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

Once you see the message "Waiting for topics to be created" in the logs,
deploy the Debezium MySQL connector:

```shell
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @mysql-source.json
```

## Consume aggregated messages

If the connector has been deployed, open the UI in the browser: http://localhost:8079/.
It shows a chart with the windowed accumulated order values which is updated in near-realtime as new orders are created via the event generator app.

Alternatively, browse the Kafka topic:

```shell
docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic sales_per_category
```

## Shut down the cluster

```shell
docker-compose down
```

## Locally testing the aggregator

Add `- ADVERTISED_HOST_NAME=<YOUR HOST IP>` to the `environment` section of the "kafka" service in _docker-compose.yaml_.

Run `io.debezium.examples.kstreams.liveupdate.aggregator.Main`, passing `<YOUR HOST IP>:9092` as program argument.

## Demo instructions

See _demo.md_ and _demo-os.md_ for steps to demo this during conference talks (running via Docker Compose and OpenShift, respectively).
