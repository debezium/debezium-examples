# Host-based PostgreSQL to HTTP pipeline

This example runs Debezium Platform with Docker Compose and deploys a Debezium Server pipeline through the Platform's host deployment path. The pipeline captures PostgreSQL changes and sends them to a small HTTP receiver.

The Compose stack and the target host both run inside a disposable Multipass Linux VM. From Conductor's point of view, the target is the `host-example` SSH entry, which resolves to `127.0.0.1` inside that VM. This is intentional: Conductor still discovers the host from an SSH configuration, runs the Ansible provisioning playbook, selects the ready host, and starts the Debezium Server container through the default `ansible` runtime. The developer workstation is never the Ansible target.

The optional `agent` runtime is a different deployment path. It requires an Agent version and a Maven repository containing that exact Agent artifact. This example deliberately uses the default `ansible` runtime, so it does not require the optional Host Agent.

## What runs where

```
developer workstation
        |
        | Multipass
        v
disposable Linux VM
  |
  +-- Docker Compose: Stage, Conductor, Conductor PostgreSQL,
  |                   source PostgreSQL, HTTP receiver
  |
  +-- Conductor -> SSH / Ansible -> host-example (127.0.0.1 in this VM)
                                      |
                                      +-> Docker Engine -> Debezium Server container
```

The Debezium Server container is not part of the Compose project. Conductor creates and removes it through the host runtime. That separation is the behavior this example demonstrates.

## Requirements

