# Debezium Server with MongoDB and Pub/Sub (GCP)

This demo explains how to deploy [Debezium Server](https://debezium.io/documentation/reference/operations/debezium-server.html) using MongoDB as source and [Google Cloud Pub/Sub](https://cloud.google.com/pubsub/docs) as sink.

## Setup

**In the terminal:**

```shell
export DEBEZIUM_VERSION=1.9
```

**In GCP:**

To run this demo you need a GCP service account with the role `pubsub.publisher`.

Create a Pub/Sub topic with the name `tutorial.inventory.customers`.

Now, download the credential JSON file for the service account.

**In this folder:**

Edit the `conf/application.properties` file, and replace `project-id` with your project id.

Open the `docker-compose.yml` file and replace `/your/path/to/service-account.json` with the path to the credential json file you've downloaded in the previous step.

## How to run

From the terminal start the MongoDB container:

```shell
# Initialize MongoDB
docker compose up -d mongodb


# Initialize MongoDB replica set and insert some test data
docker compose exec mongodb bash -c '/usr/local/bin/init-inventory.sh'


# Initialize Debezium Server
docker compose up -d debezium-server
```

With all initialized, test the setup inserting, updating or deleting some records in the customers collection. The logs will appear in Google Cloud Pub/Sub in a few seconds.
