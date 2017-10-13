# Deploying Debezium on OpenShift

This demo shows how to deploy Debezium on a developer OpenShift instance using [Minishift tool](https://github.com/minishift/minishift).

Pre-requisities
 * [Installed and running](https://docs.openshift.org/latest/minishift/getting-started/installing.html) Minishift
 * [Installed](https://docs.openshift.org/latest/minishift/command-ref/minishift_oc-env.html) OpenShift CLI


## Debezium Deployment
Albeit Debezium comes with its own set of images we are going to re-use Kafka broker and Kafka Connect images that are built and delivered as a part of project [EnMasse](https://github.com/EnMasseProject/).
One of the componnets of the project is a [Kafka as a Service](https://github.com/EnMasseProject/barnabas/).
It consists of entreprise grade of configuration files and images that brings Kafka on OpenShift.

First we install Kafka broker and Kafka Connect templates into our OpenShift project
```
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-statefulsets/resources/openshift-template.yaml
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-connect/resources/openshift-template.yaml
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-connect/s2i/resources/openshift-template.yaml
```

Next we will create a Kafka Connect image with deployed Debezium connectors and deploy a Kafka broker cluster and Kafka Connect cluster
```
# Build a Debezium image
export DEBEZIUM_VERSION=0.6.0
oc new-app -p BUILD_NAME=debezium -p TARGET_IMAGE_NAME=debezium -p TARGET_IMAGE_TAG=$DEBEZIUM_VERSION barnabas-connect-s2i
mkdir -p plugins && cd plugins &&\
for PLUGIN in {mongodb,mysql,postgres}; do \
    curl http://central.maven.org/maven2/io/debezium/debezium-connector-$PLUGIN/$DEBEZIUM_VERSION/debezium-connector-$PLUGIN-$DEBEZIUM_VERSION-plugin.tar.gz | tar xz; \
done &&\
oc start-build debezium --from-dir=. --follow &&\
cd .. && rm -rf plugins

# Deploy Kafka broker
oc new-app barnabas

# Deploy Kafka Connect cluster and enable Deloyment object to find the Debezium image from image stream
oc new-app -p IMAGE_REPO_NAME=$(oc project -q) -p IMAGE_NAME=debezium -p IMAGE_TAG=$DEBEZIUM_VERSION barnabas-connect
oc set image-lookup deploy/kafka-connect
oc delete rs,pods -l name=kafka-connect --now
```
After a while all parts should be up and running
```
oc get pods 
NAME                             READY     STATUS      RESTARTS   AGE
debezium-1-build                 0/1       Error       0          17
debezium-1-build                 0/1       Completed   0          17s
debezium-2-build                 0/1       Completed   0          17s
kafka-0                          1/1       Running     1          19s
kafka-1                          1/1       Running     0          14s
kafka-2                          1/1       Running     0          10s
kafka-connect-2447403008-qg56n   1/1       Running     0          16s
zookeeper-0                      1/1       Running     0          19s
```

## Verify Deployment
Next we are going to verify if the depolyment is correct by emulating [Debezium Tutorial](http://debezium.io/docs/tutorial/) in OpenShift environment.

First we need to start a MySQL server instance
```
# Deploy pre-populated MySQL instance
oc new-app --name=mysql debezium/example-mysql:0.6

# Configure credentials for the database
oc env dc/mysql  MYSQL_ROOT_PASSWORD=debezium  MYSQL_USER=mysqluser MYSQL_PASSWORD=mysqlpw
```

A new pod with MySQL server should be up and running
```
oc get pods 
NAME                             READY     STATUS      RESTARTS   AGE
...
mysql-1-4503l                    1/1       Running     0          2s
mysql-1-deploy                   1/1       Running     0          4s
...
```

Then we are going to register Debezium MySQL connector to run against the deployed MySQL instance.
```
cat register.json | oc exec -i kafka-0 -- curl -X POST -H "Accept:application/json" -H "Content-Type:application/json" http://kafka-connect:8083/connectors -d @-
```

Kafka Connect's log file should contain messages regarding execution of initial snapshot
```
oc logs $(oc get pods -o name -l name=kafka-connect)
```

Read customer table CDC messages from a Kafka topic
```
oc exec -it kafka-0 -- /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic dbserver1.inventory.customers
```

Modify records in the database
```
oc exec -it $(oc get pods -o custom-columns=NAME:.metadata.name --no-headers -l app=mysql) -- bash -c 'mysql -u $MYSQL_USER -p$MYSQL_PASSWORD inventory'
```

## Using Minishift add-ons
If you have already gone through the process and you would like to speed up repeated deployment then you can use Minishift [add-on feature](https://docs.openshift.org/latest/minishift/using/addons.html).
We are providing two add-ons for Minishift: `debezium` that deploys Kafka broker and Connect cluster and `tutorial-database` that deploys pre-populated and pre-configured MySQL instance.

Install add-ons
```
minishift addon install debezium
minishift addon install tutorial-database
```

Deploy Kafka broker, kafka Connect with Debezium and MySQL example database
```
minishift addon apply -a DEBEZIUM_VERSION=0.6.0 -a PROJECT=myproject debezium
minishift addon apply -a DEBEZIUM_TAG=0.6 -a DEBEZIUM_PLUGIN=mysql -a PROJECT=myproject tutorial-database
```
