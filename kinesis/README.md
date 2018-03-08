# Streaming database changes to Amazon Kinesis using Debezium

[Debezium](http://debezium.io/) was a set of Apache Kafka connectors that allows streaming of change events from different database. Using [Debezium Embedded](http://debezium.io/docs/embedded/) it is possible to stream database changes to an arbitrary destination and not be limited to Kafka broker only.

This demo shows how to stream changes from MySQL database running on a local machine to aa Amazon [Kinesis](https://aws.amazon.com/kinesis/data-streams/) stream.

## Pre-requisities
* Java 8 development environment
* Local Dockar installation to run source database
* an Amazon [AWS](https://aws.amazon.com/) account
* [AWS CLI](https://aws.amazon.com/cli/) client

## How to run
### Start MySQL source database
We will start a pre-populated MySQL database that is the same as used by Debezium [tutorial](http://debezium.io/docs/tutorial/).

```mvn docker:run```

### Initialize Kinesis stream
We suppose that you have already executed `aws configure` as descirbed in AWS CLI installation guide.

### Create the Kinesis stream

```aws kinesis create-stream --stream-name debezium --shard-count 1```

We are using only one shard to keep the events from database in ordered sequence. If you are not interested in a total order but the order per table is enough you can modify the code to use the table name as a sharding key and increase the number of shards.

### Create a stream iterator
We will use AWS CLI to read messages.

```
ITERATOR=$(aws kinesis get-shard-iterator --stream-name debezium --shard-id 0 --shard-iterator-type LATEST|jq '.ShardIterator')
```

### Connect the database to Kinesis
Start the application that uses Debezium Embedded to get change events from database to Kinesis stream.
```
mvn exec:java
```

### Read events from the stream
Execute command

```
aws kinesis get-records --shard-iterator $ITERATOR | jq -r '.Records[].Data' | base64 -d | jq .
```

that will return a sequence JSON messages similar to
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
  "ts_ms": 1520504310296
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
  "ts_ms": 1520504310296
}
.
.
.
```

