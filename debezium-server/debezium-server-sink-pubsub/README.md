# Debezium Server sink to Pub/Sub

This example demonstrates how to deploy [Debezium Server](https://debezium.io/documentation/reference/stable/operations/debezium-server.html) using Postgres, MongoDB, and MySQL as data sources and [Google Cloud Pub/Sub](https://cloud.google.com/pubsub/docs) as a destination.

**Note:** Running this example may incur costs for managed Google Cloud services. Be sure to delete all resources once you've completed the example

## Overview

This demo shows how to use Debezium Server with data sources such as Postgres, MongoDB, and MySQL. It sends data to Google Cloud Pub/Sub. We deploy the source databases using a Docker Compose file, and the Pub/Sub topic is hosted on the Google Cloud Platform.

## Prerequisites

Before getting started, ensure you have the following prerequisites:

1. Docker
2. A GCP service account with the `pubsub.publisher` role
3. A Pub/Sub topic
4. The [gcloud](https://cloud.google.com/sdk/gcloud) client installed

## Project Structure

```
└── debezium-server-sink-pubsub
    ├── README.md
    ├── config-mongodb
    │   └── application.properties
    ├── config-mysql
    │   └── application.properties
    ├── config-postgres
    │   └── application.properties
    └── docker-compose.yml
```

- `README.md` is an essential guide for this example.
- `config-mongodb/application.properties` MongoDB configuration of the Debezium Connector.
- `config-postgres/application.properties` Postgres configuration of the Debezium Connector.
- `config-mysql/application.properties` MySQL configuration of the Debezium Connector.
- `docker-compose.yml` is used for defining and running Debezium Server and the databases via Docker Compose.

## Setup

1. Edit the `docker-compose.yml` file and replace `/your/path/to/service-account.json` with the GCP Service Account path on your local machine.
2. Create a GCP Pub/Sub topic. From the terminal, create a Pub/Sub topic:

```shell
gcloud pubsub topics create tutorial.inventory.customers
```

3. Export environment variable:

```shell
export DEBEZIUM_VERSION=2.3
```

4. Edit the Debezium configurations:

- For PostgresSQL, Edit the `config-postgres/application.properties` file, and replace `project-id` with your GCP project id
- For MongoDB, Edit the `config-mongodb/application.properties` file, and replace `project-id` with your GCP project id
- For MySQL, Edit the `config-mongodb/application.properties` file, and replace `project-id` with your GCP project id


## How to run

### PostgresSQL Debezium Connector

Start the database container:

```shell
docker compose up -d postgres
```

Start the Debezium Server:

```shell
docker compose up -d debezium-server-postgres
```

Test the setup by making changes to the customers table. The logs will appear in Google Cloud Pub/Sub shortly. You can access Postgres with this command:

```shell
docker compose exec postgres env PGOPTIONS="--search_path=inventory" bash -c 'psql -U $POSTGRES_USER postgres'
```


### MongoDB Debezium Connector


Start the database container:

```shell
docker compose up -d mongodb
```

Initialize MongoDB replica set and insert some test data

```shell
docker compose exec mongodb bash -c '/usr/local/bin/init-inventory.sh'
```

Start Debezium Server:

```shell
docker compose up -d debezium-server-mongodb
```


### MySQL Debezium Connector


Start the database container:

```shell
docker compose up -d mysql
```

Start Debezium Server:

```shell
docker compose up -d debezium-server-mysql
```

Test the setup by making changes to the customers table. The logs will appear in Google Cloud Pub/Sub shortly. You can access MySQL with this command:

```shell
docker compose exec mysql bash -c 'mysql -u $MYSQL_USER -p$MYSQL_PASSWORD'
```