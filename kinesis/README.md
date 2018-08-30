# Streaming Database Changes to Amazon Kinesis Using Debezium

[Debezium](http://debezium.io/) allows to capture and stream change events from multiple databases such as MySQL and Postgres and is mostly used with Apache Kafka as the underlying messaging infrastructure.

Using [Debezium's embedded mode](http://debezium.io/docs/embedded/) it is possible though to stream database changes to arbitrary destinations and thus not be limited to Kafka as the only broker.
This demo shows how to stream changes from MySQL database running on a local machine to an Amazon [Kinesis](https://aws.amazon.com/kinesis/data-streams/) stream.

## Prerequisites

* Java 8 development environment
* Local [Docker](https://www.docker.com/) installation to run the source database
* an Amazon [AWS](https://aws.amazon.com/) account
* The [AWS CLI](https://aws.amazon.com/cli/) client
* [jq](https://stedolan.github.io/jq/) 1.6 installed

## Running the Demo

### Starting the MySQL Source Database

We will start a pre-populated MySQL database that is the same as used by the Debezium [tutorial](http://debezium.io/docs/tutorial/):

```
docker run -it --rm --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=debezium -e MYSQL_USER=mysqluser -e MYSQL_PASSWORD=mysqlpw debezium/example-mysql:0.8
```

### Preparing the CLI environment

It is assumed that you have already executed `aws configure` as described in AWS CLI [getting started](https://github.com/aws/aws-cli#getting-started) guide.

### Creating the Kinesis Stream

```
aws kinesis create-stream --stream-name kinesis.inventory.customers --shard-count 1
```

You can use an arbitrary number of shards. To keep things simple we capture only one table - `customers` from the `inventory` database, so we need only one stream.
If you want to capture multiple tables you need to create the equivalent streams for them too.
The naming scheme of streams is `<engine_name>.<database_name>.<table_name>` which in our case is `kinesis.inventory.<table_name>`.

### Creating a Stream Iterator

We will use AWS CLI to read messages.

```
ITERATOR=$(aws kinesis get-shard-iterator --stream-name kinesis.inventory.customers --shard-id 0 --shard-iterator-type TRIM_HORIZON | jq '.ShardIterator')
```

### Connecting the Database to Kinesis

Start the application that uses Debezium Embedded to get change events from database to Kinesis stream.
```
mvn exec:java
```

### Reading Events From the Kinesis Stream

Execute this command to obtain the records from the topic using the iterator and print the change event contents:

```
aws kinesis get-records --shard-iterator $ITERATOR | jq -r '.Records[].Data | @base64d' | jq .
```

This will return a sequence of JSON messages like this:

```
{
  "before": null,
  "after": {
    "id": 1001,
    "first_name": "Sally",
    "last_name": "Thomas",
    "email": "sally.thomas@acme.com"
  },
  "source": {
    "version": "0.7.4",
    "name": "kinesis",
    "server_id": 0,
    "ts_sec": 0,
    "gtid": null,
    "file": "mysql-bin.000003",
    "pos": 154,
    "row": 0,
    "snapshot": true,
    "thread": null,
    "db": "inventory",
    "table": "customers"
  },
  "op": "c",
  "ts_ms": 1520513267424
}
{
  "before": null,
  "after": {
    "id": 1002,
    "first_name": "George",
    "last_name": "Bailey",
    "email": "gbailey@foobar.com"
  },
  "source": {
    "version": "0.7.4",
    "name": "kinesis",
    "server_id": 0,
    "ts_sec": 0,
    "gtid": null,
    "file": "mysql-bin.000003",
    "pos": 154,
    "row": 0,
    "snapshot": true,
    "thread": null,
    "db": "inventory",
    "table": "customers"
  },
  "op": "c",
  "ts_ms": 1520513267424
}
...
```
