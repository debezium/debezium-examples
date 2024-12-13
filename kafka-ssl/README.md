# Debezium - Kafka with SSL example

This demo automatically deploys the topology of services as defined in the [Debezium Tutorial](https://debezium.io/documentation/reference/stable/tutorial.html) with Kafka SSL enabled.

## Configuration

The kafka instance will be deployed using the certificates at the './resources' folder:
```yaml
  kafka:
    image: quay.io/debezium/kafka:${DEBEZIUM_VERSION}
    ports:
     - "9092:9092"
    links:
     - zookeeper
    environment:
     - ZOOKEEPER_CONNECT=zookeeper:2181
     - KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=BROKER:PLAINTEXT,SSL:SSL,CONTROLLER:PLAINTEXT
     - KAFKA_LISTENERS=BROKER://0.0.0.0:9093,SSL://0.0.0.0:9092,CONTROLLER://0.0.0.0:9094
     - KAFKA_ADVERTISED_LISTENERS=SSL://localhost:9092,BROKER://localhost:9093
     - KAFKA_SSL_CLIENT_AUTH=required
     - KAFKA_SSL_KEYSTORE_LOCATION=/opt/config/ssl/kafka-ssl-keystore.p12
     - KAFKA_SSL_KEYSTORE_PASSWORD=top-secret
     - KAFKA_SSL_KEYSTORE_TYPE=PKCS12
     - KAFKA_SSL_TRUSTSTORE_LOCATION=/opt/config/ssl/kafka-ssl-truststore.p12
     - KAFKA_SSL_TRUSTSTORE_PASSWORD=top-secret
     - KAFKA_INTER_BROKER_LISTENER_NAME=BROKER
    volumes:
      - ./resources:/opt/config/ssl:z
```

The certificates will be mounted at the folder "/opt/config/ssl".

Then, when configuring the debezium connector, we need to also mount these certificates and properly configure the connector and SSL properties for the consumer and the producer.

Note that we're using the network mode as "host", because the certificates only validate the hostname "localhost", so the kafka and connector instances must be on the same network. We can also disable the hostname validation as an alternative.

## Steps
1. Start the topology (database, kafka, and debezium connector) as defined in https://debezium.io/documentation/reference/stable/tutorial.html:
```shell
export DEBEZIUM_VERSION=3.0
docker compose -f docker-compose.yaml up
```

2. Configure the debezium connector to use the postgres as source:

```shell
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-postgres.json
```

3. Modify records in the database via postgres client:

```shell
docker compose -f docker-compose.yaml exec postgres env PGOPTIONS="--search_path=inventory" bash -c 'psql -U postgres postgres'

postgres> INSERT INTO inventory.customers(first_name, last_name, email) values ('Bob', 'Marley', 'bob@marley.com');
```

4. Verify we see a new message from the Debezium topic:

```shell
docker compose -f docker-compose.yaml exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server localhost:9093 \
    --from-beginning \
    --property print.key=true \
    --topic dbserver1.inventory.customers
```

We should see the message:

```json
{
  "schema": {
    <omitted>
  },
  "payload": {
    "before": null,
    "after": {
      "id": 1005,
      "first_name": "Bob",
      "last_name": "Marley",
      "email": "bob@marley.com"
    },
    "source": {
      "version": "2.1.4.Final",
      "connector": "postgresql",
      "name": "dbserver1",
      "ts_ms": 1733906311010,
      "snapshot": "false",
      "db": "postgres",
      "sequence": "[\"34442256\",\"34466480\"]",
      "schema": "inventory",
      "table": "customers",
      "txId": 767,
      "lsn": 34466480,
      "xmin": null
    },
    "op": "c",
    "ts_ms": 1733906311190,
    "transaction": null
  }
}
```

# Shut down the cluster
docker compose -f docker-compose.yaml down
```
