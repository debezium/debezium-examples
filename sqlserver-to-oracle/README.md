# Streaming from SQL Server to Oracle

This example replicates a table from **SQL Server** into **Oracle** through Kafka,
using the Debezium SQL Server source connector and the Debezium JDBC sink connector.

```
+--------------+                 +---------+                +--------------+
|              |    Debezium     |         |    Debezium    |              |
|  SQL Server  +---------------->+  Kafka  +--------------->+    Oracle    |
|   (source)   |  SQL Server     |         |   JDBC sink    |    (sink)    |
|              |   connector     |         |   connector    |              |
+--------------+                 +---------+                +--------------+
    native CDC                                                table created
  sys.sp_cdc_*                                                by the sink
                                                          (schema evolution)
```

Oracle appears in several other examples in this repository, but always as a
*source*. Here it is the *destination*, which is a considerably simpler setup:
no LogMiner, no ARCHIVELOG mode, no supplemental logging and no `c##` common
user are required — the sink connector only needs an account that can write to
the target table.

## What this example demonstrates

* Capturing changes from SQL Server via its native CDC tables.
* Writing them into Oracle with the **Debezium** JDBC sink connector.
* Letting the sink connector create the Oracle table itself, and propagating
  the source column types so that it generates a usable schema rather than a
  column of `CLOB`s.
* Handling the cross-vendor concerns that this particular pairing raises:
  identifier case folding, Unicode text, and propagating deletes.

## Prerequisites

Docker, and enough memory for a SQL Server and an Oracle container to run side
by side (8 GB allocated to Docker is comfortable).

Both databases are available for `linux/amd64`, and Oracle additionally for
`linux/arm64`. On an arm64 host the SQL Server container therefore runs
emulated, which works but makes it noticeably slower to start.

## Running the example

```shell
# Start the topology
docker compose --env-file ../.env -f docker-compose.yaml up -d
```

**Wait for both databases before continuing.** Oracle needs a couple of
minutes to provision on a first start, and it creates the `inventory` user
towards the end of that sequence. Running the next command too early fails with
`ORA-12514` (the listener is not up yet) or `ORA-01017` (the user does not
exist yet). SQL Server is quicker, but is emulated on arm64 hosts such as Apple
Silicon, which slows it down considerably.

```shell
# Block until Oracle has finished provisioning
until docker compose --env-file ../.env -f docker-compose.yaml logs oracle \
    | grep -q "DATABASE IS READY TO USE!"; do sleep 5; done

# Block until SQL Server accepts connections
until docker compose --env-file ../.env -f docker-compose.yaml logs sqlserver \
    | grep -q "SQL Server is now ready for client connections"; do sleep 5; done
```

Then set both databases up and start the connectors:

```shell
# Create the source database, seed it and enable CDC
docker compose --env-file ../.env -f docker-compose.yaml exec sqlserver \
    /opt/mssql-tools18/bin/sqlcmd -No -S localhost -U sa -P 'Password!' -i /init/inventory.sql

# Register the SQL Server source connector
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" \
    http://localhost:8083/connectors/ -d @source-sqlserver.json

# Register the Oracle JDBC sink connector
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" \
    http://localhost:8083/connectors/ -d @sink-oracle.json

# Verify the snapshot arrived in Oracle
docker compose --env-file ../.env -f docker-compose.yaml exec oracle \
    bash -c 'echo -e "set heading off\nset pagesize 0\nset linesize 200\ncolumn first_name format a15\ncolumn email format a30\nselect first_name, email from customers order by id;\nexit;" \
    | sqlplus -s inventory/inventorypw@//localhost:1521/FREEPDB1'
```

Now change something on the source and watch it propagate:

```shell
docker compose --env-file ../.env -f docker-compose.yaml exec sqlserver \
    /opt/mssql-tools18/bin/sqlcmd -No -S localhost -U sa -P 'Password!' -d inventory \
    -Q "UPDATE customers SET first_name = N'Dana' WHERE id = 1001; DELETE FROM customers WHERE id = 1004;"
```

Shut the topology down with:

```shell
docker compose -f docker-compose.yaml down -v
```

## Notes on the configuration

### Which JDBC sink connector

The sink uses `io.debezium.connector.jdbc.JdbcSinkConnector`, which is licensed
under the Apache License 2.0. The similarly named Confluent JDBC sink connector
is *not* a drop-in replacement here: it is distributed under the Confluent
Community License, which restricts production use.

### Identifier case

Oracle folds unquoted identifiers to upper case, while Debezium emits field
names in the case used by the source database — lower case here. The target
table is therefore created with unquoted identifiers, and the sink is
configured with `"quote.identifiers": "false"` so that both sides resolve to
the same upper-case names. Quoting on one side but not the other is the usual
cause of `ORA-00942: table or view does not exist` in this setup.

