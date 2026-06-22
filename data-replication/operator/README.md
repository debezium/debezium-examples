Kafka-Less Data Replication with Debezium Operator
===
This tutorial demonstrates how to replicate data from a PostgreSQL database into MySQL using the Debezium Operator with the JDBC sink — no Kafka cluster required.

Preparing the Environment
---
As the first step we will provision a local Kubernetes cluster with running PostgreSQL (source) and MySQL (destination) databases, and deploy the Debezium Operator via Helm. The following script, when executed, will use [Kind](https://kind.sigs.k8s.io/) to provision a local k8s cluster, deploy the required infrastructure, and install the operator.

```sh
./create-environment.sh
```

Deploying Debezium Server
---
With the operator running, deploy the DebeziumServer resource configured with the JDBC sink:

```sh
kubectl create -f infra/010_debezium-server.yml -n debezium
```

Check that Debezium Server is running:

```sh
kubectl get deployments -n debezium
```

Verifying Data Replication
---
Verify that the initial snapshot was replicated to MySQL:

```sh
kubectl exec -n debezium deploy/mysql -- mysql -uroot -pdebezium target_db -e "SELECT * FROM migration_inventory_customers;"
```

Insert a new record in PostgreSQL:

```sh
kubectl exec -n debezium deploy/postgresql -- psql -U postgres -d postgres -c \
"INSERT INTO inventory.customers (first_name, last_name, email) VALUES ('John', 'Doe', 'john.doe@example.com');"
```

Verify the change was replicated to MySQL:

```sh
kubectl exec -n debezium deploy/mysql -- mysql -uroot -pdebezium target_db -e "SELECT * FROM migration_inventory_customers;"
```

Cleanup
---
To remove the local Kubernetes cluster and all resources:

```sh
./destroy-environment.sh
```
