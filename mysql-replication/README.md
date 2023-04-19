# MySQL Replication Demo
This is a showcase for MySQL deployment in primary-replica configuration and demonstration of Debezium switch to replica and back.
## Topology
The deployment consists of the following components
* Database
  * MySQL master instance
  * MySQL replica instance (configured as a slave to MySQL master)

  * Apache ZooKeeper
  * Apache Kafka broker
  * Apache Kafka Connect with Debezium MySQL Connector - the connector will connect to HAProxy
## Demonstration
1. Start the components and register Debezium to stream changes from the database
```
export DEBEZIUM_VERSION=2.1
docker-compose up --build
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-mysql.json
```

2. Create a change and verify that Debezium is streaming changes

```
# Connect to MySQL master
docker exec -it mysql-master bash -c 'mysql -u root -p$MYSQL_ROOT_PASSWORD inventory'
  INSERT INTO customers VALUES (default, 'John','Doe','john.doe@example.com');
docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --from-beginning --property print.key=true --topic dbserver1.inventory.customers
```
3. Stop the master server
```
docker-compose stop mysql_master
```

4. Update the connector to read from replica instance
```
curl -i -X PUT -H "Accept:application/json" -H "Content-Type:application/json" http://localhost:8083/connectors/inventory-connector/config -d @connector-config-replica.json
```

5. Restart the master server
```
docker-compose up mysql_master
```
6. Verify records inserted to master stream to debezium

```
docker exec -it mysql-master bash -c 'mysql -u root -p$MYSQL_ROOT_PASSWORD inventory'
  INSERT INTO customers VALUES (default, 'Jane','Doe','jane.doe@example.com');
docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --from-beginning --property print.key=true --topic dbserver1.inventory.customers

```
7. Switch connector back to master
```
curl -i -X PUT -H "Accept:application/json" -H "Content-Type:application/json" http://localhost:8083/connectors/inventory-connector/config -d @connector-config-master.json
```
8. Stop the demo
```
docker-compose down
```

### Notes:
If the master is stopped in step 3 before the replication is done, 
it will replicate after master restart resulting in duplicate entries in the kafka topic.
