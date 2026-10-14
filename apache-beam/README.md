# Debezium in Apache Beam

An Apache Beam pipeline that uses `DebeziumIO` to stream changes from MySQL table into BigQuery.

## Prerequisites

* Docker and Docker Compose
* Java 21 and Maven

## Run

Start MySQL and the BigQuery emulator:

```shell
docker compose --env-file ../.env up -d
```

Start the pipeline with the Direct runner:

```shell
mvn compile exec:java -Dexec.args="\
  --runner=DirectRunner \
  --bigQueryEndpoint=http://localhost:9050 \
  --credentialFactoryClass=org.apache.beam.sdk.extensions.gcp.auth.NoopCredentialFactory"
```

Open http://localhost:8000 to see the rows, including the update.

To get the same rows as in MySQL, keep only the latest change per customer:

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

## Clean up

Stop the pipeline with `Ctrl+C`, then:

```shell
docker compose --env-file ../.env down
```

## Others

Start the pipeline with the Dataflow Runner:

In [`Main.java`](src/main/java/io/debezium/examples/apache/beam/Main.java), replace `localhost` with a MySQL address the Dataflow workers can reach.

```shell
gcloud auth application-default login

mvn compile exec:java -Dexec.args="\
    --runner=DataflowRunner \
    --project=my-project \
    --region=asia-southeast2 \
    --streaming \
    --experiments=enable_streaming_engine \
    --jobName=debezium-mysql-to-bq \
    --tempLocation=gs://my-bucket/temp \
    --stagingLocation=gs://my-bucket/staging \
    --serviceAccount=my-service-account@my-project.iam.gserviceaccount.com \
    --defaultSdkHarnessLogLevel=WARN \
    --workerMachineType=n1-standard-1 \
    --diskSizeGb=10 \
    --numWorkers=1 \
    --maxNumWorkers=1"
```
