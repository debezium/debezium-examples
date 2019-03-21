# Streaming Database Changes to NATS Using Debezium

[Debezium](http://debezium.io/) allows to capture and stream change events from multiple databases such as MySQL and Postgres and is mostly used with Apache Kafka as the underlying messaging infrastructure.

Using [Debezium's embedded mode](http://debezium.io/docs/embedded/) it is possible though to stream database changes to arbitrary destinations and thus not be limited to Kafka as the only broker.
This demo shows how to stream changes from MySQL database running on a local machine to a local [NATS](https://nats.io) stream server.

## Prerequisites

* Java 8 development environment
* Local [Docker](https://www.docker.com/) installation to run the source database

## Running the Demo

### Starting the MySQL Source Database

We will start a pre-populated MySQL database that is the same as used by the Debezium [tutorial](http://debezium.io/docs/tutorial/):

```
docker run -it --rm --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=debezium -e MYSQL_USER=mysqluser -e MYSQL_PASSWORD=mysqlpw debezium/example-mysql:0.8
```

### Creating a NATS Stream Server

Start a NATS Server that will emit the messages from publishers to subscribers who are using the same subject.

```
docker run -it --rm --name nats-server -p 4222:4222 nats
```

### Creating a Stream Subscriber

Start a NATS stream subscriber that will receive the change events from the publisher. It simply show the raw events on standard output.

```
mvn exec:java@subscriber
```

### Creating a Stream Publisher Sending Events

Start the Debezium Embedded to get change events from database. Also create a NATS stream publisher who send change events to a specific NATS subject. All subscriber who is listening to this subject will receive these events.

```
mvn exec:java@publisher
```

### Reading Events From the NATS Stream

The subscriber will return a sequence of JSON messages like this:

```
{"key":{"id":1001},"value":{"before":null,"after":{"id":1001,"first_name":"Sally","last_name":"Thomas","email":"sally.thomas@acme.com"},"source":{"version":"0.9.2.Final","connector":"mysql","name":"nats","server_id":0,"ts_sec":0,"gtid":null,"file":"mysql-bin.000003","pos":154,"row":0,"snapshot":true,"thread":null,"db":"inventory","table":"customers","query":null},"op":"c","ts_ms":1553145497879}}
{"key":{"id":1002},"value":{"before":null,"after":{"id":1002,"first_name":"George","last_name":"Bailey","email":"gbailey@foobar.com"},"source":{"version":"0.9.2.Final","connector":"mysql","name":"nats","server_id":0,"ts_sec":0,"gtid":null,"file":"mysql-bin.000003","pos":154,"row":0,"snapshot":true,"thread":null,"db":"inventory","table":"customers","query":null},"op":"c","ts_ms":1553145497879}}
{"key":{"id":1003},"value":{"before":null,"after":{"id":1003,"first_name":"Edward","last_name":"Walker","email":"ed@walker.com"},"source":{"version":"0.9.2.Final","connector":"mysql","name":"nats","server_id":0,"ts_sec":0,"gtid":null,"file":"mysql-bin.000003","pos":154,"row":0,"snapshot":true,"thread":null,"db":"inventory","table":"customers","query":null},"op":"c","ts_ms":1553145497879}}
{"key":{"id":1004},"value":{"before":null,"after":{"id":1004,"first_name":"Anne","last_name":"Kretchmar","email":"annek@noanswer.org"},"source":{"version":"0.9.2.Final","connector":"mysql","name":"nats","server_id":0,"ts_sec":0,"gtid":null,"file":"mysql-bin.000003","pos":154,"row":0,"snapshot":true,"thread":null,"db":"inventory","table":"customers","query":null},"op":"c","ts_ms":1553145497879}}
...
```
