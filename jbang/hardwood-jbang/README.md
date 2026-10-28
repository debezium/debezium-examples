# JBang CDC Pipeline with Hardwood Parquet

This example shows how to build a CDC pipeline from PostgreSQL using the [Debezium Quarkus extension](https://debezium.io/documentation/reference/stable/integrations/quarkus-debezium-engine-extension.html) and write change events to [Apache Parquet](https://parquet.apache.org/) files using [Hardwood](https://hardwood.dev/), all in a single [JBang](https://www.jbang.dev/) script.

## Topology

* PostgreSQL database with a `customers` table
* A JBang script that runs the Debezium Quarkus extension to capture snapshot events and writes them to Parquet via Hardwood

## Prerequisites

* [JBang](https://www.jbang.dev/download/) installed
* Docker / Docker Compose
* Java 21+

## How to run

Start the PostgreSQL database:

```
$ docker compose up -d
```

Run the JBang script:

```
$ jbang DebeziumParquet.java
```

Debezium connects to PostgreSQL, reads the three customer rows via the initial snapshot, and writes them to `parquet-output/customers.parquet`.

Inspect the output with [DuckDB](https://duckdb.org/) or any Parquet reader:

```
$ duckdb -c "SELECT * FROM 'parquet-output/customers.parquet'"
┌───────┬────────────┬───────────┬─────────────────────────────┐
│  id   │ first_name │ last_name │            email            │
│ int32 │  varchar   │  varchar  │           varchar           │
├───────┼────────────┼───────────┼─────────────────────────────┤
│     1 │ Alice      │ Smith     │ alice.smith@example.com     │
│     2 │ Bob        │ Johnson   │ bob.johnson@example.com     │
│     3 │ Charlie    │ Brown     │ charlie.brown@example.com   │
└───────┴────────────┴───────────┴─────────────────────────────┘
```

## Stop

```
$ docker compose down
```
