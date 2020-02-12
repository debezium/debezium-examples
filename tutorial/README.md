# Debezium Tutorial

This demo automatically deploys the topology of services as defined in the [Debezium Tutorial](http://debezium.io/docs/tutorial/).

- [Debezium Tutorial](#debezium-tutorial)
  * [Using MySQL](#using-mysql)
    + [Using MySQL and the Avro message format](#using-mysql-and-the-avro-message-format)
      - [Kafka Connect Worker configuration](#kafka-connect-worker-configuration)
      - [Debezium Connector configuration](#debezium-connector-configuration)
    + [Using MySQL and Apicurio Registry](#using-mysql-and-apicurio-registry)
      - [JSON format](#json-format)
      - [Avro format using Confluent Avro converter](#avro-format-using-confluent-avro-converter)
      - [Avro format using Apicurio Avro converter](#avro-format-using-apicurio-avro-converter)
  * [Using Postgres](#using-postgres)
  * [Using MongoDB](#using-mongodb)
  * [Using Oracle](#using-oracle)
  * [Using SQL Server](#using-sql-server)
  * [Using Db2](#using-db2)
  * [Using externalized secrets](#using-externalized-secrets)
  * [Debugging](#debugging)

## Using MySQL

```shell
# Start the topology as defined in http://debezium.io/docs/tutorial/
export DEBEZIUM_VERSION=1.0
docker-compose -f docker-compose-mysql.yaml up

# Start MySQL connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-mysql.json

# Consume messages from a Debezium topic
docker-compose -f docker-compose-mysql.yaml exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic dbserver1.inventory.customers

# Modify records in the database via MySQL client
docker-compose -f docker-compose-mysql.yaml exec mysql bash -c 'mysql -u $MYSQL_USER -p$MYSQL_PASSWORD inventory'

# Shut down the cluster
docker-compose -f docker-compose-mysql.yaml down
```

### Using MySQL and the Avro message format

To use [Avro-style messages](http://debezium.io/docs/configuration/avro/) instead of JSON,
Avro can be configured one of two ways, 
in the Kafka Connect worker configuration or in the connector configuration.
Using Avro in conjunction with the service registry allows for much more compact messages.

#### Kafka Connect Worker configuration

Configuring Avro at the Kafka Connect worker involves using the same steps above for MySQL but instead using the _docker-compose-mysql-avro-worker.yaml_ configuration file instead.
The Compose file configures the Connect service to use the Avro (de-)serializers for the Connect instance and starts one more additional service, the Confluent schema registry.

#### Debezium Connector configuration

Configuring Avro at the Debezium Connector involves specifying the converter and schema registry as a part of the connectors configuration.
To do this, follow the same steps above for MySQL but instead using the _docker-compose-mysql-avro-connector.yaml_ and _register-mysql-avro.json_ configuration files.
The Compose file configures the Connect service to use the default (de-)serializers for the Connect instance and starts one additional service, the Confluent schema registry.
The connector configuration file configures the connector but explicitly sets the (de-)serializers for the connector to use Avro and specifies the location of the schema registry.

You can access the first version of the schema for `customers` values like so:

```shell
curl -X GET http://localhost:8081/subjects/dbserver1.inventory.customers-value/versions/1
```

Or, if you have the `jq` utility installed, you can get a formatted output like this:

```shell
curl -X GET http://localhost:8081/subjects/dbserver1.inventory.customers-value/versions/1 | jq '.schema | fromjson'
```

If you alter the structure of the `customers` table in the database and trigger another change event,
a new version of that schema will be available in the registry.

The service registry also comes with a console consumer that can read the Avro messages:

```shell
docker-compose -f docker-compose-mysql-avro.yaml exec schema-registry /usr/bin/kafka-avro-console-consumer \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --property schema.registry.url=http://schema-registry:8081 \
    --topic dbserver1.inventory.customers
```

### Using MySQL and Apicurio Registry
[Apicurio Registry](https://github.com/Apicurio/apicurio-registry) is an API/schema registry that could be used to store schemas of Kafka messages.
The registry itself is an open source project that brings further additional features
- The registry provides its own native API and an API compatible with Confluent's Schema Registry. This allows using of already existing tools like Confluent's Avro converter for easier migration.
- The registry provides its own native Avro converter and Protobuf serializer.
- The registry provides a JSON converter that exports its schema into the registry.


#### JSON format
Configuring JSON converter with externalized schema at the Debezium Connector involves specifying the converter and schema registry as a part of the connectors configuration.
To do this, follow the same steps above for MySQL but instead using the _docker-compose-mysql-apicurio.yaml_ and _register-mysql-apicurio-converter-json.json_ configuration files.
The Compose file configures the Connect service to use the default (de-)serializers for the Connect instance and starts one additional service, the Apicurio Registry.
The connector configuration file configures the connector but explicitly sets the (de-)serializers for the connector to use Avro and specifies the location of the Apicurio registry.

You can access the first version of the schema for `customers` values like so:

```shell
curl -X GET http://localhost:8080/artifacts/dbserver1.inventory.customers-value
```

Or, if you have the `jq` utility installed, you can get a formatted output like this:

```shell
curl -X GET http://localhost:8080/artifacts/dbserver1.inventory.customers-value | jq .
```

If you alter the structure of the `customers` table in the database and trigger another change event,
a new version of that schema will be available in the registry.

You can consume the JSON messages in the same way as with standard JSON converter

```shell
docker-compose -f docker-compose-mysql-apicurio.yaml exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic dbserver1.inventory.customers
```

When you look at the data message you will notice that it contains only `payload` but not `schema` part as this is externalized into the registry.

#### Avro format using Confluent Avro converter
Configuring Avro at the Debezium Connector involves specifying the converter and schema registry as a part of the connectors configuration.
To do this, follow the same steps above for MySQL but instead using the _docker-compose-mysql-apicurio.yaml_ and _register-mysql-apicurio.json_ configuration files.
The Compose file configures the Connect service to use the default (de-)serializers for the Connect instance and starts one additional service, the Apicurio Registry.
The connector configuration file configures the connector but explicitly sets the (de-)serializers for the connector to use Avro and specifies the location of the Apicurio registry.

You can access the first version of the schema for `customers` values like so:

```shell
curl -X GET http://localhost:8080/ccompat/subjects/dbserver1.inventory.customers-value/versions/1
```

Or, if you have the `jq` utility installed, you can get a formatted output like this:

```shell
curl -X GET http://localhost:8080/ccompat/subjects/dbserver1.inventory.customers-value/versions/1 | jq '.schema | fromjson'
```

If you alter the structure of the `customers` table in the database and trigger another change event,
a new version of that schema will be available in the registry.

To consume the Avro messages It is possible to use `kafkacat` tool:

```shell
docker run --rm --tty \
  --network tutorial_default \
  debezium/tooling \
  kafkacat -b kafka:9092 -C -o beginning -q -s value=avro -r http://apicurio:8080/ccompat \
  -t dbserver1.inventory.customers | jq .
```

#### Avro format using Apicurio Avro converter
Configuring Avro at the Debezium Connector involves specifying the converter and schema registry as a part of the connectors configuration.
To do this, follow the same steps above for MySQL but instead using the _docker-compose-mysql-apicurio.yaml_ and _register-mysql-apicurio-converter-avro.json_ configuration files.
The Compose file configures the Connect service to use the default (de-)serializers for the Connect instance and starts one additional service, the Apicurio Registry.
The connector configuration file configures the connector but explicitly sets the (de-)serializers for the connector to use Avro and specifies the location of the Apicurio registry.

You can access the first version of the schema for `customers` values like so:

```shell
curl -X GET http://localhost:8080/artifacts/dbserver1.inventory.customers-value
```

Or, if you have the `jq` utility installed, you can get a formatted output like this:

```shell
curl -X GET http://localhost:8080/artifacts/dbserver1.inventory.customers-value | jq .
```

If you alter the structure of the `customers` table in the database and trigger another change event,
a new version of that schema will be available in the registry.


## Using Postgres

```shell
# Start the topology as defined in http://debezium.io/docs/tutorial/
export DEBEZIUM_VERSION=1.0
docker-compose -f docker-compose-postgres.yaml up

# Start Postgres connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-postgres.json

# Consume messages from a Debezium topic
docker-compose -f docker-compose-postgres.yaml exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic dbserver1.inventory.customers

# Modify records in the database via Postgres client
docker-compose -f docker-compose-postgres.yaml exec postgres env PGOPTIONS="--search_path=inventory" bash -c 'psql -U $POSTGRES_USER postgres'

# Shut down the cluster
docker-compose -f docker-compose-postgres.yaml down
```

## Using MongoDB

```shell
# Start the topology as defined in http://debezium.io/docs/tutorial/
export DEBEZIUM_VERSION=1.0
docker-compose -f docker-compose-mongodb.yaml up

# Initialize MongoDB replica set and insert some test data
docker-compose -f docker-compose-mongodb.yaml exec mongodb bash -c '/usr/local/bin/init-inventory.sh'

# Start MongoDB connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-mongodb.json

# Consume messages from a Debezium topic
docker-compose -f docker-compose-mongodb.yaml exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic dbserver1.inventory.customers

# Modify records in the database via MongoDB client
docker-compose -f docker-compose-mongodb.yaml exec mongodb bash -c 'mongo -u $MONGODB_USER -p $MONGODB_PASSWORD --authenticationDatabase admin inventory'

db.customers.insert([
    { _id : NumberLong("1005"), first_name : 'Bob', last_name : 'Hopper', email : 'thebob@example.com', unique_id : UUID() }
]);

# Shut down the cluster
docker-compose -f docker-compose-mongodb.yaml down
```

## Using Oracle

This assumes Oracle is running on localhost
(or is reachable there, e.g. by means of running it within a VM or Docker container with appropriate port configurations)
and set up with the configuration, users and grants described in the Debezium [Vagrant set-up](https://github.com/debezium/oracle-vagrant-box).
Note that the connector is using the XStream API, which requires a license for the Golden Gate product
(which itself is not required be installed, though).

Also you must download the [Oracle instant client for Linux](http://www.oracle.com/technetwork/topics/linuxx86-64soft-092277.html)
and put it under the directory _debezium-with-oracle-jdbc/oracle_instantclient_.

```shell
# Start the topology as defined in http://debezium.io/docs/tutorial/
export DEBEZIUM_VERSION=1.0
docker-compose -f docker-compose-oracle.yaml up --build

# Insert test data
cat debezium-with-oracle-jdbc/init/inventory.sql | docker exec -i dbz_oracle sqlplus debezium/dbz@//localhost:1521/ORCLPDB1
```

Adjust the host name of the database server and the name of the XStream outbound server in `register-oracle.json` as per your environment.
Then register the Debezium Oracle connector:

```shell
# Start Oracle connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-oracle.json

# Create a test change record
echo "INSERT INTO customers VALUES (NULL, 'John', 'Doe', 'john.doe@example.com');" | docker exec -i dbz_oracle sqlplus debezium/dbz@//localhost:1521/ORCLPDB1

# Consume messages from a Debezium topic
docker-compose -f docker-compose-oracle.yaml exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic server1.DEBEZIUM.CUSTOMERS

# Modify other records in the database via Oracle client
docker exec -i dbz_oracle sqlplus debezium/dbz@//localhost:1521/ORCLPDB1

# Shut down the cluster
docker-compose -f docker-compose-oracle.yaml down
```

## Using SQL Server

```shell
# Start the topology as defined in http://debezium.io/docs/tutorial/
export DEBEZIUM_VERSION=1.0
docker-compose -f docker-compose-sqlserver.yaml up

# Initialize database and insert test data
cat debezium-sqlserver-init/inventory.sql | docker exec -i tutorial_sqlserver_1 bash -c '/opt/mssql-tools/bin/sqlcmd -U sa -P $SA_PASSWORD'

# Start SQL Server connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-sqlserver.json

# Consume messages from a Debezium topic
docker-compose -f docker-compose-sqlserver.yaml exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic server1.dbo.customers

# Modify records in the database via SQL Server client (do not forget to add `GO` command to execute the statement)
docker-compose -f docker-compose-sqlserver.yaml exec sqlserver bash -c '/opt/mssql-tools/bin/sqlcmd -U sa -P $SA_PASSWORD -d testDB'

# Shut down the cluster
docker-compose -f docker-compose-sqlserver.yaml down
```

## Using Db2

```shell
# Start the topology as defined in http://debezium.io/docs/tutorial/
export DEBEZIUM_VERSION=1.1

docker-compose -f docker-compose-db2.yaml up --build

# Start DB2 connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-db2.json

# Consume messages from a Debezium topic
docker-compose -f docker-compose-db2.yaml exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic db2server.DB2INST1.CUSTOMERS

# Modify records in the database via DB2 client
docker-compose -f docker-compose-db2.yaml exec db2server bash -c 'su - db2inst1'
db2 connect to TESTDB
db2 "INSERT INTO DB2INST1.CUSTOMERS(first_name, last_name, email) VALUES ('John', 'Doe', 'john.doe@example.com');"
# Shut down the cluster
docker-compose -f docker-compose-db2.yaml down
```

## Using externalized secrets

Kafka Connect allows [externalization](https://cwiki.apache.org/confluence/display/KAFKA/KIP-297%3A+Externalizing+Secrets+for+Connect+Configurations) of secrets into a separate configuration repository.
The configuration is done at both worker and connector level.

```shell
# Start the topology as defined in http://debezium.io/docs/tutorial/
export DEBEZIUM_VERSION=1.0
docker-compose -f docker-compose-mysql-ext-secrets.yml up

# Start MySQL connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @register-mysql-ext-secrets.json

# Check plugin configuration to see that secrets are not visible
curl -s http://localhost:8083/connectors/inventory-connector/config | jq .

# Shut down the cluster
docker-compose -f docker-compose-mysql-ext-secrets.yml down
```

## Debugging

Should you need to establish a remote debugging session into a deployed connector, add the following to the `environment` section of the `connect` in the Compose file service:

    - KAFKA_DEBUG=true
    - DEBUG_SUSPEND_FLAG=n
    - JAVA_DEBUG_PORT=*:5005

Also expose the debugging port 5005 under `ports`:

    - 5005:5005

You can then establish a remote debugging session from your IDE on localhost:5005.
