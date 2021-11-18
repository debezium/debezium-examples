# Prepare

(Tested with OpenShift OKD 3.11)

Run locally:

```
sudo ssh -L 8443:localhost:8443 -N -i <YOUR_KEY> user@<YOUR_HOST>
```

Run the following on the demo environment.

## Start OpenShift

```
git clone https://github.com/debezium/debezium-examples.git
cp debezium-examples/kstreams-live-update/os-setup.sh .
```

```
oc cluster up --routing-suffix=`ifconfig eth0 | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1'`.nip.io
```

```
./os-setup.sh <project-name>
```

## Start tooling container

```
oc run tooling -it --image=debezium/tooling --restart=Never
```

Verify connectors are deployed:

```
http http://debezium-connect-api:8083/connector-plugins/
```

If they are not deployed, run the S2I build again:

```
cd strimzi-0.8.0/plugins/ && oc start-build debezium-connect --from-dir=. --follow && cd ../..
```

# Demo (run in tooling pod)

* Register the connector:

Observe new orders being created:

```
mycli mysql://mysqluser@mysql:3306/inventory --password mysqlpw
```

```
cat <<'EOF' > register-mysql-source.json

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
        "table.include.list": "inventory.orders,inventory.categories",
        "database.history.kafka.bootstrap.servers": "my-cluster-kafka-bootstrap:9092",
        "database.history.kafka.topic": "schema-changes.inventory"
    }
}
EOF
cat register-mysql-source.json | http POST http://debezium-connect-api:8083/connectors/
```

* Consume:

```
kafkacat -b my-cluster-kafka-bootstrap -t dbserver1.inventory.categories -C -o beginning | jq ."payload"
kafkacat -b my-cluster-kafka-bootstrap -t dbserver1.inventory.orders -C -o end | jq ."payload"
kafkacat -b my-cluster-kafka-bootstrap -t sales_per_category -C -o beginning -K ' --- '
oc exec -it my-cluster-kafka-0 -- bin/kafka-topics.sh --zookeeper localhost:2181 --list
```

## Bonus: Push Data to Elasticsearch

```
cat <<'EOF' > register-elastic-sink.json
{
    "name": "elastic-sink",
    "config": {
        "connector.class": "io.confluent.connect.elasticsearch.ElasticsearchSinkConnector",
        "tasks.max": "1",
        "topics": "dbserver1.inventory.orders",
        "connection.url": "http://elasticsearch:9200",
        "key.ignore": "false",
        "type.name": "orders",
        "behavior.on.null.values" : "delete",
        "topic.index.map" : "dbserver1.inventory.orders:orders",
        "transforms": "unwrap,key",
        "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
        "transforms.key.type": "org.apache.kafka.connect.transforms.ExtractField$Key",
        "transforms.key.field": "id",
        "schema.ignore" : "true"
    }
}
EOF
cat register-elastic-sink.json | http POST http://debezium-connect-api:8083/connectors/
```

```
http elasticsearch:9200/orders/_search?pretty
```

# Misc.

Aggregator health check:

```
oc exec -c zookeeper my-cluster-zookeeper-0 -- curl -s -w "\n%{http_code}\n" -X GET \
    -H "Accept:application/json" \
    -H "Content-Type:application/json" \
    http://aggregator:8080/health
```

Get connector status:

```
oc exec -c kafka -i my-cluster-kafka-0 -- curl -s -w "\n" -X GET \
    -H "Accept:application/json" \
    -H "Content-Type:application/json" \
    http://debezium-connect-api:8083/connectors/mysql-source/status
```

Delete connector:

```
oc exec -c kafka -i my-cluster-kafka-0 -- curl -s -w "\n" -X DELETE \
    -H "Accept:application/json" \
    -H "Content-Type:application/json" \
    http://debezium-connect-api:8083/connectors/mysql-source
```

Logs:

```
oc logs $(oc get pods -o name -l strimzi.io/name=debezium-connect)

oc logs $(oc get pods -o name -l app=event-source)
```

Aggregated topic:

```
oc exec -c zookeeper -it my-cluster-zookeeper-0 -- /opt/kafka/bin/kafka-console-consumer.sh \
   --bootstrap-server my-cluster-kafka-bootstrap:9092 \
   --property print.key=true \
   --topic sales_per_category
```

List topics:

```
oc exec -it my-cluster-kafka-0 -- bin/kafka-topics.sh --zookeeper localhost:2181 --list
```

# Clean-Up

```
oc delete pod tooling
oc cluster down
mount | grep -o '/home/build/openshift.local.clusterup/[^ ]*' | xargs sudo umount; sudo rm -rf /home/build/openshift.local.clusterup
```

# Appendix: Using curl

```
oc exec -c zookeeper -it my-cluster-zookeeper-0 -- /opt/kafka/bin/kafka-console-consumer.sh \
   --bootstrap-server my-cluster-kafka-bootstrap:9092 \
   --from-beginning \
   --property print.key=true \
   --topic dbserver1.inventory.categories
```

```
oc exec -c zookeeper -it my-cluster-zookeeper-0 -- /opt/kafka/bin/kafka-console-consumer.sh \
   --bootstrap-server my-cluster-kafka-bootstrap:9092 \
   --property print.key=true \
   --topic dbserver1.inventory.orders
```

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
        "table.include.list": "inventory.orders,inventory.categories",
        "database.history.kafka.bootstrap.servers": "my-cluster-kafka-bootstrap:9092",
        "database.history.kafka.topic": "schema-changes.inventory"
    }
}
EOF
```

```
oc exec -c kafka -i my-cluster-kafka-0 -- curl -s -X POST -w "\n" \
    -H "Accept:application/json" \
    -H "Content-Type:application/json" \
    http://debezium-connect-api:8083/connectors -d @- <<'EOF'
{
    "name": "elastic-sink",
    "config": {
        "connector.class": "io.confluent.connect.elasticsearch.ElasticsearchSinkConnector",
        "tasks.max": "1",
        "topics": "dbserver1.inventory.orders",
        "connection.url": "http://elasticsearch:9200",
        "key.ignore": "false",
        "type.name": "orders5",
        "behavior.on.null.values" : "delete",
        "topic.index.map" : "dbserver1.inventory.orders:orders",
        "transforms": "unwrap,key",
        "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
        "transforms.key.type": "org.apache.kafka.connect.transforms.ExtractField$Key",
        "transforms.key.field": "id",
        "schema.ignore" : "true"
    }
}
EOF
```

```
oc exec -i my-cluster-kafka-0 -- curl -s -X GET "elasticsearch:9200/orders/_search?pretty"
```
