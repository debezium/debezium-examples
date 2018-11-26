# Prepare

(Locally) sudo ssh -L 8443:localhost:8443 -f -N -i <YOUR_KEY> user@<YOUR_HOST>

(Tested with OpenShift OKD 3.11)

wget https://github.com/strimzi/strimzi/releases/download/0.8.0/strimzi-0.8.0.tar.gz

rm -rf strimzi-0.8.0
tar xzvf strimzi-0.8.0.tar.gz
cd strimzi-0.8.0
sed -i 's/namespace: .*/namespace: devoxx-ma/' install/cluster-operator/*RoleBinding*.yaml
cd ..

# Start OpenShift

oc cluster up --routing-suffix=`ifconfig eth0 | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1'`.nip.io
oc login -u developer
oc new-project devoxx-ma

# Kafka

cd strimzi-0.8.0
oc login -u system:admin

oc apply -f install/cluster-operator -n devoxx-ma
oc apply -f examples/templates/cluster-operator -n devoxx-ma
oc process strimzi-ephemeral -p ZOOKEEPER_NODE_COUNT=1 | oc apply -f -
oc patch kafka my-cluster --type merge -p '{ "spec" : { "zookeeper" : { "resources" : { "limits" : { "memory" : "512Mi" }, "requests" : { "memory" : "512Mi" } } },  "kafka" : { "resources" : { "limits" : { "memory" : "1Gi" }, "requests" : { "memory" : "1Gi" } } } } }'

# DB

oc new-app https://github.com/gunnarmorling/debezium-examples.git#kstreams-demo --strategy=docker --name=mysql --context-dir=kstreams-live-update/example-db \
    -e MYSQL_ROOT_PASSWORD=debezium \
    -e MYSQL_USER=mysqluser \
    -e MYSQL_PASSWORD=mysqlpw

# Event Source

oc new-app --name=event-source debezium/msa-lab-s2i:latest~https://github.com/gunnarmorling/debezium-examples.git#kstreams-demo \
    --context-dir=kstreams-live-update/event-source \
    -e JAVA_MAIN_CLASS=io.debezium.examples.kstreams.liveupdate.eventsource.Main

# Debezium

oc process strimzi-connect-s2i \
    -p CLUSTER_NAME=debezium \
    -p KAFKA_CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR=1 \
    -p KAFKA_CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR=1 \
    -p KAFKA_CONNECT_STATUS_STORAGE_REPLICATION_FACTOR=1 \
    -p KAFKA_CONNECT_KEY_CONVERTER_SCHEMAS_ENABLE=false \
    -p KAFKA_CONNECT_VALUE_CONVERTER_SCHEMAS_ENABLE=false \
    | oc apply -f -

export DEBEZIUM_VERSION=0.8.3.Final
mkdir -p plugins && cd plugins && \
curl http://central.maven.org/maven2/io/debezium/debezium-connector-mysql/$DEBEZIUM_VERSION/debezium-connector-mysql-$DEBEZIUM_VERSION-plugin.tar.gz | tar xz; \
mkdir confluent-jdbc-sink && cd confluent-jdbc-sink && \
curl -O http://central.maven.org/maven2/org/postgresql/postgresql/42.2.2/postgresql-42.2.2.jar && \
curl -O http://packages.confluent.io/maven/io/confluent/kafka-connect-jdbc/5.0.0/kafka-connect-jdbc-5.0.0.jar && \
cd .. && \
oc start-build debezium-connect --from-dir=. --follow && \
cd ..

# Aggregator

oc new-app --name=aggregator debezium/msa-lab-s2i:latest~https://github.com/gunnarmorling/debezium-examples.git#kstreams-demo \
    --context-dir=kstreams-live-update/aggregator \
    -e AB_PROMETHEUS_OFF=true \
    -e KAFKA_BOOTSTRAP_SERVERS=my-cluster-kafka-bootstrap:9092 \
    -e JAVA_OPTIONS=-Djava.net.preferIPv4Stack=true

oc patch dc/aggregator -p '[{"op": "add", "path": "/spec/template/spec/containers/0/ports/1", "value":{"containerPort":8080,"protocol":"TCP"}}]' --type=json

oc patch dc/aggregator -p '[{"op": "add", "path": "/spec/template/spec/containers/0/livenessProbe", "value":{"httpGet":{ "path" : "/health", "port" : 8080, "scheme" : "HTTP"}, "initialDelaySeconds": 10}}]' --type=json

oc patch service aggregator -p '{ "spec" : { "ports" : [{ "name" : "8080-tcp", "port" : 8080, "protocol" : "TCP", "targetPort" : 8080 }] } } }'

oc expose svc aggregator

oc get routes aggregator -o=jsonpath='{.spec.host}{"\n"}'

# Demo

oc exec -c zookeeper my-cluster-zookeeper-0 -- curl -s -w "\n%{http_code}\n" -X GET \
    -H "Accept:application/json" \
    -H "Content-Type:application/json" \
    http://aggregator:8080/health

```
oc exec -c kafka -i my-cluster-kafka-0 -- curl -s -w "\n" -X POST \
    -H "Accept:application/json" \
    -H "Content-Type:application/json" \
    http://debezium-connect-api:8083/connectors -d @- <<'EOF'

{
    "name": "mysql-source",
    "config": {
        "connector.class": "io.debezium.connector.mysql.MySqlConnector",
        "tasks.max": "1",
        "database.hostname": "mysql",
        "database.port": "3306",
        "database.user": "debezium",
        "database.password": "dbz",
        "database.server.id": "184055",
        "database.server.name": "dbserver1",
        "decimal.handling.mode" : "string",
        "table.whitelist": "inventory.orders,inventory.categories",
        "database.history.kafka.bootstrap.servers": "my-cluster-kafka-bootstrap:9092",
        "database.history.kafka.topic": "schema-changes.inventory"
    }
}
EOF
```

oc exec -c zookeeper -it my-cluster-zookeeper-0 -- /opt/kafka/bin/kafka-console-consumer.sh \
   --bootstrap-server my-cluster-kafka-bootstrap:9092 \
   --from-beginning \
   --property print.key=true \
   --topic dbserver1.inventory.categories

oc exec -c zookeeper -it my-cluster-zookeeper-0 -- /opt/kafka/bin/kafka-console-consumer.sh \
   --bootstrap-server my-cluster-kafka-bootstrap:9092 \
   --property print.key=true \
   --topic dbserver1.inventory.orders

# Misc.

oc exec -c kafka -i my-cluster-kafka-0 -- curl -s -w "\n" -X GET \
    -H "Accept:application/json" \
    -H "Content-Type:application/json" \
    http://debezium-connect-api:8083/connectors/mysql-source/status

oc exec -c kafka -i my-cluster-kafka-0 -- curl -s -w "\n" -X DELETE \
    -H "Accept:application/json" \
    -H "Content-Type:application/json" \
    http://debezium-connect-api:8083/connectors/mysql-source

oc logs $(oc get pods -o name -l strimzi.io/name=debezium-connect)

oc logs $(oc get pods -o name -l app=event-source)

oc exec -c zookeeper -it my-cluster-zookeeper-0 -- /opt/kafka/bin/kafka-console-consumer.sh \
   --bootstrap-server my-cluster-kafka-bootstrap:9092 \
   --property print.key=true \
   --topic sales_per_category

oc exec -it my-cluster-kafka-0 -- bin/kafka-topics.sh --zookeeper localhost:2181 --list

# Clean-Up

oc cluster down
mount | grep -o '/home/build/openshift.local.clusterup/[^ ]*' | xargs sudo umount; sudo rm -rf /home/build/openshift.local.clusterup
