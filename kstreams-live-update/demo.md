# Prepare

mvn clean install -f event-source/pom.xml
mvn clean install -f aggregator/pom.xml
docker-compose up --build

# Console

* Observe how order entries are produced:

mycli mysql://mysqluser@localhost:3306/inventory --password mysqlpw

* Register MySQL connector:

cat mysql-source.json | http POST http://localhost:8083/connectors/

* Browse topics

docker run --tty \
  --network kstreams-live-update_default \
  debezium/tooling \
  kafkacat -b kafka:9092 -C -o end \
  -t dbserver1.inventory.orders | jq .payload

docker run --tty \
  --network kstreams-live-update_default \
  debezium/tooling \
  kafkacat -b kafka:9092 -C -o beginning \
  -t dbserver1.inventory.categories | jq .payload

docker run --tty \
  --network kstreams-live-update_default \
  debezium/tooling \
  kafkacat -b kafka:9092 -C -o beginning \
  -t sales_per_category -K " --- "

* Updated chart in browser: http://localhost:8079

# Bonus: Elasticsearch

cat es-sink.json | http POST http://localhost:8083/connectors/

http "http://localhost:9200/orders/_search?pretty"

# Misc.

docker-compose exec kafka /kafka/bin/kafka-topics.sh --zookeeper zookeeper:2181 --list
