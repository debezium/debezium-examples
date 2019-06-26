# Streaming Database Changes to Apache Camel

[Debezium](http://debezium.io/) allows to capture and stream change events from multiple databases such as MySQL and Postgres and is mostly used with Apache Kafka as the underlying messaging infrastructure.

Using [Debezium's embedded mode](http://debezium.io/docs/embedded/) it is possible though to stream database changes to arbitrary destinations and thus not be limited to Kafka as the only broker.
The destination can be Apache Camel which we can use for complex routing and transformation and also sending the message to one of more than hundred supported components.
This demo shows how to stream changes from MySQL database running on a local machine to a local file directory using [file component](https://camel.apache.org/file2.html) stream.

## Prerequisites

* Java 8 development environment
* Local [Docker](https://www.docker.com/) installation to run the source database
* [jq](https://stedolan.github.io/jq/) 1.6 installed

## Running the Demo

### Starting the MySQL Source Database

We will start a pre-populated MySQL database that is the same as used by the Debezium [tutorial](http://debezium.io/docs/tutorial/):

```
docker run -it --rm --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=debezium -e MYSQL_USER=mysqluser -e MYSQL_PASSWORD=mysqlpw debezium/example-mysql:0.8
```

### Connecting the Database to Apache Camel

Start the application that uses Debezium Embedded to get change events from database to `out` directory.
```
mvn exec:java
```

### Reading Events From the Directory

The Camel file component will create `out` sub-directory in a working directory and will create a file for each message coming from Debezium:

```
$ ls -l out/
total 16
-rw-rw-r-- 1 jpechane jpechane 371 čen 26 14:40 ID-jpechane-1561552837953-0-2
-rw-rw-r-- 1 jpechane jpechane 369 čen 26 14:40 ID-jpechane-1561552837953-0-4
-rw-rw-r-- 1 jpechane jpechane 364 čen 26 14:40 ID-jpechane-1561552837953-0-6
-rw-rw-r-- 1 jpechane jpechane 370 čen 26 14:40 ID-jpechane-1561552837953-0-8```
```

Each file contains JSON messages like this:

```
$ jq < out/ID-jpechane-1561552837953-0-2
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
```
