Kafka-Less Data Replication with Debezium
===
This example demonstrates how to set up a Kafka-less data replication pipeline between databases using the Debezium JDBC sink. Three deployment options are provided:

* [Debezium Server](./debezium-server) — Docker Compose setup replicating PostgreSQL to MySQL
* [Debezium Operator](./operator) — Kubernetes deployment using the Debezium Operator custom resource
* [Debezium Platform](./platform) — Kubernetes deployment using the Debezium Management Platform REST API
