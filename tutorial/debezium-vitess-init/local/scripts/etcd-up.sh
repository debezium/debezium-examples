#!/bin/bash

source ./env.sh

cell=${CELL:-'test'}
export ETCDCTL_API=2

# Check that etcd is not already running
curl "http://${ETCD_SERVER}" > /dev/null 2>&1 && fail "etcd is already running. Exiting."

etcd --enable-v2=true --data-dir "${VTDATAROOT}/etcd/"  --listen-client-urls "http://${ETCD_SERVER}" --advertise-client-urls "http://${ETCD_SERVER}" > "${VTDATAROOT}"/tmp/etcd.out 2>&1 &
PID=$!
echo $PID > "${VTDATAROOT}/tmp/etcd.pid"
sleep 5

echo "add /vitess/global"
etcdctl --endpoints "http://${ETCD_SERVER}" mkdir /vitess/global &

echo "add /vitess/$cell"
etcdctl --endpoints "http://${ETCD_SERVER}" mkdir /vitess/$cell &

# And also add the CellInfo description for the cell.
# If the node already exists, it's fine, means we used existing data.
echo "add $cell CellInfo"
set +e
# shellcheck disable=SC2086
vtctl $TOPOLOGY_FLAGS AddCellInfo \
  -root /vitess/$cell \
  -server_address "${ETCD_SERVER}" \
  $cell
set -e

echo "etcd start done..."
