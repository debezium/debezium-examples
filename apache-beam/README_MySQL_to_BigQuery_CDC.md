# MySQL to BigQuery CDC

Streams changes from the MySQL `inventory.customers` table into BigQuery with `DebeziumIO`.
Each change is applied as an upsert or a delete with [BigQuery change data capture (CDC)](https://cloud.google.com/bigquery/docs/change-data-capture), so the BigQuery table mirrors the MySQL table.
In Beam, BigQuery CDC is set per row with `RowMutationInformation`.

Source: [`MySQLToBigQueryCDC.java`](src/main/java/io/debezium/examples/apache/beam/MySQLToBigQueryCDC.java)

> [!WARNING]
> This pipeline does not work with the BigQuery emulator from `docker-compose.yaml`, because the emulator doesn't support BigQuery change data capture.
> Use a real BigQuery dataset for both runners.

## Pipeline

1. **ReadDatabaseEvents**: reads change events from MySQL as JSON, taking a fresh snapshot on every start.
2. **TransformToTableRow**: maps each event to a `customers` row (`id`, `first_name`, `last_name`, `email`). Snapshot reads and inserts become inserts, updates become upserts, and deletes become deletes.
3. **WriteToBigQuery**: writes the rows with the Storage Write API. The binlog position is the change sequence number, so BigQuery applies the changes in order. If the table doesn't exist, it is created with `id` as the primary key.

## Before you start

The pipeline writes to the `customers` table in the dataset given by `--dataset`, in the project given by `--project`.
Set them and create the dataset:

```shell
export PROJECT=<my-project>
export DATASET=<my_dataset>
export REGION=us-central1
export BUCKET_NAME=<bucket-name>

gcloud auth application-default login

bq mk --dataset $PROJECT:$DATASET
```

## Run with DirectRunner

Start MySQL from the `apache-beam` directory:

```shell
docker compose --env-file ../.env up -d mysql
```

Run the pipeline:

```shell
mvn compile exec:java \
  -Dexec.mainClass=io.debezium.examples.apache.beam.MySQLToBigQueryCDC \
  -Dexec.args="\
  --runner=DirectRunner \
  --project=$PROJECT \
  --dataset=$DATASET"
```

Query `$PROJECT.$DATASET.customers` in the BigQuery console to see the rows.

Stop the pipeline with `Ctrl+C`, then:

```shell
docker compose --env-file ../.env down
```

## Run with DataflowRunner

Replace `localhost` in the class with a MySQL host the Dataflow workers can reach, then run:

```shell
mvn compile exec:java \
  -Dexec.mainClass=io.debezium.examples.apache.beam.MySQLToBigQueryCDC \
  -Dexec.args="\
  --runner=DataflowRunner \
  --project=$PROJECT \
  --dataset=$DATASET \
  --region=$REGION \
  --streaming \
  --jobName=mysql-to-bigquery-cdc \
  --tempLocation=gs://$BUCKET_NAME/temp \
  --maxNumWorkers=1"
```
