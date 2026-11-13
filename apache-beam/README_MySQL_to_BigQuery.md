# MySQL to BigQuery

Streams changes from the MySQL `inventory.customers` table into BigQuery with `DebeziumIO`.
Every change is appended as a new row, so the BigQuery table is a change log.

Source: [`MySQLToBigQuery.java`](src/main/java/io/debezium/examples/apache/beam/MySQLToBigQuery.java)

## Pipeline

1. **ReadDatabaseEvents**: reads change events from MySQL as JSON, starting with an initial snapshot.
2. **TransformToTableRow**: maps each event to a row with `metadata`, `before` and `after` (as JSON strings) and `write_time`.
3. **WriteToBigQuery**: appends the rows to the `customers` table in the dataset given by `--dataset`, in the project given by `--project`, with streaming inserts. The table must already exist.

## Run with DirectRunner

Start MySQL and the BigQuery emulator from the `apache-beam` directory:

```shell
docker compose --env-file ../.env up -d
```

Run the pipeline:

```shell
mvn compile exec:java \
  -Dexec.mainClass=io.debezium.examples.apache.beam.MySQLToBigQuery \
  -Dexec.args="\
  --runner=DirectRunner \
  --project=my-project \
  --dataset=my-dataset \
  --bigQueryEndpoint=http://localhost:9050 \
  --credentialFactoryClass=org.apache.beam.sdk.extensions.gcp.auth.NoopCredentialFactory"
```

Open http://localhost:8000 to see the rows.

Stop the pipeline with `Ctrl+C`, then:

```shell
docker compose --env-file ../.env down
```

## Run with DataflowRunner

In the class, replace `localhost` with a MySQL host the Dataflow workers can reach.

Create the table:

```shell
export PROJECT=<my-project>
export DATASET=<my_dataset>
export REGION=us-central1
export BUCKET_NAME=<bucket-name>

gcloud auth application-default login

bq mk --table $PROJECT:$DATASET.customers metadata:STRING,before:STRING,after:STRING,write_time:NUMERIC
```

Run the pipeline:

```shell
mvn compile exec:java \
  -Dexec.mainClass=io.debezium.examples.apache.beam.MySQLToBigQuery \
  -Dexec.args="\
  --runner=DataflowRunner \
  --project=$PROJECT \
  --dataset=$DATASET \
  --region=$REGION \
  --streaming \
  --jobName=mysql-to-bigquery \
  --tempLocation=gs://$BUCKET_NAME/temp \
  --maxNumWorkers=1"
```

## Query the results

The table holds every change. To get the same rows as in MySQL, keep only the latest change per customer:

```sql
WITH BASE AS (
    SELECT
        PARSE_JSON(`after`).fields a,
        PARSE_JSON(`before`).fields b,
        write_time
    FROM my-dataset.customers
)

SELECT
    LAX_INT64(a.id) id,
    STRING(a.first_name) first_name,
    STRING(a.last_name) last_name,
    STRING(a.email) email,
    write_time
FROM BASE
         QUALIFY ROW_NUMBER() OVER(PARTITION BY LAX_INT64(coalesce(a.id, b.id)) ORDER BY write_time DESC) = 1
  AND a IS NOT NULL
ORDER BY 1 ASC
```
