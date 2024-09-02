#!/bin/bash

source ./env.sh

cell=${CELL:-'test'}
keyspace=${KEYSPACE:-'test_keyspace'}
shard=${SHARD:-'0'}
uid=$TABLET_UID
mysql_port=$[17000 + $uid]
port=$[15000 + $uid]
grpc_port=$[16000 + $uid]
printf -v alias '%s-%010d' $cell $uid
printf -v tablet_dir 'vt_%010d' $uid
tablet_hostname=''
printf -v tablet_logfile 'vttablet_%010d_querylog.txt' $uid

tablet_type=replica
if [[ "${uid: -1}" -gt 1 ]]; then
 tablet_type=rdonly
fi

echo "Starting vttablet for $alias..."

# shellcheck disable=SC2086
vttablet \
 $TOPOLOGY_FLAGS \
 --log_dir $VTDATAROOT/tmp \
 --log_queries_to_file $VTDATAROOT/tmp/$tablet_logfile \
 --tablet-path $alias \
 --tablet_hostname "$tablet_hostname" \
 --init_keyspace $keyspace \
 --init_shard $shard \
 --init_tablet_type $tablet_type \
 --health_check_interval 5s \
 --backup_storage_implementation file \
 --watch_replication_stream \
 --file_backup_storage_root $VTDATAROOT/backups \
 --restore_from_backup \
 --port $port \
 --grpc_port $grpc_port \
 --service_map 'grpc-queryservice,grpc-tabletmanager,grpc-updatestream' \
 --pid_file $VTDATAROOT/$tablet_dir/vttablet.pid \
 --heartbeat_enable \
 --heartbeat_interval=250ms \
 --heartbeat_on_demand_duration=5s \
 > $VTDATAROOT/$tablet_dir/vttablet.out 2>&1 &

# Block waiting for the tablet to be listening
# Not the same as healthy

for i in $(seq 0 300); do
 curl -I "http://$hostname:$port/debug/status" >/dev/null 2>&1 && break
 sleep 0.1
done

# check one last time
curl -I "http://$hostname:$port/debug/status" || fail "tablet could not be started!"

echo -e "vttablet for $alias is running!"
