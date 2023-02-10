# SQL Server Always On Cluster

This demo automatically deploys the topology of services to stream from SQL Server read-only replica as described in the diagram.

```
   +-------------+
   |             |
   | Application |
   |             |
   +------+------+
          |              SQL Server Cluster
          |
+-----------------------------------------+
|         |                               |
|  +------v-------+   +----------------+  |
|  |              |   |                |  |
|  | Primary (RW) |   | Secondary (RO) |  |
|  |              |   |                |  |
|  +--------------+   +--------+-------+  |
|                              |          |
+-----------------------------------------+
                               |
                      +--------v----------+
                      |                   |
                      | Kafka Connect     |      +-------+
                      |                   |      |       |
                      |     +----------+  +------> Kafka |
                      |     | Debezium |  |      |       |
                      |     +----------+  |      +-------+
                      |                   |
                      +-------------------+
```

## Using SQL Server


```shell
# Start the topology with two SQL Servers
export DEBEZIUM_VERSION=2.0
docker-compose up

# Initialize database and insert test data
cat debezium-sqlserver-init/inventory.sql | docker exec -i sql-server-read-replica_primary_1 bash -c '/opt/mssql-tools/bin/sqlcmd -U sa -P $SA_PASSWORD'

# Configure primary node and database for replication
cat debezium-sqlserver-init/setup-primary.sql | docker exec -i sql-server-read-replica_primary_1 bash -c '/opt/mssql-tools/bin/sqlcmd -U sa -P $SA_PASSWORD'

# Configure secondary node to join the cluster
cat debezium-sqlserver-init/setup-secondary.sql | docker exec -i sql-server-read-replica_secondary_1 bash -c '/opt/mssql-tools/bin/sqlcmd -U sa -P $SA_PASSWORD'

# Check that tables are replicated to the secondary node (read-only connection)
docker-compose exec secondary bash -c '/opt/mssql-tools/bin/sqlcmd -U sa -P $SA_PASSWORD -K readonly -d testDB'
1> SELECT COUNT(*) FROM customers;
2> GO
           
-----------
          4

(1 rows affected)


# Start SQL Server connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-sqlserver.json

# Consume messages from a Debezium topic
docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic server1.testDB.dbo.customers

# Modify records in the primary database via SQL Server client (do not forget to add `GO` command to execute the statement)
docker-compose exec primary bash -c '/opt/mssql-tools/bin/sqlcmd -U sa -P $SA_PASSWORD -d testDB'

# Shut down the cluster
docker-compose down
```




