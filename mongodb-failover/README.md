# Mongodb Failover Demo

This is a showcase for Mongodb deployment in replica set configuration and demonstration of Debezium execution after a database failure.

## Background

Mongodb needs minimum 3 nodes running in replica set mode for replication. Writes happen in primary node, read can happen from anywhere. We can pass all 3 nodes hostname and port in connection string and mongodb takes care of contacting primary for write.

## Topology

The deployment consists of the following components

* Database
  * MongoDB 1 instance (hostname configured as mongodb1. This is our primary replica when we're starting our demo)
  * MongoDB 2 instance (hostname configured as mongodb2)
  * MongoDB 3 instance (hostname configured as mongodb3)
* Streaming system
  * Apache Kafka broker
  * Apache Kafka Connect with Debezium MongoDB Connector

## Demonstration

Start the components and register Debezium to stream changes from the database
```
docker-compose --env-file ../.env -f docker-compose-mongodb-replicas.yaml up

#start mongodb connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-mongodb-connector.json

# Consume messages from a Debezium topic
docker-compose -f docker-compose-mongodb-replicas.yaml exec kafka /kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --from-beginning --property print.key=true --topic dbserver1.inventory.customers
```

Right now mongodb1 is primary replica. We can create a record and check in kafka console consumer that it's getting streamed.
```
# Connect to any mongodb, create one record
docker-compose -f docker-compose-mongodb-replicas.yaml exec mongodb3 mongosh "mongodb://mongodb1:27017,mongodb2:27017,mongodb3:27017/inventory?replicaSet=rs0&authSource=admin" -u $MONGODB_USER -p $MONGODB_PASSWORD --authenticationDatabase admin

db.customers.insertOne([{ _id : NumberLong("1005"), first_name : 'Peter', last_name : 'Parker', email : 'peter@example.com', unique_id : UUID() }]);
```

Stop the primary server
```
docker-compose -f docker-compose-mongodb-replicas.yaml stop mongodb1
```

As primary went down, mongodb will make one of the remaining replicas primary. Create one more record in the database and check in kafka console consumer if it's getting streamed.
```
# Connect to any mongodb, create one record
docker-compose -f docker-compose-mongodb-replicas.yaml exec mongodb3 mongosh "mongodb://mongodb1:27017,mongodb2:27017,mongodb3:27017/inventory?replicaSet=rs0&authSource=admin" -u $MONGODB_USER -p $MONGODB_PASSWORD --authenticationDatabase admin

db.customers.insertOne([{ _id : NumberLong("1006"), first_name : 'Clark', last_name : 'Kent', email : 'clark@example.com', unique_id : UUID() }]);
```

Stop the demo
```
docker-compose --env-file ../.env -f docker-compose-mongodb-replicas.yaml down
``` 