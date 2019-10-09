# Building Audit Logs with Change Data Capture and a Bit of Stream Processing

This demo shows how to create persistent audit logs of an application's data using change data capture and stream processing.
There are two applications (based on [Quarkus](https://quarkus.io/)):

* _vegetables-service_: a simple REST service for inserting and updating vegetable data into a Postgres database;
as part of its processing, it will not only update its actual "business table" `vegetable`,
but also insert some auditing metadata into a dedicated metadata table `transaction_context_data`:
the user (as obtained from the passed JWT token), the client's date (as passed via the HTTP 1.1 `Date` header)
and a use case identifier (as specified in an annotation on the REST API methods).
* _log-enricher_: a Kafka Streams application,
which joins the CDC topic holding the `vegetable` change events (`dbserver1.inventory.vegetable`) with the corresponding metadata in the `dbserver1.inventory.transaction_context_data` topic sourced from the `transaction_context_data` table;
this table is keyed by transaction id, allowing for joining the vegetable `KStream` with the metadata `KTable`.
The enriched vegetable change events are written to the `dbserver1.inventory.vegetable.enriched` topic.

## Building the Demo

```console
$ mvn clean package
```

```console
$ export DEBEZIUM_VERSION=0.10
$ docker-compose up --build
```

## Deploy the Debezium Postgres Connector

```console
$ http PUT http://localhost:8083/connectors/inventory-connector/config < register-postgres.json
```

## Modifying Some Data and Observing the Audit Log

```console
$ http POST http://localhost:8080/vegetables 'Authorization: Bearer eyJraWQiOiJqd3Qua2V5IiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJmYXJtZXJib2IiLCJ1cG4iOiJmYXJtZXJib2IiLCJhdXRoX3RpbWUiOjE1NjY0NTgxMTMsImlzcyI6ImZhcm1zaG9wIiwiZ3JvdXBzIjpbImZhcm1lcnMiLCJjdXN0b21lcnMiXSwiZXhwIjo0MTAyNDQ0Nzk5LCJpYXQiOjE1NjY0NTgxMTMsImp0aSI6IjQyIn0.CscbJN8amqKryYvnVO1184J8F67HN2iTEjVN2VOPodcnoeOd7_iQVKUjC3h-ye5apkJjvAsQKrjzlrGCHRfl-n6jC9F7IkOtjoWnJ4wQ9BBo1SAtPw_Czt1I_Ujm-Kb1p5-BWACCBCVVFgYZTWP_laz5JZS7dIvs6VqoNnw7A4VpA6iPfTVfYlNY3u86-k1FvEg_hW-N9Y9RuihMsPuTdpHK5xdjCrJiD0VJ7-0eRQ8RXpycHuHN4xfmV8MqXBYjYSYDOhbnYbdQVbf0YJoFFqfb75my5olN-97ITsi2MS62W_y-RNT0qZrbytqINA3fF3VQsSY6VcaqRAeygrKm_Q' 'Date: Thu, 22 Aug 2019 08:12:31 GMT' name=Tomatoe description=Yummy!
```

This uses a pre-generated JWT token (with expiration date set to 2099-12-31 and user set to "farmerbob").
To regenerate the token with different data, use the [Jwtenizr](https://github.com/AdamBien/jwtenizr) tool under _vegetables-service/jwt_.

You can also update an existing vegetable record (this token is for "farmermargaret"):

```console
$ http PUT http://localhost:8080/vegetables/10 'Authorization: Bearer eyJraWQiOiJqd3Qua2V5IiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJmYXJtZXJtYXJnYXJldCIsInVwbiI6ImZhcm1lcm1hcmdhcmV0IiwiYXV0aF90aW1lIjoxNTY5ODM1Mzk5LCJpc3MiOiJmYXJtc2hvcCIsImdyb3VwcyI6WyJmYXJtZXJzIiwiY3VzdG9tZXJzIl0sImV4cCI6NDEwMjQ0NDc5OSwiaWF0IjoxNTY5ODM1Mzk5LCJqdGkiOiI0MiJ9.DTEUA3p-xyK5nveoJIVhjfKNFdVszYIb55Qj4Xrm70DDbAXuOU2FMkffuUAUm2s7ACkp2KEmg6brRwSjvA-zhW61kDR9ZgEb9NWeDjr6Eue08xcSODKt7SGV-M7h3yhuDIhU7uaZrxRUAQTWqm1vxd2rmN_QH0frhKMUNFFsLIOGLG0zHcLosRcwZ4tAKXSSB9VE0fth6srIQCUebDkF7ucA_WSYjPRvahCBd8JvnV4VUGQxZW8zcRhTEwcaLq20ODO-dr85xgWI2Yr_1A7PDuDL4oUjCb90YyhtzaIzs2vQMjcxJ6TWmTcqJpgCfkjE-TeVwjaafcNJu0fBmcP8jA' 'Date: Thu, 22 Aug 2019 08:12:31 GMT' name=Tomatoe description=Tasty!
```

Or delete a record (again using the "farmerbob" token):

```console
$ http DELETE http://localhost:8080/vegetables/10 'Authorization: Bearer eyJraWQiOiJqd3Qua2V5IiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJmYXJtZXJib2IiLCJ1cG4iOiJmYXJtZXJib2IiLCJhdXRoX3RpbWUiOjE1NjY0NTgxMTMsImlzcyI6ImZhcm1zaG9wIiwiZ3JvdXBzIjpbImZhcm1lcnMiLCJjdXN0b21lcnMiXSwiZXhwIjo0MTAyNDQ0Nzk5LCJpYXQiOjE1NjY0NTgxMTMsImp0aSI6IjQyIn0.CscbJN8amqKryYvnVO1184J8F67HN2iTEjVN2VOPodcnoeOd7_iQVKUjC3h-ye5apkJjvAsQKrjzlrGCHRfl-n6jC9F7IkOtjoWnJ4wQ9BBo1SAtPw_Czt1I_Ujm-Kb1p5-BWACCBCVVFgYZTWP_laz5JZS7dIvs6VqoNnw7A4VpA6iPfTVfYlNY3u86-k1FvEg_hW-N9Y9RuihMsPuTdpHK5xdjCrJiD0VJ7-0eRQ8RXpycHuHN4xfmV8MqXBYjYSYDOhbnYbdQVbf0YJoFFqfb75my5olN-97ITsi2MS62W_y-RNT0qZrbytqINA3fF3VQsSY6VcaqRAeygrKm_Q' 'Date: Thu, 22 Aug 2019 08:12:31 GMT'
```

Doing so, observe the contents of the `dbserver1.inventory.vegetable`, `dbserver1.inventory.transaction_context_data` and `dbserver1.inventory.vegetable.enriched` topics:

```console
$ docker run -it --rm \
    --network auditlog_default \
    debezium/tooling:1.0 \
    /bin/bash -c "kafkacat -b kafka:9092 \
    -C -o beginning -q -u -t dbserver1.inventory.vegetable | jq ."

$ docker run -it --rm \
    --network auditlog_default \
    debezium/tooling:1.0 \
    /bin/bash -c "kafkacat -b kafka:9092 \
    -C -o beginning -q -u -t dbserver1.inventory.transaction_context_data | jq ."

$ docker run -it --rm \
    --network auditlog_default \
    debezium/tooling:1.0 \
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

### List awaiting records to be fixed

```console
http http://localhost:8085/vegetables
```

In case there are any missing transaction context events there will be instances returned

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

this would then fix the missing event in transaction context data topic and trigger the enricher to provide
new log entry.

## Stopping All Services

```console
$ docker-compose down
```

## Running the Quarkus Applications Locally

Set `ADVERTISED_HOST_NAME` of the `kafka` service in _docker-compose.yaml_ to the IP address of your host machine.
Start all services except the `vegetables-service` and the `log-enricher`:

```console
$ docker-compose up --scale vegetables-service=0 --scale log-enricher=0
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

## Useful Commands

Getting a shell for the Postgres database:

```console
$ docker run --tty --rm -i \
    --network auditlog_default \
    debezium/tooling:1.0 \
    bash -c 'pgcli postgresql://postgresuser:postgrespw@vegetables-db:5432/vegetablesdb'
```
