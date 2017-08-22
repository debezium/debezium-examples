# Debezium Tutorial
This demo automatically deploys the topology of services as defined in [Debezium Tutorial](http://debezium.io/docs/tutorial/) document.

How to run:

```shell
# Start the topology as defined in http://debezium.io/docs/tutorial/
env DEBEZIUM_VERSION=0.5 docker-compose up

# Start MySQL connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @register.json

# Consume messages from a Debezium topic
docker exec -it tutorial_kafka_1 /kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --from-beginning --topic dbserver1.inventory.customers

# Shut down the cluster
docker-compose down
```
