#!/bin/bash

source ./env.sh

# start topo server
CELL=zone1 ./scripts/etcd-up.sh

# start vtctld
CELL=zone1 ./scripts/vtctld-up.sh

vtctldclient CreateKeyspace --durability-policy=semi_sync customer || fail "Failed to create and configure customer keyspace"
vtctldclient CreateKeyspace --durability-policy=semi_sync inventory || fail "Failed to create and configure inventory keyspace"

# start vttablets for unsharded keyspace customer
for i in 100 101 102; do
	CELL=zone1 TABLET_UID=$i ./scripts/mysqlctl-up.sh
	CELL=zone1 KEYSPACE=customer TABLET_UID=$i ./scripts/vttablet-up.sh
done

# start vtorc, which handles electing shard primaries (no need for InitShardPrimary manual step)
source ./scripts/vtorc-up.sh


# start vttablets for sharded keyspace inventory
for i in 200 201 202; do
	CELL=zone1 TABLET_UID=$i ./scripts/mysqlctl-up.sh
	SHARD=-80 CELL=zone1 KEYSPACE=inventory TABLET_UID=$i ./scripts/vttablet-up.sh
done

for i in 300 301 302; do
	CELL=zone1 TABLET_UID=$i ./scripts/mysqlctl-up.sh
	SHARD=80- CELL=zone1 KEYSPACE=inventory TABLET_UID=$i ./scripts/vttablet-up.sh
done



# create seq table and unsharded table in the unsharded keyspace, sharded tables in sharded keyspace
vtctlclient ApplySchema -- --sql-file create_tables_unsharded_customer.sql customer
vtctlclient ApplyVSchema -- --vschema_file vschema_tables_unsharded_customer.json customer
vtctlclient ApplySchema -- --sql-file create_tables_sharded_inventory.sql inventory
vtctlclient ApplyVSchema -- --vschema_file vschema_tables_sharded_inventory.json inventory

# start vtgate
CELL=zone1 ./scripts/vtgate-up.sh

# insert data into unsharded keyspace
mysql -h 127.0.0.1 -P 15306 customer < insert_customer_data.sql
# insert data into sharded keyspace
mysql -h 127.0.0.1 -P 15306 inventory < insert_inventory_data.sql

# select data from unsharded keyspace
mysql -h 127.0.0.1 -P 15306 --table < select_customer0_data.sql
# select data from sharded keyspace
mysql -h 127.0.0.1 -P 15306 --table < select_inventory80-_data.sql
mysql -h 127.0.0.1 -P 15306 --table < select_inventory-80_data.sql
