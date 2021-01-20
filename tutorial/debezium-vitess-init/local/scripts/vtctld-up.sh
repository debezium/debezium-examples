#!/bin/bash

source ./env.sh

cell=${CELL:-'test'}
grpc_port=15999

echo "Starting vtctld..."
# shellcheck disable=SC2086
vtctld \
 $TOPOLOGY_FLAGS \
 -cell $cell \
 -workflow_manager_init \
 -workflow_manager_use_election \
 -service_map 'grpc-vtctl' \
 -backup_storage_implementation file \
 -file_backup_storage_root $VTDATAROOT/backups \
 -log_dir $VTDATAROOT/tmp \
 -port $vtctld_web_port \
 -grpc_port $grpc_port \
 -pid_file $VTDATAROOT/tmp/vtctld.pid \
 -grpc_auth_mode static\
 -grpc_auth_static_password_file grpc_static_auth.json\
  > $VTDATAROOT/tmp/vtctld.out 2>&1 &
