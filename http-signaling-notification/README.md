# HTTP Signaling and Notification

This example demonstrates how to create custom signaling and notification channels for Debezium connectors. It discusses how to create and utilize the `HTTP` signal and notification channels using the Debezium Postgres connector, a [Mock Server](https://www.mock-server.com/), and [Postbin](https://www.toptal.com/developers/postbin/) which is used as the HTTP endpoint to receive the notifications. It accompanies the blog post [Debezium signaling and notifications - Part 2: Customisation](https://debezium.io/blog/2023/07/10/custom-http-signaling-notification.adoc).

## Building

Prepare the Java components by first performing a Maven build.

```console
$ mvn clean install
```

## Environment

Setup the necessary environment variables

```console
$ export DEBEZIUM_VERSION=2.3
```

The `DEBEZIUM_VERSION` specifies which version of Debezium artifacts should be used, which should 2.3 or later.

## Run the services

Start all components:

```console
$ docker-compose up -d
```

This executes all configurations set forth by the `docker-compose.yaml` file.

## Configure the Debezium connector

Register Postgres Debezium connector to start streaming changes from the database:

```console
$ curl -i -X POST -H "Accept:application/json" \
    -H  "Content-Type:application/json" http://localhost:8083/connectors/ \
    -d @register-postgres.json
```

## Signal and Notification events

Once the connector is running, you can see signal event sent to the connector via `Mock Server` through the endpoint:

```console
Recorded signal event 'SignalRecord{id='924e3ff8-2245-43ca-ba77-2af9af02fa07', type='log', data='{"message":"Signal message received from http endpoint."}', additionalData={}}'    [io.debezium.examples.signal.HttpSignalChannel]
```

You can also see notification event sent from the connector to the endpoint via `Postbin`:

```console
[HTTP NOTIFICATION SERVICE] Sending notification to http channel   [io.debezium.examples.notification.HttpNotificationChannel]

Bin created: {"binId":"1688742588469-1816775151528","now":1688742588470,"expires":1688744388470}   [io.debezium.examples.notification.HttpNotificationChannel]
```

Using the `binId` from the notification event, you can see the notification event sent to the endpoint in `Postbin`. To view the notification event, you can access Postbin using the following URL: `https://www.toptal.com/developers/postbin/b/:binId`. Replace `:binId` in the URL with the actual binId obtained from the connector logs.

## Stop the services

Stop all components:

```console
$ docker-compose down
```
