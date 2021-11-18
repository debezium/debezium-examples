#!/bin/sh

if [ -z "$1" ]; then
    echo "Usage os-setup.sh <project-name>"
    exit 1
fi

PROJECT_NAME=$1
STRIMZI_VERSION="0.8.0"

wget https://github.com/strimzi/strimzi/releases/download/$STRIMZI_VERSION/strimzi-$STRIMZI_VERSION.tar.gz

rm -rf strimzi-$STRIMZI_VERSION
tar xzvf strimzi-$STRIMZI_VERSION.tar.gz
cd strimzi-$STRIMZI_VERSION
sed -i "s/namespace: .*/namespace: $PROJECT_NAME/" install/cluster-operator/*RoleBinding*.yaml
cd ..

oc login -u developer
oc new-project $PROJECT_NAME

echo "Setting up Kafka"

rm -rf strimzi-$STRIMZI_VERSION/plugins
cd strimzi-$STRIMZI_VERSION
oc login -u system:admin

oc apply -f install/cluster-operator -n $PROJECT_NAME
oc apply -f examples/templates/cluster-operator -n $PROJECT_NAME
oc process strimzi-ephemeral -p ZOOKEEPER_NODE_COUNT=1 | oc apply -f -
oc patch kafka my-cluster --type merge -p '{ "spec" : { "zookeeper" : { "resources" : { "limits" : { "memory" : "512Mi" }, "requests" : { "memory" : "512Mi" } } },  "kafka" : { "resources" : { "limits" : { "memory" : "1Gi" }, "requests" : { "memory" : "1Gi" } } } } }'

echo "Setting up DB"

oc new-app https://github.com/debezium/debezium-examples.git --strategy=docker --name=mysql --context-dir=kstreams-live-update/mysql-db \
    -e MYSQL_ROOT_PASSWORD=debezium \
    -e MYSQL_USER=mysqluser \
    -e MYSQL_PASSWORD=mysqlpw

echo "Setting up Event Source"

oc new-app --name=event-source debezium/msa-lab-s2i:latest~https://github.com/debezium/debezium-examples.git \
    --context-dir=kstreams-live-update/event-source \
    -e JAVA_MAIN_CLASS=io.debezium.examples.kstreams.liveupdate.eventsource.Main

echo "Setting up Debezium"

oc process strimzi-connect-s2i \
    -p CLUSTER_NAME=debezium \
    -p KAFKA_CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR=1 \
    -p KAFKA_CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR=1 \
    -p KAFKA_CONNECT_STATUS_STORAGE_REPLICATION_FACTOR=1 \
    -p KAFKA_CONNECT_KEY_CONVERTER_SCHEMAS_ENABLE=true \
    -p KAFKA_CONNECT_VALUE_CONVERTER_SCHEMAS_ENABLE=true \
    | oc apply -f -

export DEBEZIUM_VERSION=1.7
mkdir -p plugins && cd plugins && \
curl https://repo1.maven.org/maven2/io/debezium/debezium-connector-mysql/$DEBEZIUM_VERSION/debezium-connector-mysql-$DEBEZIUM_VERSION-plugin.tar.gz | tar xz; \
mkdir confluent-jdbc-sink && cd confluent-jdbc-sink && \
curl -O https://repo1.maven.org/maven2/org/postgresql/postgresql/42.2.2/postgresql-42.2.2.jar && \
curl -O https://packages.confluent.io/maven/io/confluent/kafka-connect-jdbc/5.0.0/kafka-connect-jdbc-5.0.0.jar && \
cd .. && \
mkdir confluent-es-sink && cd confluent-es-sink && \
curl -sO https://packages.confluent.io/maven/io/confluent/kafka-connect-elasticsearch/5.0.0/kafka-connect-elasticsearch-5.0.0.jar && \
curl -sO https://repo1.maven.org/maven2/io/searchbox/jest/2.0.0/jest-2.0.0.jar && \
curl -sO https://repo1.maven.org/maven2/org/apache/httpcomponents/httpcore-nio/4.4.4/httpcore-nio-4.4.4.jar && \
curl -sO https://repo1.maven.org/maven2/org/apache/httpcomponents/httpclient/4.5.1/httpclient-4.5.1.jar && \
curl -sO https://repo1.maven.org/maven2/org/apache/httpcomponents/httpasyncclient/4.1.1/httpasyncclient-4.1.1.jar && \
curl -sO https://repo1.maven.org/maven2/org/apache/httpcomponents/httpcore/4.4.4/httpcore-4.4.4.jar && \
curl -sO https://repo1.maven.org/maven2/commons-logging/commons-logging/1.2/commons-logging-1.2.jar && \
curl -sO https://repo1.maven.org/maven2/commons-codec/commons-codec/1.9/commons-codec-1.9.jar && \
curl -sO https://repo1.maven.org/maven2/org/apache/httpcomponents/httpcore/4.4.4/httpcore-4.4.4.jar && \
curl -sO https://repo1.maven.org/maven2/io/searchbox/jest-common/2.0.0/jest-common-2.0.0.jar && \
curl -sO https://repo1.maven.org/maven2/com/google/code/gson/gson/2.4/gson-2.4.jar && \
cd .. && \
oc start-build debezium-connect --from-dir=. --follow && \
cd ..

echo "Setting up Aggregator"

oc new-app --name=aggregator debezium/msa-lab-s2i:latest~https://github.com/debezium/debezium-examples.git \
    --context-dir=kstreams-live-update/aggregator \
    -e AB_PROMETHEUS_OFF=true \
    -e QUARKUS_KAFKA_STREAMS_BOOTSTRAP_SERVERS=my-cluster-kafka-bootstrap:9092 \
    -e JAVA_OPTIONS=-Djava.net.preferIPv4Stack=true \
    -e JAVA_APP_JAR=aggregator-runner.jar \
    -e JAVA_LIB_DIR=lib

oc set env buildconfig/aggregator ARTIFACT_COPY_ARGS='-p -r lib/ *-runner.jar'

oc patch dc/aggregator -p '[{"op": "add", "path": "/spec/template/spec/containers/0/ports/1", "value":{"containerPort":8080,"protocol":"TCP"}}]' --type=json

oc patch dc/aggregator -p '[{"op": "add", "path": "/spec/template/spec/containers/0/livenessProbe", "value":{"httpGet":{ "path" : "/health", "port" : 8080, "scheme" : "HTTP"}, "initialDelaySeconds": 10}}]' --type=json

oc patch service aggregator -p '{ "spec" : { "ports" : [{ "name" : "8080-tcp", "port" : 8080, "protocol" : "TCP", "targetPort" : 8080 }] } } }'

oc expose svc aggregator

oc get routes aggregator -o=jsonpath='{.spec.host}{"\n"}'

echo "Setting up Elasticsearch"

oc new-app -e ES_JAVA_OPTS="-Xms512m -Xmx512m" elasticsearch:6.4.2
oc expose svc/elasticsearch

cat > elasticsearch.yml << EOF
cluster.name: docker-cluster123
network.host: 0.0.0.0
discovery.zen.minimum_master_nodes: 1
discovery.type: single-node
EOF

oc create configmap es-config --from-file=elasticsearch.yml

oc set volumes dc/elasticsearch --overwrite --add \
  -t configmap \
  -m /usr/share/elasticsearch/config/elasticsearch.yml \
  --sub-path=elasticsearch.yml \
  --name=es-config \
  --configmap-name=es-config

echo "Done with set-up"