Install [Multipass](https://documentation.ubuntu.com/multipass/) on the developer workstation. Multipass is available on macOS, Linux, and Windows. The commands below use a new Ubuntu 24.04 VM with 4 CPUs, 8 GB of memory, and 30 GB of disk space.

The VM is deliberately disposable. The host provisioning playbook runs with administrator privileges and can add the Docker package repository, install Docker packages, enable Docker, and add the SSH user to the `docker` group. It must not target a developer workstation.

Inside the VM, install Docker Engine and the Docker Compose plugin from the [official Docker installation guide](https://docs.docker.com/engine/install/ubuntu/). Do not install Ubuntu's `docker.io` or `docker-compose-v2` packages: the provisioning playbook manages the Docker CE package set, and mixing the distributions can cause package conflicts. Add the VM user to the `docker` group as part of that setup, then open a new VM shell before continuing.

The example uses Linux host networking. It is therefore not supported when Docker Compose runs directly on Docker Desktop for macOS or Windows. Docker runs inside the Linux VM, so no Docker `platform:` override or `DOCKER_DEFAULT_PLATFORM` value is required.

## 1. Create a disposable Linux VM

Run these commands on the developer workstation:

```shell
multipass launch 24.04 --name dbz-platform-host-example --cpus 4 --memory 8G --disk 30G
multipass shell dbz-platform-host-example
```

The rest of this guide runs inside the VM. Ensure the SSH server is available before continuing:

```shell
sudo apt-get update
sudo apt-get install -y curl git openssh-client openssh-server python3
sudo systemctl enable --now ssh
```

Install Docker Engine and the Compose plugin as described in the requirements. Confirm that the VM user can run Docker without `sudo`:

```shell
docker version
docker compose version
```

## 2. Obtain the example and create its dedicated SSH identity

Inside the VM, clone the examples repository and enter this directory:

```shell
git clone https://github.com/debezium/debezium-examples.git
cd debezium-examples/debezium-platform/postgresql-http-host-example
```

Create an SSH directory used only by this example. It contains one temporary private key, one SSH configuration file, and one known-hosts file. It does not replace or mount the user's normal `~/.ssh/config`, and Conductor receives only these three files.

```shell
export HOST_EXAMPLE_SSH_DIR="$PWD/.ssh"
install -d -m 700 "$HOST_EXAMPLE_SSH_DIR"
ssh-keygen -t ed25519 -f "$HOST_EXAMPLE_SSH_DIR/debezium-host-example" -N ''

install -d -m 700 "$HOME/.ssh"
cat "$HOST_EXAMPLE_SSH_DIR/debezium-host-example.pub" >> "$HOME/.ssh/authorized_keys"
chmod 600 "$HOME/.ssh/authorized_keys"

sed "s/YOUR_LINUX_USER/${USER}/" ssh-config.example > "$HOST_EXAMPLE_SSH_DIR/config"
ssh-keyscan -H 127.0.0.1 > "$HOST_EXAMPLE_SSH_DIR/known_hosts"
chmod 600 "$HOST_EXAMPLE_SSH_DIR/config" "$HOST_EXAMPLE_SSH_DIR/debezium-host-example" "$HOST_EXAMPLE_SSH_DIR/known_hosts"
```

Prove that the exact key, host alias, and non-interactive administrator access work before starting Compose:

```shell
ssh -F "$HOST_EXAMPLE_SSH_DIR/config" \
  -o "IdentityFile=$HOST_EXAMPLE_SSH_DIR/debezium-host-example" \
  -o "UserKnownHostsFile=$HOST_EXAMPLE_SSH_DIR/known_hosts" \
  host-example 'sudo -n true'
```

The command must complete without a password prompt. Multipass's default `ubuntu` user has passwordless `sudo`. If a custom image uses another user, configure equivalent non-interactive `sudo` only for this disposable VM.

## 3. Start Platform and wait for host provisioning

The deployed Debezium Server container uses Docker's bridge network. From that container, `localhost` means the container itself, not the VM. Export the VM's primary IPv4 address so the container can reach the source PostgreSQL database and HTTP receiver. The same address lets a browser on your Mac reach Stage and lets Stage send API requests back to Conductor:

```shell
export HOST_IP="$(ip -4 route get 1.1.1.1 | awk '{for (i=1;i<=NF;i++) if ($i==\"src\") {print $(i+1); exit}}')"
printf 'Using VM address %s\n' "$HOST_IP"
export PLATFORM_VERSION=nightly
unset DOCKER_DEFAULT_PLATFORM

docker compose up --build -d
./wait-for-host.sh
```

`wait-for-host.sh` reads Conductor's `host_status` record. A `READY` result means that SSH discovery and Ansible provisioning completed. On a first run, the playbook may validate Docker packages and pull the initial image, so this can take several minutes. If it reports `FAILED`, inspect the stored report and `docker compose logs conductor` before continuing.

The Compose file enables `wal_level=logical` on both PostgreSQL services. Conductor needs logical decoding to consume the outbox event that starts an asynchronous deployment; the sample source needs it so the deployed Debezium Server can capture the inserted row.

The example derives a Conductor image locally because the published Conductor image does not include the Ansible command-line runtime. The derived image adds the SSH client, Python, `ansible-core`, `ansible.posix` for the configured timing callbacks, `community.docker` for the playbook's image-pull task, and `community.general` for the optional Agent runtime's Maven artifact task. This example selects Ansible mode, so the Agent task is skipped, but the derived image still contains the complete bundled playbook dependencies.

The default `PLATFORM_VERSION=nightly` uses the current multi-architecture Platform development images. Nightly is a moving development tag, not a production version pin. To deliberately test a released version, set `PLATFORM_VERSION` to a release tag that publishes images for the VM architecture.

Stage is available at `http://<VM_IP>:3000` and Conductor at `http://<VM_IP>:8081`, where `<VM_IP>` is the address shown by `hostname -I` or `multipass list` on the developer workstation.

## 4. Create and deploy the pipeline

```shell
./create-pipeline.sh
```

The script creates the source and destination connections, then the PostgreSQL source, HTTP destination, and pipeline through Conductor's REST API. Pipeline creation starts asynchronous deployment. The script waits for the associated `host_deployment` record to report `RUNNING`. That record is the host-runtime confirmation that the remote Debezium Server container has started; the next step verifies that it can capture and deliver a real change event.

Do not manually create the Debezium Server container or update Conductor's database to force either status while verifying this example. Those actions are useful for diagnosing a failed run, but they bypass the scheduling and runtime behavior that this example is intended to test. If deployment fails, save the logs, run `./cleanup.sh`, correct the underlying problem, and start again from a clean state.

To inspect the container that Conductor deployed, run:

```shell
docker ps --filter 'name=debezium-pipeline-'
```

## 5. Verify a real change event

```shell
./verify.sh
```

The script inserts a new row into the source PostgreSQL database and waits for that unique value in the receiver's event file. This verifies the entire route: PostgreSQL logical replication, the host-deployed Debezium Server container, and the HTTP sink.

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

Inside the VM, remove the pipeline and the Compose project:

```shell
./cleanup.sh
```

`cleanup.sh` first deletes the Platform pipeline, which stops and removes the host-deployed Debezium Server container. It then removes the Compose project and its volumes. The example-owned `.ssh` directory remains only inside the disposable VM.

Exit the VM. On the developer workstation, remove it when the example is no longer needed:

```shell
multipass delete --purge dbz-platform-host-example
```

This removes all Docker state, the temporary SSH key, and every package change made during the example.
