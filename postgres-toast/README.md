# Dealing With Postgres TOAST Column Values

PostgreSQL has a hard limit on the page size.
This means that values larger than ca. 8 KB need to be stored using TOAST storage.
This impacts replication messages coming from database,
as the values that were stored using the TOAST mechanism and have not been changed are not included in the message,
unless they are part of the table’s replica identity.

The corresponding fields in Debezium change events will have configurable marker value in this case
(defaulting to `__debezium_unavailable_value`).
This demo shows two ways for handling such marker values:

* ignoring any updates that'd set a column in a sink database to that marker value by means of a trigger installed in the sink database
* keeping track of the latest value of a TOAST column by means of a stateful Kafka Streams application and putting this value back into change events containing the marker value

Further strategies could be to produce dynamic updates in sink datastores (ignoring the column from update statements if the value is the marker value) or adding the affected column to the source table's replica identity,
either by using replica identity `full`  or an index-based replica idenity.

## Building the Demo

Be sure to work with the latest Debezium and Postgres container images for the following.

```console
mvn clean install -f toast-value-store/pom.xml

export DEBEZIUM_VERSION=1.2
docker-compose up --build
```

Then register an instance of the Debezium Postgres connector and an instance of the JDBC sink connector:

```console
http PUT http://localhost:8083/connectors/inventory-connector/config < debezium-source.json
http PUT http://localhost:8083/connectors/sink-connector/config < jdbc-sink.json
```

Change a record in the `customers` table by updating one column (which isn't a TOAST column):

```console
docker run --tty --rm -i \
    --network postgres-toast_default \
    debezium/tooling:1.1 \
    bash -c 'pgcli postgresql://postgresusersource:postgrespw@source-db:5432/sourcedb'
```

```console
sourcedb> update inventory.customers set first_name = 'Dana' where id = 1001;
```

Observe the marker value in the `biography` field of corresponding change events:

```console
docker run -it --rm \
    --network postgres-toast_default \
    debezium/tooling:1.1 \
    /bin/bash -c "kafkacat -b kafka:9092 \
    -C -o beginning -q -u -t dbserver1.inventory.customers | jq ."
```

Observe how the update to the `first_name` value of the corresponding sink record has been applied,
whereas the `biography` column remains unchanged:

```console
docker run --tty --rm -i \
    --network postgres-toast_default \
    debezium/tooling:1.1 \
    bash -c 'pgcli postgresql://postgresusersink:postgrespw@sink-db:5432/sinkdb'
```

```console
sinkdb> select id, first_name, last_name, email, LEFT(biography,50) from inventorysink.customers where id = 1001;
```

This is done by means of a trigger on the `biography` column in the sink database table.
This trigger will keep the original column value in case any update would change it to the special marker value (see _sink-db/schema-update.sql_).

## Making TOAST column value available via Kafka Streams

An alternative approach to dealing with unchanged TOAST column values is a stateful Kafka Streams application,
which stores the latest value of such column (as obtained from a snapshot or insert event) in a statestore and
puts that value back into change events with the marker value for the TOAST column.

As the change events for one particular record are always processed in the exact same order as they were created,
it is ensured that the latest value is available in the statestore when receiving a change event with the marker value.

```console
docker run --tty --rm -i \
    --network postgres-toast_default \
    debezium/tooling:1.1 \
    bash -c 'pgcli postgresql://postgresusersource:postgrespw@source-db:5432/sourcedb'
```

```console
sourcedb> update inventory.products set description = 'Much wow' where id = 101;
```

```console
docker run -it --rm \
    --network postgres-toast_default \
    debezium/tooling:1.0 \
    /bin/bash -c "kafkacat -b kafka:9092 \
    -C -o beginning -q -u -t dbserver1.inventory.products | jq ."
```

```console
docker run -it --rm \
    --network postgres-toast_default \
    debezium/tooling:1.1 \
    /bin/bash -c "kafkacat -b kafka:9092 \
    -C -o beginning -q -u -t dbserver1.inventory.products.enriched | jq ."
```

See the source code under _toast-value-store_ for the implementation of the stream processing application.

## Running the Quarkus Application Locally

Set `ADVERTISED_HOST_NAME` of the `kafka` service in _docker-compose.yaml_ to the IP address of your host machine.
Start all services except the `toast-value-store`:

```console
$ docker-compose up --scale toast-value-store=0
```

Then start the `toast-value-store` service via the Quarkus dev mode:

```console
mvn compile quarkus:dev -f toast-value-store/pom.xml
```

## Clean Up

```console
docker-compose down
```
