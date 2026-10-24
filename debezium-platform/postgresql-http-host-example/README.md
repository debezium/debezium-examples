# Host-based PostgreSQL to HTTP pipeline

This example runs Debezium Platform with Docker Compose on the developer machine and deploys a Debezium Server pipeline to a separate disposable Multipass Linux VM. The pipeline captures PostgreSQL changes and sends them to a small HTTP receiver.

Conductor discovers the VM from an SSH configuration, provisions it through Ansible, selects the ready host, and starts the Debezium Server container through the default `ansible` runtime. The Compose host is never the Ansible target.

The optional `agent` runtime is a different deployment path. It requires an Agent version and a Maven repository containing that exact Agent artifact. This example deliberately uses the default `ansible` runtime, so it does not require the optional Host Agent.

## What runs where

```
developer machine
  |
  +-- Docker Compose: Stage, Conductor, Conductor PostgreSQL,
  |                   source PostgreSQL, HTTP receiver
  |
  +-- Conductor -> SSH / Ansible -> disposable Multipass VM
                                         |
                                         +-> Docker Engine -> Debezium Server container
```

The Debezium Server container is not part of the Compose project. Conductor creates and removes it through the host runtime. That separation is the behavior this example demonstrates.

## Requirements

Install [Docker Engine and the Docker Compose plugin](https://docs.docker.com/engine/install/) on the developer machine. The Compose stack uses published ports, so it does not require Linux host networking and can run with Docker Desktop on macOS or Windows.

Install [Multipass](https://documentation.ubuntu.com/multipass/) on the developer machine. The commands below use a new Ubuntu 24.04 VM with 4 CPUs, 8 GB of memory, and 30 GB of disk space.

The VM is deliberately disposable. The host provisioning playbook runs with administrator privileges and can add the Docker package repository, install Docker packages, enable Docker, and add the SSH user to the `docker` group. It must not target the developer machine.

You also need `ssh`, `ssh-keygen`, `ssh-keyscan`, `curl`, and Python 3 on the developer machine. The default Multipass `ubuntu` user supports non-interactive `sudo`; if you use a custom VM image, configure equivalent access only for this disposable VM.

## 1. Create the target VM

Run these commands on the developer workstation:

```shell
multipass launch 24.04 --name dbz-platform-host-example --cpus 4 --memory 8G --disk 30G
multipass info dbz-platform-host-example
```

Copy the VM IPv4 address printed by `multipass info` and export it:

```shell
export TARGET_VM=dbz-platform-host-example
export TARGET_HOST_IP=<VM_IP>
export TARGET_HOST_USER=ubuntu
```

The target must be reachable through normal SSH from the developer machine. Confirm that the VM SSH service and passwordless administrator access are available:

```shell
multipass exec "$TARGET_VM" -- sudo systemctl is-active ssh
multipass exec "$TARGET_VM" -- sudo -n true
```

## 2. Obtain the example and create its dedicated SSH identity

Clone the examples repository on the developer machine and enter this directory:

```shell
git clone https://github.com/debezium/debezium-examples.git
cd debezium-examples/debezium-platform/postgresql-http-host-example
```

Create an SSH directory used only by this example. It contains one temporary private key, one SSH configuration file, and one known-hosts file. It does not replace or mount the user's normal `~/.ssh/config`.

```shell
export HOST_EXAMPLE_SSH_DIR="$PWD/.ssh"
install -d -m 700 "$HOST_EXAMPLE_SSH_DIR"
ssh-keygen -t ed25519 -f "$HOST_EXAMPLE_SSH_DIR/debezium-host-example" -N ''

multipass transfer "$HOST_EXAMPLE_SSH_DIR/debezium-host-example.pub" \
  "$TARGET_VM:/tmp/debezium-host-example.pub"
multipass exec "$TARGET_VM" -- bash -c \
  'install -d -m 700 ~/.ssh && cat /tmp/debezium-host-example.pub >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys && rm /tmp/debezium-host-example.pub'

sed -e "s/YOUR_LINUX_USER/${TARGET_HOST_USER}/" -e "s/TARGET_HOST_IP/${TARGET_HOST_IP}/" \
  ssh-config.example > "$HOST_EXAMPLE_SSH_DIR/config"
ssh-keyscan -H "$TARGET_HOST_IP" > "$HOST_EXAMPLE_SSH_DIR/known_hosts"
chmod 600 "$HOST_EXAMPLE_SSH_DIR/config" "$HOST_EXAMPLE_SSH_DIR/debezium-host-example" "$HOST_EXAMPLE_SSH_DIR/known_hosts"
```

Prove that the exact key, host alias, and non-interactive administrator access work before starting Compose:

```shell
ssh -F "$HOST_EXAMPLE_SSH_DIR/config" \
  -o "IdentityFile=$HOST_EXAMPLE_SSH_DIR/debezium-host-example" \
  -o "UserKnownHostsFile=$HOST_EXAMPLE_SSH_DIR/known_hosts" \
  host-example 'sudo -n true'
```

The command must complete without a password prompt.

## 3. Start Platform and wait for host provisioning

The deployed Debezium Server container runs in the target VM. It must reach the source PostgreSQL database and HTTP receiver on the developer machine. Use the target VM's default gateway as the address of the developer machine. Do not use `localhost` or `127.0.0.1` because those addresses would refer to the VM:

```shell
export HOST_IP="$(multipass exec "$TARGET_VM" -- ip route | awk '/default/ {print $3; exit}')"
```

Confirm the selected address before continuing:

```shell
printf 'Using Compose host address %s\n' "$HOST_IP"
export PLATFORM_VERSION=nightly
export DEBEZIUM_VERSION=3.7
unset DOCKER_DEFAULT_PLATFORM

docker compose up --build -d
./wait-for-host.sh
```

`wait-for-host.sh` reads Conductor's `host_status` record. A `READY` result means that SSH discovery and Ansible provisioning completed. On a first run, the playbook installs Docker CE and pulls the initial image on the target VM, so this can take several minutes. If it reports `FAILED`, inspect the stored report and `docker compose logs conductor` before continuing.

The Compose file enables `wal_level=logical` on the Conductor database. Conductor needs logical decoding to consume the outbox event that starts an asynchronous deployment. The source database uses the released `quay.io/debezium/example-postgres:3.7` image, which already provides logical replication settings and the sample `inventory.customers` table. Unlike the Platform images, this sample image does not publish a `nightly` tag.

The example derives a Conductor image locally because the published Conductor image does not include the Ansible command-line runtime. This temporary workaround is tracked in [DBZ-2860](https://github.com/debezium/dbz/issues/2860). The derived image adds the SSH client, Python, `ansible-core`, `ansible.posix` for the configured timing callbacks, `community.docker` for the playbook's image-pull task, and `community.general` for the optional Agent runtime's Maven artifact task. This example selects Ansible mode, so the Agent task is skipped.

The default `PLATFORM_VERSION=nightly` uses the current multi-architecture Platform development images. Nightly is a moving development tag, not a production version pin. To deliberately test a released version, set `PLATFORM_VERSION` to a release tag that publishes images for the developer machine architecture.

Stage is available at `http://localhost:3000` and Conductor at `http://localhost:8081` on the developer machine.

## 4. Create and deploy the pipeline

```shell
./create-pipeline.sh
```

The script creates the source and destination connections, then the PostgreSQL source, HTTP destination, and pipeline through Conductor's REST API. Pipeline creation starts asynchronous deployment. The script waits for the associated `host_deployment` record to report `RUNNING`. That record is the host-runtime confirmation that the remote Debezium Server container has started; the next step verifies that it can capture and deliver a real change event.

Do not manually create the Debezium Server container or update Conductor's database to force either status while verifying this example. Those actions are useful for diagnosing a failed run, but they bypass the scheduling and runtime behavior that this example is intended to test. If deployment fails, save the logs, run `./cleanup.sh`, correct the underlying problem, and start again from a clean state.

To inspect the container that Conductor deployed, run this command on the target VM:

```shell
multipass exec "$TARGET_VM" -- docker ps --filter 'name=host-example-pipeline'
```

## 5. Verify a real change event

```shell
./verify.sh
```

The script inserts a new row into the source PostgreSQL database and waits for that unique value in the receiver's event file. This verifies the entire route: PostgreSQL logical replication on the developer machine, the host-deployed Debezium Server container in the target VM, and the HTTP sink on the developer machine.

For live troubleshooting, use:

```shell
docker compose logs -f conductor receiver
```

The pipeline id is stored in `.host-pipeline-state`, which is ignored by Git. You can also retrieve logs from Conductor:

```shell
source .host-pipeline-state
curl "http://localhost:8081/api/pipelines/${PIPELINE_ID}/logs"
```

## Cleanup

On the developer machine, remove the pipeline and the Compose project:

```shell
./cleanup.sh
```

The script waits for the asynchronous host undeploy to remove its `host_deployment` record before it stops Conductor. It then removes the Compose project and its volumes.

Remove the disposable VM when the example is no longer needed:

```shell
multipass delete --purge dbz-platform-host-example
```

This removes the target VM, its Docker state, the temporary SSH key registered on it, and every package change made by the provisioning playbook.
