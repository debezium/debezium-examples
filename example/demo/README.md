# Debezium UI Example

This demo automatically deploys the topology of services as defined in the [Debezium Tutorial](https://debezium.io/docs/tutorial/) along with Debezium UI.


## Prerequisites

Debezium UI is only available with Debezium version 1.5 and above.


## Launching UI

Launch all the required component

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

Registration of the Postgres, Mongo DB, MySQL & SQL server(coming soon) connectors can be done using [Debezium UI](http://localhost:8080). The first two steps of *Create connector* wizard in the Debezium UI are mandatory i.r `` Connector type `` & `` Properties step`` (where all the required basic connection properties are entered).  

### MySQL

Required values **Hostname**: dbzui-db-mysql, **User**: debezium, **Password**: dbz, **Kafka broker addresses**: kafka:9092, **Database history topic name**: dbhistory.inventory.


To connect with MySQL Cli clinet 
```shell
docker exec -it dbzui-db-mysql bash -c 'mysql -u $MYSQL_USER -p$MYSQL_PASSWORD inventory'

```

### Postgres

Required values **Hostname**: dbzui-db-pg, **User**: postgres, **Password**: postgres, **Database**:postgres.


To connect with Postgres Cli clinet 
```shell
docker exec -it dbzui-db-pg bash -c 'psql -U $POSTGRES_USER $POSTGRES_PASSWORD'

```

### Mongo

Required values **Hostname**: dbzui-db-mongo:27017, **User**: debezium, **Password**: dbz.


To connect with Mongo Cli clinet 
```shell
mongo -u debezium -p dbz --authenticationDatabase admin localhost:37017/inventory

```


## Examine the change events

```shell
# Open in a new terminal
# Viewing the change events in kafka dbzui-kafka containner 
docker exec -it dbzui-kafka bash

./bin/kafka-console-consumer.sh --bootstrap-server $BOOTSTRAP_SERVERS --topic [TOPIC_NAME] --from-beginning

```


