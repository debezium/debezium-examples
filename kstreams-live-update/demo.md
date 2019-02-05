# Prepare

mvn clean install -f event-source/pom.xml
mvn clean install -f aggregator/pom.xml
docker-compose up --build

# Console

cat mysql-source.json | http POST http://localhost:8083/connectors/
cat es-sink.json | http POST http://localhost:8083/connectors/

docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
	--property print.key=true \
	--topic dbserver1.inventory.orders

docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
	--from-beginning \
	--property print.key=true \
	--topic dbserver1.inventory.categories

docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
	--from-beginning \
	--property print.key=true \
	--topic sales_per_category

docker run --tty \
  --network kstreams-live-update_default \
  confluentinc/cp-kafkacat \
  kafkacat -b kafka:9092 -C -o end \
  -t dbserver1.inventory.orders | jq .

# Misc.

docker-compose exec kafka /kafka/bin/kafka-topics.sh --zookeeper zookeeper:2181 --list

# Browser

http://localhost:8079
