#!/bin/bash

hostname=$(hostname -f)
vtctld_web_port=15000
export VTDATAROOT="${VTDATAROOT:-${PWD}/vtdataroot}"

function fail() {
  echo "ERROR: $1"
  exit 1
}

if [[ $EUID -eq 0 ]]; then
  fail "This script refuses to be run as root. Please switch to a regular user."
fi

# mysqld might be in /usr/sbin which will not be in the default PATH
PATH="/usr/sbin:$PATH"
for binary in mysqld etcd etcdctl curl vtctlclient vtctldclient vttablet vtgate vtctld mysqlctl; do
  command -v "$binary" > /dev/null || fail "${binary} is not installed in PATH. See https://vitess.io/docs/get-started/local/ for install instructions."
done;

# vtctlclient has a separate alias setup below
for binary in vttablet vtgate vtctld mysqlctl vtorc vtctl; do
  alias $binary="$binary --config-file-not-found-handling=ignore"
done;

ETCD_SERVER="localhost:2379"
TOPOLOGY_FLAGS="--topo_implementation etcd2 --topo_global_server_address $ETCD_SERVER --topo_global_root /vitess/global"
mkdir -p "${VTDATAROOT}/etcd"

mkdir -p "${VTDATAROOT}/tmp"

# Set aliases to simplify instructions.
# In your own environment you may prefer to use config files,
# such as ~/.my.cnf

alias mysql="command mysql -h 127.0.0.1 -P 15306"
alias vtctlclient="command vtctlclient --server localhost:15999 --log_dir ${VTDATAROOT}/tmp --alsologtostderr --config-file-not-found-handling=ignore --grpc_auth_static_client_creds grpc_static_client_auth.json "
alias vtctldclient="command vtctldclient --server localhost:15999 --grpc_auth_static_client_creds grpc_static_client_auth.json "

# Make sure aliases are expanded in non-interactive shell
shopt -s expand_aliases
