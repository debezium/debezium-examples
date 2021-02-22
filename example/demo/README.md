# Debezium UI Example

This demo automatically deploys the topology of services as defined in the [Debezium Tutorial](https://debezium.io/docs/tutorial/) along with Debezium UI.


## Prerequisites

Debezium UI is only available with Debezium version 1.5 and above.


## Launching UI

Lanuch all the required component

```shell
# Start the required topology as defined in https://debezium.io/docs/tutorial/
export DEBEZIUM_VERSION=1.5
docker-compose up -d

Creating dbzui-db-mysql  ... done
Creating dbzui-db-pg     ... done
Creating dbzui-zookeeper ... done
Creating dbzui-db-mongo  ... done
Creating debezium-ui_mongo-initializer_1 ... done
Creating dbzui-kafka     ... done
Creating dbzui-connect   ... done

```


Debezium UI will be available on http://localhost:8080

## Register connectors

Registration of the Postgres, Mongo DB, MySQL & SQL server(coming soon) connectors can be done using [Debezium UI](http://localhost:8080). The first two steps are mandatory in registering connector wizard `` Connector type `` & `` Properties step`` (where all the basic required connection properties are entered).  

### MySQL

Required values 
``{
  "database.server.name": "fullfillment",
  "database.server.id": 5808,
  "database.hostname": "dbzui-db-mysql",
  "database.user": "debezium",
  "database.password": "***",
  "database.history.kafka.bootstrap.servers": "kafka:9092",
  "database.history.kafka.topic": "dbhistory.inventory"
}``

To connect with MySQL Cli clinet 
```shell
docker exec -it dbzui-db-mysql bash -c 'mysql -u debezium -pdbz inventory'
```


## Examine the change events

```shell
# Open in new terminal
# Viewing the change events in kafka dbzui-kafka containner 
docker exec -it dbzui-kafka bash

./bin/kafka-console-consumer.sh --bootstrap-server $BOOTSTRAP_SERVERS --topic [TOPIC_NAME] --from-beginning

```


