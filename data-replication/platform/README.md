Kafka-Less Data Replication with Debezium Platform (PostgreSQL to PostgreSQL)
===
This example walks you through using the Debezium Management Platform to set up a Kafka-less data replication pipeline from a source PostgreSQL database to a destination PostgreSQL database via the JDBC sink. The pipeline is created entirely through the REST API using curl.

Preparing the Environment
---
Provision a local Kubernetes cluster using Kind with ingress support and update `/etc/hosts` to resolve the platform domain:

```sh
./create-environment.sh
```

> **Note:** If you are using minikube on Mac, you also need to run the `minikube tunnel` command.

Setting Up Infrastructure
---
Deploy the source and destination PostgreSQL databases, and install the Debezium Platform via Helm:

```sh
./setup-infra.sh
```

This script will:
1. Create the `debezium-platform` namespace
2. Deploy source PostgreSQL (with example inventory data)
3. Deploy destination PostgreSQL (vanilla)
4. Install Debezium Platform via Helm chart

After all pods are running, the platform UI is accessible at `http://platform.debezium.io/`.

You can check the pod status with:

```sh
kubectl get pods -n debezium-platform
```

Creating the Pipeline via REST API
---
Create the data replication pipeline using curl commands:

```sh
./create-pipeline.sh
```

This script creates:
1. A source connection to the PostgreSQL source database
2. A JDBC destination connection to the PostgreSQL destination database
3. A source referencing the source connection
4. A destination referencing the destination connection with JDBC sink configuration (upsert mode, record key, schema evolution)
5. A pipeline linking source to destination

You can also view and manage the pipeline through the UI at `http://platform.debezium.io/`.

Verifying Data Replication
---
Check that data was replicated to the destination database:

```sh
kubectl exec -n debezium-platform deploy/postgresql-dest -- psql -U debezium -d debezium -c \
  "SELECT * FROM inventory_inventory_customers;"
```

Insert a new record in the source:

```sh
kubectl exec -n debezium-platform deploy/postgresql -- psql -U debezium -d debezium -c \
  "INSERT INTO inventory.customers (first_name, last_name, email) VALUES ('John', 'Doe', 'john.doe@example.com');"
```

Verify the change was replicated:

```sh
kubectl exec -n debezium-platform deploy/postgresql-dest -- psql -U debezium -d debezium -c \
  "SELECT * FROM inventory_inventory_customers;"
```

Cleanup
---
To remove the local Kubernetes cluster and all resources:

```sh
./destroy-environment.sh
```
