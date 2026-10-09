# Host-based PostgreSQL to HTTP pipeline

This example runs Debezium Platform locally and deploys a Debezium Server pipeline to the same Linux machine that runs Docker. The pipeline captures PostgreSQL changes and sends them to a small HTTP receiver.

It demonstrates the host deployment path introduced for Debezium Platform. The Platform discovers `localhost` from an SSH configuration, provisions it through Ansible, selects it for the pipeline, and starts the Debezium Server container there. The example uses the default `ansible` container runtime. It does not require the optional Host Agent.

The optional `agent` runtime is a different deployment path. It requires an Agent version and a Maven repository that contains that exact Agent artifact. This example deliberately does not switch to that runtime, so it does not depend on an Agent artifact repository.

## What runs where

The Docker Compose project runs Stage, Conductor, Conductor's PostgreSQL database, the source PostgreSQL database, and the HTTP receiver. They use Linux host networking so Conductor can reach `localhost` through SSH.

The pipeline container is not part of the Compose project. Conductor provisions the target host and starts that container itself through the host runtime. This distinction is the point of the example.

```
browser -> Stage -> Conductor -> SSH / Ansible -> local Docker Engine -> Debezium Server
                                              -> source PostgreSQL
                                              -> HTTP receiver
```

## Requirements

Run this only on a disposable Linux machine or VM with Docker Engine. On Apple Silicon macOS, use a Linux ARM64 Multipass VM: macOS manages the VM, while the example itself runs entirely inside Linux. It is **not supported on Docker Desktop for macOS or Windows** because the example uses Linux host networking. Do not add Docker `platform:` overrides or set `DOCKER_DEFAULT_PLATFORM`; the nightly images must select the VM's native architecture.

You need:

* Docker CE, Docker CLI, containerd.io, and the Docker Compose plugin from the https://docs.docker.com/engine/install/[official Docker repository], with permission to run `docker`. Do not install Ubuntu's `docker.io` or `docker-compose-v2` packages for this example: Conductor's provisioning playbook manages the Docker CE package set, and mixing the two distributions causes APT package conflicts.
* OpenSSH client and server running locally.
* A local Linux user that can run `sudo` without an interactive password prompt. In this localhost example, Docker CE is already present because Compose needs it before Conductor starts; Ansible recognizes the required packages as installed and continues with the remaining host preparation. On a separate remote host, the same playbook can install Docker CE when necessary.
* Python 3 and `curl` on the host.
* Network access to pull container images, install Ansible dependencies while building Conductor, and pull the Debezium Server image on the target host.

The example reserves local ports `3000`, `5432`, `5433`, `8081`, `9000`, and `9900`. Stop or reconfigure any service already using one of them before starting it.

The default `PLATFORM_VERSION=nightly` uses the current multi-architecture Platform development images, which make this local example usable on both AMD64 and ARM64 Linux hosts. Nightly is a moving development tag, not a production version pin. To test a released version deliberately, set `PLATFORM_VERSION` to a release tag that publishes images for your host architecture.

## 1. Create the SSH identity used by Conductor

Create an identity solely for this example. Replace `YOUR_LINUX_USER` with the local user that Ansible should use.

```shell
ssh-keygen -t ed25519 -f ~/.ssh/debezium-host-example -N ''
cat ~/.ssh/debezium-host-example.pub >> ~/.ssh/authorized_keys
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```

Copy [`ssh-config.example`](./ssh-config.example) to `~/.ssh/config` and replace `YOUR_LINUX_USER`. If you already maintain an SSH config, add the `Host localhost` entry instead of replacing the file.

```shell
cp ssh-config.example ~/.ssh/config
chmod 600 ~/.ssh/config
```

Before starting Docker Compose, prove that this exact SSH path works and accept the host key on the host machine:

```shell
ssh localhost 'sudo -n true'
```

This command must finish without asking for a password. It also lets OpenSSH write the initial `known_hosts` entry before Conductor receives a read-only view of `~/.ssh`. Conductor only needs to read the SSH config, private key, and known-host record; it does not need to modify them.

## 2. Choose an address for deployed containers

The deployed Debezium Server container uses Docker's normal bridge network. From that container, `localhost` means the container itself, not the Linux host. Export a non-loopback IPv4 address that containers can use to reach this machine. A LAN address or VM address is suitable.

```shell
export HOST_IP=192.168.1.50
```

Do not use `localhost` or `127.0.0.1`. The source connection uses `${HOST_IP}:5433` and the HTTP destination uses `${HOST_IP}:9900`.

## 3. Start Platform and wait for host provisioning

Point Compose at the directory containing the SSH files, then start the stack:

```shell
export HOST_SSH_DIR="$HOME/.ssh"
unset DOCKER_DEFAULT_PLATFORM
docker compose up --build -d
./wait-for-host.sh
```

`wait-for-host.sh` reads the `host_status` record from Conductor's database. A `READY` result means that SSH discovery and Ansible provisioning both completed. A first run can take several minutes because it may install packages and pull images; the script waits up to the Conductor's default 30-minute Ansible timeout. If it reports `FAILED`, inspect the stored report and `docker compose logs conductor` before continuing.

The Compose file enables `wal_level=logical` on both PostgreSQL services. Conductor needs logical decoding to consume the outbox event that starts an asynchronous deployment; the sample source needs it so the deployed Debezium Server can capture the inserted row.

Inside the Linux machine, Stage is available at http://localhost:3000 and Conductor is available at http://localhost:8081. When the Linux machine is a Multipass VM, open `http://<VM_IP>:3000` from macOS instead; macOS `localhost` is not the VM.

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

```shell
./cleanup.sh
```

The script first deletes the Platform pipeline, which stops and removes the host-deployed Debezium Server container. It then removes the Compose project and its volumes. It defaults `HOST_SSH_DIR` to `$HOME/.ssh` when it is run from a fresh terminal. Host provisioning is deliberately not undone: Docker, its package configuration, and the SSH configuration belong to the local machine and may have existed before this example.
