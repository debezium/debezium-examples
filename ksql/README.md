# Debezium KSQL Demo

This demo accompanies the blog post [Querying Debezium Change Data Events With KSQL](https://debezium.io/blog/2018/05/24/querying-debezium-change-data-eEvents-with-ksql/).

```shell
# Start the Kafka, Kafka Connect, KSQL server and CLI etc.
export DEBEZIUM_VERSION=1.7
docker-compose up

# Start Debezium MySQL connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-mysql.json

# Launch KSQL CLI
docker-compose exec ksql-cli ksql http://ksql-server:8088

# Run KSQL commands as described in the blog post...

# Shut down the cluster
docker-compose down
```
