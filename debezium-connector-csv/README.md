# Debezium CSV Connector

A reference implementation of a [Debezium](https://debezium.io) source connector designed to show **how to build a CDC connector** on top of the Debezium 3.x framework (the same architecture used by the PostgreSQL, SQL Server and Oracle connectors).

---

## Overview

The connector treats a single log-style file as a "database". Every line in the file represents a change event. A Java NIO `WatchService` detects when the file is modified and the connector reads newly appended lines.

This is intentionally simple so that the code serves as a guide for the framework concepts rather than a production-grade solution.

---

## File Format

```
EventType;Id:INT;FirstName:STRING;LastName:STRING;Salary:INT;Active:BOOLEAN
S|1|Alice|Smith|3000|true
S|2|Bob|Miller|2000|false
I|3|Bruce|Masters|4000|false
U|1|Sarah|Smith|3000|false
D|3|Bruce|Masters|4000|false
EventType;Id:INT;FirstName:STRING;LastName:STRING;Salary:INT;Active:BOOLEAN;Birthday:DATE
U|1|Sarah|Smith|3000|false|1980-12-31
```

### Schema header lines

A line that starts with `EventType` defines the schema for all subsequent data rows:

```
EventType;Id:INT;FirstName:STRING;LastName:STRING;Salary:INT;Active:BOOLEAN
```

- Fields are separated by `;`
- The format of each field spec is `name` or `name:TYPE`
- `EventType` is a literal keyword that identifies a header line — it is **not** a data column
- Supported types: `STRING` (default), `INT`, `LONG`, `FLOAT`, `DOUBLE`, `BOOLEAN`, `DATE` (ISO-8601, e.g. `1980-12-31`)
- A new schema header anywhere in the file triggers **schema evolution**: all subsequent rows use the new column definitions

### Data rows

```
<EventType>|<col1>|<col2>|…
```

| Character | Meaning | Debezium `op` |
|-----------|---------|---------------|
| `S` | Snapshot — initial table state | `r` (READ) |
| `I` | Insert | `c` (CREATE) |
| `U` | Update (after-image only) | `u` (UPDATE) |
| `D` | Delete | `d` (DELETE) |

---

## Kafka Topic

Each connector instance monitors one file. The topic name is:

```
<topic.prefix>.<filename-without-extension>
```

For example, with `topic.prefix=hr` and `csv.file.path=/data/employees.csv` the topic is `hr.employees`.

---

## Configuration

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| `connector.class` | yes | — | `io.debezium.connector.csv.CsvSourceConnector` |
| `topic.prefix` | yes | — | Logical server name; used as topic prefix |
| `csv.file.path` | yes | — | Absolute path to the CSV database file |

All [common Debezium connector properties](https://debezium.io/documentation/reference/stable/connectors/index.html) (poll interval, batch size, etc.) are also supported.

### Minimal example

```properties
connector.class=io.debezium.connector.csv.CsvSourceConnector
topic.prefix=hr
csv.file.path=/data/employees.csv
```

---

## Key Design Concepts

### Offset — line number

The connector tracks progress using a **line number** (zero-based index of the next line to read), stored in the Connect offset storage as `{"line": N}`.

This makes it trivial to resume after a restart and to hand off from the snapshot phase to the streaming phase.

Relevant classes: `CsvOffsetContext`, `CsvOffsetLoader`

### Snapshot vs. Streaming

```
CsvSnapshotChangeEventSource                CsvStreamingChangeEventSource
────────────────────────────                ─────────────────────────────
Read file from line 0                       Start from handover line
Process header → register schema            Watch for file modifications (WatchService)
Emit READ for every S row                   Emit CREATE / UPDATE / DELETE for I/U/D rows
Stop at first non-S, non-header line   ──►  Continue from that line
```

The `ChangeEventSourceCoordinator` orchestrates the transition: it runs the snapshot source first, saves the resulting offset, then starts the streaming source from that offset.

Relevant classes: `CsvSnapshotChangeEventSource`, `CsvStreamingChangeEventSource`, `CsvChangeEventSourceFactory`

### Schema evolution

A new schema header line mid-file causes `CsvSchema.register()` to rebuild the Kafka Connect key/value/envelope schemas. Because all value fields are declared `OPTIONAL`, existing consumers that have not yet received the new field will treat it as `null`.

Relevant class: `CsvSchema`

### Type conversion

`ColumnType` enumerates the supported column types. `CsvSnapshotChangeEventSource.convertValue()` performs the string-to-Java conversion (e.g. `"3000"` → `Integer`, `"1980-12-31"` → days-since-epoch `int`).

Relevant classes: `ColumnType`, `ColumnDef`, `CsvSchema.toKafkaSchema()`

### Source info

Every change event includes a `source` block with Debezium metadata plus two CSV-specific fields:
- `file` — name of the database file
- `line` — zero-based line number of the record

Relevant classes: `CsvSourceInfo`, `CsvSourceInfoStructMaker`

---

## Running

**1. Build the connector JAR:**

```bash
mvn package
```

Requires Java 21+ and Maven 3.

**2. Start Kafka, Kafka Connect, and the Kafka UI:**

```bash
docker compose up -d
```

**3. Register the connector:**

```bash
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" \
  http://localhost:8083/connectors/ -d @register-csv.json
```

The connector will snapshot the initial `S` rows from `src/main/resources/employees.csv` and then stream any new lines appended to the file. Browse events at [http://localhost:8080](http://localhost:8080).

---

## Sample File

A sample database file is included at `src/main/resources/employees.csv`.

---

## Relation to the Debezium Framework

| Framework concept | CSV implementation |
|-------------------|--------------------|
| `DataCollectionId` | `CsvId` (table name = filename without extension) |
| `Partition` / `Partition.Provider` | `CsvPartition` / `CsvProvider` (keyed on `topic.prefix`) |
| `OffsetContext` / `OffsetContext.Loader` | `CsvOffsetContext` / `CsvOffsetLoader` (stores `{"line": N}`) |
| `DatabaseSchema` | `CsvSchema` (lazily built from schema header lines) |
| `SnapshotChangeEventSource` | `CsvSnapshotChangeEventSource` |
| `StreamingChangeEventSource` | `CsvStreamingChangeEventSource` |
| `ChangeEventSourceFactory` | `CsvChangeEventSourceFactory` |
| `ChangeRecordEmitter` | `CsvChangeRecordEmitter` |
| `Snapshotter` | `CsvSnapshotter` (initial snapshot only) |
| `SourceInfoStructMaker` | `CsvSourceInfoStructMaker` |
