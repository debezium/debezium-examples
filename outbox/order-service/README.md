# Order Microservice

## Prerequisites

* MySQL service
* Kafka broker service

## Deploy service to OpenShift from source

```
$ oc new-app --name=order-msa fabric8/s2i-java:latest~https://github.com/debezium/microservices-lab \
    --context-dir=ticket-msa/order \
    -e MYSQL_DATABASE=inventory \
    -e AB_PROMETHEUS_OFF=true \
    -e KAFKA_SERVICE_HOST=my-cluster-kafka-bootstrap \
    -e KAFKA_SERVICE_PORT=9092 \
    -e JAVA_OPTIONS=-Djava.net.preferIPv4Stack=true \
    -e ORDER_TOPIC_NAME=myorders

$ oc patch service order-msa -p '{ "spec" : { "ports" : [{ "name" : "8080-tcp", "port" : 8080, "protocol" : "TCP", "targetPort" : 8080 }] } } }'

$ oc expose svc order-msa

$ oc patch bc/order-msa -p '{"spec":{"strategy":{"sourceStrategy":{"incremental":true}}}}'
```

## Development workflow
Use `oc port-forward` from local machine to get connections to database and Kafka broker running in OpenShift.

Run `mvn clean thorntail:run` to test local changes - Thorntail runs on port 8180 to avoid clash with VirtualBox port-forwarding.

Run `oc start-build order-msa --from-file=target/order-thorntail.jar --wait` to test local changes inside the cluster
