ARG DEBEZIUM_VERSION=latest
FROM quay.io/debezium/connect:${DEBEZIUM_VERSION}

ARG CAMEL_KAFKA_CONNECT_VERSION=0.11.5

RUN cd /tmp && \
    curl -sO https://repo1.maven.org/maven2/org/apache/camel/kafkaconnector/camel-coap-kafka-connector/${CAMEL_KAFKA_CONNECT_VERSION}/camel-coap-kafka-connector-${CAMEL_KAFKA_CONNECT_VERSION}-package.tar.gz && \
    tar -zxvf ./camel-coap-kafka-connector-${CAMEL_KAFKA_CONNECT_VERSION}-package.tar.gz && \
    rm -f ./camel-coap-kafka-connector/connect-*.jar ./camel-coap-kafka-connector/kafka-clients-*.jar && \
    mv ./camel-coap-kafka-connector /kafka/connect/camel-kafka-connector && \
    rm ./camel-coap-kafka-connector-${CAMEL_KAFKA_CONNECT_VERSION}-package.tar.gz
