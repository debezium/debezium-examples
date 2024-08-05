# Debezium Server sink to Pub/Sub emulator with Redis storage

## Overview

This example demonstrates how to deploy [Debezium Server](https://debezium.io/documentation/reference/stable/operations/debezium-server.html) using MySQL as data sources, Redis and storage for offset and schema history, and [Google Cloud Pub/Sub emulator](https://cloud.google.com/pubsub/docs) as a destination.


## Prerequisites

Before getting started, ensure you have the following prerequisites:

1. Docker
2. To use emulator, you can either:
    - Write your own application using [Cloud Client Libraries](https://cloud.google.com/pubsub/libraries#gcloud-libraries)
    - or Use `docker compose run --rm pubsub_tools bash -c '...'` which based on [python-pubsub/samples/snippets](https://github.com/googleapis/python-pubsub/tree/main/samples/snippets)

    The emulator DOES NOT support Google Cloud console or `gcloud pubsub` commands. 

## Project Structure
```
└── debezium-server/debezium-server-sink-pubsub
    ├── config-mysql
    │   └── application.properties
    ├── docker-compose.yml
    ├── Dockerfile
    └── README.md
```

- `config-mysql/application.properties` MySQL configuration of the Debezium Connector.
- `docker-compose.yml` is used for defining and running Debezium Server and the databases via Docker Compose.
- `Dockerfile` is used for setup `pubsub_tools` which contains useful CLI tools to work with Pub/Sub emulator.
- `README.md` is an essential guide for this example.

## How to run
1. Setup environment variables
```bash
export DEBEZIUM_VERSION=2.7

# Or create .env for docker compose
echo 'DEBEZIUM_VERSION=2.7' > .env
```

2. Start all dependencies services
```bash
docker compose up -d mysql redis pubsub
```

- `mysql`: accessible via port `3306`
- `redis`: accessible via port `6379`
- `pubsub`: accessible via port `8085` with project `debezium-tutorial-local`

3. Setup Pub/Sub emulator
- Create GCP Pub/Sub topics in Pub/Sub emulator from terminal

```bash
# Topic for schema history
docker compose run --rm pubsub_tools bash -c 'python publisher.py $PUBSUB_PROJECT_ID create tutorial'

# Topic for inventory.customers table
docker compose run --rm pubsub_tools bash -c 'python publisher.py $PUBSUB_PROJECT_ID create tutorial.inventory.customers'
```

- Setup Pub/Sub emulator subscriptions. For each topic above, we can create either pull or subscription to the emulator and observe the changes

```bash
# Pull subscription
docker compose run --rm pubsub_tools bash -c 'python subscriber.py $PUBSUB_PROJECT_ID create tutorial.inventory.customers customers-pull-sub'

# Retrieve messages from your pull subscription
docker compose run --rm pubsub_tools bash -c 'python subscriber.py $PUBSUB_PROJECT_ID receive customers-pull-sub'
```

**NOTE**: If you don't use docker compose `pubsub_tools` above, [follow instructions how to use the emulator](https://cloud.google.com/pubsub/docs/emulator#using_the_emulator). These environment variables must be set:
```bash
export PUBSUB_EMULATOR_HOST=0.0.0.0:8085
export PUBSUB_PROJECT_ID=debezium-tutorial-local
```


5. Start Debezium Server:
```bash
docker compose up -d debezium-server-mysql
```

6. Test the setup by making changes to the customers table.

You can access MySQL with this command:
```bash
docker compose exec mysql bash -c 'mysql -u $MYSQL_USER -p$MYSQL_PASSWORD'
```

Observe the logs depending on the subscription you setup
