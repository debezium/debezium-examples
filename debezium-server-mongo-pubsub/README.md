# Debezium Server with MongoDB and Pub/Sub (GCP)

This demo explains how to deploy Debezium Server using MongoDB as source and Pub/Sub as sink.

<br>

## Setup

**In the terminal:**

```shell
export DEBEZIUM_VERSION=1.6
```

<br>

**In GCP:**

To run this demo you need a GCP service account with the role `pubsub.publisher`. 

Create a Pub/Sub topic with the name `tutorial.inventory.customers`.

Now, download the credential json file for the service account.

<br>

**In this folder:**

Edit the `conf/application.properties` file, and replace `project-id` with your project id. 

Open the `dbz-server-compose.yml` file and replace `/your/path/to/service-account.json` with the path to the credential json file you've downloaded in the previous step.

<br>

## How to run

From the terminal start the MongoDB container:

```shell
# Initialize MongoDB
docker-compose -f mongo-compose.yml up -d --build


# Initialize MongoDB replica set and insert some test data
docker-compose -f mongo-compose.yml  exec mongodb bash -c '/usr/local/bin/init-inventory.sh'


# Initialize Debezium Server
docker-compose -f dbz-server-compose.yml up -d --build
```

With all initialized, test the setup inserting, updating or deleting some records in the customers collection. The logs will appear in the Pub/Sub in a few seconds.
