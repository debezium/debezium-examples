# Capturing Changes from Multiple SQL Server Databases

This example demonstrates how to use a single Debezium SQL Server connector to capture changes from multiple logical databases on the same SQL Server instance using the `database.names` configuration property.

This capability was introduced via [DBZ-4783](https://issues.redhat.com/browse/DBZ-4783).

```
                  SQL Server
               +--------------+
               |              |
               |   testDB1    |
               |  (products)  |
               |              |      +------------------+      +---------+
               |   testDB2    +------> Kafka Connect    +------> Kafka   |
               |  (customers, |      |                  |      |         |
               |   orders)    |      |  +-----------+   |      | Topics: |
               |              |      |  | Debezium  |   |      | server1.|
               +--------------+      |  +-----------+   |      | testDB1.|
                                     +------------------+      | testDB2.|
                                                               +---------+
```

A single connector captures CDC events from both `testDB1` and `testDB2` and publishes them to database-specific Kafka topics.

## Using SQL Server Multi-Database CDC

```shell
# Start the topology as defined in docker-compose.yaml
export DEBEZIUM_VERSION=3.2
docker-compose up -d

# Initialize both databases with CDC enabled
cat debezium-sqlserver-init/inventory.sql | docker-compose exec -T sqlserver bash -c '/opt/mssql-tools/bin/sqlcmd -U sa -P $SA_PASSWORD'

# Start the Debezium SQL Server connector (database.names = testDB1,testDB2)
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-sqlserver.json

# Consume messages from testDB1
docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic server1.testDB1.dbo.products

# Consume messages from testDB2
docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic server1.testDB2.dbo.customers

# Modify records in both databases via SQL Server client (do not forget to add GO command to execute the statement)
docker-compose exec sqlserver bash -c '/opt/mssql-tools/bin/sqlcmd -U sa -P $SA_PASSWORD -d testDB1'

# Shut down the cluster
docker-compose down
```

The init SQL script creates:
- **testDB1** with a `products` table (6 rows)
- **testDB2** with `customers` (4 rows) and `orders` (4 rows) tables

All tables have CDC enabled via `sp_cdc_enable_table`.

The key connector configuration is `"database.names": "testDB1,testDB2"` which tells the connector to capture from both databases. Topics follow the naming pattern `<topic.prefix>.<database>.<schema>.<table>`.
