# Detect data mutation patterns with Debezium

In today's dynamic data environments, detecting and understanding data mutation patterns is critical for system reliability. 
In this example, we will explore how to use Debezium for comprehensive database activity logging and analysis in microservice architectures. 
By integrating with analytics tools, teams can build detailed activity dashboards that reveal the volume and nature of operations per table. 
These insights are invaluable for identifying unexpected patterns, such as a sudden drop in inserts caused by a new microservice deployment with a bug. 
In this example we'll see how to set up Debezium to expose advanced metrics for this specific use cases, and we'll utilize these metrics to create actionable dashboards. 

## Monitoring a Debezium instance

Debezium [collects and exports](https://debezium.io/documentation/reference/1.5/operations/monitoring.html) a set of metrics as JMX beans.
Those metrics can be displayed either via an arbitrary JMX console or, for more complex deployments, a Prometheus and Grafana based solution can be deployed.
This example uses a Docker Compose file to set up and deploy Debezium together with all components necessary to monitor it in Grafana.

## Topology

We need following components to collect and present Debezium metrics:

 * Debezium instance with [JMX Exporter](https://github.com/prometheus/jmx_exporter) Java agent installed and configured (see [Docker image](debezium-jmx-exporter))
 * Prometheus instance to collect and store exported metrics (see [Docker image](debezium-prometheus))
 * Grafana instance presenting the metrics (see [Docker image](debezium-grafana))

## Execution

Before starting running all required services we need to build our order service.

```shell
order-service/mvnw package -f order-service/pom.xml
```

then we can just run our compose file to start everything is needed. 

```shell
export DEBEZIUM_VERSION=3.0.0.Final
docker-compose up -d --build
```

When all service are up and running we can register our connector

```shell
# Start PostgreSQL connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @postgres-activity-monitoring.json
```

## Accessing the dashboard

Open a web browser and go to the Grafana UI at [http://localhost:3000](http://localhost:3000).
Login into the console as user `admin` with password `admin`.
When asked either change the password (you also can skip this step).

Then you can monitor the order service activity using the pre-built dashboard `General/ Microservices activity monitoring`.
This dashboard will show the rate of created orders within a label that indicate the version of the deployed order service. 

The order service will insert 100 orders every 10s so this means that you should see a rate of ~10 order per second. 

To simulate a drop, we can just update the `APP_VERSION` env to a value different to `1.0`. 

```shell
docker stop order-service
docker rm -f order-service && \
docker compose run -d -e APP_VERSION=1.5 --name order-service order-service
```

In that case the service will start creating orders with a ~50% drop. 

An alert has been configured to fire when the order rate is below 7. 
You can check that a mail will be sent to a Fake SMTP server, to check it you can 
open a web browser and got to the Fake SMTP UI at [http://localhost:8085](http://localhost:8085).


