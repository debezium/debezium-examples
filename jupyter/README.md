# PostgreSQL CDC Jupyter Demo

This demo runs Debezium Engine (via `pydbzengine`) inside a Jupyter notebook, captures PostgreSQL change events from the Debezium tutorial database, stores them in pandas, and shows change counts per primary key.

## What is included

- `docker-compose.yaml`: starts PostgreSQL test database + Jupyter Lab
- `postgres_cdc_pk_change_counts.ipynb`: interactive CDC notebook
- `Dockerfile`: Jupyter image with OpenJDK 21 + Python dependencies

## Prerequisites

- Docker with Compose support
- Free local ports:
  - `5432` (PostgreSQL)
  - `8888` (Jupyter)

## Start the demo

From this directory:

```bash
docker compose up --build
```

When services are ready, open:

- `http://localhost:8888`

No token/password is required in this local setup.

## Use the notebook

Open:

- `postgres_cdc_pk_change_counts.ipynb`

Run cells top-to-bottom once, then use the button cell:

- `Start Engine`: starts CDC in background thread
- `Run Sample SQL`: generates updates in `inventory.customers`
- `Stop Engine`: stops CDC engine

Refresh views by re-running:

- snapshot cell (`Captured X records so far`)
- aggregation cell (`change_counts`)

## Typical workflow

1. Run setup/config cells.
2. Click `Start Engine`.
3. Click `Run Sample SQL` one or more times.
4. Re-run snapshot and aggregation cells to see updates.
5. Click `Stop Engine` when done.

## Configuration

`docker-compose.yaml` passes environment variables to the notebook, including:

- `PG_HOST`, `PG_PORT`, `PG_USER`, `PG_PASSWORD`, `PG_DBNAME`
- `TABLE_INCLUDE_LIST` (default `inventory.customers`)
- `SCHEMA_INCLUDE_LIST` (default `inventory`)
- `TOPIC_PREFIX`, `SLOT_NAME`, `PLUGIN_NAME`, `SNAPSHOT_MODE`

Adjust these values in `docker-compose.yaml` if needed.

## Stop and clean up

Stop services:

```bash
docker compose down
```

Stop and remove volumes:

```bash
docker compose down -v
```

## Notes

- The notebook intentionally keeps the Debezium engine running in background until `Stop Engine` is clicked.
- The DataFrame is built from in-memory captured records, so restarting the kernel resets in-notebook state.
