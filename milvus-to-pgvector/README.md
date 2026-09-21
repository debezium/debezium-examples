# Streaming from Milvus to PostgreSQL (pgvector)

This example replicates a collection from **Milvus**, a vector database, into **PostgreSQL**
with the [pgvector](https://github.com/pgvector/pgvector) extension through Kafka, using the
Debezium Milvus source connector and the Debezium JDBC sink connector.

![Milvus to PostgreSQL with pgvector, streaming through Kafka](flow.png)

## What this example demonstrates

* Capturing changes from a Milvus collection: an initial snapshot, followed by streaming of
  the inserts and deletes that Milvus writes to its message queue.
* Writing them into PostgreSQL with the Debezium JDBC sink connector, which consumes Debezium
  change events directly and therefore needs no transformations.
* Letting the sink connector create the target table, with the `FloatVector` logical type of the
  embeddings mapped to a native pgvector column.
* How a Milvus upsert surfaces on the topic: a delete, a tombstone and an insert committed under
  one timestamp, which leave a single row in PostgreSQL.
* That the replicated embeddings keep their meaning: a Milvus ANN search and a pgvector `<=>`
  cosine distance search return the same nearest neighbours.
* That the source connector can be paused and resumed without losing changes, because it picks
  the message queue up at the offset it had committed.

## Prerequisites

Docker with about 8 GB of memory: Milvus runs together with its etcd and MinIO dependencies,
next to Kafka, Kafka Connect and PostgreSQL.

`test.yaml` covers every step below and is run by the `milvus-to-pgvector` GitHub workflow. To
run it locally, use `python3 scripts/run-example-test.py milvus-to-pgvector` from the repository
root. It unpacks the connector plugin itself.

## Running the example

### Get the Milvus connector

The [Milvus connector](https://github.com/debezium/debezium-connector-milvus) is incubating and
not part of the Debezium Connect image yet, so unpack its plugin archive into `./plugins`, from
where the Compose file mounts it into the Connect container:

```shell
mkdir plugins
curl -sSL https://repo1.maven.org/maven2/io/debezium/debezium-connector-milvus/3.7.0.Beta2/debezium-connector-milvus-3.7.0.Beta2-plugin.tar.gz \
    | tar -xz -C plugins
```

### Start the topology

```shell
docker compose --env-file ../.env -f docker-compose.yaml up -d
```

Milvus takes about a minute to start. It is ready when its health endpoint answers `OK`:

```shell
curl localhost:9091/healthz
```

By then Kafka Connect should list both plugins:

```shell
curl localhost:8083/connector-plugins
```

Look for `MilvusConnector` and `JdbcSinkConnector` in the output. If `MilvusConnector` is missing,
`./plugins/debezium-connector-milvus` is empty: Docker creates an empty directory for a missing
bind mount source instead of failing.

### Prepare source and sink

Create the `products` collection in Milvus and insert four rows. The collection has to exist and
hold data before the connector starts, otherwise there is nothing to snapshot:

```shell
./seed-milvus.sh
```

Now open a `psql` session in a second terminal; the rest of the example uses it:

```shell
docker compose --env-file ../.env -f docker-compose.yaml exec postgres psql -U postgres
```

Enable pgvector, so that the sink connector can create a vector column:

```sql
CREATE EXTENSION vector;
```

### Register the connectors

```shell
# Register the Milvus source connector
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" \
    http://localhost:8083/connectors/ -d @source-milvus.json

# Register the PostgreSQL JDBC sink connector
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" \
    http://localhost:8083/connectors/ -d @sink-postgres.json
```

The source connector snapshots the collection into the topic `milvus.default.products`. The sink
connector creates the `products` table from the schema of the first event and writes the rows.
After a few seconds the table is there in `psql`, with a native vector column:

```sql
\d products
```

```
               Table "public.products"
  Column   |  Type   | Collation | Nullable | Default
-----------+---------+-----------+----------+---------
 pk        | bigint  |           | not null |
 name      | text    |           |          |
 category  | text    |           |          |
 price     | real    |           |          |
 embedding | halfvec |           |          |
Indexes:
    "products_pkey" PRIMARY KEY, btree (pk)
```

And it holds the four rows from the snapshot:

```sql
SELECT * FROM products ORDER BY pk;
```

### Change data in Milvus

Milvus has no SQL shell, so changes are made through its REST API. Insert a product, upsert it
with a new price, and delete another one:

```shell
curl -X POST localhost:19530/v2/vectordb/entities/insert -H 'Content-Type: application/json' \
    -d '{"collectionName": "products", "data": [{"pk": 1005, "name": "camping stove", "category": "outdoor", "price": 59.99, "embedding": [0.1, 0.3, 0.3, 0.5]}]}'

curl -X POST localhost:19530/v2/vectordb/entities/upsert -H 'Content-Type: application/json' \
    -d '{"collectionName": "products", "data": [{"pk": 1005, "name": "camping stove", "category": "outdoor", "price": 49.99, "embedding": [0.1, 0.3, 0.3, 0.5]}]}'

curl -X POST localhost:19530/v2/vectordb/entities/delete -H 'Content-Type: application/json' \
    -d '{"collectionName": "products", "filter": "pk == 1004"}'
```

Within a few seconds PostgreSQL holds product 1005 at the new price, and product 1004 is gone:

```sql
SELECT pk, name, price FROM products ORDER BY pk;
```

### How the upsert surfaces on the topic

Milvus has no update operation. The upsert above is a delete of `pk` 1005 followed by an insert
of the new version, both committed under one timestamp oracle value (TSO), and the connector
emits exactly that pair: an `op=d` event, a tombstone for the deleted key, and an `op=c` event,
in that order. No `op=u` event appears on the topic, for any key.

```shell
docker compose --env-file ../.env -f docker-compose.yaml exec kafka \
    /kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 \
    --topic milvus.default.products --from-beginning --property print.key=true
```

The tombstone in between carries no value at all, but the delete and the insert around it carry
the same `tso` in their `source` block, which is what makes them one commit rather than an
unrelated delete and insert. `test.yaml` reads the same topic and asserts that shape: that the last three events for the key
are a delete, a tombstone and an insert, in that order, and that the delete and the insert carry
one `source.tso`.

Applied in order the pair leaves a single row, not two:

```sql
SELECT count(*) FROM products WHERE pk = 1005;
```

### Pause the source and resume it

Pausing the source connector stops it from producing, while the offsets it has committed stay
where they are:

```shell
curl -i -X PUT localhost:8083/connectors/milvus-source/pause
```

Write two more products while it is paused. They land in Milvus, but nothing reaches PostgreSQL:

```shell
curl -X POST localhost:19530/v2/vectordb/entities/insert -H 'Content-Type: application/json' \
    -d '{"collectionName": "products", "data": [
          {"pk": 1006, "name": "espresso machine", "category": "kitchen", "price": 249.00, "embedding": [0.8, 0.2, 0.1, 0.2]},
          {"pk": 1007, "name": "yoga mat",         "category": "fitness", "price": 39.50,  "embedding": [0.3, 0.9, 0.2, 0.1]}]}'
```

```sql
SELECT count(*) FROM products WHERE pk IN (1006, 1007);
```

Resuming makes the connector pick the Milvus message queue up at the offset it had committed, so
the writes made during the pause are streamed after all:

```shell
curl -i -X PUT localhost:8083/connectors/milvus-source/resume
```

Within a few seconds both sides hold the same rows again, the two written during the pause
included:

```sql
SELECT pk, name FROM products ORDER BY pk;
```

### The embeddings still find the same neighbours

A vector is only replicated faithfully if it still answers the same question. Put one query
vector to both sides: to Milvus as an ANN search over its `COSINE` index, and to PostgreSQL as an
`ORDER BY` over pgvector's `<=>` cosine distance operator on the column the sink created.

```shell
curl -X POST localhost:19530/v2/vectordb/entities/search -H 'Content-Type: application/json' \
    -d '{"collectionName": "products", "data": [[0.1, 0.2, 0.3, 0.4]], "annsField": "embedding", "limit": 3, "outputFields": ["pk", "name"]}'
```

```sql
SELECT pk, name FROM products ORDER BY embedding <=> '[0.1,0.2,0.3,0.4]' LIMIT 3;
```

Milvus returns cosine similarity and ranks it descending, pgvector returns cosine distance and
ranks it ascending, so both put the nearest neighbour first. The two rankings match: `1001`,
`1005`, `1003`.

### Shut down

```shell
docker compose --env-file ../.env -f docker-compose.yaml down -v
```

## Notes on the configuration

### Milvus must use Kafka as its message queue

The connector reads the change stream from the message queue Milvus writes it to, and supports
Kafka only; Milvus deployments backed by Pulsar or RocksMQ cannot be captured. `milvus-user.yaml`
configures Milvus accordingly, pointing it at the same broker Kafka Connect uses. It also limits
Milvus to a single DML channel, so that the collection is on the channel `by-dev-rootcoord-dml_0`
that `milvus.pchannel.name` names; a connector instance consumes one channel.

The connector also needs access to the etcd instance behind Milvus, where Milvus stores the
checkpoint per channel that the snapshot and the start of streaming are anchored on.

`milvus.wire.format` is set to `proto_single`, the message format Milvus 2.5 uses on Kafka,
instead of relying on the connector's auto-detection.

### The sink creates the target table

`"schema.evolution": "basic"` is set, so the sink connector issues the `CREATE TABLE` when the
first record arrives. `"collection.name.format": "products"` names the table, because the default,
the topic name `milvus.default.products`, is not a valid table name.

The `embedding` field carries the `io.debezium.data.FloatVector` logical type, which the sink
maps to pgvector's `halfvec` type, a vector of 16-bit floats. If you need the 32-bit `vector`
type instead, create the table yourself before registering the sink; with `basic` schema
evolution the connector then only adds missing columns.

### Upserts and deletes

Milvus implements an upsert as a delete followed by an insert with the same commit timestamp,
and the connector emits exactly that pair; there are no update (`op=u`) events. Delete events
carry a primary-key-only `before` image, since Milvus does not publish the previous state of a
deleted entity.

`"insert.mode": "upsert"` makes the sink apply snapshot and insert events as
`INSERT ... ON CONFLICT DO UPDATE`, and `"delete.enabled": "true"` together with
`"primary.key.mode": "record_key"` turns delete events into `DELETE` statements keyed by the
record key. Applied in order, the pair emitted for a Milvus upsert leaves a single updated row.

### Offsets survive a pause

The connector records its position in the Milvus message queue in the Connect offset topic
(`my_connect_offsets` here), and only after it has emitted the event that position belongs to.
Pausing the connector leaves that offset untouched, so a resumed connector continues from it
rather than snapshotting again. The handoff is at-least-once: a resumed connector can re-emit
the last few events, which is harmless here because `"insert.mode": "upsert"` makes the sink
idempotent.

### No transformations

The Debezium JDBC sink connector understands Debezium's change event envelope, so it needs no
`ExtractNewRecordState` transformation to unwrap it. The Connect worker's default JSON converters,
with schemas enabled, are used as they are; the sink derives the primary key from the schema of the
record key.
