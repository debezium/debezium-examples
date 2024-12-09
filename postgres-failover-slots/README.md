# Failover Slots with Postgres 17

This example shows the usage of Postgres 17 failover replication slots with Debezium
(Debezium 3.0.4 or newer is required).

The set-up contains a Postgres cluster with primary and read replica, fronted by pgbouncer.
Debezium connects to the cluster via this proxy.
When the primary goes down and the replica gets promoted to new primary,
connecting through the proxy allows Debezium to automatically fail over from previous primary to new without requiring to be reconfigured.

Start all the components:

```shell
export DEBEZIUM_VERSION=3.0
docker compose up
```

Get a DB session on the Postgres read replica:

```shell
docker run --tty --rm -i \
     --network failover-network \
    quay.io/debezium/tooling:1.2 \
    bash -c 'pgcli --prompt "\u@replica:\d> " postgresql://user:top-secret@postgres_replica:5432/inventorydb'
```

Add the database name to the connection info and reload the configuration of the read replica:

```sql
ALTER SYSTEM SET primary_conninfo = 'user=replicator password=''zufsob-kuvtum-bImxa6'' channel_binding=prefer host=postgres_primary port=5432 sslmode=prefer sslnegotiation=postgres sslcompression=0 sslcertmode=allow sslsni=1 ssl_min_protocol_version=TLSv1.2 gssencmode=prefer krbsrvname=postgres gssdelegation=0 target_session_attrs=any load_balance_hosts=disable dbname=inventorydb';

SELECT pg_reload_conf();
```

Register an instance of the Postgres connector. It is going to ingest changes from the current primary server (connecting through pgbouncer):

```shell
# Start Postgres connector
http PUT http://localhost:8083/connectors/inventory-source/config < inventory-source.json

# Consume messages from a Debezium topic
docker run --tty --rm \
     --network failover-network \
     quay.io/debezium/tooling:1.2 \
     kcat -b kafka:9092 -C -o beginning -q \
     -t dbserver1.inventory.customers | jq .payload

# Modify a record in the database (current primary):
docker run --tty --rm -i \
     --network failover-network \
    quay.io/debezium/tooling:1.2 \
    bash -c 'pgcli --prompt "\u@primary:\d> " postgresql://user:top-secret@postgres_primary:5432/inventorydb'

# update inventory.customers set first_name = 'Sarah' where id = 1001;
```

Next, fail over to the read replica, promoting it to new primary.
Stop the primary instance and pgbouncer:

```shell
docker compose stop postgres_primary
docker compose stop pgbouncer
```

The connector will lose the connection and enter a restart loop.
Promote the read replica to new primary:

```sql
select pg_promote();
```

Reconfigure pgbouncer and restart it:

```yaml
services:
  pgbouncer:
    image: edoburu/pgbouncer:latest
    environment:
      - DB_HOST=postgres_replica
      ...
```

```shell
docker compose up -d
```

The connector will establish the connection again,
now connecting to the new primary.
Do one more change on that Postgres node (the previous replica) and observe how the change event shows up in Kafka.

```sql
update inventory.customers set first_name = 'Rudy', last_name = 'Replica' where id = 1001;
```

Shut down the cluster:

```shell 
docker compose down
```