### The sink creates the target table

`"schema.evolution": "basic"` is set, so no DDL is written by hand: the sink
connector issues the `CREATE TABLE` against Oracle when the first record for a
topic arrives. Two settings have to be right for that to work at all:

* `"table.name.format": "customers"`. The default is the topic name, and here
  that is `server1.inventory.dbo.customers` — the dots make it an invalid
  Oracle identifier, so auto-creation fails without an explicit name.
* `"quote.identifiers": "false"`, so the generated identifiers are unquoted and
  Oracle folds them to upper case. See *Identifier case* above.

### Propagating the source column types

The type the sink chooses depends on how much the change event tells it. A
Kafka `STRING` carries no length, so by default every text column becomes a
`CLOB`:

```
ID          NUMBER NOT NULL
FIRST_NAME  CLOB NOT NULL
LAST_NAME   CLOB NOT NULL
EMAIL       CLOB NOT NULL
```

That is usually not what you want: a `CLOB` cannot carry an ordinary B-tree
index or a unique constraint, and it is far heavier to read and write than the
`NVARCHAR2` the source column deserved.

Setting `"column.propagate.source.type": ".*"` on the *source* connector makes
Debezium attach the original type, length and scale to the change event schema
as `__debezium.source.column.type`, `.length` and `.scale`. The sink reads them
when generating DDL, and the same table comes out as:

```
ID          NUMBER NOT NULL
FIRST_NAME  NVARCHAR2(510) NOT NULL
LAST_NAME   NVARCHAR2(510) NOT NULL
EMAIL       NVARCHAR2(510) NOT NULL
PK: ID
```

Note that the width doubles: SQL Server reports the length of an
`NVARCHAR(255)` column as 510, its size in bytes, and that number is passed
through as-is. The result is correct but twice as wide as it needs to be, and
for a sufficiently wide source column the doubled value can exceed Oracle's
maximum `NVARCHAR2` size — worth checking against the widest columns in the
table you are replicating.

If you need full control over the destination schema — specific types,
tablespaces, indexes, partitioning, or simply because a connector holding DDL
privileges on a production database is not acceptable — set
`"schema.evolution": "none"` instead and create the table yourself. The sink
then only writes rows. Useful mappings in that case:

| SQL Server | Oracle | Note |
|---|---|---|
| `INT` | `NUMBER(10)` | |
| `NVARCHAR(n)` | `NVARCHAR2(n)` | Keeps Unicode text intact |
| `BIT` | `NUMBER(1)` | Oracle has no boolean type before 23c |
| `DECIMAL`/`NUMERIC` | `NUMBER` | Requires `decimal.handling.mode` on the source, see below |
| `DATETIME2` | `TIMESTAMP` | Requires a matching `time.precision.mode` |
| `UNIQUEIDENTIFIER` | `RAW(16)` or `VARCHAR2(36)` | No native equivalent |

### Unicode

With the source types propagated, the generated columns are `NVARCHAR2`, so
non-ASCII text survives the round trip regardless of the Oracle database
character set.

Without propagation the columns are `CLOB`, which stores character data in the
national character set and is likewise safe. But if you write the DDL yourself
and choose plain `VARCHAR2`, the database must be created with the `AL32UTF8`
character set — otherwise non-ASCII data is corrupted silently, with no
connector error. The Oracle image used here is `AL32UTF8`. Check with:

```sql
SELECT * FROM nls_database_parameters WHERE parameter LIKE '%CHARACTERSET%';
```

### Numeric and temporal precision

`"decimal.handling.mode": "double"` and `"time.precision.mode": "connect"` are
set on the source connector. Without them, Debezium emits decimals as
base64-encoded `bytes` and timestamps as raw epoch integers, neither of which
the sink can map onto an Oracle `NUMBER` or `TIMESTAMP` column.

### Deletes

Deletes require three settings that work together: `"tombstones.on.delete":
"true"` on the source, and `"delete.enabled": "true"` together with
`"primary.key.mode": "record_key"` on the sink. The unwrap transformation is
configured with `"drop.tombstones": "false"` so that the tombstone records
actually reach the sink. Tables without a primary key can only be replicated in
append-only mode.

### Loading large tables

The initial snapshot in this example is four rows. For a table of any real
size, streaming the snapshot through the sink connector is the slow path —
Oracle's JDBC insert throughput is far below what a bulk loader achieves.
Load the existing data with Data Pump or SQL\*Loader first, then start the
connector so that it only has to stream the changes since that point.

### Transactional consistency

Each table is replicated through its own topic, which preserves per-row
ordering but not cross-table transaction boundaries. If the Oracle side has
foreign keys between replicated tables, the sink can attempt writes in an order
that violates them. Either defer or drop those constraints on the destination,
or consume Debezium's transaction metadata topic.
