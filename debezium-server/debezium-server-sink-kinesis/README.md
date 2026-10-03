# Debezium Server sink to Kinesis

This example demonstrates how to deploy [Debezium Server](https://debezium.io/documentation/reference/stable/operations/debezium-server.html) using Postgres, MongoDB, and MySQL as data sources and [Amazon Kinesis](https://docs.aws.amazon.com/kinesis/) as a destination.

**Note:** Running this example may incur costs for managed AWS services. Be sure to delete all resources once you've completed the example.

## Overview

This demo shows how to use Debezium Server with data sources such as Postgres, MongoDB, and MySQL. It sends data to Amazon Kinesis. We deploy the source databases using a Docker Compose file, and the Kinesis stream is hosted on Amazon Web Services.

## Prerequisites

Before getting started, ensure you have the following prerequisites:

1. Docker
2. An AWS IAM user account with Kinesis permission policies (see Setup)
3. The [aws cli](https://aws.amazon.com/cli/) installed
4. [jq](https://jqlang.org/) installed

## Project Structure

```
└── debezium-server-sink-kinesis
    ├── README.md
    ├── config-mongodb
    │   └── application.properties
    ├── config-mysql
    │   └── application.properties
    ├── config-postgres
    │   └── application.properties
    └── docker-compose.yml
```

- `README.md` is an essential guide for this example.
- `config-mongodb/application.properties` MongoDB configuration of the Debezium Connector.
- `config-postgres/application.properties` Postgres configuration of the Debezium Connector.
- `config-mysql/application.properties` MySQL configuration of the Debezium Connector.
- `docker-compose.yml` is used for defining and running Debezium Server and the databases via Docker Compose.

## Setup

1a. [Create an IAM user](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_users_create.html) and an [access key](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_access-keys.html) with the following policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "kinesis:PutRecords",
        "kinesis:CreateStream",
        "kinesis:DeleteStream",
        "kinesis:DescribeStream",
        "kinesis:DescribeStreamSummary",
        "kinesis:ListShards",
        "kinesis:GetShardIterator",
        "kinesis:GetRecords"
      ],
      "Resource": "arn:aws:kinesis:us-east-1:<your-account-id>:stream/tutorial.inventory.customers"
    }
  ]
}
```

Debezium Server itself only needs `kinesis:PutRecords`; the other actions are used by the AWS CLI commands in this guide.

> **NOTE:** This tutorial assumes us-east-1 as your region. If you plan to use a different one, update `AWS_DEFAULT_REGION` below, `debezium.sink.kinesis.region` in the db config `application.properties` file, and the region in the resource ARN.

1b. Export the necessary variables into your terminal environment:

```bash
export DEBEZIUM_VERSION=3.7
export AWS_ACCESS_KEY_ID=<your access key id>
export AWS_SECRET_ACCESS_KEY=<your secret access key>
export AWS_DEFAULT_REGION=us-east-1
```

> **NOTE:** This example was tested with Debezium 3.7. The MongoDB setup requires 3.1 or newer.

1c. Confirm you're using the test user:

```bash
aws sts get-caller-identity --query Arn --output text
```

2. From the terminal, create a Kinesis stream and wait for it to exist:

```bash
aws kinesis create-stream --stream-name tutorial.inventory.customers --shard-count 1
aws kinesis wait stream-exists --stream-name tutorial.inventory.customers
```

## How to run

All three Debezium Server containers bind port 8080, so run one database at a time. Run `docker compose down -v` before switching to another one.

On startup, Debezium Server takes an initial snapshot, so the 4 existing customers are sent to Kinesis as `"op": "r"` (read) events before you make any changes. Your update then appears as an `"op": "u"` event.

### PostgreSQL Debezium Connector

Start the Debezium Server and database container:

```bash
docker compose up -d debezium-server-postgres
```

Test the setup by making changes to the customers table. The change events will appear in Kinesis shortly. You can make an update to Postgres with this command:

```bash
docker compose exec postgres psql -U postgres -c "UPDATE inventory.customers SET first_name='Anne Marie' WHERE id=1004;"
```

### MySQL Debezium Connector

Start the Debezium Server and database container:

```bash
docker compose up -d debezium-server-mysql
```

Test the setup by making changes to the customers table. The change events will appear in Kinesis shortly. You can make an update to MySQL with this command:

```bash
docker compose exec mysql mysql -u mysqluser -pmysqlpw inventory -e "UPDATE customers SET first_name='Anne Marie' WHERE id=1004;"
```

### MongoDB Debezium Connector

Start the Debezium Server and database container:

```bash
docker compose up -d debezium-server-mongodb
```

Test the setup by making changes to the customers collection. The change events will appear in Kinesis shortly. You can make an update to MongoDB with this command:

```bash
docker compose exec mongodb mongosh -u debezium -p dbz --authenticationDatabase admin inventory \
  --eval 'db.customers.updateOne({_id: NumberLong("1004")}, {$set: {first_name: "Anne Marie"}})'
```

## See Updates In Kinesis

First, get a shard iterator:

```bash
SHARD_ITERATOR=$(aws kinesis get-shard-iterator \
  --stream-name tutorial.inventory.customers \
  --shard-id shardId-000000000000 \
  --shard-iterator-type TRIM_HORIZON \
  --query ShardIterator --output text)
```

Then read the records for the database you are running:

1. For PostgreSQL:

```bash
aws kinesis get-records --shard-iterator "$SHARD_ITERATOR" \
  | jq '.Records[].Data | @base64d | fromjson | select(.source.connector == "postgresql")'
```

2. For MySQL:

```bash
aws kinesis get-records --shard-iterator "$SHARD_ITERATOR" \
  | jq '.Records[].Data | @base64d | fromjson | select(.source.connector == "mysql")'
```

3. For MongoDB, the `after` field is a JSON string, so `.after |= fromjson` expands it:

```bash
aws kinesis get-records --shard-iterator "$SHARD_ITERATOR" \
  | jq '.Records[].Data | @base64d | fromjson | select(.source.connector == "mongodb") | .after |= fromjson'
```

> **NOTE:** `before` is `null` for MongoDB unless change stream pre-images are enabled.

To show only the most relevant fields, append `| {op, before, after}` to the jq filter. The update event looks like this:

```json
{
  "op": "u",
  "before": {
    "id": 1004,
    "first_name": "Anne",
    "last_name": "Kretchmar",
    "email": "annek@noanswer.org"
  },
  "after": {
    "id": 1004,
    "first_name": "Anne Marie",
    "last_name": "Kretchmar",
    "email": "annek@noanswer.org"
  }
}
```

> **NOTE:** If get-records returns no records, run it again. Shard iterators expire after 5 minutes, so rerun the first command if get-records returns an ExpiredIteratorException.

## Cleanup

1. Tear down compose stack:

```bash
docker compose down -v
```

2. Delete Kinesis stream:

```bash
aws kinesis delete-stream --stream-name tutorial.inventory.customers
aws kinesis wait stream-not-exists --stream-name tutorial.inventory.customers
```

3. Delete your test IAM user.
