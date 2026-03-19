# Building Audit Logs with Change Data Capture and a Bit of Stream Processing

This demo shows how to create persistent audit logs of an application's data using change data capture and stream processing.
It accompanies the blog post [Building Audit Logs with Change Data Capture and Stream Processing](https://debezium.io/blog/2019/10/01/audit-logs-with-change-data-capture-and-stream-processing/).

There are two applications (based on [Quarkus](https://quarkus.io/)):

- _vegetables-service_: a simple REST service for inserting and updating vegetable data into a Postgres database;
  as part of its processing, it will not only update its actual "business table" `vegetable`,
  but also insert some auditing metadata into a dedicated metadata table `transaction_context_data`:
  the user (as obtained from the passed JWT token), the client's date (as passed via the HTTP 1.1 `Date` header)
  and a use case identifier (as specified in an annotation on the REST API methods).
- _log-enricher_: a Kafka Streams application,
  which joins the CDC topic holding the `vegetable` change events (`dbserver1.inventory.vegetable`) with the corresponding metadata in the `dbserver1.inventory.transaction_context_data` topic sourced from the `transaction_context_data` table;
  this table is keyed by transaction id, allowing for joining the vegetable `KStream` with the metadata `KTable`.
  The enriched vegetable change events are written to the `dbserver1.inventory.vegetable.enriched` topic.

## Building the Demo

```console
mvn clean package
```

```console
export DEBEZIUM_VERSION=1.8
docker-compose up --build
```

## Deploy the Debezium Postgres Connector

```console
http PUT http://localhost:8083/connectors/inventory-connector/config < register-postgres.json
```

## Modifying Some Data and Observing the Audit Log

```console
http POST http://localhost:8080/vegetables 'Authorization:Bearer eyJraWQiOiJqd3Qua2V5IiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJmYXJtZXJib2IiLCJ1cG4iOiJmYXJtZXJib2IiLCJhdXRoX3RpbWUiOjE1NjY0NTgxMTMsImlzcyI6ImZhcm1zaG9wIiwiZ3JvdXBzIjpbImZhcm1lcnMiLCJjdXN0b21lcnMiXSwiZXhwIjo0MTAyNDQ0Nzk5LCJpYXQiOjE1NjY0NTgxMTMsImp0aSI6IjQyIn0.CscbJN8amqKryYvnVO1184J8F67HN2iTEjVN2VOPodcnoeOd7_iQVKUjC3h-ye5apkJjvAsQKrjzlrGCHRfl-n6jC9F7IkOtjoWnJ4wQ9BBo1SAtPw_Czt1I_Ujm-Kb1p5-BWACCBCVVFgYZTWP_laz5JZS7dIvs6VqoNnw7A4VpA6iPfTVfYlNY3u86-k1FvEg_hW-N9Y9RuihMsPuTdpHK5xdjCrJiD0VJ7-0eRQ8RXpycHuHN4xfmV8MqXBYjYSYDOhbnYbdQVbf0YJoFFqfb75my5olN-97ITsi2MS62W_y-RNT0qZrbytqINA3fF3VQsSY6VcaqRAeygrKm_Q' 'Date:Thu, 22 Aug 2019 08:12:31 GMT' name=Tomatoe description=Yummy!
```

This uses a pre-generated JWT token (with expiration date set to 2099-12-31 and user set to "farmerbob").
To regenerate the token with different data, use the [Jwtenizr](https://github.com/AdamBien/jwtenizr) tool under _vegetables-service/jwt_.

You can also update an existing vegetable record (this token is for "farmermargaret"):

```console
http PUT http://localhost:8080/vegetables/10 'Authorization:Bearer eyJraWQiOiJqd3Qua2V5IiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJmYXJtZXJtYXJnYXJldCIsInVwbiI6ImZhcm1lcm1hcmdhcmV0IiwiYXV0aF90aW1lIjoxNTY5ODM1Mzk5LCJpc3MiOiJmYXJtc2hvcCIsImdyb3VwcyI6WyJmYXJtZXJzIiwiY3VzdG9tZXJzIl0sImV4cCI6NDEwMjQ0NDc5OSwiaWF0IjoxNTY5ODM1Mzk5LCJqdGkiOiI0MiJ9.DTEUA3p-xyK5nveoJIVhjfKNFdVszYIb55Qj4Xrm70DDbAXuOU2FMkffuUAUm2s7ACkp2KEmg6brRwSjvA-zhW61kDR9ZgEb9NWeDjr6Eue08xcSODKt7SGV-M7h3yhuDIhU7uaZrxRUAQTWqm1vxd2rmN_QH0frhKMUNFFsLIOGLG0zHcLosRcwZ4tAKXSSB9VE0fth6srIQCUebDkF7ucA_WSYjPRvahCBd8JvnV4VUGQxZW8zcRhTEwcaLq20ODO-dr85xgWI2Yr_1A7PDuDL4oUjCb90YyhtzaIzs2vQMjcxJ6TWmTcqJpgCfkjE-TeVwjaafcNJu0fBmcP8jA' 'Date:Thu, 22 Aug 2019 08:12:31 GMT' name=Tomatoe description=Tasty!
```

Or delete a record (again using the "farmerbob" token):

```console
http DELETE http://localhost:8080/vegetables/10 'Authorization:Bearer eyJraWQiOiJqd3Qua2V5IiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJmYXJtZXJib2IiLCJ1cG4iOiJmYXJtZXJib2IiLCJhdXRoX3RpbWUiOjE1NjY0NTgxMTMsImlzcyI6ImZhcm1zaG9wIiwiZ3JvdXBzIjpbImZhcm1lcnMiLCJjdXN0b21lcnMiXSwiZXhwIjo0MTAyNDQ0Nzk5LCJpYXQiOjE1NjY0NTgxMTMsImp0aSI6IjQyIn0.CscbJN8amqKryYvnVO1184J8F67HN2iTEjVN2VOPodcnoeOd7_iQVKUjC3h-ye5apkJjvAsQKrjzlrGCHRfl-n6jC9F7IkOtjoWnJ4wQ9BBo1SAtPw_Czt1I_Ujm-Kb1p5-BWACCBCVVFgYZTWP_laz5JZS7dIvs6VqoNnw7A4VpA6iPfTVfYlNY3u86-k1FvEg_hW-N9Y9RuihMsPuTdpHK5xdjCrJiD0VJ7-0eRQ8RXpycHuHN4xfmV8MqXBYjYSYDOhbnYbdQVbf0YJoFFqfb75my5olN-97ITsi2MS62W_y-RNT0qZrbytqINA3fF3VQsSY6VcaqRAeygrKm_Q' 'Date:Thu, 22 Aug 2019 08:12:31 GMT'
```

Doing so, observe the contents of the `dbserver1.inventory.vegetable`, `dbserver1.inventory.transaction_context_data` and `dbserver1.inventory.vegetable.enriched` topics:

```console
$ docker run -it --rm \
    --network auditlog_default \
    quay.io/debezium/tooling:1.2 \
    /bin/bash -c "kafkacat -b kafka:9092 \
    -C -o beginning -q -u -t dbserver1.inventory.vegetable | jq ."

$ docker run -it --rm \
    --network auditlog_default \
    quay.io/debezium/tooling:1.2 \
    /bin/bash -c "kafkacat -b kafka:9092 \
    -C -o beginning -q -u -t dbserver1.inventory.transaction_context_data | jq ."

$ docker run -it --rm \
    --network auditlog_default \
    quay.io/debezium/tooling:1.2 \
    /bin/bash -c "kafkacat -b kafka:9092 \
    -C -o beginning -q -u -t dbserver1.inventory.vegetable.enriched | jq ."
```

## Administrate missing events

There could be situations when there are missing transaction context data, for example
when there is a manual update done on database. In such a case the enricher will not
be able to continue due to missing corresponding event with context data.

And here comes Kogito - a cloud native business automation toolkit to build intelligent business
application based on battle tested capabilities. In other words, it brings business processes and
rules to solve particular business problems. In this case the business problem is blocked log enrichment.

What Kogito helps us with is to define our logic to understand what might get wrong, what needs to be
done to resolve it and what are the conditions that can lead to both problem and resolution. In this
particular case we use both processes and rules to make sure we get the context right and react to
the events behind the vegetable service.

An admin service provided here can help with fixing such situation by manually filling in
the missing details

- use case
- user name

Admin service is consuming messages from both topics and attempts to correlate them,
in case there is no data to match within specific amount of time - 2 seconds by default it will
create a task for administrator to provide the missing data.

### Produce data changes without transaction metadata

```console
$ docker run --tty --rm -i \
        --network auditlog_default \
        quay.io/debezium/tooling:1.2 \
        bash -c 'pgcli postgresql://postgresuser:postgrespw@vegetables-db:5432/vegetablesdb'
```

Insert a new vegetable record; as we're bypassing the application, there'll be no corresponding transaction metadata record:

```sql
insert into inventory.vegetable (id, description, name) values (nextval('inventory.vegetables_id_seq'), 'Tasty!', 'Banana');
```

In the logs of the _enricher_ service, you'll see warnings about the unprocessable event.

### List awaiting records to be fixed

```console
http http://localhost:8085/vegetables
```

In case there are any missing transaction context events there will be instances returned;
you should see the "Banana" event in this case.

### Get list of tasks to provide missing data

```console
http http://localhost:8085/vegetables/{uuid}/tasks
```

`{uuid}` is taken from the `id` attribute of the instances returned in previous step.

This will return name of the task `auditData` and the id of the task

```console
http POST http://localhost:8085/vegetables/{uuid}/auditData/{tuuid} audit:='{"usecase":"CREATE VEGETABLE", "user_name" : "farmerjohn"}'
```

`{uuid}` is same as in the previous call and `{tuuid}` is the id of the task returned in previous call.

This would then fix the missing event in the transaction context data topic and trigger the enricher to provide a new log entry.

## Fowarding Events to a Downstream Postgres Database

```console
$ docker run -it --rm \
    --network auditlog_default \
    quay.io/debezium/tooling:1.2 \
    /bin/bash -c "kafkacat -b kafka:9092 \
    -C -o beginning -q -u -t dbserver1.inventory.vegetable.enriched | jq ."
```

This will show you the enriched events fowarded to the `dbserver1.inventory.vegetable.enriched` topic.
We need to pass this events to lightweight `postgres-sink` database defined in `docker-compose.yaml` using the [Debezium JDBC connector](https://debezium.io/documentation/reference/stable/connectors/jdbc.html).

Download the connector plugin `.tar.gz` from the Debzium [plugin archive](https://repo1.maven.org/maven2/io/debezium/debezium-connector-jdbc/) and save it to the `/config/plugins/debezium-connector-jdbc` directory.

Restart your Kafka connect container to pick up the new plugin

```console
docker compose down -v connect
docker compose up -d connect
```

Create your debezium sink connector by running

```console
http POST http://localhost:8083/connectors < connector-config/config/jdbc-connector-config.json
```

The connector should read from the `dbserver1.inventory.vegetable.enriched` topic and populate the `dbserver1_inventory_vegetable` table.

Confirm the changes by running the `exec` command into your `postgres-sink` container

```console
docker compose exec postgres-sink psql -U postgresuser -d postgres
```

View the rows in your `dbserver1_inventory_vegetable` table:

```sql
SELECT * FROM dbserver1_inventory_vegetable;
```

An exmaple of the rows should be like this:

```console
| __connect_topic                          | __connect_partition | __connect_offset | id | description | name    | __deleted | op | lsn      | ts_ms         | tx_id | client_date      | usecase           | user_name       |
|------------------------------------------|---------------------|------------------|----|-------------|---------|-----------|----|----------|---------------|-------|------------------|-------------------|-----------------|
| dbserver1.inventory.vegetable.enriched   | 0                   | 0                | 10 | Yummy!      | Tomatoe | false     | c  | 36689976 | 1773921791527 | 769   | 1566461551000000 | CREATE VEGETABLE  | farmerbob       |
| dbserver1.inventory.vegetable.enriched   | 0                   | 2                | 10 | Yummy!      | Tomatoe | false     | c  | 36689976 | 1773921791527 | 769   | 1566461551000000 | CREATE VEGETABLE  | farmerbob       |
| dbserver1.inventory.vegetable.enriched   | 0                   | 4                | 10 | Tasty!      | Tomatoe | false     | u  | 36690432 | 1773921804910 | 770   | 1566461551000000 | UPDATE VEGETABLE  | farmermargaret  |
| dbserver1.inventory.vegetable.enriched   | 0                   | 5                | 10 |             |         | true      | d  | 36690800 | 1773921809082 | 771   | 1566461551000000 | DELETE VEGETABLE  | farmerbob       |
```

With this our audit log events have been successfully propagated to our downstream database.

The table `dbserver_inventory_vegetable` can be defined as:

```sql
-- This version is used with the Debezium JDBC Sink Connector
CREATE TABLE dbserver1_inventory_vegetable_enriched (
    -- Business Data
    id INTEGER,
    name VARCHAR(255) NULL,
    description TEXT NULL,

    -- Audit Metadata
    tx_id VARCHAR(255),
    user_name VARCHAR(255) NULL,
    usecase VARCHAR(255) NULL,
    client_date BIGINT NULL,

    -- Change Metadata (for uniqueness)
    op VARCHAR(1),
    lsn BIGINT,
    ts_ms BIGINT,

    -- Kafka Metadata to prevent squashing of events
    __connect_partition INTEGER,
    __connect_offset BIGINT,
    __connect_topic VARCHAR(255),

    -- To ensure that all events are kept we will use kafka metadata to
    -- To ensure that all events are kept we will use kafka metadata to 
    -- identify unique rows
PRIMARY KEY (__connect_offset, __connect_partition, __connect_topic)
);

```

## Running the Quarkus applications locally

Set `ADVERTISED_HOST_NAME` of the `kafka` service in _docker-compose.yaml_ to the IP address of your host machine.
Start all services except the `vegetables-service` and the `log-enricher`:

```console
docker-compose up --scale vegetables-service=0 --scale log-enricher=0
```

Then start the three services via the Quarkus dev mode:

```console
mvn compile quarkus:dev -f vegetables-service/pom.xml
```

```console
$ mvn compile quarkus:dev -f log-enricher/pom.xml \
    -Dquarkus.kafka-streams.bootstrap-servers=<IP_OF_YOUR_HOST_MACHINE>:9092 \
    -Dquarkus.http.port=8081
```

```console
$ mvn compile quarkus:dev -f admin-service/pom.xml \
    -Dmp.messaging.incoming.transactions.bootstrap-servers=<IP_OF_YOUR_HOST_MACHINE>:9092 \
    -Dmp.messaging.incoming.vegetables.bootstrap-servers=<IP_OF_YOUR_HOST_MACHINE>:9092 \
    -Dmp.messaging.outgoing.missingtransactions.bootstrap-servers=<IP_OF_YOUR_HOST_MACHINE>:9092 \
    -Dquarkus.http.port=8085
```
