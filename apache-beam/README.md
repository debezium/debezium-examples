# Debezium in Apache Beam

Apache Beam pipelines that use `DebeziumIO` to stream changes from a MySQL table into BigQuery.

## Prerequisites

* Docker and Docker Compose
* Java 21 and Maven
* For DataflowRunner, or for the BigQuery CDC pipeline: a Google Cloud project and the `gcloud` and `bq` CLIs

## Pipelines

Each guide explains the pipeline and how to run it with DirectRunner and DataflowRunner.

| Pipeline | Description | Guide |
|---|---|---|
| [`MySQLToBigQuery`](src/main/java/io/debezium/examples/apache/beam/MySQLToBigQuery.java) | Appends every change to BigQuery as a change log. | [README_MySQL_to_BigQuery](README_MySQL_to_BigQuery.md) |
| [`MySQLToBigQueryCDC`](src/main/java/io/debezium/examples/apache/beam/MySQLToBigQueryCDC.java) | Applies each change as an upsert or delete, so BigQuery mirrors MySQL. Needs real BigQuery. | [README_MySQL_to_BigQuery_CDC](README_MySQL_to_BigQuery_CDC.md) |
