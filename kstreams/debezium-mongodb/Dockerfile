FROM quay.io/debezium/connect:2.0
ENV KAFKA_CONNECT_MONGODB_DIR=$KAFKA_CONNECT_PLUGINS_DIR/kafka-connect-mongodb


USER root
RUN microdnf -y install git maven java-11-openjdk-devel && microdnf clean all

ENV MONGODB_DRIVER_VERSION="4.7.2"

# These should point to the https://central.sonatype.com/artifact/org.mongodb.kafka/mongo-kafka-connect version used
ENV MAVEN_DEP_DESTINATION=$KAFKA_CONNECT_MONGODB_DIR \
    MONGODB_SINK_REPO="org/mongodb/kafka" \
    MONGODB_SINK_GROUP="mongo-kafka-connect" \
    MONGODB_SINK_VERSION="1.10.0" \
    MONGODB_SINK_MD5="e7655c74c65eeb457d3e65420b62478d" \
    MONGODB_DRIVER_REPO="org/mongodb" \
    MONGODB_DRIVER_GROUP="mongodb-driver-sync" \
    MONGODB_DRIVER_MD5="3cc24cf6ff3290cbc3bc4764eefc17b9" \
    MONGODB_DRIVER_CORE_REPO="org/mongodb" \
    MONGODB_DRIVER_CORE_GROUP="mongodb-driver-core" \
    MONGODB_DRIVER_CORE_MD5="b037bee5dfb20be843d2b425603499f5" \
    AVRO_REPO="org/apache/avro" \
    AVRO_GROUP="avro" \
    AVRO_VERSION="1.9.2" \
    AVRO_MD5="cb70195f70f52b27070f9359b77690bb" \
    BSON_REPO="org/mongodb" \
    BSON_GROUP="bson" \
    BSON_VERSION="$MONGODB_DRIVER_VERSION" \
    BSON_MD5="0e02308c0d69d7d470e1b8a83cffece7"

USER kafka

RUN mkdir -p $KAFKA_CONNECT_MONGODB_DIR && \
    docker-maven-download central "$MONGODB_SINK_REPO" "$MONGODB_SINK_GROUP" "$MONGODB_SINK_VERSION" "$MONGODB_SINK_MD5" && \
    docker-maven-download central "$AVRO_REPO" "$AVRO_GROUP" "$AVRO_VERSION" "$AVRO_MD5" && \
    docker-maven-download central "$MONGODB_DRIVER_REPO" "$MONGODB_DRIVER_GROUP" "$MONGODB_DRIVER_VERSION" "$MONGODB_DRIVER_MD5" && \
    docker-maven-download central "$MONGODB_DRIVER_CORE_REPO" "$MONGODB_DRIVER_CORE_GROUP" "$MONGODB_DRIVER_VERSION" "$MONGODB_DRIVER_CORE_MD5" && \
    docker-maven-download central "$BSON_REPO" "$BSON_GROUP" "$BSON_VERSION" "$BSON_MD5"
