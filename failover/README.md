# MySQL Failover Demo

This is a showcase for MySQL deployment in clustered configuration and demonstration of Debezium execution after a database failure.

## Topology

The deployment consists of the following components

* Database
  * MySQL 1 instance (configured as a slave to MySQL 2) with GTID enabled
  * MySQL 2 instance (configured as a slave to MySQL 1) with GTID enabled
  * HAProxy instance - MySQL 1 is configured as the primary server, MySQL 2 as a backup
* Streaming system
  * Apache ZooKeeper
  * Apache Kafka broker
  * Apache Kafka Connect with Debezium MySQL Connector - the connector will connect to HAProxy

## Demonstration

Start the components and register Debezium to stream changes from the database
```
export DEBEZIUM_VERSION=1.8
docker-compose up --build
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-mysql.json
```

Please notice that `gtid.new.channel.position` is set to `earliest`.
This ensures that Debezium will receive all events that were created on the backup server during failover.
The other value is `latest`.
In this case, Debezium will receive only events that were created after the failover.

Create a couple of changes and verify that the GTID is enabled and the transaction ids are associated with change messages.
Every record (not those created in the snapshot) will have a `gtid` field in the `source` part of change message.
The `UUID` part should be the same as the `UUID` of the primary server.
```
# Connect to MySQL 1, check server UUID and create two records
docker-compose exec mysql1 bash -c 'mysql -u root -p$MYSQL_ROOT_PASSWORD inventory'
  SHOW GLOBAL VARIABLES LIKE 'server_uuid';
  INSERT INTO customers VALUES (default, 'John','Doe','john.doe@example.com');
  INSERT INTO customers VALUES (default, 'Jane','Doe','jane.doe@example.com');

# Check UUID in change message, the 'source' will contain field "gtid":"50303655-f22a-11e8-92a5-0242ac1d0003:2"
docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --from-beginning --property print.key=true --topic dbserver1.inventory.customers
```

Stop the primary server
```
docker-compose stop mysql1
```

Debezium will fail with an error message after a while as we do not support automatic recovery of lost connection.
```
connect_1    | org.apache.kafka.connect.errors.ConnectException: Client requested master to start replication from position > file size Error code: 1236; SQLSTATE: HY000.
connect_1    | 	at io.debezium.connector.mysql.AbstractReader.wrap(AbstractReader.java:208)
connect_1    | 	at io.debezium.connector.mysql.AbstractReader.failed(AbstractReader.java:175)
connect_1    | 	at io.debezium.connector.mysql.BinlogReader$ReaderThreadLifecycleListener.onCommunicationFailure(BinlogReader.java:962)
connect_1    | 	at com.github.shyiko.mysql.binlog.BinaryLogClient.listenForEventPackets(BinaryLogClient.java:921)
connect_1    | 	at com.github.shyiko.mysql.binlog.BinaryLogClient.connect(BinaryLogClient.java:559)
connect_1    | 	at com.github.shyiko.mysql.binlog.BinaryLogClient$7.run(BinaryLogClient.java:793)
connect_1    | 	at java.lang.Thread.run(Thread.java:748)
connect_1    | Caused by: com.github.shyiko.mysql.binlog.network.ServerException: Client requested master to start replication from position > file size
connect_1    | 	at com.github.shyiko.mysql.binlog.BinaryLogClient.listenForEventPackets(BinaryLogClient.java:882)
connect_1    | 	... 3 more
```

Create two more records in the backup database while Debezium is down and check the `UUID` of the backup server.
```
# Connect to MySQL 2, check server UUID and create two records
docker-compose exec mysql2 bash -c 'mysql -u root -p$MYSQL_ROOT_PASSWORD inventory'
  SHOW GLOBAL VARIABLES LIKE 'server_uuid';
  INSERT INTO customers VALUES (default, 'Peter','Doe','peter.doe@example.com');
  INSERT INTO customers VALUES (default, 'Paul','Doe','paul.doe@example.com');
```

Restart Debezium
```
curl -iv -X POST http://localhost:8083/connectors/inventory-connector/tasks/0/restart
```

Create two more records in the backup database
```
# Connect to MySQL 2 and create two records
docker-compose exec mysql2 bash -c 'mysql -u root -p$MYSQL_ROOT_PASSWORD inventory'
  INSERT INTO customers VALUES (default, 'Mark','Doe','mark.doe@example.com');
  INSERT INTO customers VALUES (default, 'Matthew','Doe','matthew.doe@example.com');
```

Verify that all four records were created and the `gtid` field is set to the `UUID` of the backup server
```
docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --from-beginning --property print.key=true --topic dbserver1.inventory.customers
```

As the last step, check that connector offsets contains GTIDs from both primary and backup server.
In the example below, we see that offsets were stored first before the database crash and contains only the UUID of the primary server.
After failover the transactions (GTIDs) from both primary and backup server are recorded in offsets.
```
docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --from-beginning --property print.key=true --topic my_connect_offsets
["inventory-connector",{"server":"dbserver1"}]	{"file":"mysql-bin.000002","pos":154}
["inventory-connector",{"server":"dbserver1"}]	{"ts_sec":1543312728,"file":"mysql-bin.000002","pos":530,"gtids":"50303655-f22a-11e8-92a5-0242ac1d0003:1-1","row":1,"server_id":1201,"event":2}
["inventory-connector",{"server":"dbserver1"}]	{"ts_sec":1543313772,"file":"mysql-bin.000002","pos":1122,"gtids":"50303655-f22a-11e8-92a5-0242ac1d0003:1-2,50d271cb-f22a-11e8-8654-0242ac1d0005:1-1","row":1,"server_id":1202,"event":2}
["inventory-connector",{"server":"dbserver1"}]	{"ts_sec":1543313969,"file":"mysql-bin.000002","pos":1744,"gtids":"50303655-f22a-11e8-92a5-0242ac1d0003:1-2,50d271cb-f22a-11e8-8654-0242ac1d0005:1-3","row":1,"server_id":1202,"event":2}
```

Stop the demo
```
docker-compose down
```
