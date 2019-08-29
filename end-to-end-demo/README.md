# End-to-end demo of Debezium used during presentations

It does these things:

* setting up Debezium against running mysql
* displaying events using console consumer
* setting up JDBC sink connector to stream the events to Postgres
* demonstrating how a downtime of Kafka Connect doesn't affect the source app
* demonstrating how to consume events using WildFly Swarm + CDI and stream them to a WebSockets client in another browser window

---

**IMPORTANT:** Use Maven 3.5.x and latest version of docker to build and execute demo applications.

---

## Setup Environment

- export DEBEZIUM_VERSION=0.9

## Build maven artifacts used by the Docker builds

- mvn clean package -f debezium-hiking-demo/pom.xml
- mvn clean package -f debezium-swarm-demo/pom.xml

## Prepare Kafka etc.

- docker ps -a
- docker-compose up --scale swarm=0 --scale hiking-manager=0

## Prepare WildFly

- docker-compose up --build -d hiking-manager
- Go to http://localhost:8080/hikr-1.0-SNAPSHOT/hikes.html

## Register source connector (Avro)

- cat register-hiking-connector.json | http POST http://localhost:8083/connectors/
- http localhost:8083/connectors/hiking-connector/status
- docker-compose exec schema-registry /usr/bin/kafka-avro-console-consumer --bootstrap-server kafka:9092 --from-beginning --property print.key=true --property schema.registry.url=http://schema-registry:8081 --topic dbserver1_inventory_Hike
- http http://localhost:8081/subjects/dbserver1_inventory_Hike-value/versions/1 | jq '.schema | fromjson'

## Register sink connector

- Back to slides - mention SMT

- cat jdbc-sink.json | http POST http://localhost:8083/connectors/
- http localhost:8083/connectors/
- docker-compose exec postgres bash -c 'psql -U $POSTGRES_USER $POSTGRES_DB -c "select * from \\"dbserver1_inventory_Hike\\""'

## Stop Kafka Connect

- docker stop end-to-end-demo_connect_1
- Change some data
- docker-compose up -d connect
- Show PG again as it catches up

## Register source connector (JSON)

- cat register-hiking-connector-json.json | http POST http://localhost:8084/connectors/
- docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --from-beginning --property print.key=true --topic dbserver1_inventory_Hike_json

## Start WildFly Swarm app

- docker-compose up -d --build
- Open in other browser: http://localhost:8079/

## Misc.

- docker-compose exec kafka /kafka/bin/kafka-topics.sh --list --zookeeper zookeeper:2181
- docker-compose exec mysql bash -c 'mysql -u $MYSQL_USER -p$MYSQL_PASSWORD inventory'
