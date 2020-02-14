# Deploying Debezium on OpenShift

This demo shows how to deploy Debezium on a developer OpenShift instance using [Minishift tool](https://github.com/minishift/minishift).

Prerequisites:

 * [Installed and running](https://docs.openshift.org/latest/minishift/getting-started/installing.html) Minishift
 * [Installed](https://docs.openshift.org/latest/minishift/command-ref/minishift_oc-env.html) OpenShift CLI

## Debezium Deployment
Albeit Debezium comes with its own set of images we are going to re-use Kafka broker and Kafka Connect images that are built and delivered by the [Strimzi](http://strimzi.io/) project can be used, which offers "Kafka as a Service".
It consists of enterprise grade configuration files and images that bring Kafka to OpenShift.

First we install the Kafka broker and Kafka Connect templates into our OpenShift project:

```
oc create -f https://raw.githubusercontent.com/strimzi/strimzi/0.1.0/kafka-statefulsets/resources/openshift-template.yaml
oc create -f https://raw.githubusercontent.com/strimzi/strimzi/0.1.0/kafka-connect/s2i/resources/openshift-template.yaml
```

Next we will create a Kafka Connect image with deployed Debezium connectors and deploy a Kafka broker cluster and Kafka Connect cluster:

```
# Deploy a Kafka broker
oc new-app -p ZOOKEEPER_NODE_COUNT=1 strimzi

# Build a Debezium image
export DEBEZIUM_VERSION=0.8.0.Final
oc new-app -p BUILD_NAME=debezium -p TARGET_IMAGE_NAME=debezium -p TARGET_IMAGE_TAG=$DEBEZIUM_VERSION strimzi-connect-s2i
mkdir -p plugins && cd plugins && \
for PLUGIN in {mongodb,mysql,postgres}; do \
    curl https://repo1.maven.org/maven2/io/debezium/debezium-connector-$PLUGIN/$DEBEZIUM_VERSION/debezium-connector-$PLUGIN-$DEBEZIUM_VERSION-plugin.tar.gz | tar xz; \
done && \
oc start-build debezium --from-dir=. --follow && \
cd .. && rm -rf plugins
```

After a while all parts should be up and running:

```
oc get pods
NAME                    READY     STATUS      RESTARTS   AGE
debezium-1-build        0/1       Completed   0          3m
debezium-2-build        0/1       Completed   0          3m
kafka-0                 1/1       Running     2          3m
kafka-1                 1/1       Running     0          2m
kafka-2                 1/1       Running     0          2m
kafka-connect-3-3v4n9   1/1       Running     1          3m
zookeeper-0             1/1       Running     0          3m
```

## Verify Deployment
Next we are going to verify if the deployment is correct by emulating the [Debezium Tutorial](https://debezium.io/docs/tutorial/) in the OpenShift environment.

First we need to start a MySQL server instance:

```
# Deploy pre-populated MySQL instance
oc new-app --name=mysql debezium/example-mysql:0.8

# Configure credentials for the database
oc env dc/mysql  MYSQL_ROOT_PASSWORD=debezium  MYSQL_USER=mysqluser MYSQL_PASSWORD=mysqlpw
```

A new pod with MySQL server should be up and running:

```
oc get pods
NAME                             READY     STATUS      RESTARTS   AGE
...
mysql-1-4503l                    1/1       Running     0          2s
mysql-1-deploy                   1/1       Running     0          4s
...
```

Then we are going to register the Debezium MySQL connector to run against the deployed MySQL instance:

```
cat register.json | oc exec -i kafka-0 -- curl -X POST -H "Accept:application/json" -H "Content-Type:application/json" http://kafka-connect:8083/connectors -d @-
```

Kafka Connect's log file should contain messages regarding execution of initial snapshot:

```
oc logs $(oc get pods -o name -l name=kafka-connect)
```

Read customer table CDC messages from the Kafka topic:

```
oc exec -it kafka-0 -- /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic dbserver1.inventory.customers
```

Modify some records in the `CUSTOMERS` table of the database:

```
oc exec -it $(oc get pods -o custom-columns=NAME:.metadata.name --no-headers -l app=mysql) -- bash -c 'mysql -u $MYSQL_USER -p$MYSQL_PASSWORD inventory'
```

You should see additional change messages in the consumer started before.

## Using Minishift add-ons
If you have already gone through the process and you would like to speed up repeated deployment then you can use Minishift [add-on feature](https://docs.openshift.org/latest/minishift/using/addons.html).
We are providing two add-ons for Minishift: `debezium` that deploys Kafka broker and Connect cluster and `tutorial-database` that deploys pre-populated and pre-configured MySQL instance.

Install add-ons:

```
minishift addon install debezium
minishift addon install tutorial-database
```

Deploy the Kafka broker, Kafka Connect with Debezium and MySQL example database:

```
minishift addon apply -a DEBEZIUM_VERSION=0.8.0.Final -a DEBEZIUM_PLUGIN=mysql -a PROJECT=myproject debezium
minishift addon apply -a DEBEZIUM_TAG=0.8 -a PROJECT=myproject tutorial-database
```
