# Debezium Server with PostgreSQL and Pub/Sub

This example demonstrates how to deploy [Debezium Server](https://debezium.io/documentation/reference/stable/operations/debezium-server.html) using PostgreSQL as the source and [Google Cloud Pub/Sub](https://cloud.google.com/pubsub/docs) as a sink.


## Topology

This demo uses a Pub/Sub topic as a sink and the standard Debezium example PostgreSQL database. The Pub/Sub topic runs on the Google Cloud Platform and the source database is deployed via Docker Compose file.

## Prerequisites

This example needs

1. Docker
2. GCP service account with the role `pubsub.publisher`
3. A Pub/Sub topic
4. The [gcloud](https://cloud.google.com/sdk/gcloud) client

## Hierarchical File System

```
├── conf
│   └── application.properties
├── docker-compose.yml
└── README.md
```

1. `conf/application.properties` is the main configuration of the Debezium Connector
2. `docker-compose.yml` is used for defining and running Debezium Server and the Postgres database via Docker Compose
3. `README.md` is an essential guide for this example

## Demonstration

Edit the _conf/application.properties_ file, and replace `project-id` with your GCP project id

Edit the _docker-compose.yml_ file, and replace `/your/path/to/service-account.json` with the GCP Service Account path

From the terminal create a Pub/Sub topic

```shell
gcloud pubsub topics create tutorial.inventory.customers
```

Export environment variable

```shell
export DEBEZIUM_VERSION=1.8
```

Start the containers

```shell
docker-compose up -d --build
```


Once everything has started up, test the setup by inserting, updating or deleting some records in the customers table. The logs will appear in Google Cloud Pub/Sub in a few seconds.
