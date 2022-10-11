FROM quay.io/debezium/connect:2.0

ARG CAMEL_KAFKA_CONNECT_VERSION=0.1.0

RUN cd /tmp &&\
 curl -sO https://repo1.maven.org/maven2/org/apache/camel/kafkaconnector/camel-coap-kafka-connector/$CAMEL_KAFKA_CONNECT_VERSION/camel-coap-kafka-connector-$CAMEL_KAFKA_CONNECT_VERSION-package.tar.gz &&\
 tar -zxvf ./camel-coap-kafka-connector-$CAMEL_KAFKA_CONNECT_VERSION-package.tar.gz &&\
 mv ./camel-coap-kafka-connector $KAFKA_CONNECT_PLUGINS_DIR/camel-kafka-connector
